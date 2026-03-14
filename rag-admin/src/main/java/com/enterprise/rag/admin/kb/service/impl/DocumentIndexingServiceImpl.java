package com.enterprise.rag.admin.kb.service.impl;

import com.enterprise.rag.admin.kb.dto.DocumentUploadResponse;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentStatus;
import com.enterprise.rag.admin.kb.service.DocumentIndexingService;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.common.async.AsyncTask;
import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskHandle;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.idempotency.Idempotent;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.processor.DocumentInput;
import com.enterprise.rag.document.processor.DocumentProcessor;
import com.enterprise.rag.document.processor.ProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文档索引应用服务实现
 * <p>
 * 修复点：
 * - DOC-01：索引编排从 Controller 迁移到此处，Controller 只做鉴权 + 参数校验
 * - DOC-02：向量写入后立即持久化 DocumentChunk 记录并回写 contentHash
 * - DOC-04：在异步任务内通过 DB 查询 contentHash 做去重校验（取代纯内存 ConcurrentHashMap）
 * - DOC-05：通过 DocumentParserFactory 校验文件类型白名单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexingServiceImpl implements DocumentIndexingService {

    private final DocumentService documentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentProcessor documentProcessor;
    private final DocumentParserFactory documentParserFactory;
    private final AsyncTaskManager asyncTaskManager;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    @Override
    @Idempotent(keyPrefix = "kb:upload", required = false, ttlSeconds = 3600)
    public DocumentUploadResponse submitIndexing(Long kbId, Long uploaderId,
            MultipartFile file, String title) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("DOC_003", "文件名不能为空");
        }

        // DOC-05: 文件类型白名单校验
        String fileType = DocumentInput.extractFileType(fileName);
        if (!documentParserFactory.isSupported(fileType)) {
            throw new BusinessException("DOC_001",
                    "不支持的文件类型: " + fileType
                            + "，支持的类型: " + documentParserFactory.getSupportedTypes());
        }

        Path tempFilePath = createTempFile(file, fileType);

        // 创建文档记录（PENDING 状态），控制器日志由调用方记录
        Document document = new Document();
        document.setKbId(kbId);
        document.setUploaderId(uploaderId);
        document.setTitle(title != null ? title : fileName);
        document.setFileType(fileType);
        document.setStatus(DocumentStatus.PENDING.name());
        document = documentService.create(document);

        final Long documentId = document.getId();

        // 提交异步索引任务
        TaskHandle<ProcessResult> taskHandle = asyncTaskManager.submit(
                "DOCUMENT_INDEX",
                uploaderId,
                progressCallback -> doIndex(kbId, documentId, fileName, fileType, tempFilePath, progressCallback));

        log.info("文档索引任务已提交: documentId={}, taskId={}", documentId, taskHandle.taskId());

        return new DocumentUploadResponse(
                documentId,
                taskHandle.taskId(),
                fileName,
                fileType,
                "PROCESSING");
    }

    /**
     * 异步索引核心逻辑（解析 → 去重 → 向量化 → 持久化）
     */
    private ProcessResult doIndex(Long kbId, Long documentId, String fileName, String fileType,
            Path tempFilePath,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback) {
        try {
            progressCallback.accept(AsyncTask.TaskProgress.of(10, "开始解析文档"));

            try (InputStream inputStream = Files.newInputStream(tempFilePath)) {
                DocumentInput input = DocumentInput.of(
                        inputStream,
                        fileName,
                        Map.of("kbId", kbId, "documentId", documentId));

                progressCallback.accept(AsyncTask.TaskProgress.of(30, "文档解析中"));
                ProcessResult result = documentProcessor.process(input);

                // DOC-04: DB 级去重校验（覆盖重启后 in-memory map 失效的场景）
                Optional<Document> existingDoc = documentService.getByKnowledgeBaseAndContentHash(kbId,
                        result.contentHash());
                if (existingDoc.isPresent() && !existingDoc.get().getId().equals(documentId)) {
                    log.info("文档内容重复，跳过索引: documentId={}, existingDocId={}",
                            documentId, existingDoc.get().getId());
                    documentService.updateStatus(documentId, DocumentStatus.COMPLETED.name());
                    progressCallback.accept(AsyncTask.TaskProgress.of(100, "文档内容已存在，跳过重复索引"));
                    return result;
                }

                List<DocumentChunk> chunks = result.chunks();
                if (chunks != null && !chunks.isEmpty()) {
                    String collectionName = knowledgeBaseService.getById(kbId)
                            .orElseThrow(() -> new BusinessException("KB_001", "知识库不存在"))
                            .getVectorCollection();

                    List<String> chunkTexts = chunks.stream().map(DocumentChunk::content).toList();

                    progressCallback.accept(AsyncTask.TaskProgress.of(50, "生成向量嵌入"));
                    List<float[]> vectors = embeddingService.embedBatch(chunkTexts);

                    // 构造向量文档列表及对应的 DB Chunk 实体
                    List<VectorDocument> vectorDocs = new ArrayList<>(chunks.size());
                    List<com.enterprise.rag.admin.kb.entity.DocumentChunk> entityChunks = new ArrayList<>(
                            chunks.size());

                    for (int i = 0; i < chunks.size(); i++) {
                        DocumentChunk chunk = chunks.get(i);
                        vectorDocs.add(new VectorDocument(
                                chunk.id(),
                                vectors.get(i),
                                chunk.content(),
                                Map.of("documentId", documentId, "kbId", kbId,
                                        "chunkIndex", i,
                                        "startIndex", chunk.startIndex(),
                                        "endIndex", chunk.endIndex())));

                        // DOC-02: 构建 DB Chunk 实体，包含 vectorId
                        com.enterprise.rag.admin.kb.entity.DocumentChunk entityChunk = new com.enterprise.rag.admin.kb.entity.DocumentChunk();
                        entityChunk.setDocumentId(documentId);
                        entityChunk.setVectorId(chunk.id());
                        entityChunk.setContent(chunk.content());
                        entityChunk.setChunkIndex(i);
                        entityChunk.setStartPos(chunk.startIndex());
                        entityChunk.setEndPos(chunk.endIndex());
                        entityChunks.add(entityChunk);
                    }

                    progressCallback.accept(AsyncTask.TaskProgress.of(70, "写入向量数据库"));
                    vectorStore.upsert(collectionName, vectorDocs);
                    log.info("成功写入 {} 个向量到集合 {}", vectorDocs.size(), collectionName);

                    // DOC-02: 持久化分块记录和 contentHash
                    progressCallback.accept(AsyncTask.TaskProgress.of(85, "持久化分块记录"));
                    documentService.saveChunks(entityChunks);
                    documentService.updateContentHash(documentId, result.contentHash());
                }

                progressCallback.accept(AsyncTask.TaskProgress.of(90, "更新文档状态"));
                documentService.updateStatus(documentId, DocumentStatus.COMPLETED.name());
                documentService.updateChunkCount(documentId, chunks == null ? 0 : chunks.size());
                knowledgeBaseService.updateDocumentCount(kbId, 1);

                progressCallback.accept(AsyncTask.TaskProgress.of(100, "文档处理完成"));
                return result;
            }

        } catch (Exception e) {
            log.error("文档处理失败: documentId={}", documentId, e);
            documentService.updateStatus(documentId, DocumentStatus.FAILED.name());
            throw new RuntimeException(e);
        } finally {
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (IOException e) {
                log.warn("清理临时上传文件失败: documentId={}, path={}, error={}", documentId, tempFilePath, e.getMessage());
            }
        }
    }

    private Path createTempFile(MultipartFile file, String fileType) {
        try {
            Path tempFilePath = Files.createTempFile("rag-upload-", "." + fileType.toLowerCase());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            return tempFilePath;
        } catch (IOException e) {
            throw new BusinessException("DOC_007", "上传文件暂存失败", e);
        }
    }
}

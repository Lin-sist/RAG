package com.enterprise.rag.admin.kb.service.impl;

import com.enterprise.rag.admin.kb.dto.DocumentUploadResponse;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentStatus;
import com.enterprise.rag.admin.kb.service.DocumentIndexingService;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.kb.storage.IndexInputState;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.admin.kb.storage.IndexInputStorageException;
import com.enterprise.rag.admin.kb.storage.StoredIndexInput;
import com.enterprise.rag.common.async.AsyncTask;
import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskHandle;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.idempotency.Idempotent;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.KeywordDocument;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.processor.DocumentInput;
import com.enterprise.rag.document.processor.DocumentProcessor;
import com.enterprise.rag.document.processor.ProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    private static final int MAX_INDEX_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_BACKOFF_MS = 500L;

    private final DocumentService documentService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentProcessor documentProcessor;
    private final DocumentParserFactory documentParserFactory;
    private final AsyncTaskManager asyncTaskManager;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final KeywordIndex keywordIndex;
    private final IndexInputStore indexInputStore;

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

        StoredIndexInput storedInput;
        try {
            storedInput = indexInputStore.put(file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException("DOC_007", "上传文件读取失败", e);
        }

        // 创建文档记录（PENDING 状态），控制器日志由调用方记录
        Document document = new Document();
        document.setKbId(kbId);
        document.setUploaderId(uploaderId);
        document.setTitle(title != null ? title : fileName);
        document.setFileType(fileType);
        document.setFilePath(storedInput.storageKey());
        document.setInputSizeBytes(storedInput.sizeBytes());
        document.setInputSha256(storedInput.sha256());
        document.setInputState(IndexInputState.AVAILABLE.name());
        document.setStatus(DocumentStatus.PENDING.name());
        try {
            document = documentService.create(document);
        } catch (RuntimeException e) {
            cleanupUnacceptedInput(storedInput.storageKey());
            throw e;
        }

        final Long documentId = document.getId();
        final String documentTitle = document.getTitle();

        // 提交异步索引任务
        TaskHandle<ProcessResult> taskHandle;
        try {
            taskHandle = asyncTaskManager.submit(
                    "DOCUMENT_INDEX",
                    uploaderId,
                    progressCallback -> doIndex(
                            kbId, documentId, fileName, documentTitle, fileType,
                            storedInput.storageKey(), storedInput.sizeBytes(), storedInput.sha256(),
                            progressCallback));
        } catch (RuntimeException e) {
            documentService.updateStatus(documentId, DocumentStatus.FAILED.name());
            cleanupCompletedInput(documentId, storedInput.storageKey());
            throw e;
        }

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
    private ProcessResult doIndex(Long kbId, Long documentId, String fileName, String documentTitle, String fileType,
            String storageKey,
            long inputSizeBytes,
            String inputSha256,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback) {
        try {
            progressCallback.accept(AsyncTask.TaskProgress.of(10, "开始解析文档"));

            try (InputStream inputStream = indexInputStore.openVerified(storageKey, inputSizeBytes, inputSha256)) {
                DocumentInput input = DocumentInput.of(
                        inputStream,
                        fileName,
                        Map.of("kbId", kbId, "documentId", documentId,
                                "sourceFileName", fileName,
                                "originalFilename", fileName,
                                "fileName", fileName,
                                "documentTitle", documentTitle,
                                "title", documentTitle));

                progressCallback.accept(AsyncTask.TaskProgress.of(30, "文档解析中"));
                ProcessResult result = documentProcessor.process(input);

                // DOC-04: DB 级去重校验（覆盖重启后 in-memory map 失效的场景）
                Optional<Document> existingDoc = documentService.getByKnowledgeBaseAndContentHash(kbId,
                        result.contentHash());
                if (existingDoc.isPresent() && !existingDoc.get().getId().equals(documentId)) {
                    Document existing = existingDoc.get();
                    if (isIndexReady(existing)) {
                        log.info("文档内容重复，复用已有索引: documentId={}, existingDocId={}",
                                documentId, existing.getId());
                        documentService.updateStatus(documentId, DocumentStatus.COMPLETED.name());
                        documentService.updateChunkCount(documentId, existing.getChunkCount());
                        documentService.updateContentHash(documentId, result.contentHash());
                        progressCallback.accept(AsyncTask.TaskProgress.of(100, "文档内容已存在，复用已有索引"));
                        cleanupCompletedInput(documentId, storageKey);
                        return result;
                    }
                    log.warn(
                            "检测到重复内容，但已有文档索引不完整，将继续重建索引: documentId={}, existingDocId={}, existingStatus={}, existingChunkCount={}",
                            documentId,
                            existing.getId(),
                            existing.getStatus(),
                            existing.getChunkCount());
                }

                List<DocumentChunk> chunks = result.chunks();
                if (chunks == null || chunks.isEmpty()) {
                    throw new BusinessException("DOC_010", "文档解析未产生有效分块，无法建立索引");
                }

                String collectionName = knowledgeBaseService.getById(kbId)
                        .orElseThrow(() -> new BusinessException("KB_001", "知识库不存在"))
                        .getVectorCollection();

                ProcessResult indexed = indexWithRetry(
                        kbId, documentId, fileName, documentTitle, result, chunks, collectionName,
                        progressCallback);
                cleanupCompletedInput(documentId, storageKey);
                return indexed;
            }

        } catch (Exception e) {
            log.error("文档处理失败: documentId={}, errorType={}",
                    documentId, e.getClass().getSimpleName());
            if (e instanceof IndexInputStorageException storageException) {
                String inputState = "INDEX_INPUT_CORRUPT".equals(storageException.getErrorCode())
                        ? IndexInputState.CORRUPT.name()
                        : IndexInputState.MISSING.name();
                documentService.updateInputState(documentId, inputState);
            }
            documentService.updateStatus(documentId, DocumentStatus.FAILED.name());
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        }
    }

    private ProcessResult indexWithRetry(Long kbId,
            Long documentId,
            String fileName,
            String documentTitle,
            ProcessResult result,
            List<DocumentChunk> chunks,
            String collectionName,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= MAX_INDEX_ATTEMPTS; attempt++) {
            try {
                if (attempt > 1) {
                    progressCallback.accept(AsyncTask.TaskProgress.of(45, "文档入库重试中，第 " + attempt + " 次"));
                }
                return indexOnce(kbId, documentId, fileName, documentTitle, result, chunks, collectionName,
                        progressCallback);
            } catch (VectorDependencyException e) {
                throw e;
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt >= MAX_INDEX_ATTEMPTS) {
                    break;
                }
                long backoffMs = INITIAL_RETRY_BACKOFF_MS * (1L << (attempt - 1));
                log.warn("文档入库失败，准备重试: documentId={}, attempt={}/{}, backoffMs={}, errorType={}",
                        documentId, attempt, MAX_INDEX_ATTEMPTS, backoffMs,
                        e.getClass().getSimpleName());
                sleepBeforeRetry(documentId, backoffMs);
            }
        }

        throw lastException == null ? new RuntimeException("文档入库失败") : lastException;
    }

    private ProcessResult indexOnce(Long kbId,
            Long documentId,
            String fileName,
            String documentTitle,
            ProcessResult result,
            List<DocumentChunk> chunks,
            String collectionName,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback) {
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
                    buildVectorMetadata(kbId, documentId, fileName, documentTitle, i, chunk)));

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
        saveChunksIfAbsent(documentId, entityChunks);
        documentService.updateContentHash(documentId, result.contentHash());
        upsertKeywordIndex(collectionName, vectorDocs);

        progressCallback.accept(AsyncTask.TaskProgress.of(90, "更新文档状态"));
        documentService.updateStatus(documentId, DocumentStatus.COMPLETED.name());
        documentService.updateChunkCount(documentId, chunks.size());
        knowledgeBaseService.updateDocumentCount(kbId, 1);

        progressCallback.accept(AsyncTask.TaskProgress.of(100, "文档处理完成"));
        return result;
    }

    private void saveChunksIfAbsent(Long documentId, List<com.enterprise.rag.admin.kb.entity.DocumentChunk> entityChunks) {
        if (!documentService.getChunksByDocumentId(documentId).isEmpty()) {
            log.info("文档分块已存在，跳过重复持久化: documentId={}", documentId);
            return;
        }
        documentService.saveChunks(entityChunks);
    }

    private void sleepBeforeRetry(Long documentId, long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("文档入库重试等待被中断: documentId=" + documentId, e);
        }
    }

    private void upsertKeywordIndex(String collectionName, List<VectorDocument> vectorDocs) {
        try {
            List<KeywordDocument> keywordDocuments = vectorDocs.stream()
                    .map(doc -> new KeywordDocument(doc.id(), doc.content(), compactMetadata(doc.metadata())))
                    .toList();
            keywordIndex.upsert(collectionName, keywordDocuments);
        } catch (Exception e) {
            log.warn("关键词索引写入失败，保留向量主链路: collection={}, errorType={}",
                    collectionName, e.getClass().getSimpleName());
        }
    }

    private Map<String, Object> compactMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> compacted = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                compacted.put(key, value);
            }
        });
        return compacted;
    }

    private boolean isIndexReady(Document document) {
        if (document == null) {
            return false;
        }
        Integer chunkCount = document.getChunkCount();
        return DocumentStatus.COMPLETED.name().equalsIgnoreCase(document.getStatus())
                && chunkCount != null
                && chunkCount > 0;
    }

    private Map<String, Object> buildVectorMetadata(Long kbId, Long documentId, String fileName, String documentTitle,
            int chunkIndex, DocumentChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>(chunk.metadata());
        metadata.put("documentId", documentId);
        metadata.put("kbId", kbId);
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("startIndex", chunk.startIndex());
        metadata.put("endIndex", chunk.endIndex());
        metadata.put("sourceFileName", fileName);
        metadata.put("originalFilename", fileName);
        metadata.put("fileName", fileName);
        metadata.put("documentTitle", documentTitle);
        metadata.put("title", documentTitle);
        metadata.putIfAbsent("headingPath", "");
        metadata.putIfAbsent("headingLevel", null);
        metadata.putIfAbsent("tokenCount", estimateTokenCount(chunk.content()));
        return metadata;
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length();
    }

    private void cleanupCompletedInput(Long documentId, String storageKey) {
        try {
            documentService.updateInputState(documentId, IndexInputState.CLEANUP_PENDING.name());
        } catch (RuntimeException e) {
            log.warn("索引输入清理状态待协调: documentId={}, errorType={}",
                    documentId, e.getClass().getSimpleName());
            return;
        }
        try {
            IndexInputStore.DeleteResult deleteResult = indexInputStore.delete(storageKey);
            if (deleteResult != IndexInputStore.DeleteResult.DELETED
                    && deleteResult != IndexInputStore.DeleteResult.ALREADY_MISSING) {
                log.warn("索引输入等待后续清理: documentId={}, result={}",
                        documentId, IndexInputStore.DeleteResult.FAILED);
                return;
            }
        } catch (RuntimeException e) {
            log.warn("索引输入等待后续清理: documentId={}, errorType={}",
                    documentId, e.getClass().getSimpleName());
            return;
        }
        try {
            documentService.updateInputState(documentId, IndexInputState.CLEANED.name());
        } catch (RuntimeException e) {
            log.warn("索引输入已清理但状态待协调: documentId={}, errorType={}",
                    documentId, e.getClass().getSimpleName());
        }
    }

    private void cleanupUnacceptedInput(String storageKey) {
        try {
            indexInputStore.delete(storageKey);
        } catch (RuntimeException e) {
            log.warn("未接受索引输入清理失败: errorType={}", e.getClass().getSimpleName());
        }
    }
}

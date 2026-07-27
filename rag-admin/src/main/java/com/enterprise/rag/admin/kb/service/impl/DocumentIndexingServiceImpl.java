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
import com.enterprise.rag.admin.kb.task.IndexTaskLedger;
import com.enterprise.rag.admin.kb.task.DeterministicChunkIdentity;
import com.enterprise.rag.admin.kb.task.IndexTaskRecord;
import com.enterprise.rag.admin.kb.task.IndexTaskPhase;
import com.enterprise.rag.admin.kb.task.IndexTaskLeaseGuard;
import com.enterprise.rag.admin.kb.task.IndexTaskSqlFinalizer;
import com.enterprise.rag.common.async.AsyncTask;
import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskHandle;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.idempotency.Idempotent;
import com.enterprise.rag.common.trace.GenAiTelemetry;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.KeywordDocument;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.chunker.DocumentChunkingProperties;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.processor.DocumentInput;
import com.enterprise.rag.document.processor.DocumentProcessor;
import com.enterprise.rag.document.processor.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final IndexTaskLedger indexTaskLedger;
    private final IndexTaskSqlFinalizer sqlFinalizer;
    private final DocumentChunkingProperties chunkingProperties;
    private final GenAiTelemetry telemetry;

    @Autowired
    public DocumentIndexingServiceImpl(DocumentService documentService,
            KnowledgeBaseService knowledgeBaseService,
            DocumentProcessor documentProcessor,
            DocumentParserFactory documentParserFactory,
            AsyncTaskManager asyncTaskManager,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            KeywordIndex keywordIndex,
            IndexInputStore indexInputStore,
            IndexTaskLedger indexTaskLedger,
            IndexTaskSqlFinalizer sqlFinalizer,
            DocumentChunkingProperties chunkingProperties,
            GenAiTelemetry telemetry) {
        this.documentService = documentService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentProcessor = documentProcessor;
        this.documentParserFactory = documentParserFactory;
        this.asyncTaskManager = asyncTaskManager;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.keywordIndex = keywordIndex;
        this.indexInputStore = indexInputStore;
        this.indexTaskLedger = indexTaskLedger;
        this.sqlFinalizer = sqlFinalizer;
        this.chunkingProperties = chunkingProperties;
        this.telemetry = telemetry == null ? GenAiTelemetry.noop() : telemetry;
    }

    public DocumentIndexingServiceImpl(DocumentService documentService,
            KnowledgeBaseService knowledgeBaseService,
            DocumentProcessor documentProcessor,
            DocumentParserFactory documentParserFactory,
            AsyncTaskManager asyncTaskManager,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            KeywordIndex keywordIndex,
            IndexInputStore indexInputStore,
            IndexTaskLedger indexTaskLedger,
            IndexTaskSqlFinalizer sqlFinalizer,
            DocumentChunkingProperties chunkingProperties) {
        this(documentService, knowledgeBaseService, documentProcessor, documentParserFactory,
                asyncTaskManager, embeddingService, vectorStore, keywordIndex, indexInputStore,
                indexTaskLedger, sqlFinalizer, chunkingProperties, GenAiTelemetry.noop());
    }

    @Override
    @Idempotent(keyPrefix = "kb:upload", required = false, ttlSeconds = 3600)
    public DocumentUploadResponse submitIndexing(long tenantId, Long kbId, Long uploaderId,
            MultipartFile file, String title) {
        requireTenantId(tenantId);
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
            storedInput = indexInputStore.put(tenantId, file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException("DOC_007", "上传文件读取失败", e);
        }

        // 创建文档记录（PENDING 状态），控制器日志由调用方记录
        Document document = new Document();
        document.setTenantId(tenantId);
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
            cleanupUnacceptedInput(tenantId, storedInput.storageKey());
            throw e;
        }

        final Long documentId = document.getId();
        final String documentTitle = document.getTitle();

        // durable ledger 必须先于 Redis projection 与实际调度。
        TaskHandle<ProcessResult> taskHandle;
        String acceptedTaskId = null;
        try {
            String taskId = indexTaskLedger.createAccepted(tenantId, documentId, uploaderId);
            acceptedTaskId = taskId;
            io.opentelemetry.context.Context submissionContext = telemetry.captureContext();
            taskHandle = asyncTaskManager.submit(
                    tenantId,
                    taskId,
                    "DOCUMENT_INDEX",
                    uploaderId,
                    progressCallback -> runFreshIngest(
                            submissionContext, tenantId, taskId, kbId, documentId, fileName, documentTitle, fileType,
                            storedInput.storageKey(), storedInput.sizeBytes(), storedInput.sha256(),
                            progressCallback, () -> {
                            }));
        } catch (RuntimeException e) {
            if (acceptedTaskId != null) {
                try {
                    indexTaskLedger.markAcceptanceFailed(
                            tenantId, acceptedTaskId, "TASK_PROJECTION_FAILED");
                } catch (RuntimeException ledgerFailure) {
                    log.error("索引任务接受失败状态待协调: documentId={}, taskId={}, errorType={}",
                            documentId, acceptedTaskId, ledgerFailure.getClass().getSimpleName());
                }
            }
            documentService.updateStatus(tenantId, documentId, DocumentStatus.FAILED.name());
            cleanupCompletedInput(tenantId, documentId, storedInput.storageKey());
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

    private ProcessResult runFreshIngest(io.opentelemetry.context.Context submissionContext,
            long tenantId, String taskId, Long kbId, Long documentId, String fileName, String documentTitle,
            String fileType, String storageKey, long inputSizeBytes, String inputSha256,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback,
            IndexTaskLeaseGuard leaseGuard) {
        try (GenAiTelemetry.SpanScope ingest = telemetry.startRoot(
                GenAiTelemetry.SpanNames.INGEST,
                Map.of(
                        GenAiTelemetry.Attributes.OPERATION, "ingest",
                        GenAiTelemetry.Attributes.TASK_ID, taskId,
                        GenAiTelemetry.Attributes.DOCUMENT_ID, documentId,
                        GenAiTelemetry.Attributes.RESUME, false,
                        GenAiTelemetry.Attributes.INGEST_PHASE, "EXECUTION"),
                submissionContext)) {
            try {
                ProcessResult result = doIndex(tenantId, taskId, kbId, documentId, fileName, documentTitle, fileType,
                        storageKey, inputSizeBytes, inputSha256, progressCallback, leaseGuard);
                ingest.longFact(GenAiTelemetry.Attributes.INGEST_CHUNK_COUNT,
                                result.chunks() == null ? 0L : result.chunks().size())
                        .outcome("SUCCESS");
                return result;
            } catch (RuntimeException failure) {
                ingest.safeError(failure, "ingest", "INGEST_FAILED").outcome("ERROR");
                throw failure;
            }
        }
    }

    /**
     * 恢复已确认完成 vector mutation 的任务，只执行可证明幂等的收尾。
     */
    public void resumeIndexTask(IndexTaskRecord task) {
        resumeIndexTask(task, () -> {
        });
    }

    public void resumeIndexTask(IndexTaskRecord task, IndexTaskLeaseGuard leaseGuard) {
        try (GenAiTelemetry.SpanScope ingest = telemetry.startRoot(
                GenAiTelemetry.SpanNames.INGEST,
                Map.of(
                        GenAiTelemetry.Attributes.OPERATION, "ingest",
                        GenAiTelemetry.Attributes.TASK_ID, task.getTaskId(),
                        GenAiTelemetry.Attributes.DOCUMENT_ID, task.getDocumentId(),
                        GenAiTelemetry.Attributes.RESUME, true,
                        GenAiTelemetry.Attributes.INGEST_PHASE, task.getExecutionPhase()),
                null)) {
            try {
                resumeIndexTaskWithinTrace(task, leaseGuard);
                ingest.longFact(GenAiTelemetry.Attributes.INGEST_CHUNK_COUNT,
                                task.getPreparedChunkCount() == null ? 0L : task.getPreparedChunkCount())
                        .outcome("SUCCESS");
            } catch (RuntimeException failure) {
                ingest.safeError(failure, "ingest", "RESUME_FAILED").outcome("ERROR");
                throw failure;
            }
        }
    }

    private void resumeIndexTaskWithinTrace(IndexTaskRecord task, IndexTaskLeaseGuard leaseGuard) {
        long tenantId = requireTaskTenantId(task);
        validateResumeContract(tenantId, task);
        IndexTaskPhase phase = IndexTaskPhase.valueOf(task.getExecutionPhase());
        if (phase != IndexTaskPhase.ACCEPTED
                && phase != IndexTaskPhase.SAFE_PRE_VECTOR
                && phase != IndexTaskPhase.VECTOR_CONFIRMED
                && phase != IndexTaskPhase.FINALIZING) {
            throw new IllegalArgumentException("Unsupported safe resume phase: " + phase);
        }

        Document document = documentService.getById(tenantId, task.getDocumentId())
                .orElseThrow(() -> new BusinessException("DOC_002", "文档不存在"));
        if (!Long.valueOf(tenantId).equals(document.getTenantId())
                || !java.util.Objects.equals(task.getDocumentId(), document.getId())
                || !java.util.Objects.equals(task.getOwnerId(), document.getUploaderId())
                || document.getKbId() == null) {
            indexTaskLedger.markReconciliationRequired(
                    tenantId, task.getTaskId(), "TASK_SCOPE_MISMATCH");
            throw new IllegalStateException("Index task scope does not match document facts");
        }
        String fileName = "document-" + document.getId() + "." + document.getFileType();
        if (phase == IndexTaskPhase.ACCEPTED || phase == IndexTaskPhase.SAFE_PRE_VECTOR) {
            doIndex(tenantId, task.getTaskId(), document.getKbId(), document.getId(), fileName, document.getTitle(),
                    document.getFileType(), document.getFilePath(), document.getInputSizeBytes(),
                    document.getInputSha256(), ignored -> {
                    }, leaseGuard);
            return;
        }
        InputStream verifiedInput = traceStage(
                GenAiTelemetry.SpanNames.INGEST_INPUT_OPEN,
                () -> indexInputStore.openVerified(
                        tenantId,
                        document.getFilePath(),
                        document.getInputSizeBytes(),
                        document.getInputSha256()));
        try (InputStream inputStream = verifiedInput) {
            DocumentInput input = DocumentInput.of(
                    inputStream,
                    fileName,
                    Map.of("kbId", document.getKbId(), "documentId", document.getId(),
                            "sourceFileName", fileName,
                            "documentTitle", document.getTitle(),
                            "title", document.getTitle()));
            ProcessResult result = traceStage(
                    GenAiTelemetry.SpanNames.INGEST_PARSE_CHUNK,
                    () -> documentProcessor.process(input));
            if (!result.contentHash().equals(task.getPreparedContentHash())
                    || result.chunks().size() != task.getPreparedChunkCount()) {
                indexTaskLedger.markReconciliationRequired(
                        tenantId, task.getTaskId(), "PREPARED_FACTS_MISMATCH");
                throw new IllegalStateException("Prepared index facts do not match durable checkpoint");
            }
            List<DocumentChunk> chunks = DeterministicChunkIdentity.remap(
                    task.getIndexContractVersion(), document.getId(), result.contentHash(), result.chunks());
            String collectionName = knowledgeBaseService
                    .requireReadyVectorScope(tenantId, document.getKbId()).collectionName();
            PreparedIndex prepared = prepareFinalization(
                    task.getTaskId(), document.getKbId(), document.getId(), fileName, document.getTitle(), chunks);
            leaseGuard.assertOwned();
            finalizeIndex(tenantId, task.getTaskId(), document.getKbId(), document.getId(), result, chunks,
                    collectionName, prepared, ignored -> {
                    });
            cleanupCompletedInput(tenantId, document.getId(), document.getFilePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to close durable index input", e);
        }
    }

    private void validateResumeContract(long tenantId, IndexTaskRecord task) {
        boolean matches = DeterministicChunkIdentity.CONTRACT_VERSION.equals(task.getIndexContractVersion())
                && Integer.valueOf(chunkingProperties.getChunkSize()).equals(task.getChunkSize())
                && Integer.valueOf(chunkingProperties.getChunkOverlap()).equals(task.getChunkOverlap());
        if (!matches) {
            indexTaskLedger.markReconciliationRequired(
                    tenantId, task.getTaskId(), "INDEX_CONTRACT_MISMATCH");
            throw new IllegalStateException("Index task contract does not match current runtime");
        }
    }

    /**
     * 异步索引核心逻辑（解析 → 去重 → 向量化 → 持久化）
     */
    private ProcessResult doIndex(long tenantId, String taskId, Long kbId, Long documentId,
            String fileName, String documentTitle,
            String fileType,
            String storageKey,
            long inputSizeBytes,
            String inputSha256,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback,
            IndexTaskLeaseGuard leaseGuard) {
        try {
            indexTaskLedger.markSafePreVector(tenantId, taskId);
            progressCallback.accept(AsyncTask.TaskProgress.of(10, "开始解析文档"));

            InputStream verifiedInput = traceStage(
                    GenAiTelemetry.SpanNames.INGEST_INPUT_OPEN,
                    () -> indexInputStore.openVerified(tenantId, storageKey, inputSizeBytes, inputSha256));
            try (InputStream inputStream = verifiedInput) {
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
                ProcessResult result = traceStage(
                        GenAiTelemetry.SpanNames.INGEST_PARSE_CHUNK,
                        () -> documentProcessor.process(input));

                // DOC-04: DB 级去重校验（覆盖重启后 in-memory map 失效的场景）
                Optional<Document> existingDoc = documentService.getByKnowledgeBaseAndContentHash(
                        tenantId, kbId, result.contentHash());
                if (existingDoc.isPresent() && !existingDoc.get().getId().equals(documentId)) {
                    Document existing = existingDoc.get();
                    if (isIndexReady(existing)) {
                        log.info("文档内容重复，复用已有索引: documentId={}, existingDocId={}",
                                documentId, existing.getId());
                        documentService.updateStatus(
                                tenantId, documentId, DocumentStatus.COMPLETED.name());
                        documentService.updateChunkCount(
                                tenantId, documentId, existing.getChunkCount());
                        documentService.updateContentHash(
                                tenantId, documentId, result.contentHash());
                        progressCallback.accept(AsyncTask.TaskProgress.of(100, "文档内容已存在，复用已有索引"));
                        cleanupCompletedInput(tenantId, documentId, storageKey);
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
                chunks = DeterministicChunkIdentity.remap(
                        DeterministicChunkIdentity.CONTRACT_VERSION,
                        documentId,
                        result.contentHash(),
                        chunks);

                String collectionName = knowledgeBaseService
                        .requireReadyVectorScope(tenantId, kbId).collectionName();

                ProcessResult indexed = indexWithRetry(
                        tenantId, taskId, kbId, documentId, fileName, documentTitle, result, chunks, collectionName,
                        progressCallback, leaseGuard);
                cleanupCompletedInput(tenantId, documentId, storageKey);
                return indexed;
            }

        } catch (Exception e) {
            log.error("文档处理失败: documentId={}, errorType={}",
                    documentId, e.getClass().getSimpleName());
            if (e instanceof IndexInputStorageException storageException) {
                String inputState = "INDEX_INPUT_CORRUPT".equals(storageException.getErrorCode())
                        ? IndexInputState.CORRUPT.name()
                        : IndexInputState.MISSING.name();
                documentService.updateInputState(tenantId, documentId, inputState);
            }
            if (e instanceof VectorDependencyException vectorException
                    && VectorDependencyException.ERROR_CODE_OUTCOME_UNKNOWN.equals(vectorException.getErrorCode())) {
                try {
                    indexTaskLedger.markReconciliationRequired(
                            tenantId, taskId, vectorException.getErrorCode());
                } catch (RuntimeException ledgerFailure) {
                    log.error("向量结果未知任务隔离失败: documentId={}, taskId={}, errorType={}",
                            documentId, taskId, ledgerFailure.getClass().getSimpleName());
                }
            }
            documentService.updateStatus(tenantId, documentId, DocumentStatus.FAILED.name());
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e);
        }
    }

    private ProcessResult indexWithRetry(long tenantId, String taskId,
            Long kbId,
            Long documentId,
            String fileName,
            String documentTitle,
            ProcessResult result,
            List<DocumentChunk> chunks,
            String collectionName,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback,
            IndexTaskLeaseGuard leaseGuard) {
        RuntimeException lastException = null;
        leaseGuard.assertOwned();
        PreparedIndex preparedIndex = prepareIndex(
                tenantId, taskId, kbId, documentId, fileName, documentTitle, chunks, progressCallback);
        boolean vectorConfirmed = false;

        for (int attempt = 1; attempt <= MAX_INDEX_ATTEMPTS; attempt++) {
            try {
                if (attempt > 1) {
                    progressCallback.accept(AsyncTask.TaskProgress.of(45, "文档入库重试中，第 " + attempt + " 次"));
                }
                if (!vectorConfirmed) {
                    writeVectorOnce(tenantId, kbId, taskId, result, chunks,
                            collectionName, preparedIndex.vectorDocuments(),
                            progressCallback, leaseGuard);
                    vectorConfirmed = true;
                }
                return finalizeIndex(tenantId, taskId, kbId, documentId, result, chunks, collectionName,
                        preparedIndex, progressCallback);
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

    private PreparedIndex prepareIndex(long tenantId,
            String taskId,
            Long kbId,
            Long documentId,
            String fileName,
            String documentTitle,
            List<DocumentChunk> chunks,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback) {
        List<String> chunkTexts = chunks.stream().map(DocumentChunk::content).toList();

        progressCallback.accept(AsyncTask.TaskProgress.of(50, "生成向量嵌入"));
        List<float[]> vectors = traceStage(
                GenAiTelemetry.SpanNames.DOCUMENT_EMBEDDING,
                () -> embeddingService.embedBatch(tenantId, chunkTexts));

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
                    buildVectorMetadata(taskId, kbId, documentId, fileName, documentTitle, i, chunk)));

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

        return new PreparedIndex(vectorDocs, entityChunks);
    }

    private PreparedIndex prepareFinalization(String taskId,
            Long kbId,
            Long documentId,
            String fileName,
            String documentTitle,
            List<DocumentChunk> chunks) {
        List<VectorDocument> vectorDocs = new ArrayList<>(chunks.size());
        List<com.enterprise.rag.admin.kb.entity.DocumentChunk> entityChunks = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            vectorDocs.add(new VectorDocument(
                    chunk.id(), new float[0], chunk.content(),
                    buildVectorMetadata(taskId, kbId, documentId, fileName, documentTitle, i, chunk)));
            com.enterprise.rag.admin.kb.entity.DocumentChunk entityChunk =
                    new com.enterprise.rag.admin.kb.entity.DocumentChunk();
            entityChunk.setDocumentId(documentId);
            entityChunk.setVectorId(chunk.id());
            entityChunk.setContent(chunk.content());
            entityChunk.setChunkIndex(i);
            entityChunk.setStartPos(chunk.startIndex());
            entityChunk.setEndPos(chunk.endIndex());
            entityChunks.add(entityChunk);
        }
        return new PreparedIndex(vectorDocs, entityChunks);
    }

    private void writeVectorOnce(long tenantId, long kbId, String taskId,
            ProcessResult result,
            List<DocumentChunk> chunks,
            String collectionName,
            List<VectorDocument> vectorDocs,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback,
            IndexTaskLeaseGuard leaseGuard) {
        progressCallback.accept(AsyncTask.TaskProgress.of(70, "写入向量数据库"));
        leaseGuard.assertOwned();
        indexTaskLedger.markVectorInFlight(tenantId, taskId, result.contentHash(), chunks.size());
        leaseGuard.assertOwned();
        traceStage(GenAiTelemetry.SpanNames.VECTOR_UPSERT,
                () -> vectorStore.upsert(
                        new TenantVectorScope(tenantId, kbId, collectionName), vectorDocs));
        log.info("成功写入 {} 个向量到集合 {}", vectorDocs.size(), collectionName);
        try {
            indexTaskLedger.markVectorConfirmed(tenantId, taskId);
        } catch (RuntimeException e) {
            throw VectorDependencyException.outcomeUnknown("checkpoint_after_upsert", e);
        }
    }

    private ProcessResult finalizeIndex(long tenantId, String taskId,
            Long kbId,
            Long documentId,
            ProcessResult result,
            List<DocumentChunk> chunks,
            String collectionName,
            PreparedIndex preparedIndex,
            java.util.function.Consumer<AsyncTask.TaskProgress> progressCallback) {
        indexTaskLedger.markFinalizing(tenantId, taskId);
        progressCallback.accept(AsyncTask.TaskProgress.of(85, "持久化分块记录"));
        traceStage(GenAiTelemetry.SpanNames.KEYWORD_UPSERT,
                () -> upsertKeywordIndex(
                        new TenantVectorScope(tenantId, kbId, collectionName),
                        preparedIndex.vectorDocuments()));

        progressCallback.accept(AsyncTask.TaskProgress.of(90, "更新文档状态"));
        traceStage(GenAiTelemetry.SpanNames.INDEX_FINALIZE,
                () -> sqlFinalizer.finalizeSql(tenantId, taskId, kbId, documentId, result.contentHash(),
                        preparedIndex.entityChunks()));

        progressCallback.accept(AsyncTask.TaskProgress.of(100, "文档处理完成"));
        return result;
    }

    private record PreparedIndex(
            List<VectorDocument> vectorDocuments,
            List<com.enterprise.rag.admin.kb.entity.DocumentChunk> entityChunks) {
    }

    private long requireTaskTenantId(IndexTaskRecord task) {
        Long tenantId = task.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
        }
        return requireTenantId(tenantId);
    }

    private long requireTenantId(long tenantId) {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        return tenantId;
    }

    private <T> T traceStage(String spanName, java.util.function.Supplier<T> action) {
        try (GenAiTelemetry.SpanScope stage = telemetry.startSpan(spanName, Map.of())) {
            try {
                T result = action.get();
                stage.outcome("SUCCESS");
                return result;
            } catch (RuntimeException failure) {
                stage.safeError(failure, "ingest", "STAGE_FAILED").outcome("ERROR");
                throw failure;
            }
        }
    }

    private void traceStage(String spanName, Runnable action) {
        traceStage(spanName, () -> {
            action.run();
            return null;
        });
    }

    private void sleepBeforeRetry(Long documentId, long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("文档入库重试等待被中断: documentId=" + documentId, e);
        }
    }

    private void upsertKeywordIndex(TenantVectorScope scope, List<VectorDocument> vectorDocs) {
        try {
            List<KeywordDocument> keywordDocuments = vectorDocs.stream()
                    .map(doc -> new KeywordDocument(doc.id(), doc.content(), compactMetadata(doc.metadata())))
                    .toList();
            keywordIndex.upsert(scope, keywordDocuments);
        } catch (Exception e) {
            log.warn("关键词索引写入失败，保留向量主链路: collection={}, errorType={}",
                    scope.collectionName(), e.getClass().getSimpleName());
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

    private Map<String, Object> buildVectorMetadata(String taskId, Long kbId, Long documentId,
            String fileName, String documentTitle,
            int chunkIndex, DocumentChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>(chunk.metadata());
        metadata.put("ingestTaskId", taskId);
        metadata.put("documentId", documentId);
        metadata.put("chunkId", chunk.id());
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

    @SuppressWarnings("unused")
    private Map<String, Object> buildVectorMetadata(Long kbId, Long documentId,
            String fileName, String documentTitle, int chunkIndex, DocumentChunk chunk) {
        return buildVectorMetadata(null, kbId, documentId, fileName, documentTitle, chunkIndex, chunk);
    }

    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length();
    }

    private void cleanupCompletedInput(long tenantId, Long documentId, String storageKey) {
        try {
            documentService.updateInputState(
                    tenantId, documentId, IndexInputState.CLEANUP_PENDING.name());
        } catch (RuntimeException e) {
            log.warn("索引输入清理状态待协调: documentId={}, errorType={}",
                    documentId, e.getClass().getSimpleName());
            return;
        }
        try {
            IndexInputStore.DeleteResult deleteResult = indexInputStore.delete(tenantId, storageKey);
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
            documentService.updateInputState(tenantId, documentId, IndexInputState.CLEANED.name());
        } catch (RuntimeException e) {
            log.warn("索引输入已清理但状态待协调: documentId={}, errorType={}",
                    documentId, e.getClass().getSimpleName());
        }
    }

    private void cleanupUnacceptedInput(long tenantId, String storageKey) {
        try {
            indexInputStore.delete(tenantId, storageKey);
        } catch (RuntimeException e) {
            log.warn("未接受索引输入清理失败: errorType={}", e.getClass().getSimpleName());
        }
    }
}

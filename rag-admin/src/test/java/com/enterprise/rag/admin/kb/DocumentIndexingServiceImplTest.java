package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.dto.DocumentUploadResponse;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentStatus;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.kb.service.impl.DocumentIndexingServiceImpl;
import com.enterprise.rag.admin.kb.storage.IndexInputState;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.admin.kb.storage.IndexInputStorageException;
import com.enterprise.rag.admin.kb.storage.StoredIndexInput;
import com.enterprise.rag.admin.kb.task.IndexTaskLedger;
import com.enterprise.rag.admin.kb.task.IndexTaskRecord;
import com.enterprise.rag.admin.kb.task.IndexTaskPhase;
import com.enterprise.rag.admin.kb.task.IndexTaskLeaseLostException;
import com.enterprise.rag.admin.kb.task.IndexTaskSqlFinalizer;
import com.enterprise.rag.admin.kb.task.DeterministicChunkIdentity;
import com.enterprise.rag.common.async.AsyncTask;
import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskHandle;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.NoOpKeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.chunker.DocumentChunkingProperties;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.processor.DocumentProcessor;
import com.enterprise.rag.document.processor.ProcessResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

class DocumentIndexingServiceImplTest {

    @TempDir
    Path durableRoot;

    private final DocumentService documentService = mock(DocumentService.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final DocumentProcessor documentProcessor = mock(DocumentProcessor.class);
    private final DocumentParserFactory documentParserFactory = mock(DocumentParserFactory.class);
    private final AsyncTaskManager asyncTaskManager = mock(AsyncTaskManager.class);
    private final EmbeddingService embeddingService = mock(EmbeddingService.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final IndexInputStore indexInputStore = mock(IndexInputStore.class);
    private final IndexTaskLedger indexTaskLedger = mock(IndexTaskLedger.class);
    private final IndexTaskSqlFinalizer sqlFinalizer = mock(IndexTaskSqlFinalizer.class);

    private final DocumentIndexingServiceImpl service = new DocumentIndexingServiceImpl(
            documentService,
            knowledgeBaseService,
            documentProcessor,
            documentParserFactory,
            asyncTaskManager,
            embeddingService,
            vectorStore,
            new NoOpKeywordIndex(),
            indexInputStore,
            indexTaskLedger,
            sqlFinalizer,
            new DocumentChunkingProperties());

    @BeforeEach
    void setUpAcceptedTaskId() {
        when(indexTaskLedger.createAccepted(any(Long.class), any(Long.class)))
                .thenAnswer(invocation -> "task-" + invocation.<Long>getArgument(0));
    }

    private void stubStoredInput() {
        when(indexInputStore.put(any(InputStream.class)))
                .thenReturn(new StoredIndexInput("objects/input.bin", 12L, "abc123"));
        when(indexInputStore.openVerified("objects/input.bin", 12L, "abc123"))
                .thenAnswer(invocation -> new java.io.ByteArrayInputStream("durable-body".getBytes()));
        when(indexInputStore.delete("objects/input.bin"))
                .thenReturn(IndexInputStore.DeleteResult.DELETED);
    }

    @Test
    void submitIndexingShouldRejectLegacyDocFiles() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "legacy.doc",
                "application/msword",
                "legacy-doc".getBytes());

        when(documentParserFactory.isSupported("doc")).thenReturn(false);
        when(documentParserFactory.getSupportedTypes()).thenReturn(Set.of("pdf", "docx", "md"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.submitIndexing(10L, 20L, file, null));

        assertEquals("DOC_001", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("不支持的文件类型: doc"));
        verify(documentService, never()).create(any());
        verify(asyncTaskManager, never()).submit(eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any());
    }

    @Test
    void submitIndexingShouldCreateTaskForSupportedDocxFiles() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx-content".getBytes());

        Document created = new Document();
        created.setId(99L);

        when(documentParserFactory.isSupported("docx")).thenReturn(true);
        stubStoredInput();
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(asyncTaskManager.submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-99", CompletableFuture.completedFuture(mock(ProcessResult.class))));

        DocumentUploadResponse response = service.submitIndexing(10L, 20L, file, "manual");

        assertEquals(99L, response.documentId());
        assertEquals("task-99", response.taskId());
        assertEquals("manual.docx", response.fileName());
        assertEquals("docx", response.fileType());
        assertEquals("PROCESSING", response.status());
        verify(documentService).create(any(Document.class));
        verify(asyncTaskManager).submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any());
    }

    @Test
    void submitIndexingShouldPersistDurableInputFactsBeforeAcceptingTask() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "durable.md", "text/markdown", "durable-body".getBytes());
        StoredIndexInput stored = new StoredIndexInput("objects/durable.bin", 12L, "sha256-value");
        Document created = new Document();
        created.setId(120L);

        when(documentParserFactory.isSupported("md")).thenReturn(true);
        when(indexInputStore.put(any(InputStream.class))).thenReturn(stored);
        when(documentService.create(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(120L);
            return document;
        });
        when(asyncTaskManager.submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-120", CompletableFuture.completedFuture(mock(ProcessResult.class))));

        service.submitIndexing(10L, 20L, file, "durable");

        var order = inOrder(indexInputStore, documentService, asyncTaskManager);
        order.verify(indexInputStore).put(any(InputStream.class));
        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        order.verify(documentService).create(documentCaptor.capture());
        order.verify(asyncTaskManager).submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any());

        Document persisted = documentCaptor.getValue();
        assertEquals(stored.storageKey(), persisted.getFilePath());
        assertEquals(stored.sizeBytes(), persisted.getInputSizeBytes());
        assertEquals(stored.sha256(), persisted.getInputSha256());
        assertEquals(IndexInputState.AVAILABLE.name(), persisted.getInputState());
    }

    @Test
    void submitIndexingShouldPersistLedgerBeforeCreatingRedisProjection() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "ledger.md", "text/markdown", "durable-body".getBytes());
        Document created = new Document();
        created.setId(120L);

        when(documentParserFactory.isSupported("md")).thenReturn(true);
        stubStoredInput();
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(asyncTaskManager.submit(eq("task-120"), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-120",
                        CompletableFuture.completedFuture(mock(ProcessResult.class))));

        DocumentUploadResponse response = service.submitIndexing(10L, 20L, file, "ledger");

        assertEquals("task-120", response.taskId());
        var order = inOrder(indexInputStore, documentService, indexTaskLedger, asyncTaskManager);
        order.verify(indexInputStore).put(any(InputStream.class));
        order.verify(documentService).create(any(Document.class));
        order.verify(indexTaskLedger).createAccepted(120L, 20L);
        order.verify(asyncTaskManager).submit(eq("task-120"), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildVectorMetadataShouldPreserveChunkMetadataAndFillSourceFileName() {
        DocumentChunk chunk = new DocumentChunk(
                "chunk-1",
                "RAG overview",
                0,
                12,
                Map.of(
                        "headingPath", "RAG > Retrieval",
                        "headingLevel", 2,
                        "tokenCount", 12,
                        "sourceFileName", ""));

        Map<String, Object> metadata = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                service,
                "buildVectorMetadata",
                10L,
                99L,
                "rag.md",
                "RAG Guide",
                0,
                chunk);

        assertEquals("RAG > Retrieval", metadata.get("headingPath"));
        assertEquals(2, metadata.get("headingLevel"));
        assertEquals(12, metadata.get("tokenCount"));
        assertEquals("rag.md", metadata.get("sourceFileName"));
        assertEquals("RAG Guide", metadata.get("documentTitle"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void compactMetadataShouldRemoveNullValuesForKeywordIndex() {
        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("headingPath", "RAG");
        metadata.put("headingLevel", null);
        metadata.put("tokenCount", 12);

        Map<String, Object> compacted = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                service,
                "compactMetadata",
                metadata);

        assertEquals("RAG", compacted.get("headingPath"));
        assertEquals(12, compacted.get("tokenCount"));
        assertFalse(compacted.containsKey("headingLevel"));
    }

    @Test
    void indexingRetryShouldNotPersistDuplicateChunks() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "retry.md",
                "text/markdown",
                "# Retry\n\ncontent".getBytes());
        Document created = new Document();
        created.setId(99L);
        created.setTitle("retry.md");
        KnowledgeBaseDTO kb = new KnowledgeBaseDTO();
        kb.setVectorCollection("kb_retry");
        DocumentChunk chunk = new DocumentChunk("chunk-1", "content", 0, 7, Map.of("tokenCount", 7));
        ProcessResult result = ProcessResult.newDocument("doc-1", "hash-1", "content", List.of(chunk));
        com.enterprise.rag.admin.kb.entity.DocumentChunk savedChunk = new com.enterprise.rag.admin.kb.entity.DocumentChunk();
        savedChunk.setDocumentId(99L);
        savedChunk.setVectorId("chunk-1");

        when(documentParserFactory.isSupported("md")).thenReturn(true);
        stubStoredInput();
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(asyncTaskManager.submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-99", CompletableFuture.completedFuture(result)));
        when(documentProcessor.process(any())).thenReturn(result);
        when(documentService.getByKnowledgeBaseAndContentHash(10L, "hash-1")).thenReturn(Optional.empty());
        when(knowledgeBaseService.getById(10L)).thenReturn(Optional.of(kb));
        when(embeddingService.embedBatch(anyList())).thenReturn(List.of(new float[] { 0.1f, 0.2f }));
        doThrow(new RuntimeException("db glitch"))
                .doNothing()
                .when(sqlFinalizer).finalizeSql(eq("task-99"), eq(10L), eq(99L),
                        eq("hash-1"), anyList());

        service.submitIndexing(10L, 20L, file, "retry.md");

        ArgumentCaptor<AsyncTask<ProcessResult>> taskCaptor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskManager).submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L), taskCaptor.capture());
        taskCaptor.getValue().execute(progress -> {
        });

        verify(vectorStore, times(1)).upsert(eq("kb_retry"), anyList());
        verify(indexTaskLedger).markVectorConfirmed("task-99");
        verify(sqlFinalizer, times(2)).finalizeSql(eq("task-99"), eq(10L), eq(99L),
                eq("hash-1"), anyList());
        verify(documentService).updateInputState(99L, IndexInputState.CLEANUP_PENDING.name());
        verify(indexInputStore).delete("objects/input.bin");
        verify(documentService).updateInputState(99L, IndexInputState.CLEANED.name());
        verify(documentService, never()).updateStatus(99L, DocumentStatus.FAILED.name());
        verify(knowledgeBaseService, never()).updateDocumentCount(10L, 1);
    }

    @Test
    void taskAcceptanceFailureCleansDurableInputAndDoesNotReturnFakeTask() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "task-failure.md", "text/markdown", "durable-body".getBytes());
        Document created = new Document();
        created.setId(130L);
        created.setTitle("task-failure.md");

        when(documentParserFactory.isSupported("md")).thenReturn(true);
        stubStoredInput();
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(asyncTaskManager.submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenThrow(new BusinessException("REDIS_UNAVAILABLE", "任务状态不可用"));

        assertThrows(BusinessException.class,
                () -> service.submitIndexing(10L, 20L, file, "task-failure"));

        verify(documentService).updateStatus(130L, DocumentStatus.FAILED.name());
        verify(indexTaskLedger).markAcceptanceFailed("task-130", "TASK_PROJECTION_FAILED");
        verify(documentService).updateInputState(130L, IndexInputState.CLEANUP_PENDING.name());
        verify(indexInputStore).delete("objects/input.bin");
        verify(documentService).updateInputState(130L, IndexInputState.CLEANED.name());
    }

    @Test
    void corruptDurableInputFailsBeforeParsingAndPersistsCorruptState() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "corrupt.md", "text/markdown", "durable-body".getBytes());
        Document created = new Document();
        created.setId(140L);
        created.setTitle("corrupt.md");

        when(documentParserFactory.isSupported("md")).thenReturn(true);
        stubStoredInput();
        when(indexInputStore.openVerified("objects/input.bin", 12L, "abc123"))
                .thenThrow(IndexInputStorageException.corrupt());
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(asyncTaskManager.submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-140", CompletableFuture.completedFuture(mock(ProcessResult.class))));

        service.submitIndexing(10L, 20L, file, "corrupt");
        ArgumentCaptor<AsyncTask<ProcessResult>> taskCaptor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskManager).submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L), taskCaptor.capture());

        assertThrows(IndexInputStorageException.class,
                () -> taskCaptor.getValue().execute(progress -> {
                }));

        verify(documentService).updateInputState(140L, IndexInputState.CORRUPT.name());
        verify(documentService).updateStatus(140L, DocumentStatus.FAILED.name());
        verify(documentProcessor, never()).process(any());
        verify(embeddingService, never()).embedBatch(anyList());
        verify(vectorStore, never()).upsert(any(), anyList());
    }

    @Test
    void missingDurableInputFailsBeforeParsingAndPersistsMissingState() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "missing.md", "text/markdown", "durable-body".getBytes());
        Document created = new Document();
        created.setId(145L);
        created.setTitle("missing.md");
        var emptyStore = new com.enterprise.rag.admin.kb.storage.FileSystemIndexInputStore(durableRoot);

        when(documentParserFactory.isSupported("md")).thenReturn(true);
        stubStoredInput();
        when(indexInputStore.openVerified("objects/input.bin", 12L, "abc123"))
                .thenAnswer(invocation -> emptyStore.openVerified("objects/missing.bin", 12L, "abc123"));
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(asyncTaskManager.submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-145", CompletableFuture.completedFuture(mock(ProcessResult.class))));

        service.submitIndexing(10L, 20L, file, "missing");
        ArgumentCaptor<AsyncTask<ProcessResult>> taskCaptor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskManager).submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L), taskCaptor.capture());

        assertThrows(IndexInputStorageException.class,
                () -> taskCaptor.getValue().execute(progress -> {
                }));

        verify(documentService).updateInputState(145L, IndexInputState.MISSING.name());
        verify(documentService).updateStatus(145L, DocumentStatus.FAILED.name());
        verify(documentProcessor, never()).process(any());
        verify(embeddingService, never()).embedBatch(anyList());
        verify(vectorStore, never()).upsert(any(), anyList());
    }

    @Test
    void vectorMutationOutcomeUnknownShouldNotBeRetried() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vector-failure.md", "text/markdown", "content".getBytes());
        Document created = new Document();
        created.setId(101L);
        created.setTitle("vector-failure.md");
        KnowledgeBaseDTO kb = new KnowledgeBaseDTO();
        kb.setVectorCollection("kb_vector_failure");
        DocumentChunk chunk = new DocumentChunk("chunk-1", "content", 0, 7, Map.of());
        ProcessResult result = ProcessResult.newDocument("doc-1", "hash-1", "content", List.of(chunk));

        when(documentParserFactory.isSupported("md")).thenReturn(true);
        stubStoredInput();
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(asyncTaskManager.submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-101", CompletableFuture.completedFuture(result)));
        when(documentProcessor.process(any())).thenReturn(result);
        when(documentService.getByKnowledgeBaseAndContentHash(10L, "hash-1")).thenReturn(Optional.empty());
        when(knowledgeBaseService.getById(10L)).thenReturn(Optional.of(kb));
        when(embeddingService.embedBatch(anyList())).thenReturn(List.of(new float[] { 0.1f, 0.2f }));
        doThrow(VectorDependencyException.outcomeUnknown("upsert", new IllegalStateException("raw-marker")))
                .when(vectorStore).upsert(eq("kb_vector_failure"), anyList());

        service.submitIndexing(10L, 20L, file, "vector-failure.md");
        ArgumentCaptor<AsyncTask<ProcessResult>> taskCaptor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskManager).submit(any(String.class), eq("DOCUMENT_INDEX"), eq(20L), taskCaptor.capture());

        assertThrows(RuntimeException.class, () -> taskCaptor.getValue().execute(progress -> {
        }));
        verify(vectorStore, times(1)).upsert(eq("kb_vector_failure"), anyList());
        verify(indexTaskLedger).markVectorInFlight("task-101", "hash-1", 1);
        verify(indexTaskLedger).markReconciliationRequired(
                "task-101", VectorDependencyException.ERROR_CODE_OUTCOME_UNKNOWN);
        verify(documentService).updateStatus(101L, DocumentStatus.FAILED.name());
        verify(documentService, never()).saveChunks(anyList());
        verify(indexInputStore, never()).delete("objects/input.bin");
        verify(documentService, never()).updateInputState(101L, IndexInputState.CLEANUP_PENDING.name());
    }

    @Test
    void vectorConfirmedResumeFinalizesWithoutEmbeddingOrVectorReplay() throws Exception {
        Document document = new Document();
        document.setId(160L);
        document.setKbId(10L);
        document.setUploaderId(20L);
        document.setTitle("resume");
        document.setFileType("md");
        document.setFilePath("objects/input.bin");
        document.setInputSizeBytes(12L);
        document.setInputSha256("abc123");
        DocumentChunk chunk = new DocumentChunk("random", "content", 0, 7, Map.of());
        ProcessResult result = ProcessResult.newDocument("doc-160", "hash-160", "content", List.of(chunk));
        KnowledgeBaseDTO kb = new KnowledgeBaseDTO();
        kb.setVectorCollection("kb_resume");
        IndexTaskRecord task = new IndexTaskRecord();
        task.setTaskId("task-160");
        task.setDocumentId(160L);
        task.setExecutionPhase(IndexTaskPhase.VECTOR_CONFIRMED.name());
        task.setIndexContractVersion(DeterministicChunkIdentity.CONTRACT_VERSION);
        task.setChunkSize(500);
        task.setChunkOverlap(50);
        task.setPreparedContentHash("hash-160");
        task.setPreparedChunkCount(1);

        when(documentService.getById(160L)).thenReturn(Optional.of(document));
        stubStoredInput();
        when(documentProcessor.process(any())).thenReturn(result);
        when(knowledgeBaseService.getById(10L)).thenReturn(Optional.of(kb));
        when(documentService.getChunksByDocumentId(160L)).thenReturn(List.of());

        service.resumeIndexTask(task);

        verify(embeddingService, never()).embedBatch(anyList());
        verify(vectorStore, never()).upsert(any(String.class), anyList());
        verify(sqlFinalizer).finalizeSql(eq("task-160"), eq(10L), eq(160L),
                eq("hash-160"), anyList());
        verify(indexInputStore).delete("objects/input.bin");
    }

    @Test
    void safePreVectorResumeKeepsTaskIdAndPerformsOneVectorMutation() throws Exception {
        Document document = new Document();
        document.setId(170L);
        document.setKbId(10L);
        document.setUploaderId(20L);
        document.setTitle("safe-resume");
        document.setFileType("md");
        document.setFilePath("objects/input.bin");
        document.setInputSizeBytes(12L);
        document.setInputSha256("abc123");
        DocumentChunk chunk = new DocumentChunk("random", "content", 0, 7, Map.of());
        ProcessResult result = ProcessResult.newDocument("doc-170", "hash-170", "content", List.of(chunk));
        KnowledgeBaseDTO kb = new KnowledgeBaseDTO();
        kb.setVectorCollection("kb_safe_resume");
        IndexTaskRecord task = new IndexTaskRecord();
        task.setTaskId("task-170");
        task.setDocumentId(170L);
        task.setExecutionPhase(IndexTaskPhase.SAFE_PRE_VECTOR.name());
        task.setIndexContractVersion(DeterministicChunkIdentity.CONTRACT_VERSION);
        task.setChunkSize(500);
        task.setChunkOverlap(50);

        when(documentService.getById(170L)).thenReturn(Optional.of(document));
        stubStoredInput();
        when(documentProcessor.process(any())).thenReturn(result);
        when(documentService.getByKnowledgeBaseAndContentHash(10L, "hash-170")).thenReturn(Optional.empty());
        when(knowledgeBaseService.getById(10L)).thenReturn(Optional.of(kb));
        when(embeddingService.embedBatch(anyList())).thenReturn(List.of(new float[] { 0.1f }));
        when(documentService.getChunksByDocumentId(170L)).thenReturn(List.of());

        service.resumeIndexTask(task);

        verify(indexTaskLedger).markSafePreVector("task-170");
        verify(indexTaskLedger).markVectorInFlight("task-170", "hash-170", 1);
        verify(vectorStore, times(1)).upsert(eq("kb_safe_resume"), anyList());
        verify(sqlFinalizer).finalizeSql(eq("task-170"), eq(10L), eq(170L),
                eq("hash-170"), anyList());
    }

    @Test
    void lostLeaseStopsBeforeEmbeddingAndVectorMutation() throws Exception {
        Document document = new Document();
        document.setId(171L);
        document.setKbId(10L);
        document.setTitle("lost-lease");
        document.setFileType("md");
        document.setFilePath("objects/input.bin");
        document.setInputSizeBytes(12L);
        document.setInputSha256("abc123");
        DocumentChunk chunk = new DocumentChunk("random", "content", 0, 7, Map.of());
        ProcessResult result = ProcessResult.newDocument("doc-171", "hash-171", "content", List.of(chunk));
        KnowledgeBaseDTO kb = new KnowledgeBaseDTO();
        kb.setVectorCollection("kb_lost_lease");
        IndexTaskRecord task = new IndexTaskRecord();
        task.setTaskId("task-171");
        task.setDocumentId(171L);
        task.setExecutionPhase(IndexTaskPhase.SAFE_PRE_VECTOR.name());
        task.setIndexContractVersion(DeterministicChunkIdentity.CONTRACT_VERSION);
        task.setChunkSize(500);
        task.setChunkOverlap(50);

        when(documentService.getById(171L)).thenReturn(Optional.of(document));
        stubStoredInput();
        when(documentProcessor.process(any())).thenReturn(result);
        when(documentService.getByKnowledgeBaseAndContentHash(10L, "hash-171"))
                .thenReturn(Optional.empty());
        when(knowledgeBaseService.getById(10L)).thenReturn(Optional.of(kb));
        when(embeddingService.embedBatch(anyList())).thenReturn(List.of(new float[] {0.1f}));

        assertThrows(IndexTaskLeaseLostException.class,
                () -> service.resumeIndexTask(task,
                        () -> { throw new IndexTaskLeaseLostException(task.getTaskId()); }));

        verify(embeddingService, never()).embedBatch(anyList());
        verify(vectorStore, never()).upsert(any(String.class), anyList());
        verify(indexTaskLedger, never()).markVectorInFlight(any(), any(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void completedResultRemainsCompletedWhenInputCleanupFails() {
        when(indexInputStore.delete("objects/input.bin"))
                .thenReturn(IndexInputStore.DeleteResult.FAILED);

        ReflectionTestUtils.invokeMethod(service, "cleanupCompletedInput", 150L, "objects/input.bin");

        verify(documentService).updateInputState(150L, IndexInputState.CLEANUP_PENDING.name());
        verify(indexInputStore).delete("objects/input.bin");
        verify(documentService, never()).updateInputState(150L, IndexInputState.CLEANED.name());
        verify(documentService, never()).updateStatus(150L, DocumentStatus.FAILED.name());
    }
}

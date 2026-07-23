package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.kb.service.impl.DocumentIndexingServiceImpl;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.admin.kb.storage.StoredIndexInput;
import com.enterprise.rag.admin.kb.task.IndexTaskLedger;
import com.enterprise.rag.admin.kb.task.IndexTaskPhase;
import com.enterprise.rag.admin.kb.task.IndexTaskRecord;
import com.enterprise.rag.admin.kb.task.IndexTaskSqlFinalizer;
import com.enterprise.rag.common.async.AsyncTask;
import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskHandle;
import com.enterprise.rag.common.trace.GenAiTelemetry;
import com.enterprise.rag.common.trace.TraceContext;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.NoOpKeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.chunker.DocumentChunkingProperties;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.processor.DocumentProcessor;
import com.enterprise.rag.document.processor.ProcessResult;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentIndexingTelemetryTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    private final OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(provider)
            .build();

    @AfterEach
    void cleanup() {
        provider.close();
        exporter.reset();
    }

    @Test
    @SuppressWarnings("unchecked")
    void ingestCreatesIndependentRootWithSubmissionLinkAndDurableLineage() throws Exception {
        DocumentService documentService = mock(DocumentService.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        DocumentProcessor documentProcessor = mock(DocumentProcessor.class);
        DocumentParserFactory parserFactory = mock(DocumentParserFactory.class);
        AsyncTaskManager asyncTaskManager = mock(AsyncTaskManager.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        IndexInputStore inputStore = mock(IndexInputStore.class);
        IndexTaskLedger ledger = mock(IndexTaskLedger.class);
        IndexTaskSqlFinalizer finalizer = mock(IndexTaskSqlFinalizer.class);
        GenAiTelemetry telemetry = new GenAiTelemetry(openTelemetry);
        DocumentIndexingServiceImpl service = new DocumentIndexingServiceImpl(
                documentService, knowledgeBaseService, documentProcessor, parserFactory,
                asyncTaskManager, embeddingService, vectorStore, new NoOpKeywordIndex(),
                inputStore, ledger, finalizer, new DocumentChunkingProperties(), telemetry);

        Document created = new Document();
        created.setId(99L);
        created.setTitle("raw-sensitive-title");
        KnowledgeBaseDTO kb = new KnowledgeBaseDTO();
        kb.setVectorCollection("kb_test");
        DocumentChunk chunk = new DocumentChunk("chunk-stable", "content", 0, 7, Map.of());
        ProcessResult result = ProcessResult.newDocument("doc-99", "hash-99", "content", List.of(chunk));
        when(parserFactory.isSupported("md")).thenReturn(true);
        when(inputStore.put(any(InputStream.class)))
                .thenReturn(new StoredIndexInput("objects/input.bin", 12L, "abc123"));
        when(inputStore.openVerified("objects/input.bin", 12L, "abc123"))
                .thenReturn(new ByteArrayInputStream("durable-body".getBytes()));
        when(inputStore.delete("objects/input.bin")).thenReturn(IndexInputStore.DeleteResult.DELETED);
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(ledger.createAccepted(99L, 20L)).thenReturn("task-99");
        when(asyncTaskManager.submit(eq("task-99"), eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-99", CompletableFuture.completedFuture(result)));
        when(documentProcessor.process(any())).thenReturn(result);
        when(documentService.getByKnowledgeBaseAndContentHash(10L, "hash-99"))
                .thenReturn(Optional.empty());
        when(knowledgeBaseService.getById(10L)).thenReturn(Optional.of(kb));
        when(embeddingService.embedBatch(anyList())).thenReturn(List.of(new float[] {0.1f, 0.2f}));
        doThrow(new IllegalStateException("raw-finalize-retry-message"))
                .doNothing()
                .when(finalizer).finalizeSql(eq("task-99"), eq(10L), eq(99L), eq("hash-99"), anyList());

        Span submission = openTelemetry.getTracer("test").spanBuilder("upload.request").startSpan();
        try (Scope ignored = submission.makeCurrent()) {
            service.submitIndexing(10L, 20L,
                    new MockMultipartFile("file", "raw-sensitive-file.md", "text/markdown", "content".getBytes()),
                    "raw-sensitive-title");
        } finally {
            submission.end();
        }

        ArgumentCaptor<AsyncTask<ProcessResult>> taskCaptor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskManager).submit(eq("task-99"), eq("DOCUMENT_INDEX"), eq(20L), taskCaptor.capture());
        taskCaptor.getValue().execute(progress -> { });
        assertFalse(Span.current().getSpanContext().isValid());
        assertNull(TraceContext.getTraceId());
        assertNull(TraceContext.getSpanId());

        var ingest = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.INGEST.equals(span.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.INGEST.equals(span.getName()))
                .count());
        assertEquals("SUCCESS", ingest.getAttributes().get(GenAiTelemetry.Attributes.OUTCOME));
        assertFalse(ingest.getParentSpanContext().isValid());
        assertEquals(1, ingest.getLinks().size());
        assertEquals(submission.getSpanContext().getTraceId(), ingest.getLinks().get(0).getSpanContext().getTraceId());
        Set<String> spanNames = exporter.getFinishedSpanItems().stream()
                .map(span -> span.getName())
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(spanNames.containsAll(Set.of(
                GenAiTelemetry.SpanNames.INGEST_INPUT_OPEN,
                GenAiTelemetry.SpanNames.INGEST_PARSE_CHUNK,
                GenAiTelemetry.SpanNames.DOCUMENT_EMBEDDING,
                GenAiTelemetry.SpanNames.VECTOR_UPSERT,
                GenAiTelemetry.SpanNames.KEYWORD_UPSERT,
                GenAiTelemetry.SpanNames.INDEX_FINALIZE)));

        ArgumentCaptor<List<VectorDocument>> vectors = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).upsert(eq("kb_test"), vectors.capture());
        Map<String, Object> metadata = vectors.getValue().get(0).metadata();
        assertEquals("task-99", metadata.get("ingestTaskId"));
        assertEquals(99L, metadata.get("documentId"));
        assertEquals(vectors.getValue().get(0).id(), metadata.get("chunkId"));
        String exported = exporter.getFinishedSpanItems().toString();
        assertFalse(exported.contains("raw-sensitive-file"));
        assertFalse(exported.contains("raw-sensitive-title"));
        assertFalse(exported.contains("raw-finalize-retry-message"));
    }

    @Test
    void resumedIngestCreatesIndependentRootWithoutCurrentRequestLink() {
        DocumentService documentService = mock(DocumentService.class);
        DocumentIndexingServiceImpl service = new DocumentIndexingServiceImpl(
                documentService, mock(KnowledgeBaseService.class), mock(DocumentProcessor.class),
                mock(DocumentParserFactory.class), mock(AsyncTaskManager.class), mock(EmbeddingService.class),
                mock(VectorStore.class), new NoOpKeywordIndex(), mock(IndexInputStore.class),
                mock(IndexTaskLedger.class), mock(IndexTaskSqlFinalizer.class),
                new DocumentChunkingProperties(), new GenAiTelemetry(openTelemetry));
        IndexTaskRecord task = new IndexTaskRecord();
        task.setTaskId("task-resume");
        task.setDocumentId(404L);
        task.setExecutionPhase(IndexTaskPhase.ACCEPTED.name());
        task.setIndexContractVersion(com.enterprise.rag.admin.kb.task.DeterministicChunkIdentity.CONTRACT_VERSION);
        task.setChunkSize(500);
        task.setChunkOverlap(50);
        when(documentService.getById(404L)).thenReturn(Optional.empty());

        Span request = openTelemetry.getTracer("test").spanBuilder("request").startSpan();
        try (Scope ignored = request.makeCurrent()) {
            assertThrows(RuntimeException.class, () -> service.resumeIndexTask(task));
        } finally {
            request.end();
        }

        var ingest = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.INGEST.equals(span.getName()))
                .findFirst().orElseThrow();
        assertFalse(ingest.getParentSpanContext().isValid());
        assertTrue(ingest.getLinks().isEmpty());
        assertEquals(true, ingest.getAttributes().get(GenAiTelemetry.Attributes.RESUME));
        assertEquals("ERROR", ingest.getAttributes().get(GenAiTelemetry.Attributes.OUTCOME));
        assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, ingest.getStatus().getStatusCode());
    }
}

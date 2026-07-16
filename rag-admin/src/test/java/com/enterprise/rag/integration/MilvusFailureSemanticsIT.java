package com.enterprise.rag.integration;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentStatus;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.kb.service.impl.DocumentIndexingServiceImpl;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.admin.kb.storage.StoredIndexInput;
import com.enterprise.rag.common.async.AsyncTask;
import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskHandle;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.rag.keyword.NoOpKeywordIndex;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.query.QueryEngineImpl;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.rag.rerank.HeuristicReranker;
import com.enterprise.rag.core.rag.rerank.RerankerRegistry;
import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.config.VectorStoreProperties;
import com.enterprise.rag.core.vectorstore.milvus.MilvusVectorStore;
import com.enterprise.rag.document.chunker.DocumentChunk;
import com.enterprise.rag.document.parser.DocumentParserFactory;
import com.enterprise.rag.document.processor.DocumentProcessor;
import com.enterprise.rag.document.processor.ProcessResult;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
class MilvusFailureSemanticsIT {

    private static final String ETCD_IMAGE = "quay.io/coreos/etcd:v3.5.5";
    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2023-03-20T20-16-18Z";
    private static final String MILVUS_IMAGE = "milvusdb/milvus:v2.3.4";
    private static final Network NETWORK = Network.newNetwork();
    private static final int MILVUS_HOST_PORT = findFreePort();

    @Container
    private static final GenericContainer<?> ETCD = new GenericContainer<>(DockerImageName.parse(ETCD_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases("etcd")
            .withExposedPorts(2379)
            .withCommand(
                    "etcd",
                    "-advertise-client-urls=http://127.0.0.1:2379",
                    "-listen-client-urls=http://0.0.0.0:2379",
                    "--data-dir=/etcd")
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(1));

    @Container
    private static final GenericContainer<?> MINIO = new GenericContainer<>(DockerImageName.parse(MINIO_IMAGE))
            .withNetwork(NETWORK)
            .withNetworkAliases("minio")
            .withEnv("MINIO_ACCESS_KEY", "minioadmin")
            .withEnv("MINIO_SECRET_KEY", "minioadmin")
            .withExposedPorts(9000)
            .withCommand("minio", "server", "/minio_data")
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(1));

    @Container
    private static final IsolatedMilvusContainer MILVUS = new IsolatedMilvusContainer(
            DockerImageName.parse(MILVUS_IMAGE), MILVUS_HOST_PORT)
            .dependsOn(ETCD, MINIO)
            .withNetwork(NETWORK)
            .withEnv("ETCD_ENDPOINTS", "etcd:2379")
            .withEnv("MINIO_ADDRESS", "minio:9000")
            .withCommand("milvus", "run", "standalone")
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                    .withSecurityOpts(List.of("seccomp=unconfined")))
            .waitingFor(Wait.forHttp("/healthz").forPort(9091).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    private static MilvusServiceClient client;
    private static MilvusVectorStore vectorStore;

    @BeforeAll
    static void createClient() {
        MilvusServiceClient baseClient = new MilvusServiceClient(ConnectParam.newBuilder()
                .withHost(MILVUS.getHost())
                .withPort(MILVUS_HOST_PORT)
                .withConnectTimeout(3, TimeUnit.SECONDS)
                .withRpcDeadline(3, TimeUnit.SECONDS)
                .build());
        client = (MilvusServiceClient) baseClient.withRetry(1);
        VectorStoreProperties.MilvusProperties properties = new VectorStoreProperties.MilvusProperties();
        properties.setNlist(1);
        properties.setNprobe(1);
        vectorStore = new MilvusVectorStore(client, properties);
    }

    @AfterAll
    static void closeClient() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void isolatedStopStartShouldProduceStableFailureAndRecoverApplicationSearch() throws Exception {
        String collection = "c4d_" + UUID.randomUUID().toString().replace("-", "");
        vectorStore.createCollection(collection, 2);
        vectorStore.upsert(collection, List.of(
                new VectorDocument("synthetic-1", new float[] { 1.0f, 0.0f }, "synthetic evidence", Map.of())));
        assertFalse(vectorStore.search(
                collection, new float[] { 1.0f, 0.0f }, new SearchOptions(3, 0.0f, Map.of())).isEmpty());

        String containerId = MILVUS.getContainerId();
        MILVUS.getDockerClient().stopContainerCmd(containerId).exec();
        VectorDependencyException outage = assertNoKeywordStableErrorFromRealOutage(collection);
        assertEquals(VectorDependencyException.ERROR_CODE_UNAVAILABLE, outage.getErrorCode());
        assertKeywordOnlyDegradationFromObservedOutage(outage, collection);
        assertIndexTaskFailureFromObservedOutage(outage, collection);

        MILVUS.getDockerClient().startContainerCmd(containerId).exec();
        assertEquals(MILVUS_HOST_PORT, MILVUS.getMappedPort(19530));
        awaitSearchRecovery(collection, Duration.ofMinutes(2));
        vectorStore.dropCollection(collection);
    }

    private static VectorDependencyException assertNoKeywordStableErrorFromRealOutage(String collection) {
        EmbeddingService embeddingService = deterministicEmbedding();
        RetrievalProperties properties = new RetrievalProperties();
        properties.getKeyword().setEnabled(false);
        QueryEngineImpl queryEngine = queryEngine(
                embeddingService, vectorStore, new NoOpKeywordIndex(), properties);

        return assertThrows(
                VectorDependencyException.class,
                () -> queryEngine.retrieveWithDiagnostics(
                        "synthetic no-keyword query",
                        new RetrieveOptions(collection, 3, 0.0f, Map.of(), false)));
    }

    private static void assertKeywordOnlyDegradationFromObservedOutage(
            VectorDependencyException outage,
            String collection) {
        VectorStore unavailableVectorStore = mock(VectorStore.class);
        when(unavailableVectorStore.search(anyString(), any(float[].class), any(SearchOptions.class)))
                .thenThrow(outage);
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        when(keywordIndex.search(eq(collection), anyString(), eq(6), eq(Map.of())))
                .thenReturn(List.of(new RetrievedContext(
                        "synthetic keyword evidence", "keyword-1", 0.8f, Map.of())));
        QueryEngineImpl queryEngine = queryEngine(
                deterministicEmbedding(), unavailableVectorStore, keywordIndex, new RetrievalProperties());

        RetrievalResult result = queryEngine.retrieveWithDiagnostics(
                "synthetic keyword query",
                new RetrieveOptions(collection, 3, 0.0f, Map.of(), false));

        assertEquals("keyword_only", result.diagnostics().get("retrievalMode"));
        assertEquals(true, result.diagnostics().get("retrievalDegraded"));
        assertEquals("milvus", result.diagnostics().get("degradedDependency"));
        assertEquals(1, result.contexts().size());
    }

    @SuppressWarnings("unchecked")
    private static void assertIndexTaskFailureFromObservedOutage(
            VectorDependencyException outage,
            String collection) throws Exception {
        DocumentService documentService = mock(DocumentService.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        DocumentProcessor documentProcessor = mock(DocumentProcessor.class);
        DocumentParserFactory parserFactory = mock(DocumentParserFactory.class);
        AsyncTaskManager taskManager = mock(AsyncTaskManager.class);
        EmbeddingService embeddingService = deterministicEmbedding();
        VectorStore unavailableVectorStore = mock(VectorStore.class);
        IndexInputStore indexInputStore = mock(IndexInputStore.class);
        DocumentIndexingServiceImpl indexingService = new DocumentIndexingServiceImpl(
                documentService,
                knowledgeBaseService,
                documentProcessor,
                parserFactory,
                taskManager,
                embeddingService,
                unavailableVectorStore,
                new NoOpKeywordIndex(),
                indexInputStore);

        MockMultipartFile file = new MockMultipartFile(
                "file", "synthetic.md", "text/markdown", "synthetic content".getBytes());
        Document created = new Document();
        created.setId(101L);
        created.setTitle("synthetic.md");
        KnowledgeBaseDTO kb = new KnowledgeBaseDTO();
        kb.setVectorCollection(collection);
        DocumentChunk chunk = new DocumentChunk("chunk-1", "synthetic content", 0, 17, Map.of());
        ProcessResult processed = ProcessResult.newDocument(
                "doc-1", "hash-1", "synthetic content", List.of(chunk));

        when(parserFactory.isSupported("md")).thenReturn(true);
        when(indexInputStore.put(any())).thenReturn(
                new StoredIndexInput("objects/synthetic.bin", 17L, "synthetic-sha"));
        when(indexInputStore.openVerified("objects/synthetic.bin", 17L, "synthetic-sha"))
                .thenReturn(new ByteArrayInputStream("synthetic content".getBytes()));
        when(documentService.create(any(Document.class))).thenReturn(created);
        when(taskManager.submit(eq("DOCUMENT_INDEX"), eq(20L),
                org.mockito.ArgumentMatchers.<AsyncTask<ProcessResult>>any()))
                .thenReturn(new TaskHandle<>("task-101", CompletableFuture.completedFuture(processed)));
        when(documentProcessor.process(any())).thenReturn(processed);
        when(documentService.getByKnowledgeBaseAndContentHash(10L, "hash-1")).thenReturn(Optional.empty());
        when(knowledgeBaseService.getById(10L)).thenReturn(Optional.of(kb));
        doThrow(outage).when(unavailableVectorStore).upsert(eq(collection), anyList());

        indexingService.submitIndexing(10L, 20L, file, "synthetic.md");
        ArgumentCaptor<AsyncTask<ProcessResult>> taskCaptor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(taskManager).submit(eq("DOCUMENT_INDEX"), eq(20L), taskCaptor.capture());

        assertThrows(RuntimeException.class, () -> taskCaptor.getValue().execute(progress -> {
        }));
        verify(unavailableVectorStore, times(1)).upsert(eq(collection), anyList());
        verify(documentService).updateStatus(101L, DocumentStatus.FAILED.name());
        verify(documentService, never()).saveChunks(anyList());
    }

    private static QueryEngineImpl queryEngine(
            EmbeddingService embeddingService,
            VectorStore routeVectorStore,
            KeywordIndex keywordIndex,
            RetrievalProperties properties) {
        return new QueryEngineImpl(
                embeddingService,
                routeVectorStore,
                keywordIndex,
                properties,
                new RerankerRegistry(List.of(new HeuristicReranker()), properties));
    }

    private static EmbeddingService deterministicEmbedding() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        when(embeddingService.embed(anyString())).thenReturn(new float[] { 1.0f, 0.0f });
        when(embeddingService.embedBatch(anyList())).thenReturn(List.of(new float[] { 1.0f, 0.0f }));
        when(embeddingService.getDimension()).thenReturn(2);
        return embeddingService;
    }

    private static void awaitSearchRecovery(String collection, Duration timeout) throws InterruptedException {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            try {
                if (!vectorStore.search(
                        collection, new float[] { 1.0f, 0.0f }, new SearchOptions(3, 0.0f, Map.of())).isEmpty()) {
                    return;
                }
            } catch (VectorDependencyException ignored) {
                // Bounded readiness polling against this test's isolated container only.
            }
            Thread.sleep(1_000L);
        }
        fail("isolated Milvus did not recover application-level search within timeout");
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("cannot allocate isolated Milvus host port", e);
        }
    }

    private static final class IsolatedMilvusContainer extends GenericContainer<IsolatedMilvusContainer> {
        private IsolatedMilvusContainer(DockerImageName imageName, int hostPort) {
            super(imageName);
            addFixedExposedPort(hostPort, 19530);
            addExposedPort(9091);
        }
    }
}

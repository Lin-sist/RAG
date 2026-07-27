package com.enterprise.rag.integration;

import com.enterprise.rag.RagQaApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@ActiveProfiles("c14-isolation-eval")
@Import({DeterministicEmbeddingTestConfig.class, DeterministicGenerationTestConfig.class})
@SpringBootTest(
        classes = RagQaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rag.embedding.enable-fallback=false",
                "rag.embedding.openai.enabled=false",
                "rag.embedding.qwen.enabled=false",
                "rag.embedding.bge.enabled=false",
                "retrieval.rerank.provider=heuristic",
                "retrieval.rerank.model.enabled=false"
        })
class C14IsolationAdversarialIT {

    private static final String MYSQL_IMAGE = "mysql:8.0.36";
    private static final String REDIS_IMAGE =
            "redis:7-alpine@sha256:8b81dd37ff027bec4e516d41acfbe9fe2460070dc6d4a4570a2ac5b9d59df065";
    private static final String ETCD_IMAGE = "quay.io/coreos/etcd:v3.5.5";
    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2023-03-20T20-16-18Z";
    private static final String MILVUS_IMAGE = "milvusdb/milvus:v2.3.4";

    private static final String BOOTSTRAP_USERNAME = "admin";
    private static final String BOOTSTRAP_PASSWORD = "C14-A!" + UUID.randomUUID() + "xY9";
    private static final String TENANT_B_USERNAME = "c14-b-" + UUID.randomUUID();
    private static final String TENANT_B_PASSWORD = "C14-B!" + UUID.randomUUID() + "xY9";
    private static final String TENANT_B_CODE = "c14-b-" + UUID.randomUUID();
    private static final String TENANT_A_READER_USERNAME = "c14-a-reader-" + UUID.randomUUID();
    private static final String TENANT_A_READER_PASSWORD = "C14-R!" + UUID.randomUUID() + "xY9";
    private static final String REDIS_PASSWORD = "c14-redis-" + UUID.randomUUID();
    private static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final String FOREIGN_CANARY = "c14_foreign_canary_"
            + UUID.randomUUID().toString().replace("-", "");
    private static final int WARMUP_PAIRS = 10;
    private static final int MEASURED_PAIRS = 40;
    private static final long TIMING_SEED = 14001L;

    private static final Network MILVUS_NETWORK = Network.newNetwork();

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("rag_c14_isolation")
            .withUsername("rag_c14")
            .withPassword("mysql-" + UUID.randomUUID())
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(1));

    @Container
    private static final GenericContainer<?> ETCD = new GenericContainer<>(DockerImageName.parse(ETCD_IMAGE))
            .withNetwork(MILVUS_NETWORK)
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
            .withNetwork(MILVUS_NETWORK)
            .withNetworkAliases("minio")
            .withEnv("MINIO_ACCESS_KEY", "minioadmin")
            .withEnv("MINIO_SECRET_KEY", "minioadmin")
            .withExposedPorts(9000)
            .withCommand("minio", "server", "/minio_data")
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(1));

    @Container
    private static final GenericContainer<?> MILVUS = new GenericContainer<>(DockerImageName.parse(MILVUS_IMAGE))
            .dependsOn(ETCD, MINIO)
            .withNetwork(MILVUS_NETWORK)
            .withEnv("ETCD_ENDPOINTS", "etcd:2379")
            .withEnv("MINIO_ADDRESS", "minio:9000")
            .withExposedPorts(19530, 9091)
            .withCommand("milvus", "run", "standalone")
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                    .withSecurityOpts(List.of("seccomp=unconfined")))
            .waitingFor(Wait.forHttp("/healthz").forPort(9091).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DeterministicEmbeddingTestConfig.DeterministicEmbeddingProvider embeddingProvider;

    @Autowired
    private DeterministicGenerationTestConfig.DeterministicAnswerGenerator answerGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureDependencies(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> REDIS_PASSWORD);
        registry.add("rag.vectorstore.milvus.host", MILVUS::getHost);
        registry.add("rag.vectorstore.milvus.port", () -> MILVUS.getMappedPort(19530));
        registry.add("auth.bootstrap.enabled", () -> true);
        registry.add("auth.bootstrap.username", () -> BOOTSTRAP_USERNAME);
        registry.add("auth.bootstrap.password", () -> BOOTSTRAP_PASSWORD);
        registry.add("jwt.secret", () -> JWT_SECRET);
    }

    @Test
    void fixedSyntheticMatrixKeepsForeignPublicKnowledgeBaseOpaqueAndUnchanged() throws Exception {
        assertOwnedInfrastructure();
        SyntheticIdentity tenantB = insertTenantB();
        String tenantAToken = login(BOOTSTRAP_USERNAME, BOOTSTRAP_PASSWORD);
        String tenantBToken = login(TENANT_B_USERNAME, TENANT_B_PASSWORD);
        long tenantAId = jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM `user` WHERE username = ?", Long.class, BOOTSTRAP_USERNAME);
        assertNotEquals(tenantAId, tenantB.tenantId());
        long tenantAReaderId = insertTenantAReader(tenantAId);
        String tenantAReaderToken = login(TENANT_A_READER_USERNAME, TENANT_A_READER_PASSWORD);

        HttpHeaders selectorHeaders = bearerHeaders(tenantAToken);
        selectorHeaders.set("X-Tenant-Id", String.valueOf(tenantB.tenantId()));
        selectorHeaders.add(HttpHeaders.COOKIE, "tenantId=" + tenantB.tenantId());
        long tenantAKb = createKnowledgeBase(
                tenantAToken,
                "/api/knowledge-bases?tenantId=" + tenantB.tenantId(),
                Map.of(
                        "name", "c14-tenant-a-" + UUID.randomUUID(),
                        "description", "tenant A control",
                        "isPublic", false,
                        "tenantId", tenantB.tenantId(),
                        "metadata", Map.of("tenantId", tenantB.tenantId())),
                selectorHeaders);
        assertEquals(tenantAId, tenantOfKnowledgeBase(tenantAKb));
        jdbcTemplate.update("""
                INSERT INTO kb_permission (tenant_id, kb_id, user_id, permission_type, deleted, version)
                VALUES (?, ?, ?, 'READ', 0, 0)
                """, tenantAId, tenantAKb, tenantAReaderId);

        long tenantBKb = createKnowledgeBase(
                tenantBToken,
                "/api/knowledge-bases",
                Map.of(
                        "name", FOREIGN_CANARY,
                        "description", "foreign identity " + FOREIGN_CANARY,
                        "isPublic", true),
                bearerHeaders(tenantBToken));
        assertEquals(tenantB.tenantId(), tenantOfKnowledgeBase(tenantBKb));

        Upload tenantADocument = uploadDocument(
                tenantAKb,
                tenantAToken,
                "tenant-a-control.txt",
                "Tenant A local synthetic fact has no foreign canary.");
        Upload tenantBDocument = uploadDocument(
                tenantBKb,
                tenantBToken,
                "tenant-b-canary.txt",
                "Tenant B protected content includes " + FOREIGN_CANARY + ".");
        awaitSuccessfulTask(tenantADocument.taskId(), tenantAToken);
        awaitSuccessfulTask(tenantBDocument.taskId(), tenantBToken);
        KnowledgeBaseSnapshot before = snapshot(tenantBKb);

        ResponseEntity<JsonNode> listAsA = get("/api/knowledge-bases", tenantAToken);
        assertEquals(HttpStatus.OK, listAsA.getStatusCode());
        assertFalse(bodyText(listAsA).contains(FOREIGN_CANARY));

        long nonexistentKb = Long.MAX_VALUE - 14001;
        ResponseEntity<JsonNode> foreignDetail = get("/api/knowledge-bases/" + tenantBKb, tenantAToken);
        ResponseEntity<JsonNode> controlDetail = get("/api/knowledge-bases/" + nonexistentKb, tenantAToken);
        assertOpaqueMatchedControl(foreignDetail, controlDetail);

        ResponseEntity<JsonNode> foreignStatistics = get(
                "/api/knowledge-bases/" + tenantBKb + "/statistics", tenantAToken);
        ResponseEntity<JsonNode> controlStatistics = get(
                "/api/knowledge-bases/" + nonexistentKb + "/statistics", tenantAToken);
        assertOpaqueMatchedControl(foreignStatistics, controlStatistics);

        ResponseEntity<JsonNode> foreignDocuments = get(
                "/api/knowledge-bases/" + tenantBKb + "/documents", tenantAToken);
        ResponseEntity<JsonNode> controlDocuments = get(
                "/api/knowledge-bases/" + nonexistentKb + "/documents", tenantAToken);
        assertOpaqueMatchedControl(foreignDocuments, controlDocuments);

        ResponseEntity<JsonNode> update = http.exchange(
                "/api/knowledge-bases/" + tenantBKb,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "name", "attacker-overwrite",
                        "description", "attacker-overwrite",
                        "isPublic", false), bearerHeaders(tenantAToken)),
                JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, update.getStatusCode(), bodyText(update));

        ResponseEntity<JsonNode> delete = http.exchange(
                "/api/knowledge-bases/" + tenantBKb,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(tenantAToken)),
                JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, delete.getStatusCode(), bodyText(delete));
        assertEquals(before, snapshot(tenantBKb));

        DocumentSnapshot foreignDocumentBefore = documentSnapshot(tenantBDocument.documentId());
        ResponseEntity<JsonNode> foreignDocumentDelete = http.exchange(
                "/api/knowledge-bases/" + tenantBKb + "/documents/" + tenantBDocument.documentId(),
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(tenantAToken)),
                JsonNode.class);
        ResponseEntity<JsonNode> controlDocumentDelete = http.exchange(
                "/api/knowledge-bases/" + nonexistentKb + "/documents/" + Long.MAX_VALUE,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(tenantAToken)),
                JsonNode.class);
        assertOpaqueMatchedControl(foreignDocumentDelete, controlDocumentDelete);
        assertEquals(foreignDocumentBefore, documentSnapshot(tenantBDocument.documentId()));

        ResponseEntity<JsonNode> foreignUpload = uploadAttempt(
                tenantBKb, tenantAToken, "attacker.txt", "attacker mutation");
        ResponseEntity<JsonNode> controlUpload = uploadAttempt(
                nonexistentKb, tenantAToken, "control.txt", "control mutation");
        assertOpaqueMatchedControl(foreignUpload, controlUpload);

        assertForeignTaskOpaqueAndUnchanged(tenantBDocument.taskId(), tenantAToken);
        assertReservedScopeFiltersRejected(tenantAKb, tenantAToken);
        assertRagSyncAndStreamIsolation(
                tenantAKb, tenantBKb, nonexistentKb, tenantAToken, tenantAReaderToken);
        assertHistoryAndFeedbackIsolation(tenantB, tenantBKb, tenantAToken);

        ResponseEntity<JsonNode> ownerRead = get("/api/knowledge-bases/" + tenantBKb, tenantBToken);
        assertEquals(HttpStatus.OK, ownerRead.getStatusCode(), bodyText(ownerRead));
        assertTrue(bodyText(ownerRead).contains(FOREIGN_CANARY));

        Map<String, Object> ownerTiming = timingOracle(tenantBKb, nonexistentKb, tenantAToken);
        Map<String, Object> readerTiming = timingOracle(tenantBKb, nonexistentKb, tenantAReaderToken);
        assertTrue(embeddingProvider.invocationCount() > 0,
                "deterministic in-process embedding stub must serve the synthetic fixture");
        assertTrue(answerGenerator.invocationCount() > 0,
                "deterministic in-process generation stub must serve sync and stream controls");
        writeDriverEvidence(Map.of(
                "timing-foreign-kb", ownerTiming,
                "timing-foreign-public-kb", readerTiming));
    }

    private void assertOwnedInfrastructure() {
        assertTrue(MYSQL.isRunning());
        assertTrue(REDIS.isRunning());
        assertTrue(ETCD.isRunning());
        assertTrue(MINIO.isRunning());
        assertTrue(MILVUS.isRunning());
        assertEquals(MYSQL_IMAGE, MYSQL.getDockerImageName());
        assertEquals(REDIS_IMAGE, REDIS.getDockerImageName());
        assertEquals(ETCD_IMAGE, ETCD.getDockerImageName());
        assertEquals(MINIO_IMAGE, MINIO.getDockerImageName());
        assertEquals(MILVUS_IMAGE, MILVUS.getDockerImageName());
    }

    private SyntheticIdentity insertTenantB() {
        jdbcTemplate.update(
                "INSERT INTO tenant (code, name, enabled, deleted, version) VALUES (?, ?, 1, 0, 0)",
                TENANT_B_CODE, "C14 Synthetic Tenant B");
        long tenantId = jdbcTemplate.queryForObject(
                "SELECT id FROM tenant WHERE code = ?", Long.class, TENANT_B_CODE);
        jdbcTemplate.update("""
                INSERT INTO `user`
                    (tenant_id, username, password_hash, email, enabled, deleted, version)
                VALUES (?, ?, ?, ?, 1, 0, 0)
                """, tenantId, TENANT_B_USERNAME, passwordEncoder.encode(TENANT_B_PASSWORD),
                TENANT_B_USERNAME + "@example.test");
        long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM `user` WHERE username = ?", Long.class, TENANT_B_USERNAME);
        jdbcTemplate.update("""
                INSERT INTO user_role (user_id, role_id)
                SELECT ?, id FROM role WHERE name = 'ADMIN'
                """, userId);
        return new SyntheticIdentity(tenantId, userId);
    }

    private long insertTenantAReader(long tenantId) {
        jdbcTemplate.update("""
                INSERT INTO `user`
                    (tenant_id, username, password_hash, email, enabled, deleted, version)
                VALUES (?, ?, ?, ?, 1, 0, 0)
                """, tenantId, TENANT_A_READER_USERNAME,
                passwordEncoder.encode(TENANT_A_READER_PASSWORD),
                TENANT_A_READER_USERNAME + "@example.test");
        long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM `user` WHERE username = ?", Long.class, TENANT_A_READER_USERNAME);
        jdbcTemplate.update("""
                INSERT INTO user_role (user_id, role_id)
                SELECT ?, id FROM role WHERE name = 'USER'
                """, userId);
        return userId;
    }

    private Upload uploadDocument(long kbId, String token, String fileName, String content) {
        ResponseEntity<JsonNode> response = uploadAttempt(kbId, token, fileName, content);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), bodyText(response));
        JsonNode data = requireBody(response).path("data");
        long documentId = data.path("documentId").asLong();
        String taskId = data.path("taskId").asText();
        assertTrue(documentId > 0);
        assertFalse(taskId.isBlank());
        return new Upload(documentId, taskId);
    }

    private ResponseEntity<JsonNode> uploadAttempt(
            long kbId, String token, String fileName, String content) {
        ByteArrayResource file = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        body.add("title", fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return http.exchange(
                "/api/knowledge-bases/" + kbId + "/documents",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                JsonNode.class);
    }

    private void awaitSuccessfulTask(String taskId, String token) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(60));
        String lastState = "UNKNOWN";
        while (Instant.now().isBefore(deadline)) {
            ResponseEntity<JsonNode> response = get("/api/tasks/" + taskId, token);
            assertEquals(HttpStatus.OK, response.getStatusCode(), bodyText(response));
            lastState = requireBody(response).path("data").path("state").asText();
            if ("COMPLETED".equals(lastState)) {
                return;
            }
            assertFalse("FAILED".equals(lastState) || "CANCELLED".equals(lastState),
                    "index task ended in " + lastState);
            TimeUnit.MILLISECONDS.sleep(250);
        }
        throw new AssertionError("index task timed out: taskId=" + taskId + ", state=" + lastState);
    }

    private void assertForeignTaskOpaqueAndUnchanged(String foreignTaskId, String token) {
        TaskSnapshot before = taskSnapshot(foreignTaskId);
        String nonexistentTaskId = "c14-missing-" + UUID.randomUUID();
        for (String suffix : List.of("", "/result", "/exists", "/completed")) {
            ResponseEntity<JsonNode> foreign = get("/api/tasks/" + foreignTaskId + suffix, token);
            ResponseEntity<JsonNode> control = get("/api/tasks/" + nonexistentTaskId + suffix, token);
            assertOpaqueMatchedControl(foreign, control);
        }
        ResponseEntity<JsonNode> foreignCancel = http.exchange(
                "/api/tasks/" + foreignTaskId + "/cancel",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class);
        ResponseEntity<JsonNode> controlCancel = http.exchange(
                "/api/tasks/" + nonexistentTaskId + "/cancel",
                HttpMethod.POST,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class);
        assertOpaqueMatchedControl(foreignCancel, controlCancel);
        assertEquals(before, taskSnapshot(foreignTaskId));
    }

    private void assertReservedScopeFiltersRejected(long localKb, String token) {
        ResponseEntity<JsonNode> ask = postJson(
                "/api/qa/ask",
                Map.of(
                        "kbId", localKb,
                        "question", "scope override must fail",
                        "filter", Map.of("tenantId", Long.MAX_VALUE),
                        "enableCache", false),
                token);
        assertEquals(HttpStatus.BAD_REQUEST, ask.getStatusCode(), bodyText(ask));
        assertTrue(bodyText(ask).contains("RAG_SCOPE_FILTER_RESERVED"), bodyText(ask));

        ResponseEntity<JsonNode> retrieve = postJson(
                "/api/qa/debug/retrieve",
                Map.of(
                        "kbId", localKb,
                        "question", "collection override must fail",
                        "filter", Map.of("VECTOR_COLLECTION_NAME", "forged"),
                        "enableRerank", false),
                token);
        assertEquals(HttpStatus.BAD_REQUEST, retrieve.getStatusCode(), bodyText(retrieve));
        assertTrue(bodyText(retrieve).contains("RAG_SCOPE_FILTER_RESERVED"), bodyText(retrieve));
    }

    private void assertRagSyncAndStreamIsolation(
            long localKb, long foreignKb, long nonexistentKb, String syncToken, String streamToken) {
        Map<String, Object> localRequest = Map.of(
                "kbId", localKb,
                "question", "What is the Tenant A local synthetic fact?",
                "topK", 5,
                "minScore", 0.0,
                "filter", Map.of(),
                "enableCache", false);
        ResponseEntity<JsonNode> local = postJson("/api/qa/ask", localRequest, syncToken);
        assertEquals(HttpStatus.OK, local.getStatusCode(), bodyText(local));
        assertTrue(bodyText(local).contains("c14 deterministic synthetic answer"), bodyText(local));
        assertFalse(bodyText(local).contains(FOREIGN_CANARY), bodyText(local));

        ResponseEntity<JsonNode> foreign = postJson(
                "/api/qa/ask", withKnowledgeBase(localRequest, foreignKb), syncToken);
        ResponseEntity<JsonNode> control = postJson(
                "/api/qa/ask", withKnowledgeBase(localRequest, nonexistentKb), syncToken);
        assertOpaqueMatchedControl(foreign, control);

        HttpHeaders streamHeaders = bearerHeaders(streamToken);
        streamHeaders.setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
        ResponseEntity<String> localStream = http.exchange(
                "/api/qa/ask/stream",
                HttpMethod.POST,
                new HttpEntity<>(localRequest, streamHeaders),
                String.class);
        assertEquals(HttpStatus.OK, localStream.getStatusCode(), String.valueOf(localStream.getBody()));
        assertTrue(String.valueOf(localStream.getBody()).contains("c14 deterministic"));
        assertFalse(String.valueOf(localStream.getBody()).contains(FOREIGN_CANARY));

        ResponseEntity<JsonNode> foreignStream = postStreamError(
                "/api/qa/ask/stream", withKnowledgeBase(localRequest, foreignKb), streamToken);
        ResponseEntity<JsonNode> controlStream = postStreamError(
                "/api/qa/ask/stream", withKnowledgeBase(localRequest, nonexistentKb), streamToken);
        assertOpaqueMatchedControl(foreignStream, controlStream);
    }

    private Map<String, Object> withKnowledgeBase(Map<String, Object> request, long kbId) {
        Map<String, Object> copy = new java.util.HashMap<>(request);
        copy.put("kbId", kbId);
        return copy;
    }

    private void assertHistoryAndFeedbackIsolation(
            SyntheticIdentity tenantB, long tenantBKb, String token) {
        jdbcTemplate.update("""
                INSERT INTO qa_history
                    (tenant_id, user_id, kb_id, question, answer, citations, trace_id,
                     latency_ms, deleted, version)
                VALUES (?, ?, ?, ?, ?, '[]', ?, 1, 0, 0)
                """, tenantB.tenantId(), tenantB.userId(), tenantBKb,
                "foreign question " + FOREIGN_CANARY,
                "foreign answer " + FOREIGN_CANARY,
                "c14-trace-" + UUID.randomUUID());
        long historyId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM qa_history WHERE tenant_id = ? AND user_id = ?",
                Long.class, tenantB.tenantId(), tenantB.userId());
        long nonexistentHistory = Long.MAX_VALUE - 14002;
        HistorySnapshot before = historySnapshot(historyId);
        int feedbackBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qa_feedback WHERE qa_id = ?", Integer.class, historyId);

        assertOpaqueMatchedControl(
                get("/api/history/" + historyId, token),
                get("/api/history/" + nonexistentHistory, token));
        ResponseEntity<JsonNode> foreignDelete = http.exchange(
                "/api/history/" + historyId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class);
        ResponseEntity<JsonNode> controlDelete = http.exchange(
                "/api/history/" + nonexistentHistory,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class);
        assertOpaqueMatchedControl(foreignDelete, controlDelete);
        assertOpaqueMatchedControl(
                postJson("/api/history/" + historyId + "/feedback",
                        Map.of("rating", 5, "comment", "attacker"), token),
                postJson("/api/history/" + nonexistentHistory + "/feedback",
                        Map.of("rating", 5, "comment", "control"), token));
        assertOpaqueMatchedControl(
                get("/api/history/" + historyId + "/feedback", token),
                get("/api/history/" + nonexistentHistory + "/feedback", token));
        assertEquals(before, historySnapshot(historyId));
        assertEquals(feedbackBefore, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qa_feedback WHERE qa_id = ?", Integer.class, historyId));
    }

    private ResponseEntity<JsonNode> postJson(String path, Map<String, Object> request, String token) {
        return http.exchange(
                path,
                HttpMethod.POST,
                new HttpEntity<>(request, bearerHeaders(token)),
                JsonNode.class);
    }

    private ResponseEntity<JsonNode> postStreamError(
            String path, Map<String, Object> request, String token) {
        HttpHeaders headers = bearerHeaders(token);
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
        return http.exchange(
                path,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                JsonNode.class);
    }

    private String login(String username, String password) {
        ResponseEntity<JsonNode> response = http.postForEntity(
                "/auth/login", Map.of("username", username, "password", password), JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), bodyText(response));
        JsonNode body = requireBody(response);
        String token = body.path("data").path("accessToken").asText();
        assertFalse(token.isBlank());
        return token;
    }

    private long createKnowledgeBase(
            String token,
            String path,
            Map<String, Object> request,
            HttpHeaders headers) {
        headers.setBearerAuth(token);
        ResponseEntity<JsonNode> response = http.exchange(
                path, HttpMethod.POST, new HttpEntity<>(request, headers), JsonNode.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), bodyText(response));
        long id = requireBody(response).path("data").path("id").asLong();
        assertTrue(id > 0);
        return id;
    }

    private long tenantOfKnowledgeBase(long kbId) {
        return jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM knowledge_base WHERE id = ?", Long.class, kbId);
    }

    private KnowledgeBaseSnapshot snapshot(long kbId) {
        return jdbcTemplate.queryForObject("""
                SELECT tenant_id, name, description, is_public, deleted, version
                FROM knowledge_base WHERE id = ?
                """, (rs, row) -> new KnowledgeBaseSnapshot(
                rs.getLong("tenant_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("is_public"),
                rs.getBoolean("deleted"),
                rs.getInt("version")), kbId);
    }

    private DocumentSnapshot documentSnapshot(long documentId) {
        return jdbcTemplate.queryForObject("""
                SELECT tenant_id, kb_id, uploader_id, title, status, chunk_count, deleted, version
                FROM document WHERE id = ?
                """, (rs, row) -> new DocumentSnapshot(
                rs.getLong("tenant_id"),
                rs.getLong("kb_id"),
                rs.getLong("uploader_id"),
                rs.getString("title"),
                rs.getString("status"),
                rs.getInt("chunk_count"),
                rs.getBoolean("deleted"),
                rs.getInt("version")), documentId);
    }

    private TaskSnapshot taskSnapshot(String taskId) {
        return jdbcTemplate.queryForObject("""
                SELECT tenant_id, task_id, document_id, owner_id, status, progress,
                       execution_phase, attempt_count, deleted, version
                FROM async_task WHERE task_id = ?
                """, (rs, row) -> new TaskSnapshot(
                rs.getLong("tenant_id"),
                rs.getString("task_id"),
                rs.getLong("document_id"),
                rs.getLong("owner_id"),
                rs.getString("status"),
                rs.getInt("progress"),
                rs.getString("execution_phase"),
                rs.getInt("attempt_count"),
                rs.getBoolean("deleted"),
                rs.getInt("version")), taskId);
    }

    private HistorySnapshot historySnapshot(long historyId) {
        return jdbcTemplate.queryForObject("""
                SELECT tenant_id, user_id, kb_id, question, answer, deleted, version
                FROM qa_history WHERE id = ?
                """, (rs, row) -> new HistorySnapshot(
                rs.getLong("tenant_id"),
                rs.getLong("user_id"),
                rs.getLong("kb_id"),
                rs.getString("question"),
                rs.getString("answer"),
                rs.getBoolean("deleted"),
                rs.getInt("version")), historyId);
    }

    private ResponseEntity<JsonNode> get(String path, String token) {
        return http.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                JsonNode.class);
    }

    private void assertOpaqueMatchedControl(
            ResponseEntity<JsonNode> foreign,
            ResponseEntity<JsonNode> control) {
        assertEquals(HttpStatus.NOT_FOUND, foreign.getStatusCode(), bodyText(foreign));
        assertEquals(fingerprint(control), fingerprint(foreign));
        assertFalse(bodyText(foreign).contains(FOREIGN_CANARY));
        assertFalse(bodyText(foreign).contains(TENANT_B_CODE));
        assertFalse(bodyText(foreign).contains(TENANT_B_USERNAME));
    }

    private String fingerprint(ResponseEntity<JsonNode> response) {
        JsonNode body = requireBody(response);
        List<String> fields = new ArrayList<>();
        body.fieldNames().forEachRemaining(fields::add);
        fields.sort(Comparator.naturalOrder());
        return response.getStatusCode().value()
                + "|" + body.path("code").asText()
                + "|" + body.path("errorCode").asText()
                + "|" + String.join(",", fields);
    }

    private Map<String, Object> timingOracle(long foreignKb, long controlKb, String token) {
        for (int i = 0; i < WARMUP_PAIRS; i++) {
            get("/api/knowledge-bases/" + foreignKb, token);
            get("/api/knowledge-bases/" + controlKb, token);
        }
        List<Boolean> schedule = new ArrayList<>();
        for (int i = 0; i < MEASURED_PAIRS; i++) {
            schedule.add(Boolean.TRUE);
            schedule.add(Boolean.FALSE);
        }
        Collections.shuffle(schedule, new Random(TIMING_SEED));
        List<Double> foreign = new ArrayList<>();
        List<Double> control = new ArrayList<>();
        for (boolean useForeign : schedule) {
            long started = System.nanoTime();
            ResponseEntity<JsonNode> response = get(
                    "/api/knowledge-bases/" + (useForeign ? foreignKb : controlKb), token);
            double elapsedMs = (System.nanoTime() - started) / 1_000_000.0d;
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), bodyText(response));
            (useForeign ? foreign : control).add(elapsedMs);
        }
        assertEquals(MEASURED_PAIRS, foreign.size());
        assertEquals(MEASURED_PAIRS, control.size());
        double medianDelta = Math.abs(percentile(foreign, 0.50d) - percentile(control, 0.50d));
        double p95Delta = Math.abs(percentile(foreign, 0.95d) - percentile(control, 0.95d));
        assertTrue(medianDelta <= Math.max(10.0d, percentile(control, 0.50d) * 0.5d),
                "foreign/nonexistent median delta exceeded the frozen coarse threshold");
        assertTrue(p95Delta <= Math.max(25.0d, percentile(control, 0.95d) * 0.5d),
                "foreign/nonexistent p95 delta exceeded the frozen coarse threshold");
        return Map.of(
                "warmupPairs", WARMUP_PAIRS,
                "measuredPairs", MEASURED_PAIRS,
                "interleavingSeed", TIMING_SEED,
                "foreignMedianMs", percentile(foreign, 0.50d),
                "controlMedianMs", percentile(control, 0.50d),
                "foreignP95Ms", percentile(foreign, 0.95d),
                "controlP95Ms", percentile(control, 0.95d),
                "requestErrors", 0);
    }

    private void writeDriverEvidence(Map<String, Object> timing) throws Exception {
        String gitHead = new String(
                new ProcessBuilder("git", "rev-parse", "HEAD")
                        .redirectErrorStream(true)
                        .start()
                        .getInputStream()
                        .readAllBytes(),
                StandardCharsets.UTF_8).trim();
        assertTrue(gitHead.matches("[0-9a-f]{40}"), "unable to bind driver evidence to Git HEAD");
        Map<String, Object> evidence = Map.ofEntries(
                Map.entry("schemaVersion", "tenant-isolation-driver-evidence-v1"),
                Map.entry("releaseVersion", "tenant-isolation-adversarial-v1"),
                Map.entry("driverVersion", "tenant-isolation-driver-v1"),
                Map.entry("fixtureVersion", "tenant-isolation-fixture-v1"),
                Map.entry("profileVersion", "tenant-isolation-profile-v1"),
                Map.entry("gitHead", gitHead),
                Map.entry("infrastructure", Map.of(
                        "owned", true,
                        "healthy", MYSQL.isRunning() && REDIS.isRunning() && MILVUS.isRunning(),
                        "mysqlImage", MYSQL_IMAGE,
                        "redisImage", REDIS_IMAGE,
                        "milvusImage", MILVUS_IMAGE)),
                Map.entry("providerCallCount", 0),
                Map.entry("businessDataOutbound", false),
                Map.entry("realMaintenanceStatus", "SKIPPED"),
                Map.entry("timing", timing));
        Path target = Path.of(System.getProperty("basedir"), "target", "c14-isolation-driver-evidence.json");
        Files.createDirectories(target.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), evidence);
    }

    private double percentile(List<Double> source, double fraction) {
        List<Double> sorted = new ArrayList<>(source);
        sorted.sort(Comparator.naturalOrder());
        int index = (int) Math.ceil(fraction * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    private static HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private static JsonNode requireBody(ResponseEntity<JsonNode> response) {
        JsonNode body = response.getBody();
        assertNotNull(body);
        return body;
    }

    private static String bodyText(ResponseEntity<JsonNode> response) {
        return response.getBody() == null ? "<empty>" : response.getBody().toString();
    }

    private record SyntheticIdentity(long tenantId, long userId) {
    }

    private record KnowledgeBaseSnapshot(
            long tenantId,
            String name,
            String description,
            boolean isPublic,
            boolean deleted,
            int version) {
    }

    private record Upload(long documentId, String taskId) {
    }

    private record DocumentSnapshot(
            long tenantId,
            long kbId,
            long uploaderId,
            String title,
            String status,
            int chunkCount,
            boolean deleted,
            int version) {
    }

    private record TaskSnapshot(
            long tenantId,
            String taskId,
            long documentId,
            long ownerId,
            String status,
            int progress,
            String executionPhase,
            int attemptCount,
            boolean deleted,
            int version) {
    }

    private record HistorySnapshot(
            long tenantId,
            long userId,
            long kbId,
            String question,
            String answer,
            boolean deleted,
            int version) {
    }
}

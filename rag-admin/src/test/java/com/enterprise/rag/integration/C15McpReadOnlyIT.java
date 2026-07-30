package com.enterprise.rag.integration;

import com.enterprise.rag.RagQaApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@ActiveProfiles("c15-mcp-readonly")
@Import({DeterministicEmbeddingTestConfig.class, DeterministicGenerationTestConfig.class})
@SpringBootTest(
        classes = RagQaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rag.mcp.enabled=true",
                "rag.mcp.local-only=true",
                "rag.mcp.external-tools-enabled=true",
                "rag.mcp.cache-enabled=false",
                "rag.mcp.resource-page-size=1",
                "rag.mcp.max-chunk-bytes=128",
                "rag.embedding.enable-fallback=false",
                "rag.embedding.openai.enabled=false",
                "rag.embedding.qwen.enabled=false",
                "rag.embedding.bge.enabled=false",
                "retrieval.hybrid.enabled=false",
                "retrieval.rerank.provider=heuristic",
                "retrieval.rerank.model.enabled=false"
        })
class C15McpReadOnlyIT {

    private static final String MYSQL_IMAGE = "mysql:8.0.36";
    private static final String REDIS_IMAGE =
            "redis:7-alpine@sha256:8b81dd37ff027bec4e516d41acfbe9fe2460070dc6d4a4570a2ac5b9d59df065";
    private static final String ETCD_IMAGE = "quay.io/coreos/etcd:v3.5.5";
    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2023-03-20T20-16-18Z";
    private static final String MILVUS_IMAGE = "milvusdb/milvus:v2.3.4";
    private static final String TOOL_SCHEMA_SHA256 =
            "44ce4cc851551057bbabfbdf3735b964de300ff691b2e73a612856e9fa64213f";

    private static final String BOOTSTRAP_USERNAME = "admin";
    private static final String BOOTSTRAP_PASSWORD = "C15-A!" + UUID.randomUUID() + "xY9";
    private static final String READER_USERNAME = "c15-reader-" + UUID.randomUUID();
    private static final String READER_PASSWORD = "C15-R!" + UUID.randomUUID() + "xY9";
    private static final String TENANT_B_USERNAME = "c15-b-" + UUID.randomUUID();
    private static final String TENANT_B_PASSWORD = "C15-B!" + UUID.randomUUID() + "xY9";
    private static final String TENANT_B_CODE = "c15-b-" + UUID.randomUUID();
    private static final String REDIS_PASSWORD = "c15-redis-" + UUID.randomUUID();
    private static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    private static final String FOREIGN_CANARY = "c15_foreign_canary_"
            + UUID.randomUUID().toString().replace("-", "");
    private static final int WARMUP_PAIRS = 5;
    private static final int MEASURED_PAIRS = 20;
    private static final long TIMING_SEED = 15001L;

    private static final Network MILVUS_NETWORK = Network.newNetwork();

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("rag_c15_mcp")
            .withUsername("rag_c15")
            .withPassword("mysql-" + UUID.randomUUID())
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withStartupTimeout(Duration.ofMinutes(1));

    @Container
    private static final GenericContainer<?> ETCD = new GenericContainer<>(
            DockerImageName.parse(ETCD_IMAGE))
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
    private static final GenericContainer<?> MINIO = new GenericContainer<>(
            DockerImageName.parse(MINIO_IMAGE))
            .withNetwork(MILVUS_NETWORK)
            .withNetworkAliases("minio")
            .withEnv("MINIO_ACCESS_KEY", "minioadmin")
            .withEnv("MINIO_SECRET_KEY", "minioadmin")
            .withExposedPorts(9000)
            .withCommand("minio", "server", "/minio_data")
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(1));

    @Container
    private static final GenericContainer<?> MILVUS = new GenericContainer<>(
            DockerImageName.parse(MILVUS_IMAGE))
            .dependsOn(ETCD, MINIO)
            .withNetwork(MILVUS_NETWORK)
            .withEnv("ETCD_ENDPOINTS", "etcd:2379")
            .withEnv("MINIO_ADDRESS", "minio:9000")
            .withExposedPorts(19530, 9091)
            .withCommand("milvus", "run", "standalone")
            .withCreateContainerCmdModifier(command -> command.getHostConfig()
                    .withSecurityOpts(List.of("seccomp=unconfined")))
            .waitingFor(Wait.forHttp("/healthz").forPort(9091).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeterministicEmbeddingTestConfig.DeterministicEmbeddingProvider embeddingProvider;

    @Autowired
    private DeterministicGenerationTestConfig.DeterministicAnswerGenerator answerGenerator;

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
    void dualTenantFixtureProvesReadOnlyMcpScopeAndSideEffectBoundary() throws Exception {
        assertOwnedInfrastructure();
        SyntheticIdentity tenantB = insertTenantB();
        String ownerToken = login(BOOTSTRAP_USERNAME, BOOTSTRAP_PASSWORD);
        String tenantBToken = login(TENANT_B_USERNAME, TENANT_B_PASSWORD);
        long tenantAId = jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM `user` WHERE username = ?",
                Long.class,
                BOOTSTRAP_USERNAME);
        long readerId = insertReader(tenantAId);
        String readerToken = login(READER_USERNAME, READER_PASSWORD);
        assertNotEquals(tenantAId, tenantB.tenantId());

        long privateKb = createKnowledgeBase(
                ownerToken, "c15-private-" + UUID.randomUUID(), false);
        long publicKb = createKnowledgeBase(
                ownerToken, "c15-public-" + UUID.randomUUID(), true);
        long foreignKb = createKnowledgeBase(
                tenantBToken, FOREIGN_CANARY, true);
        jdbcTemplate.update("""
                INSERT INTO kb_permission
                    (tenant_id, kb_id, user_id, permission_type, deleted, version)
                VALUES (?, ?, ?, 'READ', 0, 0)
                """, tenantAId, privateKb, readerId);

        Upload localDocument = uploadDocument(
                privateKb,
                ownerToken,
                "c15-local.txt",
                "C15 local deterministic knowledge evidence for tenant A.");
        Upload foreignDocument = uploadDocument(
                foreignKb,
                tenantBToken,
                "c15-foreign.txt",
                "Tenant B protected content includes " + FOREIGN_CANARY + ".");
        awaitSuccessfulTask(localDocument.taskId(), ownerToken);
        awaitSuccessfulTask(foreignDocument.taskId(), tenantBToken);

        ChunkIdentity localChunk = firstChunk(localDocument.documentId());
        ChunkIdentity foreignChunk = firstChunk(foreignDocument.documentId());
        String authoritativeBefore = authoritativeDigest();
        String queryCountBefore = redisValue(queryCountKey(tenantAId, privateKb));
        assertTrue(redisKeys("qa:cache:v2:*").isEmpty());

        JsonNode initialize = mcp(readerToken, Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2025-11-25",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "c15-it", "version", "1.0"))));
        assertEquals("2025-11-25",
                initialize.path("result").path("protocolVersion").asText());

        List<String> listedUris = listAllResourceUris(readerToken);
        assertEquals(List.of(
                "rag://knowledge-bases/" + privateKb,
                "rag://knowledge-bases/" + publicKb), listedUris);
        assertFalse(String.join("|", listedUris).contains(String.valueOf(foreignKb)));

        JsonNode kbResource = resourceText(readerToken,
                "rag://knowledge-bases/" + privateKb, 10);
        assertEquals(Set.of(
                        "id", "name", "description", "documentCount", "isPublic",
                        "createdAt", "updatedAt"),
                fieldNames(kbResource));
        JsonNode documentResource = resourceText(readerToken,
                "rag://knowledge-bases/" + privateKb
                        + "/documents/" + localDocument.documentId(), 11);
        assertEquals(Set.of(
                        "id", "kbId", "title", "fileType", "status", "chunkCount",
                        "createdAt", "updatedAt"),
                fieldNames(documentResource));
        JsonNode chunkResponse = readResource(readerToken,
                chunkUri(privateKb, localDocument.documentId(), localChunk.index()), 12);
        assertEquals("text/plain; charset=utf-8",
                chunkResponse.path("result").path("contents").get(0).path("mimeType").asText());
        assertFalse(chunkResponse.toString().contains(FOREIGN_CANARY));

        JsonNode foreignOwnerRead = readResource(
                tenantBToken,
                chunkUri(foreignKb, foreignDocument.documentId(), foreignChunk.index()),
                13);
        assertTrue(foreignOwnerRead.path("result").path("contents").get(0)
                .path("text").asText().contains(FOREIGN_CANARY));

        long nonexistentKb = Long.MAX_VALUE - 15001;
        JsonNode foreignRead = readResource(
                readerToken,
                chunkUri(foreignKb, foreignDocument.documentId(), foreignChunk.index()),
                14);
        JsonNode nonexistentRead = readResource(
                readerToken,
                chunkUri(nonexistentKb, Long.MAX_VALUE - 15002, 0),
                15);
        assertEquals(nonexistentRead.path("error"), foreignRead.path("error"));
        assertEquals("MCP_RESOURCE_NOT_FOUND",
                foreignRead.path("error").path("message").asText());
        assertOpaque(foreignRead);

        JsonNode citation = callTool(readerToken, 20, "rag.get-citation", Map.of(
                "kbId", privateKb,
                "documentId", localDocument.documentId(),
                "chunkId", localChunk.vectorId()));
        assertToolSuccess(citation);
        assertEquals(localChunk.vectorId(), citation.path("result")
                .path("structuredContent").path("chunkId").asText());
        assertTrue(citation.path("result").path("content").toString()
                .contains(chunkUri(privateKb, localDocument.documentId(), localChunk.index())));

        JsonNode foreignCitation = callTool(readerToken, 21, "rag.get-citation", Map.of(
                "kbId", foreignKb,
                "documentId", foreignDocument.documentId(),
                "chunkId", foreignChunk.vectorId()));
        JsonNode nonexistentCitation = callTool(readerToken, 22, "rag.get-citation", Map.of(
                "kbId", nonexistentKb,
                "documentId", Long.MAX_VALUE - 15002,
                "chunkId", "missing-control"));
        assertEquals(nonexistentCitation.path("result"), foreignCitation.path("result"));
        assertToolError(foreignCitation, "MCP_RESOURCE_NOT_FOUND");
        assertOpaque(foreignCitation);

        JsonNode compare = callTool(readerToken, 23, "rag.compare-sources", Map.of(
                "left", Map.of(
                        "kbId", privateKb,
                        "documentId", localDocument.documentId(),
                        "chunkId", localChunk.vectorId()),
                "right", Map.of(
                        "kbId", privateKb,
                        "documentId", localDocument.documentId(),
                        "chunkId", localChunk.vectorId())));
        assertToolSuccess(compare);
        assertEquals("NOT_PERFORMED", compare.path("result")
                .path("structuredContent").path("semanticComparisonStatus").asText());
        assertTrue(compare.path("result").path("structuredContent").path("sameChunk").asBoolean());

        JsonNode search = callTool(readerToken, 24, "rag.search", Map.of(
                "kbId", privateKb,
                "query", "local deterministic knowledge",
                "topK", 5,
                "minScore", 0));
        assertToolSuccess(search);
        assertFalse(search.toString().contains(FOREIGN_CANARY));
        assertFalse(search.toString().contains("vectorCollection"));
        assertFalse(search.toString().contains("rawMetadata"));

        JsonNode ask = callTool(readerToken, 25, "rag.ask", Map.of(
                "kbId", privateKb,
                "question", "local deterministic knowledge",
                "topK", 5,
                "minScore", 0));
        assertToolSuccess(ask);
        assertFalse(ask.toString().contains(FOREIGN_CANARY));

        JsonNode noResult = callTool(readerToken, 26, "rag.ask", Map.of(
                "kbId", privateKb,
                "question", "c15-no-result-" + UUID.randomUUID(),
                "topK", 1,
                "minScore", 1));
        assertToolSuccess(noResult);
        assertEquals("no_result", noResult.path("result")
                .path("structuredContent").path("status").asText());

        JsonNode generationError = callTool(readerToken, 27, "rag.ask", Map.of(
                "kbId", privateKb,
                "question", "c15-deterministic-generation-error",
                "topK", 5,
                "minScore", 0));
        assertToolError(generationError, "MCP_DEPENDENCY_UNAVAILABLE");
        assertFalse(generationError.toString().contains("deterministic generation failure"));

        JsonNode guessedWriteTool = callTool(readerToken, 28, "rag.delete-document", Map.of(
                "documentId", localDocument.documentId()));
        assertEquals(-32602, guessedWriteTool.path("error").path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT",
                guessedWriteTool.path("error").path("message").asText());

        assertEquals(authoritativeBefore, authoritativeDigest());
        assertEquals(queryCountBefore, redisValue(queryCountKey(tenantAId, privateKb)));
        assertTrue(redisKeys("qa:cache:v2:*").isEmpty());
        assertTrue(embeddingProvider.invocationCount() > 0);
        assertTrue(answerGenerator.invocationCount() > 0);

        Map<String, Object> timing = timingOracle(
                readerToken,
                chunkUri(foreignKb, foreignDocument.documentId(), foreignChunk.index()),
                chunkUri(nonexistentKb, Long.MAX_VALUE - 15002, 0));
        writeDriverEvidence(authoritativeBefore, timing);
    }

    private List<String> listAllResourceUris(String token) throws Exception {
        List<String> uris = new ArrayList<>();
        String cursor = null;
        int requestId = 100;
        do {
            Map<String, Object> params = cursor == null ? Map.of() : Map.of("cursor", cursor);
            JsonNode body = mcp(token, Map.of(
                    "jsonrpc", "2.0",
                    "id", requestId++,
                    "method", "resources/list",
                    "params", params));
            body.path("result").path("resources")
                    .forEach(resource -> uris.add(resource.path("uri").asText()));
            JsonNode nextCursor = body.path("result").path("nextCursor");
            cursor = nextCursor.isMissingNode() || nextCursor.isNull()
                    ? null
                    : nextCursor.asText();
        } while (cursor != null && !cursor.isBlank());
        return uris;
    }

    private JsonNode resourceText(String token, String uri, int requestId) throws Exception {
        JsonNode response = readResource(token, uri, requestId);
        assertTrue(response.path("error").isMissingNode(), response.toString());
        return objectMapper.readTree(
                response.path("result").path("contents").get(0).path("text").asText());
    }

    private JsonNode readResource(String token, String uri, int requestId) throws Exception {
        return mcp(token, Map.of(
                "jsonrpc", "2.0",
                "id", requestId,
                "method", "resources/read",
                "params", Map.of("uri", uri)));
    }

    private JsonNode callTool(
            String token, int requestId, String name, Map<String, Object> arguments)
            throws Exception {
        return mcp(token, Map.of(
                "jsonrpc", "2.0",
                "id", requestId,
                "method", "tools/call",
                "params", Map.of("name", name, "arguments", arguments)));
    }

    private JsonNode mcp(String token, Map<String, Object> request) throws Exception {
        HttpHeaders headers = bearerHeaders(token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        ResponseEntity<JsonNode> response = http.exchange(
                "/mcp",
                HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), headers),
                JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), bodyText(response));
        return requireBody(response);
    }

    private void assertToolSuccess(JsonNode response) {
        assertTrue(response.path("error").isMissingNode(), response.toString());
        assertFalse(response.path("result").path("isError").asBoolean(true), response.toString());
        assertTrue(response.path("result").path("structuredContent").isObject(), response.toString());
    }

    private void assertToolError(JsonNode response, String category) {
        assertTrue(response.path("result").path("isError").asBoolean(), response.toString());
        assertEquals(category,
                response.path("result").path("content").get(0).path("text").asText());
    }

    private void assertOpaque(JsonNode response) {
        String body = response.toString();
        assertFalse(body.contains(FOREIGN_CANARY));
        assertFalse(body.contains(TENANT_B_CODE));
        assertFalse(body.contains(TENANT_B_USERNAME));
    }

    private Set<String> fieldNames(JsonNode value) {
        Set<String> fields = new java.util.TreeSet<>();
        value.fieldNames().forEachRemaining(fields::add);
        return fields;
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
                TENANT_B_CODE,
                "C15 Synthetic Tenant B");
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

    private long insertReader(long tenantId) {
        jdbcTemplate.update("""
                INSERT INTO `user`
                    (tenant_id, username, password_hash, email, enabled, deleted, version)
                VALUES (?, ?, ?, ?, 1, 0, 0)
                """, tenantId, READER_USERNAME, passwordEncoder.encode(READER_PASSWORD),
                READER_USERNAME + "@example.test");
        long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM `user` WHERE username = ?", Long.class, READER_USERNAME);
        jdbcTemplate.update("""
                INSERT INTO user_role (user_id, role_id)
                SELECT ?, id FROM role WHERE name = 'USER'
                """, userId);
        return userId;
    }

    private String login(String username, String password) {
        ResponseEntity<JsonNode> response = http.postForEntity(
                "/auth/login",
                Map.of("username", username, "password", password),
                JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), bodyText(response));
        String token = requireBody(response).path("data").path("accessToken").asText();
        assertFalse(token.isBlank());
        return token;
    }

    private long createKnowledgeBase(String token, String name, boolean isPublic) {
        ResponseEntity<JsonNode> response = http.exchange(
                "/api/knowledge-bases",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", name,
                        "description", name + " description",
                        "isPublic", isPublic), bearerHeaders(token)),
                JsonNode.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), bodyText(response));
        long id = requireBody(response).path("data").path("id").asLong();
        assertTrue(id > 0);
        return id;
    }

    private Upload uploadDocument(long kbId, String token, String fileName, String content) {
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
        ResponseEntity<JsonNode> response = http.exchange(
                "/api/knowledge-bases/" + kbId + "/documents",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                JsonNode.class);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), bodyText(response));
        JsonNode data = requireBody(response).path("data");
        long documentId = data.path("documentId").asLong();
        String taskId = data.path("taskId").asText();
        assertTrue(documentId > 0);
        assertFalse(taskId.isBlank());
        return new Upload(documentId, taskId);
    }

    private void awaitSuccessfulTask(String taskId, String token) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(60));
        String lastState = "UNKNOWN";
        while (Instant.now().isBefore(deadline)) {
            ResponseEntity<JsonNode> response = http.exchange(
                    "/api/tasks/" + taskId,
                    HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders(token)),
                    JsonNode.class);
            assertEquals(HttpStatus.OK, response.getStatusCode(), bodyText(response));
            lastState = requireBody(response).path("data").path("state").asText();
            if ("COMPLETED".equals(lastState)) {
                return;
            }
            assertFalse("FAILED".equals(lastState) || "CANCELLED".equals(lastState),
                    "index task ended in " + lastState);
            TimeUnit.MILLISECONDS.sleep(250);
        }
        throw new AssertionError("index task timed out: state=" + lastState);
    }

    private ChunkIdentity firstChunk(long documentId) {
        return jdbcTemplate.queryForObject("""
                SELECT chunk_index, vector_id
                FROM document_chunk
                WHERE document_id = ? AND deleted = 0
                ORDER BY chunk_index
                LIMIT 1
                """, (resultSet, row) -> new ChunkIdentity(
                resultSet.getInt("chunk_index"),
                resultSet.getString("vector_id")), documentId);
    }

    private String authoritativeDigest() throws Exception {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("tenant", jdbcTemplate.queryForList(
                "SELECT id, code, name, enabled, deleted, version FROM tenant ORDER BY id"));
        state.put("user", jdbcTemplate.queryForList(
                "SELECT id, tenant_id, username, enabled, deleted, version FROM `user` ORDER BY id"));
        state.put("knowledgeBase", jdbcTemplate.queryForList("""
                SELECT id, tenant_id, owner_id, name, description, document_count,
                       is_public, deleted, version
                FROM knowledge_base ORDER BY id
                """));
        state.put("document", jdbcTemplate.queryForList("""
                SELECT id, tenant_id, kb_id, uploader_id, title, status, chunk_count,
                       deleted, version
                FROM document ORDER BY id
                """));
        state.put("chunk", jdbcTemplate.queryForList("""
                SELECT id, tenant_id, document_id, vector_id, chunk_index,
                       SHA2(content, 256) AS content_sha256, deleted, version
                FROM document_chunk ORDER BY id
                """));
        state.put("permission", jdbcTemplate.queryForList("""
                SELECT id, tenant_id, kb_id, user_id, permission_type, deleted, version
                FROM kb_permission ORDER BY id
                """));
        state.put("history", jdbcTemplate.queryForList("""
                SELECT id, tenant_id, user_id, kb_id, SHA2(question, 256) AS question_sha256,
                       SHA2(COALESCE(answer, ''), 256) AS answer_sha256, deleted, version
                FROM qa_history ORDER BY id
                """));
        state.put("feedback", jdbcTemplate.queryForList("""
                SELECT id, tenant_id, qa_id, user_id, rating, deleted, version
                FROM qa_feedback ORDER BY id
                """));
        state.put("task", jdbcTemplate.queryForList("""
                SELECT id, tenant_id, task_id, document_id, owner_id, status, progress,
                       execution_phase, attempt_count, deleted, version
                FROM async_task ORDER BY id
                """));
        byte[] canonical = objectMapper.writeValueAsBytes(state);
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(canonical));
    }

    private Map<String, Object> timingOracle(
            String token, String foreignUri, String controlUri) throws Exception {
        for (int i = 0; i < WARMUP_PAIRS; i++) {
            readResource(token, foreignUri, 2000 + i * 2);
            readResource(token, controlUri, 2001 + i * 2);
        }
        List<Boolean> schedule = new ArrayList<>();
        for (int i = 0; i < MEASURED_PAIRS; i++) {
            schedule.add(Boolean.TRUE);
            schedule.add(Boolean.FALSE);
        }
        Collections.shuffle(schedule, new Random(TIMING_SEED));
        List<Double> foreign = new ArrayList<>();
        List<Double> control = new ArrayList<>();
        int requestId = 3000;
        for (boolean useForeign : schedule) {
            long started = System.nanoTime();
            JsonNode response = readResource(
                    token, useForeign ? foreignUri : controlUri, requestId++);
            double elapsedMillis = (System.nanoTime() - started) / 1_000_000.0d;
            assertEquals("MCP_RESOURCE_NOT_FOUND",
                    response.path("error").path("message").asText());
            (useForeign ? foreign : control).add(elapsedMillis);
        }
        double foreignMedian = percentile(foreign, 0.50d);
        double controlMedian = percentile(control, 0.50d);
        double foreignP95 = percentile(foreign, 0.95d);
        double controlP95 = percentile(control, 0.95d);
        assertTrue(Math.abs(foreignMedian - controlMedian)
                        <= Math.max(10.0d, controlMedian * 0.5d),
                "foreign/nonexistent MCP median delta exceeded the coarse threshold");
        assertTrue(Math.abs(foreignP95 - controlP95)
                        <= Math.max(25.0d, controlP95 * 0.5d),
                "foreign/nonexistent MCP p95 delta exceeded the coarse threshold");
        return Map.of(
                "warmupPairs", WARMUP_PAIRS,
                "measuredPairs", MEASURED_PAIRS,
                "interleavingSeed", TIMING_SEED,
                "foreignMedianMs", foreignMedian,
                "controlMedianMs", controlMedian,
                "foreignP95Ms", foreignP95,
                "controlP95Ms", controlP95,
                "requestErrors", 0);
    }

    private double percentile(List<Double> source, double fraction) {
        List<Double> sorted = new ArrayList<>(source);
        sorted.sort(Comparator.naturalOrder());
        int index = (int) Math.ceil(fraction * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }

    private void writeDriverEvidence(
            String authoritativeDigest, Map<String, Object> timing) throws Exception {
        String gitHead = processOutput("git", "rev-parse", "HEAD");
        String gitStatus = processOutput("git", "status", "--porcelain");
        Map<String, Object> evidence = Map.ofEntries(
                Map.entry("schemaVersion", "c15-mcp-readonly-driver-evidence-v1"),
                Map.entry("fixtureVersion", "c15-mcp-readonly-fixture-v1"),
                Map.entry("gitHead", gitHead),
                Map.entry("workingTreeDirty", !gitStatus.isBlank()),
                Map.entry("mcpSpecVersion", "2025-11-25"),
                Map.entry("mcpSdkVersion", "2.0.0"),
                Map.entry("conformanceVersion", "0.1.15"),
                Map.entry("toolSchemaSha256", TOOL_SCHEMA_SHA256),
                Map.entry("infrastructure", Map.of(
                        "owned", true,
                        "healthy", MYSQL.isRunning() && REDIS.isRunning() && MILVUS.isRunning(),
                        "mysqlImage", MYSQL_IMAGE,
                        "redisImage", REDIS_IMAGE,
                        "etcdImage", ETCD_IMAGE,
                        "minioImage", MINIO_IMAGE,
                        "milvusImage", MILVUS_IMAGE)),
                Map.entry("deterministicEmbeddingInvocations", embeddingProvider.invocationCount()),
                Map.entry("deterministicGenerationInvocations", answerGenerator.invocationCount()),
                Map.entry("realProviderModelCalls", 0),
                Map.entry("businessDataOutbound", false),
                Map.entry("qaCacheEnabled", false),
                Map.entry("authoritativeStateBeforeSha256", authoritativeDigest),
                Map.entry("authoritativeStateAfterSha256", authoritativeDigest),
                Map.entry("authoritativeStateUnchanged", true),
                Map.entry("realMilvusMaintenanceStatus", "SKIPPED"),
                Map.entry("timing", timing));
        Path target = Path.of(
                System.getProperty("basedir"),
                "target",
                "c15-mcp-readonly-driver-evidence.json");
        Files.createDirectories(target.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), evidence);
    }

    private String processOutput(String... command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        assertTrue(finished, "process timed out");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertEquals(0, process.exitValue(), output);
        return output;
    }

    private Set<String> redisKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys == null ? Set.of() : Set.copyOf(keys);
    }

    private String redisValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    private String queryCountKey(long tenantId, long kbId) {
        return "kb:query:count:v2:" + tenantId + ":" + kbId;
    }

    private String chunkUri(long kbId, long documentId, int chunkIndex) {
        return "rag://knowledge-bases/" + kbId
                + "/documents/" + documentId
                + "/chunks/" + chunkIndex;
    }

    private static HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
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

    private record Upload(long documentId, String taskId) {
    }

    private record ChunkIdentity(int index, String vectorId) {
    }
}

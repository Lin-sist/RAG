package com.enterprise.rag.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@ActiveProfiles("c3-integration")
@Import(DeterministicEmbeddingTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HappyPathIT {

    private static final String MYSQL_IMAGE = "mysql:8.0.36";
    private static final String REDIS_IMAGE =
            "redis:7-alpine@sha256:8b81dd37ff027bec4e516d41acfbe9fe2460070dc6d4a4570a2ac5b9d59df065";
    private static final String ETCD_IMAGE = "quay.io/coreos/etcd:v3.5.5";
    private static final String MINIO_IMAGE = "minio/minio:RELEASE.2023-03-20T20-16-18Z";
    private static final String MILVUS_IMAGE = "milvusdb/milvus:v2.3.4";

    private static final String BOOTSTRAP_USERNAME = "admin";
    private static final String BOOTSTRAP_PASSWORD = "C3!" + UUID.randomUUID() + "xY9";
    private static final String REDIS_PASSWORD = "redis-" + UUID.randomUUID();
    private static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();

    private static final Network MILVUS_NETWORK = Network.newNetwork();

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName("rag_c3_happy_path")
            .withUsername("rag_c3")
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
    private DeterministicEmbeddingTestConfig.DeterministicEmbeddingProvider embeddingProvider;

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
    void authenticatedUserCanCompleteDocumentLifecycle() throws InterruptedException {
        ResponseEntity<JsonNode> login = http.postForEntity(
                "/auth/login",
                Map.of("username", BOOTSTRAP_USERNAME, "password", BOOTSTRAP_PASSWORD),
                JsonNode.class);

        assertEquals(HttpStatus.OK, login.getStatusCode());
        JsonNode loginBody = requireSuccessBody(login);
        String accessToken = loginBody.path("data").path("accessToken").asText();
        assertFalse(accessToken.isBlank());

        HttpHeaders headers = bearerHeaders(accessToken);
        ResponseEntity<JsonNode> create = http.exchange(
                "/api/knowledge-bases",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "c3-happy-path-" + UUID.randomUUID(),
                        "description", "C3 isolated integration fixture",
                        "isPublic", false), headers),
                JsonNode.class);

        assertEquals(HttpStatus.CREATED, create.getStatusCode());
        JsonNode createBody = requireSuccessBody(create);
        long kbId = createBody.path("data").path("id").asLong();
        assertTrue(kbId > 0);
        assertFalse(createBody.path("data").path("vectorCollection").asText().isBlank());

        String uniqueToken = "c3_unique_" + UUID.randomUUID().toString().replace("-", "");
        Upload target = uploadDocument(
                kbId,
                accessToken,
                "target.txt",
                "The integration target contains " + uniqueToken
                        + ". This fact proves the isolated retrieval path works.");
        Upload distractor = uploadDocument(
                kbId,
                accessToken,
                "distractor.txt",
                "The distractor discusses a separate subject without the integration marker.");

        awaitSuccessfulTask(target.taskId(), accessToken);
        awaitSuccessfulTask(distractor.taskId(), accessToken);

        ResponseEntity<JsonNode> documents = http.exchange(
                "/api/knowledge-bases/" + kbId + "/documents",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class);
        JsonNode documentBody = requireSuccessBody(documents);
        List<JsonNode> indexedDocuments = new ArrayList<>();
        documentBody.path("data").forEach(indexedDocuments::add);
        assertEquals(2, indexedDocuments.size(), documentBody.toString());
        assertTrue(indexedDocuments.stream().allMatch(document -> "COMPLETED".equals(document.path("status").asText())));
        assertTrue(indexedDocuments.stream().allMatch(document -> document.path("chunkCount").asInt() > 0));

        JsonNode retrieval = retrieve(kbId, accessToken, uniqueToken);
        assertEquals("ok", retrieval.path("status").asText(), retrieval.toString());
        assertTrue(retrieval.path("contextCount").asInt() > 0, retrieval.toString());
        assertEquals(target.documentId(), retrieval.path("contexts").path(0).path("documentId").asLong(),
                retrieval.toString());
        assertTrue(embeddingProvider.invocationCount() >= 3,
                "deterministic provider must serve both document indexing and query embedding");

        deleteDocument(kbId, target.documentId(), accessToken);
        awaitDocumentAbsentFromRetrieval(kbId, target.documentId(), accessToken, uniqueToken);
        deleteDocument(kbId, distractor.documentId(), accessToken);

        ResponseEntity<JsonNode> deleteKb = http.exchange(
                "/api/knowledge-bases/" + kbId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                JsonNode.class);
        assertEquals(HttpStatus.OK, deleteKb.getStatusCode());
        requireSuccessBody(deleteKb);

        ResponseEntity<JsonNode> deletedKb = http.exchange(
                "/api/knowledge-bases/" + kbId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class);
        assertFalse(deletedKb.getStatusCode().is2xxSuccessful(), deletedKb.toString());
    }

    private Upload uploadDocument(long kbId, String accessToken, String fileName, String content) {
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
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        ResponseEntity<JsonNode> upload = http.exchange(
                "/api/knowledge-bases/" + kbId + "/documents",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                JsonNode.class);

        assertEquals(HttpStatus.ACCEPTED, upload.getStatusCode(), String.valueOf(upload.getBody()));
        JsonNode uploadBody = requireSuccessBody(upload);
        long documentId = uploadBody.path("data").path("documentId").asLong();
        String taskId = uploadBody.path("data").path("taskId").asText();
        assertTrue(documentId > 0);
        assertFalse(taskId.isBlank());
        return new Upload(documentId, taskId);
    }

    private void awaitSuccessfulTask(String taskId, String accessToken) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(60));
        String lastState = "UNKNOWN";
        String lastError = null;
        while (Instant.now().isBefore(deadline)) {
            ResponseEntity<JsonNode> response = http.exchange(
                    "/api/tasks/" + taskId,
                    HttpMethod.GET,
                    new HttpEntity<>(bearerHeaders(accessToken)),
                    JsonNode.class);
            JsonNode data = requireSuccessBody(response).path("data");
            lastState = data.path("state").asText();
            lastError = data.path("error").asText(null);
            if ("COMPLETED".equals(lastState)) {
                return;
            }
            if ("FAILED".equals(lastState) || "CANCELLED".equals(lastState)) {
                fail("index task ended in " + lastState + ", error=" + lastError);
            }
            TimeUnit.MILLISECONDS.sleep(250);
        }
        fail("index task timed out: taskId=" + taskId + ", lastState=" + lastState + ", error=" + lastError);
    }

    private JsonNode retrieve(long kbId, String accessToken, String question) {
        ResponseEntity<JsonNode> response = http.exchange(
                "/api/qa/debug/retrieve",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "kbId", kbId,
                        "question", question,
                        "topK", 5,
                        "minScore", 0.0,
                        "filter", Map.of(),
                        "enableRerank", false), bearerHeaders(accessToken)),
                JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return requireSuccessBody(response).path("data");
    }

    private void deleteDocument(long kbId, long documentId, String accessToken) {
        ResponseEntity<JsonNode> response = http.exchange(
                "/api/knowledge-bases/" + kbId + "/documents/" + documentId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(accessToken)),
                JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), String.valueOf(response.getBody()));
        requireSuccessBody(response);
    }

    private void awaitDocumentAbsentFromRetrieval(
            long kbId,
            long deletedDocumentId,
            String accessToken,
            String question) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        JsonNode lastRetrieval = null;
        while (Instant.now().isBefore(deadline)) {
            lastRetrieval = retrieve(kbId, accessToken, question);
            boolean stillPresent = false;
            for (JsonNode context : lastRetrieval.path("contexts")) {
                if (context.path("documentId").asLong() == deletedDocumentId) {
                    stillPresent = true;
                    break;
                }
            }
            if (!stillPresent) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(250);
        }
        fail("deleted document remained retrievable: " + lastRetrieval);
    }

    private static HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static JsonNode requireSuccessBody(ResponseEntity<JsonNode> response) {
        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals(200, body.path("code").asInt(), body.toString());
        return body;
    }

    private record Upload(long documentId, String taskId) {
    }
}

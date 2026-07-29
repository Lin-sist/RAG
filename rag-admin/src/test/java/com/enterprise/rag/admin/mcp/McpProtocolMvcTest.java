package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = McpProtocolMvcTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rag.mcp.enabled=true",
                "rag.mcp.local-only=false",
                "rag.mcp.allowed-origins=https://trusted.example",
                "rag.mcp.resource-page-size=2"
        })
class McpProtocolMvcTest {

    private static final String INITIALIZE_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "method": "initialize",
              "params": {
                "protocolVersion": "2025-11-25",
                "capabilities": {},
                "clientInfo": {
                  "name": "c15-transport-test",
                  "version": "1.0"
                }
              }
            }
            """;

    private static final String LIST_RESOURCE_TEMPLATES_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 2,
              "method": "resources/templates/list",
              "params": {}
            }
            """;

    private static final String LIST_RESOURCES_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 3,
              "method": "resources/list",
              "params": {}
            }
            """;

    private static final String READ_KNOWLEDGE_BASE_RESOURCE_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 7,
              "method": "resources/read",
              "params": {"uri": "rag://knowledge-bases/1"}
            }
            """;

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enabledSessionlessTransportNegotiatesTargetProtocol() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertFalse(response.headers().firstValue("MCP-Session-Id").isPresent());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("2.0", body.path("jsonrpc").asText());
        assertEquals(1, body.path("id").asInt());
        assertEquals("2025-11-25", body.path("result").path("protocolVersion").asText());
        assertEquals("enterprise-rag-readonly",
                body.path("result").path("serverInfo").path("name").asText());
        assertEquals("c15-v1",
                body.path("result").path("serverInfo").path("version").asText());
        assertFalse(body.path("result").path("capabilities").path("resources").isMissingNode());
        assertFalse(body.path("result").path("capabilities").path("tools").isMissingNode());
    }

    @Test
    void statelessTransportRejectsGetWithoutCreatingASession() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        assertFalse(response.headers().firstValue("MCP-Session-Id").isPresent());
    }

    @Test
    void invalidOriginIsRejectedBeforeProtocolHandling() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Origin", "https://attacker.example")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
        assertTrue(response.body().contains("MCP_FORBIDDEN"));
    }

    @Test
    void authenticatedClientDiscoversExactlyThreeCanonicalResourceTemplates() throws Exception {
        HttpResponse<String> response = postJson(LIST_RESOURCE_TEMPLATES_REQUEST);

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(2, body.path("id").asInt());
        List<String> uriTemplates = body.path("result").path("resourceTemplates")
                .findValuesAsText("uriTemplate");
        assertEquals(Set.of(
                "rag://knowledge-bases/{kbId}",
                "rag://knowledge-bases/{kbId}/documents/{documentId}",
                "rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}"),
                Set.copyOf(uriTemplates));
        assertEquals(3, uriTemplates.size());
        assertTrue(body.path("result").path("nextCursor").isMissingNode()
                || body.path("result").path("nextCursor").isNull());
    }

    @Test
    void resourcesListReauthorizesAnOpaqueCursorForEveryRequestIdentity() throws Exception {
        HttpResponse<String> tenantAResponse = postJson(
                LIST_RESOURCES_REQUEST, "tenant-a");

        assertEquals(200, tenantAResponse.statusCode());
        JsonNode tenantABody = objectMapper.readTree(tenantAResponse.body());
        assertEquals(List.of(
                        "rag://knowledge-bases/1",
                        "rag://knowledge-bases/3"),
                tenantABody.path("result").path("resources")
                        .findValuesAsText("uri"));
        String cursor = tenantABody.path("result").path("nextCursor").asText();
        assertFalse(cursor.isBlank());

        String tenantBRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": 4,
                  "method": "resources/list",
                  "params": {"cursor": "%s"}
                }
                """.formatted(cursor);
        HttpResponse<String> tenantBResponse = postJson(
                tenantBRequest, "tenant-b");

        assertEquals(200, tenantBResponse.statusCode());
        JsonNode tenantBBody = objectMapper.readTree(tenantBResponse.body());
        assertEquals(List.of(
                        "rag://knowledge-bases/4",
                        "rag://knowledge-bases/6"),
                tenantBBody.path("result").path("resources")
                        .findValuesAsText("uri"));
        assertFalse(tenantBResponse.body().contains("tenant-a-next-canary"));
    }

    @Test
    void resourcesListRejectsReservedOrMalformedPaginationInputWithoutEchoingIt() throws Exception {
        String marker = "foreign-cursor-marker";
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 5,
                  "method": "resources/list",
                  "params": {
                    "cursor": "%s",
                    "tenantId": 2
                  }
                }
                """.formatted(marker);

        HttpResponse<String> response = postJson(request, "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(-32602, body.path("error").path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT", body.path("error").path("message").asText());
        assertFalse(response.body().contains(marker));
        assertFalse(response.body().contains("tenantId"));
    }

    @Test
    void resourcesListRejectsANonCanonicalCursorAsInvalidArgument() throws Exception {
        String marker = "not-a-canonical-cursor";
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 6,
                  "method": "resources/list",
                  "params": {"cursor": "%s"}
                }
                """.formatted(marker);

        HttpResponse<String> response = postJson(request, "tenant-a");

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(-32602, body.path("error").path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT", body.path("error").path("message").asText());
        assertFalse(response.body().contains(marker));
    }

    @Test
    void resourcesReadReturnsOnlyTheAuthorizedKnowledgeBaseJsonWhitelist() throws Exception {
        HttpResponse<String> response = postJson(
                READ_KNOWLEDGE_BASE_RESOURCE_REQUEST, "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(7, body.path("id").asInt());
        assertTrue(body.path("error").isMissingNode(), response.body());
        JsonNode resource = body.path("result").path("contents").get(0);
        assertEquals("rag://knowledge-bases/1", resource.path("uri").asText());
        assertEquals("application/json", resource.path("mimeType").asText());

        JsonNode content = objectMapper.readTree(resource.path("text").asText());
        Set<String> fieldNames = new HashSet<>();
        content.fieldNames().forEachRemaining(fieldNames::add);
        assertEquals(Set.of(
                "id",
                "name",
                "description",
                "documentCount",
                "isPublic",
                "createdAt",
                "updatedAt"), fieldNames);
        assertEquals(1L, content.path("id").asLong());
        assertEquals("tenant-a-one", content.path("name").asText());
        assertEquals("tenant-a-one description", content.path("description").asText());
        assertEquals(2, content.path("documentCount").asInt());
        assertFalse(content.path("isPublic").asBoolean());
        assertEquals("2026-07-28T09:15:00", content.path("createdAt").asText());
        assertEquals("2026-07-29T10:30:00", content.path("updatedAt").asText());
        assertFalse(response.body().contains("ownerId"));
        assertFalse(response.body().contains("vectorCollection"));
        assertFalse(response.body().contains("tenantId"));
    }

    @Test
    void resourcesReadMakesForeignAndNonexistentKnowledgeBasesIndistinguishable() throws Exception {
        HttpResponse<String> foreign = postJson(
                readKnowledgeBaseRequest(8, 1L), "tenant-b");
        HttpResponse<String> nonexistent = postJson(
                readKnowledgeBaseRequest(8, 99L), "tenant-a");

        assertEquals(200, foreign.statusCode());
        assertEquals(200, nonexistent.statusCode());
        JsonNode foreignError = objectMapper.readTree(foreign.body()).path("error");
        JsonNode nonexistentError = objectMapper.readTree(nonexistent.body()).path("error");
        assertEquals(nonexistentError, foreignError);
        assertEquals(-32603, foreignError.path("code").asInt());
        assertEquals("MCP_RESOURCE_NOT_FOUND", foreignError.path("message").asText());
        assertFalse(foreign.body().contains("tenant-a-one"));
        assertFalse(foreign.body().contains("知识库"));
        assertFalse(nonexistent.body().contains("知识库"));
    }

    @Test
    void resourcesReadReturnsAStableForbiddenErrorForSameTenantPrivateKnowledgeBase()
            throws Exception {
        HttpResponse<String> response = postJson(
                readKnowledgeBaseRequest(9, 9L), "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body()).path("error");
        assertEquals(-32603, error.path("code").asInt());
        assertEquals("MCP_FORBIDDEN", error.path("message").asText());
        assertFalse(response.body().contains("无权"));
        assertFalse(response.body().contains("ownerId"));
    }

    @Test
    void resourcesReadRejectsClientSuppliedTenantSelectors() throws Exception {
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 10,
                  "method": "resources/read",
                  "params": {
                    "uri": "rag://knowledge-bases/1",
                    "tenantId": 2
                  }
                }
                """;

        HttpResponse<String> response = postJson(request, "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(-32602, body.path("error").path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT", body.path("error").path("message").asText());
        assertTrue(body.path("result").isMissingNode());
        assertFalse(response.body().contains("tenantId"));
    }

    @Test
    void resourcesReadSanitizesUnexpectedDependencyFailures() throws Exception {
        String canary = "sql-resource-read-canary";

        HttpResponse<String> response = postJson(
                readKnowledgeBaseRequest(11, 77L), "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body()).path("error");
        assertEquals(-32603, error.path("code").asInt());
        assertEquals("MCP_INTERNAL_ERROR", error.path("message").asText());
        assertFalse(response.body().contains(canary));
    }

    @Test
    void resourcesReadRejectsANonCanonicalUriWithoutEchoingIt() throws Exception {
        String marker = "query-resource-canary";
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 12,
                  "method": "resources/read",
                  "params": {
                    "uri": "rag://knowledge-bases/1?marker=%s"
                  }
                }
                """.formatted(marker);

        HttpResponse<String> response = postJson(request, "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body()).path("error");
        assertEquals(-32603, error.path("code").asInt());
        assertEquals("MCP_RESOURCE_NOT_FOUND", error.path("message").asText());
        assertFalse(response.body().contains(marker));
    }

    @Test
    void resourcesReadKeepsDocumentAndChunkTemplatesFailClosedInThisSlice()
            throws Exception {
        String documentRequest = readResourceRequest(
                13, "rag://knowledge-bases/1/documents/2");
        String chunkRequest = readResourceRequest(
                14, "rag://knowledge-bases/1/documents/2/chunks/0");

        JsonNode documentError = objectMapper.readTree(
                postJson(documentRequest, "tenant-a").body()).path("error");
        JsonNode chunkError = objectMapper.readTree(
                postJson(chunkRequest, "tenant-a").body()).path("error");

        assertEquals("MCP_RESOURCE_NOT_FOUND", documentError.path("message").asText());
        assertEquals("MCP_RESOURCE_NOT_FOUND", chunkError.path("message").asText());
    }

    private String readResourceRequest(int requestId, String uri) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "resources/read",
                  "params": {"uri": "%s"}
                }
                """.formatted(requestId, uri);
    }

    private String readKnowledgeBaseRequest(int requestId, long knowledgeBaseId) {
        return readResourceRequest(
                requestId, "rag://knowledge-bases/" + knowledgeBaseId);
    }

    private HttpResponse<String> postJson(String requestBody) throws Exception {
        return postJson(requestBody, null);
    }

    private HttpResponse<String> postJson(String requestBody, String testIdentity) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream");
        if (testIdentity != null) {
            builder.header("X-C15-Test-Identity", testIdentity);
        }
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            SecurityAutoConfiguration.class
    })
    @Import({McpServerConfiguration.class, CurrentUserService.class})
    static class TestApplication {

        @Bean
        FilterRegistrationBean<Filter> syntheticAuthenticatedPrincipalFilter() {
            Filter filter = (request, response, chain) -> {
                boolean tenantB = "tenant-b".equals(
                        ((HttpServletRequest) request).getHeader("X-C15-Test-Identity"));
                UserPrincipal principal = UserPrincipal.builder()
                        .id(tenantB ? 2L : 1L)
                        .tenantId(tenantB ? 2L : 1L)
                        .username(tenantB ? "tenant-b" : "tenant-a")
                        .enabled(true)
                        .roles(Set.of("USER"))
                        .build();
                Principal authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {
                        @Override
                        public Principal getUserPrincipal() {
                            return authentication;
                        }
                    }, response);
            };
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
            registration.setName("syntheticMcpPrincipalFilter");
            registration.addUrlPatterns(McpServerConfiguration.ENDPOINT);
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
            return registration;
        }

        @Bean
        KnowledgeBaseService syntheticKnowledgeBaseService() {
            KnowledgeBaseService service = org.mockito.Mockito.mock(KnowledgeBaseService.class);
            org.mockito.Mockito.when(service.getAccessibleByIdentity(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(invocation -> {
                        RequestIdentity identity = invocation.getArgument(0, RequestIdentity.class);
                        if (identity.tenantId() == 2L) {
                            return List.of(
                                    knowledgeBase(2L, "tenant-b-before-cursor"),
                                    knowledgeBase(4L, "tenant-b-four"),
                                    knowledgeBase(6L, "tenant-b-six"));
                        }
                        return List.of(
                                knowledgeBase(1L, "tenant-a-one"),
                                knowledgeBase(3L, "tenant-a-three"),
                                knowledgeBase(5L, "tenant-a-next-canary"));
                    });
            org.mockito.Mockito.when(service.getById(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.any(RequestIdentity.class)))
                    .thenAnswer(invocation -> {
                        long id = invocation.getArgument(0, Long.class);
                        RequestIdentity identity = invocation.getArgument(1, RequestIdentity.class);
                        if (identity.tenantId() == 1L && id == 77L) {
                            throw new IllegalStateException("sql-resource-read-canary");
                        }
                        if (identity.tenantId() != 1L || (id != 1L && id != 9L)) {
                            return Optional.empty();
                        }
                        return Optional.of(id == 1L
                                ? knowledgeBase(1L, "tenant-a-one")
                                : knowledgeBase(9L, "tenant-a-private"));
                    });
            return service;
        }

        @Bean
        AuthorizationService syntheticAuthorizationService(KnowledgeBaseService knowledgeBaseService) {
            return new AuthorizationService(
                    knowledgeBaseService,
                    org.mockito.Mockito.mock(KBPermissionService.class),
                    org.mockito.Mockito.mock(QAHistoryService.class));
        }

        private KnowledgeBaseDTO knowledgeBase(long id, String name) {
            return KnowledgeBaseDTO.builder()
                    .id(id)
                    .name(name)
                    .description(name + " description")
                    .ownerId(id == 1L ? 1L : 9000L + id)
                    .vectorCollection("vector-canary-" + id)
                    .documentCount(id == 1L ? 2 : 0)
                    .isPublic(false)
                    .createdAt(LocalDateTime.of(2026, 7, 28, 9, 15))
                    .updatedAt(LocalDateTime.of(2026, 7, 29, 10, 30))
                    .build();
        }
    }
}

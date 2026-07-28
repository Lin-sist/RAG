package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
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
import java.time.Duration;
import java.security.Principal;
import java.util.List;
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
            return service;
        }

        private KnowledgeBaseDTO knowledgeBase(long id, String name) {
            return KnowledgeBaseDTO.builder()
                    .id(id)
                    .name(name)
                    .description(name + " description")
                    .documentCount(0)
                    .isPublic(false)
                    .build();
        }
    }
}

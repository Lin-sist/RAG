package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.security.CurrentUserService;
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
                "rag.mcp.allowed-origins=https://trusted.example"
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
            UserPrincipal principal = UserPrincipal.builder()
                    .id(1L)
                    .tenantId(1L)
                    .username("c15-transport-test")
                    .enabled(true)
                    .roles(Set.of("USER"))
                    .build();
            Principal authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            Filter filter = (request, response, chain) -> chain.doFilter(
                    new HttpServletRequestWrapper((HttpServletRequest) request) {
                        @Override
                        public Principal getUserPrincipal() {
                            return authentication;
                        }
                    },
                    response);
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
            registration.setName("syntheticMcpPrincipalFilter");
            registration.addUrlPatterns(McpServerConfiguration.ENDPOINT);
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
            return registration;
        }
    }
}

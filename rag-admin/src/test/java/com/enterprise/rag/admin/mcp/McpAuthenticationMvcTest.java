package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.auth.config.SecurityConfig;
import com.enterprise.rag.auth.filter.JwtAuthenticationFilter;
import com.enterprise.rag.auth.handler.JwtAccessDeniedHandler;
import com.enterprise.rag.auth.handler.JwtAuthenticationEntryPoint;
import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.auth.service.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = McpAuthenticationMvcTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rag.mcp.enabled=true",
                "rag.mcp.local-only=false"
        })
class McpAuthenticationMvcTest {

    private static final String INITIALIZE_REQUEST = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{
              "protocolVersion":"2025-11-25","capabilities":{},
              "clientInfo":{"name":"unauthenticated-test","version":"1.0"}}}
            """;

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void missingBearerTokenIsRejectedBeforeCapabilityDiscovery() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("AUTH_001"));
        assertFalse(response.body().contains("enterprise-rag-readonly"));
    }

    @Test
    void expiredBearerTokenIsRejectedBeforeProtocolHandling() throws Exception {
        when(tokenProvider.isTokenValid("expired-token")).thenReturn(false);

        HttpRequest request = baseRequest("/mcp")
                .header("Authorization", "Bearer expired-token")
                .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST))
                .build();

        HttpResponse<String> response = send(request);

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("AUTH_001"));
        assertFalse(response.body().contains("enterprise-rag-readonly"));
        verify(tokenProvider).isTokenValid("expired-token");
    }

    @Test
    void cookieAndQueryTokensAreNotAcceptedAsBearerAuthentication() throws Exception {
        HttpRequest cookieRequest = baseRequest("/mcp")
                .header("Cookie", "access_token=cookie-token")
                .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST))
                .build();
        HttpRequest queryRequest = baseRequest("/mcp?access_token=query-token")
                .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST))
                .build();

        HttpResponse<String> cookieResponse = send(cookieRequest);
        HttpResponse<String> queryResponse = send(queryRequest);

        assertEquals(401, cookieResponse.statusCode());
        assertEquals(401, queryResponse.statusCode());
        assertTrue(cookieResponse.body().contains("AUTH_001"));
        assertTrue(queryResponse.body().contains("AUTH_001"));
        assertFalse(cookieResponse.body().contains("cookie-token"));
        assertFalse(queryResponse.body().contains("query-token"));
        verify(tokenProvider, never()).isTokenValid("cookie-token");
        verify(tokenProvider, never()).isTokenValid("query-token");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "resources/templates/list",
            "resources/list",
            "resources/read",
            "tools/list",
            "tools/call"
    })
    void everyDeclaredProtocolMethodRequiresFreshAuthentication(String method) throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\""
                + method + "\",\"params\":{}}";
        HttpRequest request = baseRequest("/mcp")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = send(request);

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("AUTH_001"));
        assertFalse(response.body().contains("jsonrpc"));
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream");
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @Import({
            McpServerConfiguration.class,
            CurrentUserService.class,
            SecurityConfig.class,
            SecuritySupport.class
    })
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class SecuritySupport {

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        TokenBlacklistService tokenBlacklistService() {
            return mock(TokenBlacklistService.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(
                JwtTokenProvider tokenProvider,
                TokenBlacklistService tokenBlacklistService) {
            return new JwtAuthenticationFilter(tokenProvider, tokenBlacklistService);
        }

        @Bean
        JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
            return new JwtAuthenticationEntryPoint(objectMapper);
        }

        @Bean
        JwtAccessDeniedHandler jwtAccessDeniedHandler(ObjectMapper objectMapper) {
            return new JwtAccessDeniedHandler(objectMapper);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return username -> {
                throw new UsernameNotFoundException("Synthetic test user does not exist");
            };
        }
    }
}

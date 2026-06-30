package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelRerankerTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldStayUnavailableByDefault() {
        RetrievalProperties properties = new RetrievalProperties();
        ModelReranker reranker = new ModelReranker(properties);

        assertFalse(reranker.available());
    }

    @Test
    void shouldBecomeAvailableWhenHealthCheckPasses() throws IOException {
        startServer();
        server.createContext("/health", exchange -> sendJson(exchange, 200, "{\"status\":\"ok\"}"));
        RetrievalProperties properties = configuredProperties();
        properties.getRerank().getModel().setHealthCheckEnabled(true);

        ModelReranker reranker = new ModelReranker(properties);

        assertTrue(reranker.available());
    }

    @Test
    void shouldRerankUsingProviderScores() throws IOException {
        startServer();
        AtomicReference<String> requestBody = new AtomicReference<>("");
        AtomicReference<String> authorization = new AtomicReference<>("");
        server.createContext("/rerank", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sendJson(exchange, 200, """
                    {
                      "results": [
                        {"index": 1, "relevance_score": 0.98},
                        {"index": 0, "relevance_score": 0.21}
                      ]
                    }
                    """);
        });
        RetrievalProperties properties = configuredProperties();
        properties.getRerank().getModel().setHealthCheckEnabled(false);
        ModelReranker reranker = new ModelReranker(properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first document", "first", 0.8f, Map.of("rank", 1)),
                new RetrievedContext("second document", "second", 0.7f, Map.of("rank", 2)));

        List<RetrievedContext> reranked = reranker.rerank("query text", contexts);

        assertEquals("second", reranked.get(0).source());
        assertEquals("first", reranked.get(1).source());
        assertEquals(0.98f, reranked.get(0).relevanceScore(), 0.0001f);
        assertEquals(0.7f, (Float) reranked.get(0).metadata().get("originalRelevanceScore"), 0.0001f);
        assertEquals("model", reranked.get(0).metadata().get("rerankProvider"));
        assertEquals("Bearer test-key", authorization.get());
        assertTrue(requestBody.get().contains("\"model\":\"test-reranker\""));
        assertTrue(requestBody.get().contains("\"query\":\"query text\""));
        assertTrue(requestBody.get().contains("first document"));
    }

    @Test
    void shouldThrowWhenProviderFailsSoRegistryCanFallback() throws IOException {
        startServer();
        server.createContext("/rerank", exchange -> sendJson(exchange, 500, "{\"error\":\"boom\"}"));
        RetrievalProperties properties = configuredProperties();
        properties.getRerank().getModel().setHealthCheckEnabled(false);
        ModelReranker reranker = new ModelReranker(properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first", "a", 0.8f, Map.of()),
                new RetrievedContext("second", "b", 0.7f, Map.of()));

        assertThrows(IllegalStateException.class, () -> reranker.rerank("query", contexts));
    }

    @Test
    void shouldBeUnavailableWhenHealthCheckFails() throws IOException {
        startServer();
        server.createContext("/health", exchange -> sendJson(exchange, 503, "{\"status\":\"down\"}"));
        RetrievalProperties properties = configuredProperties();
        properties.getRerank().getModel().setHealthCheckEnabled(true);

        ModelReranker reranker = new ModelReranker(properties);

        assertFalse(reranker.available());
    }

    private RetrievalProperties configuredProperties() {
        RetrievalProperties properties = new RetrievalProperties();
        RetrievalProperties.ModelReranker model = properties.getRerank().getModel();
        model.setEnabled(true);
        model.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        model.setEndpointPath("/rerank");
        model.setApiKey("test-key");
        model.setModel("test-reranker");
        model.setTimeoutMillis(1000);
        model.setHealthPath("/health");
        model.setHealthCacheMillis(0);
        return properties;
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}

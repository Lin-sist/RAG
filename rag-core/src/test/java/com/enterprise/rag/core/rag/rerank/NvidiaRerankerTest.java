package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NvidiaRerankerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldUseNvidiaRankingContractAndPreserveRetrievalScore() throws Exception {
        startServer();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/v1/ranking", exchange -> {
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sendJson(exchange, 200, """
                    {"rankings":[{"index":1,"logit":2.75},{"index":0,"logit":-0.25}]}
                    """);
        });

        RetrievalProperties properties = configuredProperties();
        properties.getRerank().setProvider("nvidia");
        NvidiaReranker reranker = new NvidiaReranker(properties);
        RerankerRegistry registry = new RerankerRegistry(List.of(new HeuristicReranker(), reranker), properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first passage", "first", 0.8f, Map.of("rank", 1)),
                new RetrievedContext("second passage", "second", 0.7f, Map.of("rank", 2)));

        RerankOutcome outcome = registry.rerankWithDiagnostics("query text", contexts);
        List<RetrievedContext> reranked = outcome.contexts();

        assertEquals("/v1/ranking", requestPath.get());
        JsonNode body = objectMapper.readTree(requestBody.get());
        assertEquals("nvidia/test-reranker", body.path("model").asText());
        assertEquals("query text", body.path("query").path("text").asText());
        assertEquals("first passage", body.path("passages").get(0).path("text").asText());
        assertEquals("second passage", body.path("passages").get(1).path("text").asText());
        assertEquals("NONE", body.path("truncate").asText());
        assertEquals("Bearer test-key", authorization.get());
        assertEquals("second", reranked.get(0).source());
        assertEquals(0.7f, reranked.get(0).relevanceScore());
        assertEquals(2.75d, reranked.get(0).metadata().get("rerankLogit"));
        assertEquals(1, reranked.get(0).metadata().get("rerankRank"));
        assertEquals("nvidia", reranked.get(0).metadata().get("rerankProvider"));
        assertEquals("nvidia", outcome.diagnostics().requestedProvider());
        assertEquals("nvidia", outcome.diagnostics().effectiveProvider());
        assertEquals(0, outcome.diagnostics().fallbackCount());
        assertEquals(1, outcome.diagnostics().modelCallCount());
        assertEquals(2, outcome.diagnostics().candidateCount());
        assertEquals(2, outcome.diagnostics().scoredCount());
        assertEquals(1.0d, outcome.diagnostics().coverage());
        assertEquals("nvidia-ranking-v1", outcome.diagnostics().protocol());
    }

    @Test
    void shouldNotCallNvidiaWhenProviderIsNotSelected() throws Exception {
        startServer();
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/ranking", exchange -> {
            calls.incrementAndGet();
            sendJson(exchange, 200, "{\"rankings\":[]}");
        });
        RetrievalProperties properties = configuredProperties();
        NvidiaReranker nvidia = new NvidiaReranker(properties);
        RerankerRegistry registry = new RerankerRegistry(List.of(new HeuristicReranker(), nvidia), properties);

        RerankOutcome outcome = registry.rerankWithDiagnostics(
                "query",
                List.of(new RetrievedContext("passage", "first", 0.8f, Map.of())));

        assertEquals("heuristic", outcome.diagnostics().effectiveProvider());
        assertEquals(0, outcome.diagnostics().modelCallCount());
        assertEquals(0, calls.get());
    }

    @Test
    void shouldRejectIncompleteRankingsAsOneFailedModelCall() throws Exception {
        startServer();
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/ranking", exchange -> {
            calls.incrementAndGet();
            sendJson(exchange, 200, """
                    {"rankings":[{"index":1,"logit":2.75}]}
                    """);
        });
        RetrievalProperties properties = configuredProperties();
        properties.getRerank().setProvider("nvidia");
        NvidiaReranker reranker = new NvidiaReranker(properties);
        RerankerRegistry registry = new RerankerRegistry(List.of(new HeuristicReranker(), reranker), properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first passage", "first", 0.8f, Map.of()),
                new RetrievedContext("second passage", "second", 0.7f, Map.of()));

        RerankOutcome outcome = registry.rerankWithDiagnostics("query text", contexts);

        assertEquals("heuristic", outcome.diagnostics().effectiveProvider());
        assertEquals(1, outcome.diagnostics().fallbackCount());
        assertEquals("incomplete_rankings", outcome.diagnostics().fallbackReason());
        assertEquals(1, outcome.diagnostics().modelCallCount());
        assertEquals("first", outcome.contexts().get(0).source());
        assertEquals(1, calls.get());
    }

    @Test
    void shouldRejectMalformedRankingsWithoutApplyingAnyPartialResult() throws Exception {
        startServer();
        AtomicReference<String> responseBody = new AtomicReference<>();
        server.createContext("/v1/ranking", exchange -> sendJson(exchange, 200, responseBody.get()));
        NvidiaReranker reranker = new NvidiaReranker(configuredProperties());
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first passage", "first", 0.8f, Map.of()),
                new RetrievedContext("second passage", "second", 0.7f, Map.of()));
        Map<String, String> invalidResponses = Map.of(
                "duplicate", "{\"rankings\":[{\"index\":0,\"logit\":2},{\"index\":0,\"logit\":1}]}",
                "out-of-range", "{\"rankings\":[{\"index\":0,\"logit\":2},{\"index\":2,\"logit\":1}]}",
                "missing-index", "{\"rankings\":[{\"index\":0,\"logit\":2},{\"logit\":1}]}",
                "missing-logit", "{\"rankings\":[{\"index\":0,\"logit\":2},{\"index\":1}]}",
                "infinite-logit", "{\"rankings\":[{\"index\":0,\"logit\":2},{\"index\":1,\"logit\":1e400}]}");

        for (Map.Entry<String, String> invalid : invalidResponses.entrySet()) {
            responseBody.set(invalid.getValue());
            RerankProviderException failure = assertThrows(
                    RerankProviderException.class,
                    () -> reranker.rerank("query text", contexts),
                    invalid.getKey());
            assertEquals("invalid_response", failure.reason(), invalid.getKey());
            assertEquals(1, failure.modelCallCount(), invalid.getKey());
        }
    }

    @Test
    void shouldRejectInvalidInputBeforeMakingHttpRequest() throws Exception {
        startServer();
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/v1/ranking", exchange -> {
            calls.incrementAndGet();
            sendJson(exchange, 200, "{\"rankings\":[]}");
        });
        RetrievalProperties properties = configuredProperties();
        properties.getRerank().getNvidia().setMaxCandidates(1);
        NvidiaReranker reranker = new NvidiaReranker(properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first passage", "first", 0.8f, Map.of()),
                new RetrievedContext("second passage", "second", 0.7f, Map.of()));

        RerankProviderException blankQuery = assertThrows(
                RerankProviderException.class,
                () -> reranker.rerank(" ", contexts));
        RerankProviderException tooMany = assertThrows(
                RerankProviderException.class,
                () -> reranker.rerank("query", contexts));

        assertEquals("invalid_input", blankQuery.reason());
        assertEquals(0, blankQuery.modelCallCount());
        assertEquals("invalid_input", tooMany.reason());
        assertEquals(0, tooMany.modelCallCount());
        assertEquals(0, calls.get());
    }

    @Test
    void shouldClassifyHttpFailuresWithoutExposingResponseBody() throws Exception {
        startServer();
        AtomicInteger status = new AtomicInteger(400);
        server.createContext("/v1/ranking", exchange -> sendJson(
                exchange, status.get(), "{\"error\":\"secret raw response\"}"));
        NvidiaReranker reranker = new NvidiaReranker(configuredProperties());
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first passage", "first", 0.8f, Map.of()));

        RerankProviderException clientFailure = assertThrows(
                RerankProviderException.class,
                () -> reranker.rerank("query", contexts));
        status.set(500);
        RerankProviderException serverFailure = assertThrows(
                RerankProviderException.class,
                () -> reranker.rerank("query", contexts));

        assertEquals("http_4xx", clientFailure.reason());
        assertEquals("http_5xx", serverFailure.reason());
        assertFalse(clientFailure.getMessage().contains("secret raw response"));
        assertFalse(serverFailure.getMessage().contains("secret raw response"));
    }

    @Test
    void shouldReportHealthFailureBeforeRankingCall() throws Exception {
        startServer();
        AtomicInteger rankingCalls = new AtomicInteger();
        server.createContext("/health", exchange -> sendJson(exchange, 503, "{\"status\":\"down\"}"));
        server.createContext("/v1/ranking", exchange -> {
            rankingCalls.incrementAndGet();
            sendJson(exchange, 200, "{\"rankings\":[]}");
        });
        RetrievalProperties properties = configuredProperties();
        properties.getRerank().setProvider("nvidia");
        properties.getRerank().getNvidia().setHealthCheckEnabled(true);
        properties.getRerank().getNvidia().setHealthCacheMillis(0);
        NvidiaReranker nvidia = new NvidiaReranker(properties);
        RerankerRegistry registry = new RerankerRegistry(List.of(new HeuristicReranker(), nvidia), properties);

        RerankOutcome outcome = registry.rerankWithDiagnostics(
                "query",
                List.of(new RetrievedContext("passage", "first", 0.8f, Map.of())));

        assertEquals("heuristic", outcome.diagnostics().effectiveProvider());
        assertEquals("health_check_failed", outcome.diagnostics().fallbackReason());
        assertEquals(0, outcome.diagnostics().modelCallCount());
        assertEquals(0, rankingCalls.get());
    }

    @Test
    void shouldClassifyTimeoutAndNetworkFailures() throws Exception {
        startServer();
        server.createContext("/v1/ranking", exchange -> {
            try {
                Thread.sleep(200);
                sendJson(exchange, 200, "{\"rankings\":[{\"index\":0,\"logit\":1}]}");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                exchange.close();
            }
        });
        RetrievalProperties timeoutProperties = configuredProperties();
        timeoutProperties.getRerank().getNvidia().setTimeoutMillis(20);
        NvidiaReranker timeoutReranker = new NvidiaReranker(timeoutProperties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("passage", "first", 0.8f, Map.of()));

        RerankProviderException timeout = assertThrows(
                RerankProviderException.class,
                () -> timeoutReranker.rerank("query", contexts));

        int unusedPort = server.getAddress().getPort();
        server.stop(0);
        server = null;
        RetrievalProperties networkProperties = new RetrievalProperties();
        RetrievalProperties.NvidiaReranker networkConfig = networkProperties.getRerank().getNvidia();
        networkConfig.setEnabled(true);
        networkConfig.setBaseUrl("http://127.0.0.1:" + unusedPort);
        networkConfig.setEndpointPath("/v1/ranking");
        networkConfig.setApiKey("test-key");
        networkConfig.setModel("nvidia/test-reranker");
        networkConfig.setHealthCheckEnabled(false);
        networkConfig.setTimeoutMillis(200);
        NvidiaReranker networkReranker = new NvidiaReranker(networkProperties);
        RerankProviderException network = assertThrows(
                RerankProviderException.class,
                () -> networkReranker.rerank("query", contexts));

        assertEquals("timeout", timeout.reason());
        assertEquals("network", network.reason());
        assertEquals(1, timeout.modelCallCount());
        assertEquals(1, network.modelCallCount());
    }

    private RetrievalProperties configuredProperties() {
        RetrievalProperties properties = new RetrievalProperties();
        RetrievalProperties.NvidiaReranker nvidia = properties.getRerank().getNvidia();
        nvidia.setEnabled(true);
        nvidia.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        nvidia.setEndpointPath("/v1/ranking");
        nvidia.setApiKey("test-key");
        nvidia.setModel("nvidia/test-reranker");
        nvidia.setTimeoutMillis(1000);
        nvidia.setHealthCheckEnabled(false);
        nvidia.setTruncate("NONE");
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

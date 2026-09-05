package com.enterprise.rag.core.embedding;

import com.enterprise.rag.core.embedding.config.EmbeddingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIEmbeddingProviderContractTest {

    private static final String MODEL = "nvidia/nemotron-3-embed-1b";
    private static final int DIMENSION = 2048;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void queryRequestUsesFrozenNemotronContractAndActualModelIdentity() throws Exception {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        startServer(exchange -> {
            request.set(objectMapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, response(List.of(item(0, 0.25f)), MODEL));
        });

        OpenAIEmbeddingProvider provider = provider(0);
        float[] vector = provider.getEmbedding("synthetic-query");

        assertEquals(DIMENSION, vector.length);
        assertEquals(MODEL, provider.getModelName());
        JsonNode body = request.get();
        assertEquals(Set.of("input", "model", "input_type", "modality", "embedding_type",
                        "encoding_format", "truncate"),
                objectMapper.convertValue(body, java.util.Map.class).keySet());
        assertEquals("query", body.get("input_type").asText());
        assertEquals("text", body.get("modality").asText());
        assertEquals("float", body.get("embedding_type").asText());
        assertEquals("float", body.get("encoding_format").asText());
        assertEquals("NONE", body.get("truncate").asText());
        assertFalse(body.has("dimensions"));
    }

    @Test
    void batchResponseIsValidatedAndRestoredByIndex() throws Exception {
        startServer(exchange -> respond(exchange, 200,
                response(List.of(item(1, 2.0f), item(0, 1.0f)), MODEL)));

        List<float[]> vectors = provider(0).getEmbeddings(List.of("first", "second"));

        assertEquals(2, vectors.size());
        assertEquals(1.0f, vectors.get(0)[0]);
        assertEquals(2.0f, vectors.get(1)[0]);
    }

    @Test
    void invalidDimensionAndCountFailClosed() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        startServer(exchange -> {
            int call = calls.incrementAndGet();
            String body = call == 1
                    ? response(List.of(itemWithDimension(0, DIMENSION - 1, 0.1f)), MODEL)
                    : response(List.of(item(0, 0.1f)), MODEL);
            respond(exchange, 200, body);
        });
        OpenAIEmbeddingProvider provider = provider(0);

        assertThrows(EmbeddingException.class, () -> provider.getEmbedding("bad-dimension"));
        assertThrows(EmbeddingException.class,
                () -> provider.getEmbeddings(List.of("first", "missing-second")));
    }

    @Test
    void zeroRetryMakesOnlyOneAttemptOnProviderFailure() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        startServer(exchange -> {
            calls.incrementAndGet();
            respond(exchange, 503, "{\"error\":\"synthetic\"}");
        });

        assertThrows(EmbeddingException.class, () -> provider(0).getEmbedding("synthetic-query"));
        assertEquals(1, calls.get());
    }

    private OpenAIEmbeddingProvider provider(int maxRetries) {
        EmbeddingProperties.OpenAI config = new EmbeddingProperties.OpenAI();
        config.setApiKey("test-only");
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
        config.setModel(MODEL);
        config.setDimension(DIMENSION);
        config.setMaxRetries(maxRetries);
        config.setRetryDelayMs(1);
        config.setTimeoutMs(5000);
        return new OpenAIEmbeddingProvider(config, WebClient.builder());
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private String response(List<String> items, String model) {
        return "{\"data\":[" + String.join(",", items) + "],\"model\":\"" + model + "\"}";
    }

    private String item(int index, float firstValue) {
        return itemWithDimension(index, DIMENSION, firstValue);
    }

    private String itemWithDimension(int index, int dimension, float firstValue) {
        List<String> values = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            values.add(Float.toString(i == 0 ? firstValue : 0.0f));
        }
        return "{\"embedding\":[" + String.join(",", values)
                + "],\"index\":" + index + ",\"object\":\"embedding\"}";
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}

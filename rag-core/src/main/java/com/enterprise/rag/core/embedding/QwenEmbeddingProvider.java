package com.enterprise.rag.core.embedding;

import com.enterprise.rag.core.embedding.config.EmbeddingProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 通义千问 Embedding Provider
 * 实现 text-embedding-v1 模型调用
 */
public class QwenEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(QwenEmbeddingProvider.class);
    private static final String MODEL_NAME = "qwen";

    private final WebClient webClient;
    private final EmbeddingProperties.Qwen config;

    public QwenEmbeddingProvider(EmbeddingProperties.Qwen config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Input text cannot be null or empty", MODEL_NAME, false);
        }

        try {
            QwenEmbeddingRequest request = new QwenEmbeddingRequest(
                    config.getModel(),
                    new QwenInput(List.of(text)),
                    Map.of("text_type", "query")
            );

            QwenEmbeddingResponse response = webClient.post()
                    .uri("/services/embeddings/text-embedding/text-embedding")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(QwenEmbeddingResponse.class)
                    .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(config.getRetryDelayMs()))
                            .filter(this::isRetryableError)
                            .onRetryExhaustedThrow((spec, signal) ->
                                    new EmbeddingException("Max retries exceeded for Qwen API",
                                            signal.failure(), MODEL_NAME, false)))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            if (response == null || response.output() == null || response.output().embeddings() == null 
                    || response.output().embeddings().isEmpty()) {
                throw new EmbeddingException("Empty response from Qwen API", MODEL_NAME, true);
            }

            List<Float> embedding = response.output().embeddings().get(0).embedding();
            return toFloatArray(embedding);

        } catch (WebClientResponseException e) {
            log.error("Qwen API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingException("Qwen API error: " + e.getMessage(), e, MODEL_NAME,
                    isRetryableStatusCode(e.getStatusCode().value()));
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling Qwen API", e);
            throw new EmbeddingException("Failed to get embedding from Qwen: " + e.getMessage(),
                    e, MODEL_NAME, true);
        }
    }

    @Override
    public List<float[]> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new EmbeddingException("Input texts cannot be null or empty", MODEL_NAME, false);
        }

        try {
            QwenEmbeddingRequest request = new QwenEmbeddingRequest(
                    config.getModel(),
                    new QwenInput(texts),
                    Map.of("text_type", "query")
            );

            QwenEmbeddingResponse response = webClient.post()
                    .uri("/services/embeddings/text-embedding/text-embedding")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(QwenEmbeddingResponse.class)
                    .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(config.getRetryDelayMs()))
                            .filter(this::isRetryableError)
                            .onRetryExhaustedThrow((spec, signal) ->
                                    new EmbeddingException("Max retries exceeded for Qwen API",
                                            signal.failure(), MODEL_NAME, false)))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            if (response == null || response.output() == null || response.output().embeddings() == null) {
                throw new EmbeddingException("Empty response from Qwen API", MODEL_NAME, true);
            }

            return response.output().embeddings().stream()
                    .map(e -> toFloatArray(e.embedding()))
                    .toList();

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling Qwen API for batch", e);
            throw new EmbeddingException("Failed to get batch embeddings from Qwen: " + e.getMessage(),
                    e, MODEL_NAME, true);
        }
    }

    @Override
    public int getDimension() {
        return config.getDimension();
    }

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public boolean isAvailable() {
        return config.isEnabled() && config.getApiKey() != null && !config.getApiKey().isBlank();
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException e) {
            return isRetryableStatusCode(e.getStatusCode().value());
        }
        return true;
    }

    private boolean isRetryableStatusCode(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    // Request/Response DTOs for Qwen API
    record QwenEmbeddingRequest(
            String model,
            QwenInput input,
            Map<String, String> parameters
    ) {}

    record QwenInput(List<String> texts) {}

    record QwenEmbeddingResponse(
            QwenOutput output,
            QwenUsage usage,
            @JsonProperty("request_id") String requestId
    ) {}

    record QwenOutput(List<QwenEmbeddingData> embeddings) {}

    record QwenEmbeddingData(
            List<Float> embedding,
            @JsonProperty("text_index") int textIndex
    ) {}

    record QwenUsage(
            @JsonProperty("total_tokens") int totalTokens
    ) {}
}

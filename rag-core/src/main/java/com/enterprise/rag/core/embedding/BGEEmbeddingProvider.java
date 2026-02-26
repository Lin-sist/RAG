package com.enterprise.rag.core.embedding;

import com.enterprise.rag.core.embedding.config.EmbeddingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * BGE 本地模型 Embedding Provider
 * 通过 HTTP 调用本地部署的 BGE 推理服务
 */
public class BGEEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(BGEEmbeddingProvider.class);
    private static final String MODEL_NAME = "bge";

    private final WebClient webClient;
    private final EmbeddingProperties.BGE config;
    private volatile boolean available = true;

    public BGEEmbeddingProvider(EmbeddingProperties.BGE config) {
        this.config = config;
        this.webClient = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Input text cannot be null or empty", MODEL_NAME, false);
        }

        try {
            BGEEmbeddingRequest request = new BGEEmbeddingRequest(List.of(text), config.getModel());

            BGEEmbeddingResponse response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BGEEmbeddingResponse.class)
                    .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(config.getRetryDelayMs()))
                            .filter(this::isRetryableError)
                            .onRetryExhaustedThrow((spec, signal) ->
                                    new EmbeddingException("Max retries exceeded for BGE service",
                                            signal.failure(), MODEL_NAME, false)))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
                throw new EmbeddingException("Empty response from BGE service", MODEL_NAME, true);
            }

            available = true;
            return toFloatArray(response.embeddings().get(0));

        } catch (WebClientResponseException e) {
            log.error("BGE service error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().is5xxServerError()) {
                available = false;
            }
            throw new EmbeddingException("BGE service error: " + e.getMessage(), e, MODEL_NAME,
                    isRetryableStatusCode(e.getStatusCode().value()));
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling BGE service", e);
            available = false;
            throw new EmbeddingException("Failed to get embedding from BGE: " + e.getMessage(),
                    e, MODEL_NAME, true);
        }
    }

    @Override
    public List<float[]> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new EmbeddingException("Input texts cannot be null or empty", MODEL_NAME, false);
        }

        try {
            BGEEmbeddingRequest request = new BGEEmbeddingRequest(texts, config.getModel());

            BGEEmbeddingResponse response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BGEEmbeddingResponse.class)
                    .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(config.getRetryDelayMs()))
                            .filter(this::isRetryableError)
                            .onRetryExhaustedThrow((spec, signal) ->
                                    new EmbeddingException("Max retries exceeded for BGE service",
                                            signal.failure(), MODEL_NAME, false)))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            if (response == null || response.embeddings() == null) {
                throw new EmbeddingException("Empty response from BGE service", MODEL_NAME, true);
            }

            available = true;
            return response.embeddings().stream()
                    .map(this::toFloatArray)
                    .toList();

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling BGE service for batch", e);
            available = false;
            throw new EmbeddingException("Failed to get batch embeddings from BGE: " + e.getMessage(),
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
        return config.isEnabled() && available;
    }

    @Override
    public int getPriority() {
        return config.getPriority();
    }

    /**
     * 健康检查 - 检测本地服务是否可用
     */
    public boolean healthCheck() {
        try {
            webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            available = true;
            return true;
        } catch (Exception e) {
            log.warn("BGE service health check failed: {}", e.getMessage());
            available = false;
            return false;
        }
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

    // Request/Response DTOs for BGE local service
    record BGEEmbeddingRequest(
            List<String> texts,
            String model
    ) {}

    record BGEEmbeddingResponse(
            List<List<Float>> embeddings,
            String model,
            int dimension
    ) {}
}

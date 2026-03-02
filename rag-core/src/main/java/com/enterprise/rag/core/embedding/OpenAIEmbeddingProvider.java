package com.enterprise.rag.core.embedding;

import com.enterprise.rag.core.embedding.config.EmbeddingProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

/**
 * OpenAI Embedding Provider
 * 实现 text-embedding-ada-002 模型调用
 */
public class OpenAIEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingProvider.class);
    private static final String MODEL_NAME = "openai";

    private final WebClient webClient;
    private final EmbeddingProperties.OpenAI config;

    public OpenAIEmbeddingProvider(EmbeddingProperties.OpenAI config) {
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
            EmbeddingRequest request = new EmbeddingRequest(text, config.getModel());

            EmbeddingResponse response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(config.getRetryDelayMs()))
                            .filter(this::isRetryableError)
                            .onRetryExhaustedThrow(
                                    (spec, signal) -> new EmbeddingException("Max retries exceeded for OpenAI API",
                                            signal.failure(), MODEL_NAME, false)))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new EmbeddingException("Empty response from OpenAI API", MODEL_NAME, true);
            }

            List<Float> embedding = response.data().get(0).embedding();
            return toFloatArray(embedding);

        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingException("OpenAI API error: " + e.getMessage(), e, MODEL_NAME,
                    isRetryableStatusCode(e.getStatusCode().value()));
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI API", e);
            throw new EmbeddingException("Failed to get embedding from OpenAI: " + e.getMessage(),
                    e, MODEL_NAME, true);
        }
    }

    @Override
    public List<float[]> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new EmbeddingException("Input texts cannot be null or empty", MODEL_NAME, false);
        }

        try {
            BatchEmbeddingRequest request = new BatchEmbeddingRequest(texts, config.getModel());

            EmbeddingResponse response = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class)
                    .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofMillis(config.getRetryDelayMs()))
                            .filter(this::isRetryableError)
                            .onRetryExhaustedThrow(
                                    (spec, signal) -> new EmbeddingException("Max retries exceeded for OpenAI API",
                                            signal.failure(), MODEL_NAME, false)))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            if (response == null || response.data() == null) {
                throw new EmbeddingException("Empty response from OpenAI API", MODEL_NAME, true);
            }

            return response.data().stream()
                    .map(d -> toFloatArray(d.embedding()))
                    .toList();

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI API for batch", e);
            throw new EmbeddingException("Failed to get batch embeddings from OpenAI: " + e.getMessage(),
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

    // Request/Response DTOs
    record EmbeddingRequest(
            String input,
            String model,
            @JsonProperty("input_type") String inputType,
            @JsonProperty("encoding_format") String encodingFormat) {
        EmbeddingRequest(String input, String model) {
            this(input, model, "query", "float");
        }
    }

    record BatchEmbeddingRequest(
            List<String> input,
            String model,
            @JsonProperty("input_type") String inputType,
            @JsonProperty("encoding_format") String encodingFormat) {
        BatchEmbeddingRequest(List<String> input, String model) {
            this(input, model, "query", "float");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResponse(
            List<EmbeddingData> data,
            String model,
            Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingData(
            List<Float> embedding,
            int index,
            String object) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Usage(
            @JsonProperty("prompt_tokens") int promptTokens,
            @JsonProperty("total_tokens") int totalTokens) {
    }
}

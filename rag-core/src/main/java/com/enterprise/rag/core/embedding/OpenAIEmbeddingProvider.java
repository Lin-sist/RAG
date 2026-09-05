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
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenAI Embedding Provider
 * 实现 text-embedding-ada-002 模型调用
 */
public class OpenAIEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingProvider.class);
    private static final String PROVIDER_NAME = "openai-compatible";

    private final WebClient webClient;
    private final EmbeddingProperties.OpenAI config;

    /**
     * 批量请求时，每批最多处理的文本数量。
     * NVIDIA NIM embedding 模型返回 2048 维向量，单个 embedding 的 JSON 约 40KB，
     * 5 个 chunk 的响应约 200KB，在 WebClient 默认 buffer (256KB) 内安全处理。
     */
    private static final int BATCH_CHUNK_SIZE = 5;

    public OpenAIEmbeddingProvider(EmbeddingProperties.OpenAI config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = webClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    @Override
    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Input text cannot be null or empty", PROVIDER_NAME, false);
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
                                            signal.failure(), PROVIDER_NAME, false)))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            if (response == null || response.data() == null || response.data().isEmpty()) {
                throw new EmbeddingException("Empty response from OpenAI API", PROVIDER_NAME, true);
            }
            return validateAndOrder(response, 1).get(0);

        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: status={}, errorType={}",
                    e.getStatusCode(), e.getClass().getSimpleName());
            throw new EmbeddingException("OpenAI API error: " + e.getMessage(), e, PROVIDER_NAME,
                    isRetryableStatusCode(e.getStatusCode().value()));
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI API: errorType={}",
                    e.getClass().getSimpleName());
            throw new EmbeddingException("Failed to get embedding from OpenAI: " + e.getMessage(),
                    e, PROVIDER_NAME, true);
        }
    }

    @Override
    public List<float[]> getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new EmbeddingException("Input texts cannot be null or empty", PROVIDER_NAME, false);
        }

        // 分批处理：避免单次请求返回的 JSON 过大（每个 2048 维 embedding ≈ 40KB）
        if (texts.size() > BATCH_CHUNK_SIZE) {
            log.info("Splitting batch of {} texts into sub-batches of {}", texts.size(), BATCH_CHUNK_SIZE);
            List<float[]> allResults = new ArrayList<>();
            for (int i = 0; i < texts.size(); i += BATCH_CHUNK_SIZE) {
                int end = Math.min(i + BATCH_CHUNK_SIZE, texts.size());
                List<String> subBatch = texts.subList(i, end);
                log.debug("Processing sub-batch {}-{} of {}", i, end, texts.size());
                allResults.addAll(getEmbeddingsSingleBatch(subBatch));
            }
            return allResults;
        }

        return getEmbeddingsSingleBatch(texts);
    }

    /**
     * 单批次调用 embedding API（不超过 BATCH_CHUNK_SIZE 个文本）
     */
    private List<float[]> getEmbeddingsSingleBatch(List<String> texts) {
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
                                            signal.failure(), PROVIDER_NAME, false)))
                    .timeout(Duration.ofMillis(config.getTimeoutMs()))
                    .block();

            if (response == null || response.data() == null) {
                throw new EmbeddingException("Empty response from OpenAI API", PROVIDER_NAME, true);
            }
            return validateAndOrder(response, texts.size());

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling OpenAI API for batch: size={}, errorType={}",
                    texts.size(), e.getClass().getSimpleName());
            throw new EmbeddingException("Failed to get batch embeddings from OpenAI: " + e.getMessage(),
                    e, PROVIDER_NAME, true);
        }
    }

    @Override
    public int getDimension() {
        return config.getDimension();
    }

    @Override
    public String getModelName() {
        return config.getModel();
    }

    @Override
    public String getProviderFamily() {
        return "openai-compatible";
    }

    @Override
    public String getEndpointIdentity() {
        URI uri = URI.create(config.getBaseUrl());
        String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
        return uri.getScheme() + "://" + uri.getHost() + path + "/embeddings";
    }

    @Override
    public String getRequestContractVersion() {
        return "nvidia-openai-embedding-v1";
    }

    @Override
    public int getMaxBatchSize() {
        return BATCH_CHUNK_SIZE;
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

    private List<float[]> validateAndOrder(EmbeddingResponse response, int expectedCount) {
        if (!config.getModel().equals(response.model())) {
            throw new EmbeddingException("Embedding response model mismatch", PROVIDER_NAME, false);
        }
        if (response.data().size() != expectedCount) {
            throw new EmbeddingException("Embedding response count mismatch", PROVIDER_NAME, false);
        }
        Set<Integer> indexes = new HashSet<>();
        for (EmbeddingData data : response.data()) {
            if (data == null || data.index() < 0 || data.index() >= expectedCount
                    || !indexes.add(data.index())) {
                throw new EmbeddingException("Embedding response index mismatch", PROVIDER_NAME, false);
            }
        }
        return response.data().stream()
                .sorted(Comparator.comparingInt(EmbeddingData::index))
                .map(data -> toFloatArray(data.embedding()))
                .toList();
    }

    private float[] toFloatArray(List<Float> list) {
        if (list == null || list.size() != config.getDimension()) {
            throw new EmbeddingException("Embedding response dimension mismatch", PROVIDER_NAME, false);
        }
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Float value = list.get(i);
            if (value == null || !Float.isFinite(value)) {
                throw new EmbeddingException("Embedding response contains non-finite value", PROVIDER_NAME, false);
            }
            array[i] = value;
        }
        return array;
    }

    // Request/Response DTOs
    record EmbeddingRequest(
            String input,
            String model,
            @JsonProperty("input_type") String inputType,
            String modality,
            @JsonProperty("embedding_type") String embeddingType,
            @JsonProperty("encoding_format") String encodingFormat,
            String truncate) {
        EmbeddingRequest(String input, String model) {
            this(input, model, "query", "text", "float", "float", "NONE");
        }
    }

    record BatchEmbeddingRequest(
            List<String> input,
            String model,
            @JsonProperty("input_type") String inputType,
            String modality,
            @JsonProperty("embedding_type") String embeddingType,
            @JsonProperty("encoding_format") String encodingFormat,
            String truncate) {
        BatchEmbeddingRequest(List<String> input, String model) {
            this(input, model, "passage", "text", "float", "float", "NONE");
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

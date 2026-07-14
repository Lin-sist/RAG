package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * HTTP model reranker adapter. It is disabled by default and falls back to the
 * heuristic reranker through {@link RerankerRegistry} whenever configuration,
 * health checks, or requests fail.
 */
@Slf4j
@Component
public class ModelReranker implements Reranker {

    private final RetrievalProperties retrievalProperties;
    private final WebClient webClient;

    private volatile Boolean cachedHealth;
    private volatile Instant cachedHealthCheckedAt = Instant.EPOCH;

    public ModelReranker(RetrievalProperties retrievalProperties) {
        this(retrievalProperties, WebClient.builder());
    }

    @Autowired
    public ModelReranker(RetrievalProperties retrievalProperties, WebClient.Builder webClientBuilder) {
        this.retrievalProperties = retrievalProperties;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String provider() {
        return "model";
    }

    @Override
    public boolean available() {
        RetrievalProperties.ModelReranker model = retrievalProperties.getRerank().getModel();
        if (!isConfigured(model)) {
            return false;
        }
        if (!model.isHealthCheckEnabled()) {
            return true;
        }
        return healthCheck(model);
    }

    @Override
    public List<RetrievedContext> rerank(String query, List<RetrievedContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }

        RetrievalProperties.ModelReranker model = retrievalProperties.getRerank().getModel();
        if (!available()) {
            log.warn("Model reranker unavailable; enabled={}, baseUrlConfigured={}, modelConfigured={}, apiKeyConfigured={}",
                    model.isEnabled(),
                    hasText(model.getBaseUrl()),
                    hasText(model.getModel()),
                    hasText(model.getApiKey()));
            return contexts;
        }

        try {
            RerankResponse response = webClient.post()
                    .uri(joinUrl(model.getBaseUrl(), model.getEndpointPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + model.getApiKey())
                    .bodyValue(new RerankRequest(
                            model.getModel(),
                            query == null ? "" : query,
                            contexts.stream().map(RetrievedContext::content).toList()))
                    .retrieve()
                    .bodyToMono(RerankResponse.class)
                    .timeout(timeout(model))
                    .block();

            List<RerankResult> results = response == null ? List.of() : response.normalizedResults();
            if (results.isEmpty()) {
                throw new IllegalStateException("Model reranker returned no usable results");
            }

            Map<Integer, Double> scores = new LinkedHashMap<>();
            for (RerankResult result : results) {
                if (result.index() >= 0 && result.index() < contexts.size()) {
                    scores.put(result.index(), result.effectiveScore());
                }
            }
            if (scores.isEmpty()) {
                throw new IllegalStateException("Model reranker returned only out-of-range indices");
            }

            return IntStream.range(0, contexts.size())
                    .boxed()
                    .sorted(Comparator
                            .comparingDouble((Integer index) -> scores.getOrDefault(index, Double.NEGATIVE_INFINITY))
                            .reversed()
                            .thenComparingInt(Integer::intValue))
                    .map(index -> withRerankScore(contexts.get(index), scores.get(index)))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Model reranker request failed", e);
        }
    }

    private boolean healthCheck(RetrievalProperties.ModelReranker model) {
        Instant now = Instant.now();
        long cacheMillis = Math.max(0, model.getHealthCacheMillis());
        Boolean cached = cachedHealth;
        if (cached != null && cachedHealthCheckedAt.plusMillis(cacheMillis).isAfter(now)) {
            return cached;
        }

        boolean healthy;
        try {
            webClient.get()
                    .uri(joinUrl(model.getBaseUrl(), model.getHealthPath()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + model.getApiKey())
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(timeout(model))
                    .block();
            healthy = true;
        } catch (Exception e) {
            log.warn("Model reranker health check failed; provider disabled until next cache window: errorType={}",
                    e.getClass().getSimpleName());
            healthy = false;
        }

        cachedHealth = healthy;
        cachedHealthCheckedAt = now;
        return healthy;
    }

    private boolean isConfigured(RetrievalProperties.ModelReranker model) {
        return model != null
                && model.isEnabled()
                && hasText(model.getBaseUrl())
                && hasText(model.getEndpointPath())
                && hasText(model.getApiKey())
                && hasText(model.getModel());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Duration timeout(RetrievalProperties.ModelReranker model) {
        return Duration.ofMillis(Math.max(1, model.getTimeoutMillis()));
    }

    private String joinUrl(String baseUrl, String path) {
        String normalizedBase = Optional.ofNullable(baseUrl).orElse("").replaceAll("/+$", "");
        String normalizedPath = Optional.ofNullable(path).orElse("").replaceAll("^/+", "");
        return normalizedBase + "/" + normalizedPath;
    }

    private RetrievedContext withRerankScore(RetrievedContext context, Double rerankScore) {
        if (rerankScore == null) {
            return context;
        }
        Map<String, Object> metadata = new LinkedHashMap<>(context.metadata() == null ? Map.of() : context.metadata());
        metadata.put("originalRelevanceScore", context.relevanceScore());
        metadata.put("rerankScore", rerankScore);
        metadata.put("rerankProvider", provider());
        return new RetrievedContext(
                context.content(),
                context.source(),
                rerankScore.floatValue(),
                metadata);
    }

    private record RerankRequest(String model, String query, List<String> documents) {
    }

    private record RerankResponse(List<RerankResult> results) {
        private List<RerankResult> normalizedResults() {
            return results == null ? List.of() : results;
        }
    }

    private record RerankResult(int index, Double relevance_score, Double score) {
        private double effectiveScore() {
            return relevance_score != null ? relevance_score : score == null ? 0.0d : score;
        }
    }
}

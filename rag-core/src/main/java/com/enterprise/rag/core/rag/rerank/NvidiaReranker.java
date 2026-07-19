package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/** NVIDIA {@code /v1/ranking} protocol adapter. */
@Component
public class NvidiaReranker implements Reranker {

    private final RetrievalProperties retrievalProperties;
    private final WebClient webClient;
    private volatile Boolean cachedHealth;
    private volatile Instant cachedHealthCheckedAt = Instant.EPOCH;

    public NvidiaReranker(RetrievalProperties retrievalProperties) {
        this(retrievalProperties, WebClient.builder());
    }

    @Autowired
    public NvidiaReranker(RetrievalProperties retrievalProperties, WebClient.Builder webClientBuilder) {
        this.retrievalProperties = retrievalProperties;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String provider() {
        return "nvidia";
    }

    @Override
    public boolean available() {
        RetrievalProperties.NvidiaReranker config = retrievalProperties.getRerank().getNvidia();
        if (!isConfigured(config)) {
            return false;
        }
        return !config.isHealthCheckEnabled() || healthCheck(config);
    }

    @Override
    public String unavailableReason() {
        RetrievalProperties.NvidiaReranker config = retrievalProperties.getRerank().getNvidia();
        return isConfigured(config) ? "health_check_failed" : "not_configured";
    }

    @Override
    public List<RetrievedContext> rerank(String query, List<RetrievedContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }
        RetrievalProperties.NvidiaReranker config = retrievalProperties.getRerank().getNvidia();
        validateInput(query, contexts, config);

        RankingResponse response;
        try {
            response = webClient.post()
                    .uri(joinUrl(config.getBaseUrl(), config.getEndpointPath()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .bodyValue(new RankingRequest(
                            config.getModel(),
                            new TextInput(query),
                            contexts.stream().map(context -> new TextInput(context.content())).toList(),
                            config.getTruncate()))
                    .retrieve()
                    .bodyToMono(RankingResponse.class)
                    .timeout(Duration.ofMillis(Math.max(1, config.getTimeoutMillis())))
                    .block();
        } catch (Exception exception) {
            throw new RerankProviderException(
                    classifyRequestFailure(exception),
                    1,
                    "NVIDIA ranking request failed",
                    exception);
        }

        List<Ranking> rankings = response == null || response.rankings() == null
                ? List.of()
                : response.rankings();
        validateRankings(rankings, contexts.size());
        Map<Integer, Ranking> byIndex = new LinkedHashMap<>();
        rankings.forEach(ranking -> byIndex.put(ranking.index(), ranking));

        List<Integer> orderedIndices = rankings.stream()
                .sorted(Comparator.comparingDouble(Ranking::logit).reversed()
                        .thenComparingInt(Ranking::index))
                .map(Ranking::index)
                .toList();
        return java.util.stream.IntStream.range(0, orderedIndices.size())
                .mapToObj(rank -> withRankingMetadata(
                        contexts.get(orderedIndices.get(rank)),
                        byIndex.get(orderedIndices.get(rank)).logit(),
                        rank + 1))
                .toList();
    }

    private void validateInput(
            String query,
            List<RetrievedContext> contexts,
            RetrievalProperties.NvidiaReranker config) {
        if (!available()) {
            throw new RerankProviderException("not_configured", 0, "NVIDIA reranker is not configured");
        }
        if (!hasText(query) || contexts.stream().anyMatch(context -> context == null || !hasText(context.content()))) {
            throw new RerankProviderException("invalid_input", 0, "NVIDIA ranking requires non-empty text inputs");
        }
        if (contexts.size() > Math.max(1, config.getMaxCandidates())) {
            throw new RerankProviderException("invalid_input", 0, "NVIDIA ranking candidate limit exceeded");
        }
    }

    private void validateRankings(List<Ranking> rankings, int candidateCount) {
        if (rankings.size() != candidateCount) {
            throw new RerankProviderException(
                    "incomplete_rankings", 1, "NVIDIA ranking response did not cover every candidate");
        }
        Set<Integer> seen = new HashSet<>();
        for (Ranking ranking : rankings) {
            if (ranking == null
                    || ranking.index() == null
                    || ranking.index() < 0
                    || ranking.index() >= candidateCount
                    || !seen.add(ranking.index())
                    || ranking.logit() == null
                    || !Double.isFinite(ranking.logit())) {
                throw new RerankProviderException("invalid_response", 1, "NVIDIA ranking response is invalid");
            }
        }
    }

    private boolean isConfigured(RetrievalProperties.NvidiaReranker config) {
        return config != null
                && config.isEnabled()
                && hasText(config.getBaseUrl())
                && hasText(config.getEndpointPath())
                && hasText(config.getApiKey())
                && hasText(config.getModel());
    }

    private boolean healthCheck(RetrievalProperties.NvidiaReranker config) {
        Instant now = Instant.now();
        long cacheMillis = Math.max(0, config.getHealthCacheMillis());
        Boolean cached = cachedHealth;
        if (cached != null && cachedHealthCheckedAt.plusMillis(cacheMillis).isAfter(now)) {
            return cached;
        }
        boolean healthy;
        try {
            webClient.get()
                    .uri(joinUrl(config.getBaseUrl(), config.getHealthPath()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(Math.max(1, config.getTimeoutMillis())))
                    .block();
            healthy = true;
        } catch (Exception exception) {
            healthy = false;
        }
        cachedHealth = healthy;
        cachedHealthCheckedAt = now;
        return healthy;
    }

    private String classifyRequestFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return "timeout";
            }
            if (current instanceof WebClientResponseException responseException) {
                return responseException.getStatusCode().is4xxClientError() ? "http_4xx" : "http_5xx";
            }
            if (current instanceof WebClientRequestException) {
                return "network";
            }
            current = current.getCause();
        }
        return "provider_failure";
    }

    private RetrievedContext withRankingMetadata(RetrievedContext context, double logit, int rank) {
        Map<String, Object> metadata = new LinkedHashMap<>(
                context.metadata() == null ? Map.of() : context.metadata());
        metadata.put("rerankLogit", logit);
        metadata.put("rerankRank", rank);
        metadata.put("rerankProvider", provider());
        return new RetrievedContext(context.content(), context.source(), context.relevanceScore(), metadata);
    }

    private String joinUrl(String baseUrl, String path) {
        String normalizedBase = Optional.ofNullable(baseUrl).orElse("").replaceAll("/+$", "");
        String normalizedPath = Optional.ofNullable(path).orElse("").replaceAll("^/+", "");
        return normalizedBase + "/" + normalizedPath;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record RankingRequest(String model, TextInput query, List<TextInput> passages, String truncate) {
    }

    private record TextInput(String text) {
    }

    private record RankingResponse(List<Ranking> rankings) {
    }

    private record Ranking(Integer index, Double logit) {
    }
}

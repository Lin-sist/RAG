package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class RerankerRegistry {

    private static final Set<String> STABLE_FALLBACK_REASONS = Set.of(
            "not_configured",
            "health_check_failed",
            "timeout",
            "http_4xx",
            "http_5xx",
            "network",
            "invalid_response",
            "incomplete_rankings",
            "invalid_input",
            "provider_failure");

    private final List<Reranker> rerankers;
    private final RetrievalProperties retrievalProperties;

    public RerankerRegistry(List<Reranker> rerankers, RetrievalProperties retrievalProperties) {
        this.rerankers = rerankers;
        this.retrievalProperties = retrievalProperties;
    }

    public Reranker activeReranker() {
        String provider = requestedProvider();
        Reranker requested = findExact(provider);
        if (requested != null && requested.available()) {
            return requested;
        }

        if (requested != null) {
            log.warn("Reranker provider '{}' is unavailable; falling back to heuristic", provider);
        }
        return heuristic();
    }

    public List<RetrievedContext> rerank(
            String query,
            List<RetrievedContext> contexts) {
        return rerankWithDiagnostics(query, contexts).contexts();
    }

    public RerankOutcome rerankWithDiagnostics(
            String query,
            List<RetrievedContext> contexts) {
        long startedAt = System.nanoTime();
        List<RetrievedContext> candidates = contexts == null ? List.of() : contexts;
        String requestedProvider = requestedProvider();
        Reranker requested = findExact(requestedProvider);
        Reranker heuristic = heuristic();

        if (requested == null || !requested.available()) {
            List<RetrievedContext> fallback = heuristic.rerank(query, candidates);
            return outcome(
                    fallback,
                    requestedProvider,
                    "heuristic",
                    1,
                    requested == null ? "not_configured" : stableFallbackReason(requested.unavailableReason()),
                    0,
                    candidates.size(),
                    fallback.size(),
                    startedAt);
        }

        try {
            List<RetrievedContext> reranked = requested.rerank(query, candidates);
            int modelCalls = "heuristic".equalsIgnoreCase(requested.provider()) ? 0 : 1;
            return outcome(
                    reranked,
                    requestedProvider,
                    requested.provider(),
                    0,
                    "none",
                    modelCalls,
                    candidates.size(),
                    scoredCount(requested.provider(), reranked),
                    startedAt);
        } catch (Exception e) {
            if (!"heuristic".equalsIgnoreCase(requested.provider())) {
                log.warn("Reranker provider '{}' failed; falling back to heuristic: errorType={}",
                        requested.provider(), e.getClass().getSimpleName());
                List<RetrievedContext> fallback = heuristic.rerank(query, candidates);
                String fallbackReason = e instanceof RerankProviderException providerException
                        ? stableFallbackReason(providerException.reason())
                        : "provider_failure";
                int modelCallCount = e instanceof RerankProviderException providerException
                        ? providerException.modelCallCount()
                        : 1;
                return outcome(
                        fallback,
                        requestedProvider,
                        "heuristic",
                        1,
                        fallbackReason,
                        modelCallCount,
                        candidates.size(),
                        fallback.size(),
                        startedAt);
            }
            throw e;
        }
    }

    public RerankDiagnostics diagnosticsWithoutExecution(boolean enabled, int candidateCount) {
        String requestedProvider = requestedProvider();
        return new RerankDiagnostics(
                requestedProvider,
                enabled ? "not_run" : "disabled",
                0,
                enabled ? "no_candidates" : "none",
                0,
                Math.max(0, candidateCount),
                0,
                0.0d,
                0,
                modelFor(requestedProvider),
                protocolFor(requestedProvider));
    }

    private RerankOutcome outcome(
            List<RetrievedContext> contexts,
            String requestedProvider,
            String effectiveProvider,
            int fallbackCount,
            String fallbackReason,
            int modelCallCount,
            int candidateCount,
            int scoredCount,
            long startedAt) {
        double coverage = candidateCount == 0 ? 0.0d : (double) scoredCount / candidateCount;
        RerankDiagnostics diagnostics = new RerankDiagnostics(
                requestedProvider,
                effectiveProvider,
                fallbackCount,
                fallbackReason,
                modelCallCount,
                candidateCount,
                scoredCount,
                coverage,
                Math.max(0, (System.nanoTime() - startedAt) / 1_000_000),
                modelFor(requestedProvider),
                protocolFor(requestedProvider));
        return new RerankOutcome(contexts, diagnostics);
    }

    private int scoredCount(String provider, List<RetrievedContext> contexts) {
        if ("heuristic".equalsIgnoreCase(provider)) {
            return contexts.size();
        }
        return (int) contexts.stream()
                .filter(context -> context.metadata() != null
                        && provider.equalsIgnoreCase(String.valueOf(context.metadata().get("rerankProvider"))))
                .count();
    }

    private String requestedProvider() {
        String provider = retrievalProperties.getRerank().getProvider();
        return provider == null || provider.isBlank() ? "heuristic" : provider.toLowerCase(java.util.Locale.ROOT);
    }

    private String stableFallbackReason(String reason) {
        return STABLE_FALLBACK_REASONS.contains(reason) ? reason : "provider_failure";
    }

    private String modelFor(String provider) {
        if ("nvidia".equalsIgnoreCase(provider)) {
            return retrievalProperties.getRerank().getNvidia().getModel();
        }
        if ("model".equalsIgnoreCase(provider)) {
            return retrievalProperties.getRerank().getModel().getModel();
        }
        return "";
    }

    private String protocolFor(String provider) {
        if ("nvidia".equalsIgnoreCase(provider)) {
            return "nvidia-ranking-v1";
        }
        if ("model".equalsIgnoreCase(provider)) {
            return "generic-rerank-v1";
        }
        return "heuristic-v1";
    }

    private Reranker heuristic() {
        return rerankers.stream()
                .filter(reranker -> "heuristic".equalsIgnoreCase(reranker.provider()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No heuristic reranker configured"));
    }

    private Reranker findExact(String provider) {
        String normalized = provider == null || provider.isBlank() ? "heuristic" : provider;
        return rerankers.stream()
                .filter(reranker -> reranker.provider().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }
}

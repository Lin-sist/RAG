package com.enterprise.rag.core.rag.rerank;

import java.util.LinkedHashMap;
import java.util.Map;

/** Sanitized facts describing which reranker actually determined the final order. */
public record RerankDiagnostics(
        String requestedProvider,
        String effectiveProvider,
        int fallbackCount,
        String fallbackReason,
        int modelCallCount,
        int candidateCount,
        int scoredCount,
        double coverage,
        long latencyMillis,
        String model,
        String protocol) {

    public Map<String, Object> toMap() {
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("rerankRequestedProvider", requestedProvider);
        facts.put("rerankEffectiveProvider", effectiveProvider);
        facts.put("rerankFallbackCount", fallbackCount);
        facts.put("rerankFallbackReason", fallbackReason);
        facts.put("rerankModelCallCount", modelCallCount);
        facts.put("rerankCandidateCount", candidateCount);
        facts.put("rerankScoredCount", scoredCount);
        facts.put("rerankCoverage", coverage);
        facts.put("rerankLatencyMillis", latencyMillis);
        facts.put("rerankModel", model);
        facts.put("rerankProtocol", protocol);
        return Map.copyOf(facts);
    }
}

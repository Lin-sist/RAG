package com.enterprise.rag.admin.qa.dto;

import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.router.QueryBudgetUsage;
import com.enterprise.rag.core.rag.service.RAGService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Versioned, allowlisted SSE terminal payload. A null usage means it was not observed. */
public record StreamTerminalEvent(
        String schemaVersion,
        String finalState,
        String reason,
        List<Citation> citations,
        Map<String, Object> metadata,
        String classifierVersion,
        String effectiveStrategy,
        String policyVersion,
        String routeReason,
        String budgetOutcome,
        QueryBudgetUsage usage) {

    public static final String VERSION = "structured-v1";

    public static StreamTerminalEvent completed(RAGService.StreamTerminalSignal signal) {
        RAGService.StreamTerminalSignal.ExecutionResult result = signal.executionResult();
        if (result == null) {
            return error(signal, "RESULT_UNAVAILABLE");
        }
        return new StreamTerminalEvent(
                VERSION,
                result.finalState(),
                result.reason(),
                "ANSWER".equals(result.finalState()) ? result.citations() : List.of(),
                safeMetadata(result.metadata()),
                signal.classifierVersion(),
                signal.effectiveStrategy(),
                signal.policyVersion(),
                signal.routeReason(),
                result.usage() == null ? null : result.usage().outcome().name(),
                result.usage());
    }

    public static StreamTerminalEvent error(RAGService.StreamTerminalSignal signal, String reason) {
        QueryBudgetUsage usage = signal.budgetUsage();
        return new StreamTerminalEvent(
                VERSION, "ERROR", reason, List.of(), Map.of(),
                signal.classifierVersion(), signal.effectiveStrategy(), signal.policyVersion(),
                signal.routeReason(), usage == null ? null : usage.outcome().name(), usage);
    }

    private static Map<String, Object> safeMetadata(Map<String, Object> source) {
        Map<String, Object> safe = new LinkedHashMap<>();
        for (String key : List.of("validCitations", "droppedCitations", "citationCoverage",
                "estimatedContextTokens", "estimatedOutputTokens")) {
            Object value = source.get(key);
            if (value instanceof Number) {
                safe.put(key, value);
            }
        }
        return Map.copyOf(safe);
    }
}

package com.enterprise.rag.core.rag.router;

public record QueryExecutionBudget(
        int maxQueryVariants,
        int maxRetrievalPasses,
        int maxRerankCalls,
        int maxGenerationCalls,
        int maxContextTokens,
        int maxOutputTokens,
        long deadlineMillis) {
}

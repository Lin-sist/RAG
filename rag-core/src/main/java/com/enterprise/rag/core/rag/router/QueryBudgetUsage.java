package com.enterprise.rag.core.rag.router;

public record QueryBudgetUsage(
        int queryVariants,
        int retrievalPasses,
        int rerankCalls,
        int generationCalls,
        int candidateCount,
        int contextCount,
        int estimatedContextTokens,
        int estimatedOutputTokens,
        long elapsedMillis,
        BudgetOutcome outcome) {
}

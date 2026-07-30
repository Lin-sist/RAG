package com.enterprise.rag.core.rag.router;

public record QueryBudgetUsage(
        int queryVariants,
        int retrievalPasses,
        int rerankCalls,
        int generationCalls,
        long elapsedMillis,
        BudgetOutcome outcome) {
}

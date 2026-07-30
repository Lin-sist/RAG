package com.enterprise.rag.core.rag.router;

import java.util.Objects;
import java.util.function.LongSupplier;

public final class QueryBudgetLedger {
    private final QueryExecutionBudget budget;
    private final LongSupplier monotonicMillis;
    private final long startedAtMillis;

    private int queryVariants;
    private int retrievalPasses;
    private int rerankCalls;
    private int generationCalls;
    private BudgetOutcome outcome = BudgetOutcome.WITHIN_BUDGET;

    public QueryBudgetLedger(QueryExecutionBudget budget) {
        this(budget, () -> System.nanoTime() / 1_000_000L);
    }

    QueryBudgetLedger(QueryExecutionBudget budget, LongSupplier monotonicMillis) {
        this.budget = Objects.requireNonNull(budget, "budget");
        this.monotonicMillis = Objects.requireNonNull(monotonicMillis, "monotonicMillis");
        this.startedAtMillis = monotonicMillis.getAsLong();
    }

    public synchronized void recordQueryVariants(int actualCount) {
        ensureDeadline();
        if (actualCount < 1 || actualCount > budget.maxQueryVariants()) {
            fail(BudgetOutcome.CALL_LIMIT_EXCEEDED);
        }
        queryVariants = actualCount;
    }

    public synchronized void beginRetrieval() {
        ensureDeadline();
        if (retrievalPasses >= budget.maxRetrievalPasses()) {
            fail(BudgetOutcome.CALL_LIMIT_EXCEEDED);
        }
        retrievalPasses++;
    }

    public synchronized void recordRerankCalls(int actualCount) {
        ensureDeadline();
        if (actualCount < 0 || actualCount > budget.maxRerankCalls()) {
            fail(BudgetOutcome.CALL_LIMIT_EXCEEDED);
        }
        rerankCalls = actualCount;
    }

    public synchronized void beginGeneration() {
        ensureDeadline();
        if (generationCalls >= budget.maxGenerationCalls()) {
            fail(BudgetOutcome.CALL_LIMIT_EXCEEDED);
        }
        generationCalls++;
    }

    public synchronized QueryBudgetUsage snapshot() {
        return new QueryBudgetUsage(
                queryVariants,
                retrievalPasses,
                rerankCalls,
                generationCalls,
                Math.max(0L, monotonicMillis.getAsLong() - startedAtMillis),
                outcome);
    }

    private void ensureDeadline() {
        if (monotonicMillis.getAsLong() - startedAtMillis > budget.deadlineMillis()) {
            fail(BudgetOutcome.DEADLINE_EXCEEDED);
        }
    }

    private void fail(BudgetOutcome failure) {
        outcome = failure;
        throw new QueryBudgetExceededException(failure);
    }
}

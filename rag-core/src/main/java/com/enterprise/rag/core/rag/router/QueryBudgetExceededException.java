package com.enterprise.rag.core.rag.router;

public final class QueryBudgetExceededException extends RuntimeException {
    private final BudgetOutcome outcome;

    QueryBudgetExceededException(BudgetOutcome outcome) {
        super(outcome.name());
        this.outcome = outcome;
    }

    public BudgetOutcome outcome() {
        return outcome;
    }
}

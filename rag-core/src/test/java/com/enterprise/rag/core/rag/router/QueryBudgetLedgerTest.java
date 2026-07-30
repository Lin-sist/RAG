package com.enterprise.rag.core.rag.router;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryBudgetLedgerTest {

    @Test
    void stopsBeforeAStageThatWouldExceedCallsOrDeadline() {
        AtomicLong monotonicMillis = new AtomicLong(10_000L);
        QueryExecutionBudget budget = new QueryExecutionBudget(2, 1, 1, 1, 1200, 2048, 1_000L);
        QueryBudgetLedger ledger = new QueryBudgetLedger(budget, monotonicMillis::get);

        ledger.recordQueryVariants(2);
        ledger.beginRetrieval();
        assertThrows(QueryBudgetExceededException.class, ledger::beginRetrieval);

        monotonicMillis.addAndGet(1_001L);
        assertThrows(QueryBudgetExceededException.class, ledger::beginGeneration);

        QueryBudgetUsage usage = ledger.snapshot();
        assertEquals(2, usage.queryVariants());
        assertEquals(1, usage.retrievalPasses());
        assertEquals(0, usage.generationCalls());
        assertEquals(BudgetOutcome.DEADLINE_EXCEEDED, usage.outcome());
    }
}

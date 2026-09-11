package com.enterprise.rag.core.rag;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class C18BudgetAuditTest {
    @Test
    void completeAskCallGraphIncludesExplanatoryFallbackBeyondInitialVariants() throws Exception {
        var audit = C18BudgetAudit.auditRelease();
        assertEquals(150, audit.samples().size());
        assertEquals(451, audit.samples().stream().mapToInt(C18BudgetAudit.Sample::initialVariants).sum());
        assertTrue(audit.samples().stream().mapToInt(C18BudgetAudit.Sample::queryEmbeddingUpperBound).sum() > 902,
                "Counting only debug plus initial ask variants omits explanatory fallback");
        assertTrue(audit.samples().stream().allMatch(s -> s.askRetrievalPasses() >= 1));
        C18BudgetAudit.writeIfRequested(audit);
    }
}

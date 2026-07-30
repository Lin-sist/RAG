package com.enterprise.rag.core.rag.router;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedQueryRouterTest {

    @Test
    void defaultConfigurationLeavesLegacyExecutionUnrouted() {
        RouterProperties properties = new RouterProperties();
        BoundedQueryRouter router = new BoundedQueryRouter(
                properties,
                new DeterministicFactIntentClassifier());

        assertTrue(router.plan("什么是 JWT？").isEmpty());
    }

    @Test
    void enabledRouterCreatesVersionedFactPlanForDefinitionLookup() {
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        BoundedQueryRouter router = new BoundedQueryRouter(
                properties,
                new DeterministicFactIntentClassifier());

        QueryRoutePlan plan = router.plan("什么是 JWT？").orElseThrow();

        assertEquals("fact-intent-v1", plan.classifierVersion());
        assertEquals("evidence-no-answer-v1", plan.policyVersion());
        assertEquals(QueryIntent.FACT, plan.intent());
        assertEquals(QueryRouteReason.DEFINITION_CUE, plan.reason());
        assertEquals(QueryStrategyId.FACT_V1, plan.effectiveStrategy());
    }

    @Test
    void enabledRouterRejectsMultiHopInsteadOfFallingBackToLegacy() {
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        BoundedQueryRouter router = new BoundedQueryRouter(
                properties,
                new DeterministicFactIntentClassifier());

        QueryRoutePlan plan = router.plan("比较 JWT 与 OAuth，并总结它们的共同点").orElseThrow();

        assertEquals(QueryIntent.UNSUPPORTED, plan.intent());
        assertEquals(QueryRouteReason.MULTI_HOP_CUE, plan.reason());
        assertEquals(null, plan.effectiveStrategy());
    }

    @Test
    void enabledRouterMarksBlankInputInvalidBeforeExecution() {
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        BoundedQueryRouter router = new BoundedQueryRouter(
                properties,
                new DeterministicFactIntentClassifier());

        QueryRoutePlan plan = router.plan("   ").orElseThrow();

        assertEquals(QueryIntent.INVALID, plan.intent());
        assertEquals(QueryRouteReason.INVALID_INPUT, plan.reason());
        assertEquals(null, plan.effectiveStrategy());
    }

    @Test
    void enabledFactPlanCarriesServerOwnedExecutionBudget() {
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        BoundedQueryRouter router = new BoundedQueryRouter(
                properties,
                new DeterministicFactIntentClassifier());

        QueryExecutionBudget budget = router.plan("什么是 JWT？")
                .orElseThrow()
                .budget();

        assertEquals(8, budget.maxQueryVariants());
        assertEquals(1, budget.maxRetrievalPasses());
        assertEquals(1, budget.maxRerankCalls());
        assertEquals(1, budget.maxGenerationCalls());
        assertEquals(1200, budget.maxContextTokens());
        assertEquals(2048, budget.maxOutputTokens());
        assertEquals(120_000L, budget.deadlineMillis());
    }

    @Test
    void invalidBudgetFailsClosedWhenRouterIsConstructed() {
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        properties.getFact().setMaxRetrievalPasses(2);

        assertThrows(
                IllegalStateException.class,
                () -> new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));
    }

    @Test
    void classifierUsesBoundedClosedWorldCuePriority() {
        DeterministicFactIntentClassifier classifier = new DeterministicFactIntentClassifier();

        assertEquals(
                new QueryClassification(QueryIntent.INVALID, QueryRouteReason.INVALID_INPUT),
                classifier.classify("什么是 JWT\u0000"));
        assertEquals(
                new QueryClassification(QueryIntent.INVALID, QueryRouteReason.INVALID_INPUT),
                classifier.classify("x".repeat(513)));
        assertEquals(
                new QueryClassification(QueryIntent.UNSUPPORTED, QueryRouteReason.MULTI_HOP_CUE),
                classifier.classify("什么是 JWT，并比较它与 OAuth 的区别？"));
        assertEquals(
                new QueryClassification(QueryIntent.UNSUPPORTED, QueryRouteReason.GLOBAL_CUE),
                classifier.classify("总结所有文档中的安全建议"));
        assertEquals(
                new QueryClassification(QueryIntent.UNSUPPORTED, QueryRouteReason.HIGH_RISK_CUE),
                classifier.classify("根据这些材料给我投资建议"));
        assertEquals(
                new QueryClassification(QueryIntent.FACT, QueryRouteReason.FACT_LOOKUP_CUE),
                classifier.classify("JWT 是什么？"));
        assertEquals(
                new QueryClassification(QueryIntent.FACT, QueryRouteReason.FACT_LOOKUP_CUE),
                classifier.classify("OAuth 2.0 发布于哪一年？"));
        assertEquals(
                new QueryClassification(QueryIntent.UNSUPPORTED, QueryRouteReason.AMBIGUOUS),
                classifier.classify("聊聊 JWT"));
    }
}

package com.enterprise.rag.core.rag.router;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

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
    void unknownClassifierPolicyOrStrategyFailsClosedAtConstruction() {
        List<java.util.function.Consumer<RouterProperties>> mutations = List.of(
                properties -> properties.setClassifierVersion("classifier-v2"),
                properties -> properties.setPolicyVersion("policy-v2"),
                properties -> properties.setStrategyVersion("agentic-v1"));

        for (var mutation : mutations) {
            RouterProperties properties = new RouterProperties();
            mutation.accept(properties);
            assertThrows(IllegalStateException.class, () -> new BoundedQueryRouter(
                    properties, new DeterministicFactIntentClassifier()));
        }
    }

    @Test
    void everyBudgetHardLimitRejectsBelowMinimumOrAboveMaximum() {
        List<java.util.function.Consumer<RouterProperties.Fact>> mutations = List.of(
                fact -> fact.setMaxQueryVariants(0),
                fact -> fact.setMaxQueryVariants(33),
                fact -> fact.setMaxRetrievalPasses(0),
                fact -> fact.setMaxRetrievalPasses(2),
                fact -> fact.setMaxRerankCalls(-1),
                fact -> fact.setMaxRerankCalls(2),
                fact -> fact.setMaxGenerationCalls(0),
                fact -> fact.setMaxGenerationCalls(2),
                fact -> fact.setMaxContextTokens(0),
                fact -> fact.setMaxContextTokens(1201),
                fact -> fact.setMaxOutputTokens(0),
                fact -> fact.setMaxOutputTokens(2049),
                fact -> fact.setDeadlineMillis(999),
                fact -> fact.setDeadlineMillis(120_001));

        for (var mutation : mutations) {
            RouterProperties properties = new RouterProperties();
            mutation.accept(properties.getFact());
            assertThrows(IllegalStateException.class, () -> new BoundedQueryRouter(
                    properties, new DeterministicFactIntentClassifier()));
        }
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


    @Test
    void classifierMatrixCoversDefinitionUnsupportedAmbiguousAndInvalidInputs() {
        DeterministicFactIntentClassifier classifier = new DeterministicFactIntentClassifier();
        List<TestCase> cases = List.of(
                new TestCase("何谓 RRF？", QueryIntent.FACT, QueryRouteReason.DEFINITION_CUE),
                new TestCase("谁是 Java 的最初设计者？", QueryIntent.FACT, QueryRouteReason.FACT_LOOKUP_CUE),
                new TestCase("OAuth 定义在哪里？", QueryIntent.FACT, QueryRouteReason.FACT_LOOKUP_CUE),
                new TestCase("为什么要分块？", QueryIntent.UNSUPPORTED, QueryRouteReason.AMBIGUOUS),
                new TestCase("规划一个完整 RAG 平台", QueryIntent.UNSUPPORTED, QueryRouteReason.AMBIGUOUS),
                new TestCase("全面分析知识库", QueryIntent.UNSUPPORTED, QueryRouteReason.GLOBAL_CUE),
                new TestCase("给我用药建议", QueryIntent.UNSUPPORTED, QueryRouteReason.HIGH_RISK_CUE),
                new TestCase("什么是 RAG，并分别说明优缺点", QueryIntent.UNSUPPORTED, QueryRouteReason.MULTI_HOP_CUE),
                new TestCase("\t\r\n", QueryIntent.INVALID, QueryRouteReason.INVALID_INPUT),
                new TestCase("正常文本\u0007", QueryIntent.INVALID, QueryRouteReason.INVALID_INPUT));

        for (TestCase testCase : cases) {
            assertEquals(
                    new QueryClassification(testCase.intent(), testCase.reason()),
                    classifier.classify(testCase.query()),
                    testCase.query());
        }
    }

    @Test
    void classifierIsByteStableAcrossRepeatedConcurrentLocaleAndTimezoneChanges() throws Exception {
        DeterministicFactIntentClassifier classifier = new DeterministicFactIntentClassifier();
        QueryClassification expected = classifier.classify("  什么是  ＪＷＴ？  ");
        Locale originalLocale = Locale.getDefault();
        TimeZone originalTimeZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
            var executor = Executors.newFixedThreadPool(8);
            try {
                List<Callable<QueryClassification>> tasks = new ArrayList<>();
                for (int index = 0; index < 1_000; index++) {
                    tasks.add(() -> classifier.classify("  什么是  ＪＷＴ？  "));
                }
                for (var future : executor.invokeAll(tasks)) {
                    assertEquals(expected, future.get());
                }
            } finally {
                executor.shutdownNow();
            }
        } finally {
            Locale.setDefault(originalLocale);
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    void classifierFuzzRemainsBoundedAndDeterministicForUnicode() {
        DeterministicFactIntentClassifier classifier = new DeterministicFactIntentClassifier();
        assertTimeout(Duration.ofSeconds(2), () -> {
            long state = 0x5eedL;
            for (int sample = 0; sample < 10_000; sample++) {
                StringBuilder query = new StringBuilder();
                int length = sample % 513;
                for (int index = 0; index < length; index++) {
                    state = state * 6364136223846793005L + 1442695040888963407L;
                    int codePoint = 0x20 + (int) Math.floorMod(state, 0xD7FF - 0x20);
                    query.appendCodePoint(codePoint);
                }
                String value = query.toString();
                assertEquals(classifier.classify(value), classifier.classify(value));
            }
        });
    }

    private record TestCase(String query, QueryIntent intent, QueryRouteReason reason) {
    }
}

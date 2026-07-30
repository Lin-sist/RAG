package com.enterprise.rag.core.rag;

import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.generator.LLMException;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.rag.service.RAGServiceImpl;
import com.enterprise.rag.core.rag.router.BoundedQueryRouter;
import com.enterprise.rag.core.rag.router.DeterministicFactIntentClassifier;
import com.enterprise.rag.core.rag.router.RouterProperties;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RAGServiceImplTest {
    private static final TenantVectorScope SCOPE = new TenantVectorScope(1L, 1L, "kb_rag");

    private QueryEngine queryEngine;
    private AnswerGenerator answerGenerator;
    private RedisUtil redisUtil;
    private RAGServiceImpl ragService;

    @BeforeEach
    void setUp() {
        queryEngine = mock(QueryEngine.class);
        answerGenerator = mock(AnswerGenerator.class);
        redisUtil = mock(RedisUtil.class);

        when(answerGenerator.getModelName()).thenReturn("mock-model");
        when(redisUtil.getString(any())).thenReturn(null);
        when(queryEngine.retrieve(any(), org.mockito.ArgumentMatchers.<RetrieveOptions>any())).thenReturn(List.of());
        doAnswer(invocation -> RetrievalResult.complete(queryEngine.retrieve(
                        invocation.getArgument(0, String.class),
                        invocation.getArgument(1, RetrieveOptions.class))))
                .when(queryEngine).retrieveWithDiagnostics(any(), org.mockito.ArgumentMatchers.<RetrieveOptions>any());
        when(answerGenerator.generate(any(), any())).thenReturn(
                GeneratedAnswer.of("RAG 的工作原理是先检索再生成。", List.of(), Map.of("model", "mock-model")));

        ragService = new RAGServiceImpl(queryEngine, answerGenerator, redisUtil, new ObjectMapper());
    }

    @Test
    void shouldRetryExplanatoryQuestionWithFallbackQueryWhenInitialRetrievalIsEmpty() {
        RetrievedContext fallbackContext = new RetrievedContext(
                "RAG 的工作原理是先检索知识片段，再把上下文交给大模型生成答案。",
                "rag-doc",
                0.28f,
                Map.of("title", "RAG 原理"));

        doReturn(new RetrievalResult(List.of(), Map.of(
                "rerankRequestedProvider", "nvidia",
                "rerankEffectiveProvider", "nvidia",
                "rerankFallbackCount", 0,
                "rerankFallbackReason", "none",
                "rerankModelCallCount", 1,
                "rerankLatencyMillis", 20)))
                .when(queryEngine).retrieveWithDiagnostics(
                        eq("你认为RAG是如何运作的？"), org.mockito.ArgumentMatchers.<RetrieveOptions>any());
        doReturn(new RetrievalResult(List.of(fallbackContext), Map.of(
                "rerankRequestedProvider", "nvidia",
                "rerankEffectiveProvider", "heuristic",
                "rerankFallbackCount", 1,
                "rerankFallbackReason", "timeout",
                "rerankModelCallCount", 1,
                "rerankLatencyMillis", 30)))
                .when(queryEngine).retrieveWithDiagnostics(
                        eq("RAG 工作原理"), org.mockito.ArgumentMatchers.<RetrieveOptions>any());

        QAResponse response = ragService.ask(QARequest.of("你认为RAG是如何运作的？", SCOPE));

        assertTrue(response.hasResult());
        assertEquals(1, response.contexts().size());
        assertEquals("rag-doc", response.contexts().get(0).source());
        assertEquals("heuristic", response.metadata().get("rerankEffectiveProvider"));
        assertEquals("timeout", response.metadata().get("rerankFallbackReason"));
        assertEquals(1, response.metadata().get("rerankFallbackCount"));
        assertEquals(2, response.metadata().get("rerankModelCallCount"));
        assertEquals(50L, response.metadata().get("rerankLatencyMillis"));
        verify(queryEngine).retrieveWithDiagnostics(eq("RAG 工作原理"),
                argThat((RetrieveOptions options) -> options.minScore() <= 0.15f));
    }

    @Test
    void shouldNotRetryNonExplanatoryQuestionWhenInitialRetrievalIsEmpty() {
        when(queryEngine.retrieve(eq("RAG"), org.mockito.ArgumentMatchers.<RetrieveOptions>any())).thenReturn(List.of());

        QAResponse response = ragService.ask(QARequest.of("RAG", SCOPE));

        assertTrue(!response.hasResult());
        verify(queryEngine, never()).retrieve(eq("RAG 工作原理"), org.mockito.ArgumentMatchers.<RetrieveOptions>any());
    }

    @Test
    void shouldExposeLlmDiagnosticsOnGenerationFailure() {
        RetrievedContext context = new RetrievedContext(
                "Spring Boot 提供自动配置、起步依赖和 Actuator。",
                "springboot-basics.md",
                0.91f,
                Map.of("title", "Spring Boot"));
        when(queryEngine.retrieve(eq("Spring Boot 的核心特性有哪些？"), org.mockito.ArgumentMatchers.<RetrieveOptions>any()))
                .thenReturn(List.of(context));
        when(answerGenerator.generate(eq("Spring Boot 的核心特性有哪些？"), org.mockito.ArgumentMatchers.anyList()))
                .thenThrow(new LLMException(
                        "synthetic provider secret marker",
                        Map.of(
                                "provider", "openai",
                                "endpoint", "/chat/completions",
                                "model", "nvidia/test",
                                "timeoutSeconds", 120,
                                "maxRetries", 3,
                                "attemptCount", 4,
                                "retryCount", 3,
                                "retryExhausted", true,
                                "errorType", "TimeoutException",
                                "errorCategory", "timeout")));

        QAResponse response = ragService.ask(QARequest.of("Spring Boot 的核心特性有哪些？", SCOPE));

        assertEquals("error", response.metadata().get("status"));
        assertEquals("openai", response.metadata().get("llmProvider"));
        assertEquals("/chat/completions", response.metadata().get("llmEndpoint"));
        assertEquals(120, response.metadata().get("llmTimeoutSeconds"));
        assertEquals(4, response.metadata().get("llmAttemptCount"));
        assertEquals(3, response.metadata().get("llmRetryCount"));
        assertEquals(true, response.metadata().get("llmRetryExhausted"));
        assertEquals("timeout", response.metadata().get("llmErrorCategory"));
        assertEquals("抱歉，处理您的问题时发生错误：模型服务响应超时，请稍后重试", response.answer());
        assertTrue(!response.answer().contains("synthetic provider secret marker"));
        assertTrue(response.citations().isEmpty());
        assertTrue(response.contexts().isEmpty());
        verify(redisUtil, never()).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldContinueCanonicalAnswerWhenQaCacheReadFails() {
        RetrievedContext context = new RetrievedContext(
                "Redis cache 不是问答事实源。",
                "cache-contract.md",
                0.88f,
                Map.of());
        when(redisUtil.getString(anyString())).thenThrow(new RuntimeException("synthetic redis marker"));
        when(queryEngine.retrieve(eq("缓存故障时还能回答吗？"),
                org.mockito.ArgumentMatchers.<RetrieveOptions>any())).thenReturn(List.of(context));

        QAResponse response = ragService.ask(QARequest.of("缓存故障时还能回答吗？", SCOPE));

        assertTrue(response.hasResult());
        assertEquals("RAG 的工作原理是先检索再生成。", response.answer());
    }

    @Test
    void shouldKeepCanonicalAnswerWhenQaCacheWriteFails() {
        RetrievedContext context = new RetrievedContext(
                "缓存写入失败只影响后续命中。",
                "cache-contract.md",
                0.88f,
                Map.of());
        when(queryEngine.retrieve(eq("缓存写失败会丢答案吗？"),
                org.mockito.ArgumentMatchers.<RetrieveOptions>any())).thenReturn(List.of(context));
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        QAResponse response = ragService.ask(QARequest.of("缓存写失败会丢答案吗？", SCOPE));

        assertTrue(response.hasResult());
        assertEquals("RAG 的工作原理是先检索再生成。", response.answer());
    }

    @Test
    void keywordOnlyDegradationShouldBeVisibleAndShouldNotWriteSuccessCache() {
        RetrievedContext context = new RetrievedContext(
                "关键词路线仍有可用证据。",
                "keyword-doc",
                0.72f,
                Map.of());
        doReturn(new RetrievalResult(List.of(context), Map.of(
                "retrievalMode", "keyword_only",
                "retrievalDegraded", true,
                "degradedDependency", "milvus",
                "rerankRequestedProvider", "nvidia",
                "rerankEffectiveProvider", "heuristic",
                "rerankFallbackReason", "timeout",
                "rerankModelCallCount", 1)))
                .when(queryEngine).retrieveWithDiagnostics(eq("Milvus 故障时还能回答吗？"),
                        org.mockito.ArgumentMatchers.<RetrieveOptions>any());

        QAResponse response = ragService.ask(QARequest.of("Milvus 故障时还能回答吗？", SCOPE));

        assertTrue(response.hasResult());
        assertEquals("keyword_only", response.metadata().get("retrievalMode"));
        assertEquals(true, response.metadata().get("retrievalDegraded"));
        assertEquals("milvus", response.metadata().get("degradedDependency"));
        assertEquals("nvidia", response.metadata().get("rerankRequestedProvider"));
        assertEquals("heuristic", response.metadata().get("rerankEffectiveProvider"));
        assertEquals("timeout", response.metadata().get("rerankFallbackReason"));
        assertEquals(1, response.metadata().get("rerankModelCallCount"));
        verify(answerGenerator).generate(eq("Milvus 故障时还能回答吗？"), eq(List.of(context)));
        verify(redisUtil, never()).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void qaCacheEvictionShouldRemainBestEffortWhenRedisFails() {
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).deleteByPattern(anyString());

        ragService.evictCache("缓存失效", SCOPE);
    }

    @Test
    void qaCacheClearShouldRemainBestEffortWhenRedisFails() {
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).deleteByPattern(anyString());

        ragService.clearCache(SCOPE);
    }

    @Test
    void enabledRouterStopsUnsupportedQuestionBeforeRetrievalOrGeneration() {
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        RAGServiceImpl routedService = new RAGServiceImpl(
                queryEngine,
                answerGenerator,
                redisUtil,
                new ObjectMapper(),
                new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));

        QAResponse response = routedService.ask(QARequest.of(
                "比较 JWT 与 OAuth，并总结它们的共同点",
                SCOPE));

        assertEquals("unsupported", response.metadata().get("status"));
        assertEquals("fact-intent-v1", response.metadata().get("routeClassifierVersion"));
        assertEquals("evidence-no-answer-v1", response.metadata().get("routePolicyVersion"));
        assertEquals("UNSUPPORTED", response.metadata().get("routeFinalState"));
        assertEquals("MULTI_HOP_CUE", response.metadata().get("routeReason"));
        verify(queryEngine, never()).retrieveWithDiagnostics(any(), any());
        verify(answerGenerator, never()).generate(any(), any());
    }

    @Test
    void enabledFactRouteReturnsUnifiedNoAnswerWithoutGenerationWhenEvidenceIsEmpty() {
        doReturn(new RetrievalResult(
                List.of(),
                Map.of("queryVariantCount", 1, "rerankModelCallCount", 0)))
                .when(queryEngine).retrieveWithDiagnostics(eq("什么是 JWT？"), any());
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        RAGServiceImpl routedService = new RAGServiceImpl(
                queryEngine,
                answerGenerator,
                redisUtil,
                new ObjectMapper(),
                new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));

        QAResponse response = routedService.ask(QARequest.of("什么是 JWT？", SCOPE));

        assertEquals("no_result", response.metadata().get("status"));
        assertEquals("fact-v1", response.metadata().get("routeEffectiveStrategy"));
        assertEquals("NO_ANSWER", response.metadata().get("routeFinalState"));
        assertEquals("INSUFFICIENT_EVIDENCE", response.metadata().get("noAnswerReason"));
        assertEquals(1, response.metadata().get("routeRetrievalPasses"));
        assertEquals(0, response.metadata().get("routeGenerationCalls"));
        verify(queryEngine).retrieveWithDiagnostics(
                eq("什么是 JWT？"),
                argThat(options -> options.maxQueryVariants() == 8));
        verify(answerGenerator, never()).generate(any(), any());
    }

    @Test
    void enabledFactRouteReturnsEvidenceBackedAnswerWithBoundedAttribution() {
        RetrievedContext context = new RetrievedContext(
                "JWT 是一种紧凑的声明传输格式。",
                "chunk-1",
                0.91f,
                Map.of("documentId", 1L, "chunkId", "chunk-1"));
        Citation citation = Citation.grounded(
                "chunk-1", 1L, "chunk-1", 0.91d, "JWT 是一种紧凑的声明传输格式。", -1, -1);
        doReturn(new RetrievalResult(
                List.of(context),
                Map.of("queryVariantCount", 1, "rerankModelCallCount", 0)))
                .when(queryEngine).retrieveWithDiagnostics(eq("什么是 JWT？"), any());
        when(answerGenerator.generate(eq("什么是 JWT？"), eq(List.of(context))))
                .thenReturn(GeneratedAnswer.of(
                        "JWT 是一种紧凑的声明传输格式。",
                        List.of(citation),
                        Map.of("model", "deterministic", "validCitations", 1)));
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        RAGServiceImpl routedService = new RAGServiceImpl(
                queryEngine,
                answerGenerator,
                redisUtil,
                new ObjectMapper(),
                new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));

        QAResponse response = routedService.ask(QARequest.of("什么是 JWT？", SCOPE));

        assertEquals("fact-v1", response.metadata().get("routeEffectiveStrategy"));
        assertEquals("ANSWER", response.metadata().get("routeFinalState"));
        assertEquals("NONE", response.metadata().get("noAnswerReason"));
        assertEquals(1, response.metadata().get("routeRetrievalPasses"));
        assertEquals(1, response.metadata().get("routeGenerationCalls"));
        assertEquals(1, response.metadata().get("routeQueryVariants"));
        assertEquals(0, response.metadata().get("routeRerankCalls"));
        assertEquals("WITHIN_BUDGET", response.metadata().get("routeBudgetOutcome"));
        assertEquals(1, response.citations().size());
    }

    @Test
    void enabledFactRouteRefusesAnswerWithoutValidatedCitationEvidence() {
        RetrievedContext context = new RetrievedContext(
                "JWT 是一种紧凑的声明传输格式。",
                "chunk-1",
                0.91f,
                Map.of("documentId", 1L, "chunkId", "chunk-1"));
        doReturn(new RetrievalResult(
                List.of(context),
                Map.of("queryVariantCount", 1, "rerankModelCallCount", 0)))
                .when(queryEngine).retrieveWithDiagnostics(eq("什么是 JWT？"), any());
        when(answerGenerator.generate(eq("什么是 JWT？"), eq(List.of(context))))
                .thenReturn(GeneratedAnswer.of(
                        "JWT 是一种紧凑的声明传输格式。",
                        List.of(),
                        Map.of("model", "deterministic")));
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        RAGServiceImpl routedService = new RAGServiceImpl(
                queryEngine,
                answerGenerator,
                redisUtil,
                new ObjectMapper(),
                new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));

        QAResponse response = routedService.ask(QARequest.of("什么是 JWT？", SCOPE));

        assertEquals("no_result", response.metadata().get("status"));
        assertEquals("NO_ANSWER", response.metadata().get("routeFinalState"));
        assertEquals("UNVALIDATED_EVIDENCE", response.metadata().get("noAnswerReason"));
        assertEquals(1, response.metadata().get("routeGenerationCalls"));
        assertTrue(response.citations().isEmpty());
    }

    @Test
    void enabledFactRouteKeepsRetrievalDependencyFailureOutOfNoAnswer() {
        doThrow(VectorDependencyException.unavailable(
                "search",
                new IllegalStateException("synthetic vector marker")))
                .when(queryEngine).retrieveWithDiagnostics(eq("什么是 JWT？"), any());
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        RAGServiceImpl routedService = new RAGServiceImpl(
                queryEngine,
                answerGenerator,
                redisUtil,
                new ObjectMapper(),
                new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));

        QAResponse response = routedService.ask(QARequest.of("什么是 JWT？", SCOPE));

        assertEquals("error", response.metadata().get("status"));
        assertEquals("ERROR", response.metadata().get("routeFinalState"));
        assertEquals("NONE", response.metadata().get("noAnswerReason"));
        assertEquals(0, response.metadata().get("routeGenerationCalls"));
        verify(answerGenerator, never()).generate(any(), any());
    }

    @Test
    void enabledStreamRejectsUnsupportedBeforeRetrievalAndRecordsTerminalRoute() {
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        RAGServiceImpl routedService = new RAGServiceImpl(
                queryEngine,
                answerGenerator,
                redisUtil,
                new ObjectMapper(),
                new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));
        com.enterprise.rag.core.rag.service.RAGService.StreamTerminalSignal terminalSignal =
                new com.enterprise.rag.core.rag.service.RAGService.StreamTerminalSignal();

        List<String> chunks = routedService.askStream(QARequest.stream(
                        "比较 JWT 与 OAuth，并总结它们的共同点",
                        SCOPE))
                .contextWrite(context -> context.put(
                        com.enterprise.rag.core.rag.service.RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY,
                        terminalSignal))
                .collectList()
                .block();

        assertEquals(List.of("当前有界路由仅支持事实型问题，请改为单一事实查询。"), chunks);
        assertEquals("UNSUPPORTED", terminalSignal.finalState());
        assertEquals("MULTI_HOP_CUE", terminalSignal.routeReason());
        assertEquals("none", terminalSignal.effectiveStrategy());
        assertEquals("evidence-no-answer-v1", terminalSignal.policyVersion());
        verify(queryEngine, never()).retrieveWithDiagnostics(any(), any());
        verify(answerGenerator, never()).generateStream(any(), any());
    }

    @Test
    void enabledFactStreamUsesSingleBoundedRetrievalAndRecordsNoAnswer() {
        doReturn(new RetrievalResult(
                List.of(),
                Map.of("queryVariantCount", 1, "rerankModelCallCount", 0)))
                .when(queryEngine).retrieveWithDiagnostics(eq("什么是 JWT？"), any());
        RouterProperties properties = new RouterProperties();
        properties.setEnabled(true);
        RAGServiceImpl routedService = new RAGServiceImpl(
                queryEngine,
                answerGenerator,
                redisUtil,
                new ObjectMapper(),
                new BoundedQueryRouter(properties, new DeterministicFactIntentClassifier()));
        com.enterprise.rag.core.rag.service.RAGService.StreamTerminalSignal terminalSignal =
                new com.enterprise.rag.core.rag.service.RAGService.StreamTerminalSignal();

        List<String> chunks = routedService.askStream(QARequest.stream("什么是 JWT？", SCOPE))
                .contextWrite(context -> context.put(
                        com.enterprise.rag.core.rag.service.RAGService.STREAM_TERMINAL_SIGNAL_CONTEXT_KEY,
                        terminalSignal))
                .collectList()
                .block();

        assertEquals(List.of("抱歉，未能找到与您问题相关的信息。请尝试换一种方式提问或提供更多细节。"), chunks);
        assertEquals("NO_ANSWER", terminalSignal.finalState());
        assertEquals("fact-v1", terminalSignal.effectiveStrategy());
        verify(queryEngine).retrieveWithDiagnostics(
                eq("什么是 JWT？"),
                argThat(options -> options.maxQueryVariants() == 8));
        verify(answerGenerator, never()).generateStream(any(), any());
    }
}

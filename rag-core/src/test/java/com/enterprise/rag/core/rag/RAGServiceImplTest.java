package com.enterprise.rag.core.rag;

import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.generator.LLMException;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.rag.service.RAGServiceImpl;
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

        when(queryEngine.retrieve(eq("你认为RAG是如何运作的？"), org.mockito.ArgumentMatchers.<RetrieveOptions>any()))
                .thenReturn(List.of());
        when(queryEngine.retrieve(eq("RAG 工作原理"), org.mockito.ArgumentMatchers.<RetrieveOptions>any()))
                .thenReturn(List.of(fallbackContext));

        QAResponse response = ragService.ask(QARequest.of("你认为RAG是如何运作的？", "kb_rag"));

        assertTrue(response.hasResult());
        assertEquals(1, response.contexts().size());
        assertEquals("rag-doc", response.contexts().get(0).source());
        verify(queryEngine).retrieve(eq("RAG 工作原理"),
                argThat((RetrieveOptions options) -> options.minScore() <= 0.15f));
    }

    @Test
    void shouldNotRetryNonExplanatoryQuestionWhenInitialRetrievalIsEmpty() {
        when(queryEngine.retrieve(eq("RAG"), org.mockito.ArgumentMatchers.<RetrieveOptions>any())).thenReturn(List.of());

        QAResponse response = ragService.ask(QARequest.of("RAG", "kb_rag"));

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

        QAResponse response = ragService.ask(QARequest.of("Spring Boot 的核心特性有哪些？", "kb_rag"));

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

        QAResponse response = ragService.ask(QARequest.of("缓存故障时还能回答吗？", "kb_rag"));

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

        QAResponse response = ragService.ask(QARequest.of("缓存写失败会丢答案吗？", "kb_rag"));

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
        doReturn(RetrievalResult.keywordOnly(List.of(context)))
                .when(queryEngine).retrieveWithDiagnostics(eq("Milvus 故障时还能回答吗？"),
                        org.mockito.ArgumentMatchers.<RetrieveOptions>any());

        QAResponse response = ragService.ask(QARequest.of("Milvus 故障时还能回答吗？", "kb_rag"));

        assertTrue(response.hasResult());
        assertEquals("keyword_only", response.metadata().get("retrievalMode"));
        assertEquals(true, response.metadata().get("retrievalDegraded"));
        assertEquals("milvus", response.metadata().get("degradedDependency"));
        verify(answerGenerator).generate(eq("Milvus 故障时还能回答吗？"), eq(List.of(context)));
        verify(redisUtil, never()).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void qaCacheEvictionShouldRemainBestEffortWhenRedisFails() {
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).deleteByPattern(anyString());

        ragService.evictCache("缓存失效", "kb_rag");
    }

    @Test
    void qaCacheClearShouldRemainBestEffortWhenRedisFails() {
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(redisUtil).deleteByPattern(anyString());

        ragService.clearAllCache();
    }
}

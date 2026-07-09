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
import com.enterprise.rag.core.rag.service.RAGServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
                        "LLM API call failed: timed out",
                        Map.of(
                                "provider", "openai",
                                "endpoint", "/chat/completions",
                                "model", "nvidia/test",
                                "timeoutSeconds", 120,
                                "maxRetries", 3,
                                "errorType", "TimeoutException",
                                "errorCategory", "timeout")));

        QAResponse response = ragService.ask(QARequest.of("Spring Boot 的核心特性有哪些？", "kb_rag"));

        assertEquals("error", response.metadata().get("status"));
        assertEquals("openai", response.metadata().get("llmProvider"));
        assertEquals("/chat/completions", response.metadata().get("llmEndpoint"));
        assertEquals(120, response.metadata().get("llmTimeoutSeconds"));
        assertEquals("timeout", response.metadata().get("llmErrorCategory"));
    }
}

package com.enterprise.rag.core.rag;

import com.enterprise.rag.common.util.RedisUtil;
import com.enterprise.rag.core.rag.generator.AnswerGenerator;
import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.GeneratedAnswer;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.service.RAGService;
import com.enterprise.rag.core.rag.service.RAGServiceImpl;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RAGServiceCacheKeyTest {

    private QueryEngine queryEngine;
    private AnswerGenerator answerGenerator;
    private RedisUtil redisUtil;
    private RAGService ragService;
    private Map<String, String> cache;

    @BeforeEach
    void setUp() {
        queryEngine = mock(QueryEngine.class);
        answerGenerator = mock(AnswerGenerator.class);
        redisUtil = mock(RedisUtil.class);

        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("Java 线程池参数详解", "doc-thread-pool", 0.9f, Map.of()));
        when(queryEngine.retrieve(anyString(), any(RetrieveOptions.class))).thenReturn(contexts);

        when(answerGenerator.getModelName()).thenReturn("mock-model");
        when(answerGenerator.generate(anyString(), any(List.class))).thenReturn(
                GeneratedAnswer.of("answer", List.of(Citation.of("doc-thread-pool", "Java 线程池参数详解")),
                        Map.of("model", "mock-model")));

        cache = new HashMap<>();
        when(redisUtil.getString(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            cache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(redisUtil).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        ragService = new RAGServiceImpl(queryEngine, answerGenerator, redisUtil, new ObjectMapper());
    }

    @Test
    void shouldNotShareCacheBetweenDifferentTopK() {
        TenantVectorScope scope = new TenantVectorScope(11L, 31L, "kb_java");
        QARequest topK3 = new QARequest("什么是线程池", scope, 3, Map.of(), true, false);
        QARequest topK6 = new QARequest("什么是线程池", scope, 6, Map.of(), true, false);

        ragService.ask(topK3);
        ragService.ask(topK6);

        verify(queryEngine, times(2)).retrieve(anyString(), any(RetrieveOptions.class));

        ragService.ask(topK3);
        verify(queryEngine, times(2)).retrieve(anyString(), any(RetrieveOptions.class));
    }

    @Test
    void shouldNotShareCacheBetweenDifferentFilter() {
        TenantVectorScope scope = new TenantVectorScope(11L, 31L, "kb_java");
        QARequest filterA = new QARequest("什么是线程池", scope, 5, Map.of("docType", "md"), true, false);
        QARequest filterB = new QARequest("什么是线程池", scope, 5, Map.of("docType", "pdf"), true, false);

        var responseA = ragService.ask(filterA);
        var responseB = ragService.ask(filterB);

        assertTrue(responseA.isSuccess());
        assertTrue(responseB.isSuccess());
        verify(queryEngine, times(2)).retrieve(anyString(), any(RetrieveOptions.class));
    }

    @Test
    void shouldNamespaceCacheByTenantAndKnowledgeBase() {
        TenantVectorScope tenantA = new TenantVectorScope(11L, 31L, "kb_shared");
        TenantVectorScope tenantB = new TenantVectorScope(12L, 31L, "kb_shared");
        QARequest requestA = new QARequest(
                "什么是线程池", tenantA, 5, QARequest.DEFAULT_MIN_SCORE, Map.of(), true, false);
        QARequest requestB = new QARequest(
                "什么是线程池", tenantB, 5, QARequest.DEFAULT_MIN_SCORE, Map.of(), true, false);

        ragService.ask(requestA);
        ragService.ask(requestB);
        ragService.ask(requestA);

        verify(queryEngine, times(2)).retrieve(anyString(), any(RetrieveOptions.class));
        var writtenKeys = new java.util.ArrayList<String>();
        verify(redisUtil, times(2)).setString(
                org.mockito.ArgumentMatchers.argThat(key -> {
                    writtenKeys.add(key);
                    return key.startsWith("qa:cache:v2:");
                }),
                anyString(), anyLong(), any(TimeUnit.class));
        assertEquals(2, writtenKeys.stream().distinct().count());
        assertTrue(writtenKeys.stream().anyMatch(key -> key.startsWith("qa:cache:v2:11:31:")));
        assertTrue(writtenKeys.stream().anyMatch(key -> key.startsWith("qa:cache:v2:12:31:")));
    }

    @Test
    void shouldTreatCachedPayloadScopeMismatchAsMiss() {
        TenantVectorScope tenantA = new TenantVectorScope(11L, 31L, "kb_shared");
        TenantVectorScope tenantB = new TenantVectorScope(12L, 31L, "kb_shared");
        QARequest requestA = new QARequest(
                "什么是线程池", tenantA, 5, QARequest.DEFAULT_MIN_SCORE, Map.of(), true, false);
        QARequest requestB = new QARequest(
                "什么是线程池", tenantB, 5, QARequest.DEFAULT_MIN_SCORE, Map.of(), true, false);

        ragService.ask(requestA);
        String tenantAKey = cache.keySet().iterator().next();
        String tenantBKey = tenantAKey.replace("qa:cache:v2:11:31:", "qa:cache:v2:12:31:");
        cache.put(tenantBKey, cache.get(tenantAKey));

        ragService.ask(requestB);

        verify(queryEngine, times(2)).retrieve(anyString(), any(RetrieveOptions.class));
    }
}

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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
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

        Map<String, String> cache = new HashMap<>();
        when(redisUtil.getString(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            cache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(redisUtil).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        ragService = new RAGServiceImpl(queryEngine, answerGenerator, redisUtil, new ObjectMapper());
    }

    @Test
    void shouldNotShareCacheBetweenDifferentTopK() {
        QARequest topK3 = new QARequest("什么是线程池", "kb_java", 3, Map.of(), true, false);
        QARequest topK6 = new QARequest("什么是线程池", "kb_java", 6, Map.of(), true, false);

        ragService.ask(topK3);
        ragService.ask(topK6);

        verify(queryEngine, times(2)).retrieve(anyString(), any(RetrieveOptions.class));

        ragService.ask(topK3);
        verify(queryEngine, times(2)).retrieve(anyString(), any(RetrieveOptions.class));
    }

    @Test
    void shouldNotShareCacheBetweenDifferentFilter() {
        QARequest filterA = new QARequest("什么是线程池", "kb_java", 5, Map.of("docType", "md"), true, false);
        QARequest filterB = new QARequest("什么是线程池", "kb_java", 5, Map.of("docType", "pdf"), true, false);

        var responseA = ragService.ask(filterA);
        var responseB = ragService.ask(filterB);

        assertTrue(responseA.isSuccess());
        assertTrue(responseB.isSuccess());
        verify(queryEngine, times(2)).retrieve(anyString(), any(RetrieveOptions.class));
    }
}

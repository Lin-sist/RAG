package com.enterprise.rag.core.rag.query;

import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryEngineImplTest {

    private EmbeddingService embeddingService;
    private VectorStore vectorStore;
    private QueryEngineImpl queryEngine;

    @BeforeEach
    void setUp() {
        embeddingService = mock(EmbeddingService.class);
        vectorStore = mock(VectorStore.class);
        queryEngine = new QueryEngineImpl(embeddingService, vectorStore);

        when(embeddingService.embed(anyString())).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });
    }

    @Test
    void shouldRerankForChineseQueryUsingCjkTerms() {
        List<SearchResult> results = List.of(
                new SearchResult("a", "这是缓存实现细节说明", 0.90f, Map.of()),
                new SearchResult("b", "本文介绍重排策略优化方法与实践", 0.70f, Map.of()));
        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class))).thenReturn(results);

        List<RetrievedContext> contexts = queryEngine.retrieve(
                "重排优化",
                new RetrieveOptions("kb_test", 5, 0.0f, Map.of(), true));

        assertEquals(2, contexts.size());
        assertEquals("b", contexts.get(0).source());
    }

    @Test
    void shouldKeepEnglishKeywordRerankBehavior() {
        List<SearchResult> results = List.of(
                new SearchResult("a", "mysql index tuning guide", 0.88f, Map.of()),
                new SearchResult("b", "spring security authentication best practices", 0.72f, Map.of()));
        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class))).thenReturn(results);

        List<RetrievedContext> contexts = queryEngine.retrieve(
                "spring security auth",
                new RetrieveOptions("kb_test", 5, 0.0f, Map.of(), true));

        assertEquals(2, contexts.size());
        assertEquals("b", contexts.get(0).source());
    }
}

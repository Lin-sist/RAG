package com.enterprise.rag.core.rag.query;

import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.rerank.HeuristicReranker;
import com.enterprise.rag.core.rag.rerank.ModelReranker;
import com.enterprise.rag.core.rag.rerank.RerankerRegistry;
import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Test
    void shouldRewriteConversationalJwtQueryAndMergeResults() {
        when(embeddingService.embed(anyString())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0, String.class).toLowerCase();
            if (query.contains("json web token")) {
                return new float[] { 2.0f, 0.0f, 0.0f };
            }
            if ("jwt".equals(query)) {
                return new float[] { 1.0f, 0.0f, 0.0f };
            }
            return new float[] { 0.0f, 0.0f, 0.0f };
        });

        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class))).thenAnswer(invocation -> {
            float[] queryVector = invocation.getArgument(1, float[].class);
            if (queryVector[0] == 2.0f) {
                return List.of(
                        new SearchResult("jwt-doc", "JSON Web Token is a compact claims format", 0.92f, Map.of()));
            }
            if (queryVector[0] == 1.0f) {
                return List.of(
                        new SearchResult("jwt-doc", "JWT is the short name for JSON Web Token", 0.82f, Map.of()),
                        new SearchResult("noise-doc", "Session storage cleanup guide", 0.65f, Map.of()));
            }
            return List.of(
                    new SearchResult("noise-doc", "Session storage cleanup guide", 0.75f, Map.of()));
        });

        List<RetrievedContext> contexts = queryEngine.retrieve(
                "什么是JWT？",
                new RetrieveOptions("kb_test", 5, 0.0f, Map.of(), true));

        assertEquals(2, contexts.size());
        assertEquals("jwt-doc", contexts.get(0).source());
        verify(embeddingService).embed("JWT");
        verify(embeddingService).embed("json web token");
    }

    @Test
    void shouldLimitMergedResultsBackToRequestedTopK() {
        when(embeddingService.embed(anyString())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0, String.class).toLowerCase();
            if (query.contains("json web token")) {
                return new float[] { 2.0f, 0.0f, 0.0f };
            }
            if ("jwt".equals(query)) {
                return new float[] { 1.0f, 0.0f, 0.0f };
            }
            return new float[] { 0.0f, 0.0f, 0.0f };
        });

        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class))).thenAnswer(invocation -> {
            float[] queryVector = invocation.getArgument(1, float[].class);
            if (queryVector[0] == 2.0f) {
                return List.of(
                        new SearchResult("a", "JSON Web Token specification overview", 0.95f, Map.of()),
                        new SearchResult("b", "JWT header and payload explanation", 0.91f, Map.of()));
            }
            if (queryVector[0] == 1.0f) {
                return List.of(
                        new SearchResult("c", "JWT refresh token flow", 0.89f, Map.of()),
                        new SearchResult("d", "JWT logout blacklist strategy", 0.87f, Map.of()));
            }
            return List.of();
        });

        List<RetrievedContext> contexts = queryEngine.retrieve(
                "请介绍一下JWT",
                new RetrieveOptions("kb_test", 2, 0.0f, Map.of(), true));

        assertEquals(2, contexts.size());
        assertEquals("b", contexts.get(0).source());
        assertFalse(contexts.stream().map(RetrievedContext::source).toList().contains("d"));
    }

    @Test
    void shouldRewriteExplanatoryHowQueryToPrincipleAndProcessVariants() {
        when(embeddingService.embed(anyString())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0, String.class);
            return switch (query) {
                case "RAG" -> new float[] { 1.0f, 0.0f, 0.0f };
                case "RAG 工作原理" -> new float[] { 2.0f, 0.0f, 0.0f };
                case "RAG 运行流程" -> new float[] { 3.0f, 0.0f, 0.0f };
                default -> new float[] { 0.0f, 0.0f, 0.0f };
            };
        });

        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class))).thenAnswer(invocation -> {
            float[] queryVector = invocation.getArgument(1, float[].class);
            if (queryVector[0] == 2.0f) {
                return List.of(
                        new SearchResult("rag-principle", "RAG 的工作原理是先检索相关片段，再结合上下文生成回答", 0.94f,
                                Map.of()));
            }
            if (queryVector[0] == 3.0f) {
                return List.of(
                        new SearchResult("rag-flow", "RAG 的运行流程包括问题编码、向量检索、上下文拼接和答案生成", 0.92f,
                                Map.of()));
            }
            if (queryVector[0] == 1.0f) {
                return List.of(
                        new SearchResult("rag-intro", "RAG 是检索增强生成", 0.83f, Map.of()));
            }
            return List.of();
        });

        List<RetrievedContext> contexts = queryEngine.retrieve(
                "你认为RAG是如何运作的？",
                new RetrieveOptions("kb_test", 5, 0.0f, Map.of(), true));

        assertEquals(3, contexts.size());
        assertEquals("rag-principle", contexts.get(0).source());
        verify(embeddingService, times(1)).embed("RAG");
        verify(embeddingService, times(1)).embed("RAG 工作原理");
        verify(embeddingService, times(1)).embed("RAG 运行流程");
    }

    @Test
    void shouldFuseVectorAndKeywordRoutesWithRrfWhenHybridEnabled() {
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        RetrievalProperties properties = new RetrievalProperties();
        queryEngine = new QueryEngineImpl(embeddingService, vectorStore, keywordIndex, properties);

        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class))).thenReturn(List.of(
                new SearchResult("vector-only", "向量召回靠前但不是关键词最佳证据", 0.95f, Map.of()),
                new SearchResult("shared", "缓存穿透 是指查询不存在的数据导致请求打到数据库", 0.70f, Map.of())));
        when(keywordIndex.search(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), any())).thenReturn(List.of(
                new RetrievedContext("缓存穿透 是指查询不存在的数据导致请求打到数据库", "shared", 1.0f, Map.of())));

        RetrievalResult result = queryEngine.retrieveWithDiagnostics(
                "缓存穿透",
                new RetrieveOptions("kb_test", 2, 0.0f, Map.of(), false));

        assertEquals(2, result.contexts().size());
        assertEquals("shared", result.contexts().get(0).source());
        assertEquals("disabled", result.diagnostics().get("rerankEffectiveProvider"));
        assertEquals(0, result.diagnostics().get("rerankModelCallCount"));
        verify(keywordIndex).search(eq("kb_test"), eq("缓存穿透"), eq(4), eq(Map.of()));
    }

    @Test
    void shouldDegradeToKeywordOnlyAndStopVectorVariantsAfterMilvusFailure() {
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        RetrievalProperties properties = new RetrievalProperties();
        queryEngine = new QueryEngineImpl(embeddingService, vectorStore, keywordIndex, properties);
        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class)))
                .thenThrow(VectorDependencyException.unavailable("search", new IllegalStateException("raw-marker")));
        when(keywordIndex.search(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(List.of(new RetrievedContext("关键词证据", "keyword-doc", 0.8f, Map.of())));

        RetrievalResult result = queryEngine.retrieveWithDiagnostics(
                "什么是JWT？",
                new RetrieveOptions("kb_test", 5, 0.0f, Map.of(), false));

        assertEquals(List.of("keyword-doc"), result.contexts().stream().map(RetrievedContext::source).toList());
        assertEquals("keyword_only", result.diagnostics().get("retrievalMode"));
        assertEquals(true, result.diagnostics().get("retrievalDegraded"));
        assertEquals("milvus", result.diagnostics().get("degradedDependency"));
        verify(vectorStore, times(1)).search(anyString(), any(float[].class), any(SearchOptions.class));
        verify(keywordIndex, times(1)).search(eq("kb_test"), eq("什么是JWT？"), eq(10), eq(Map.of()));
    }

    @Test
    void shouldKeepMilvusDegradationAndRerankFallbackDiagnosticsTogether() {
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        RetrievalProperties properties = new RetrievalProperties();
        properties.getRerank().setProvider("nvidia");
        RerankerRegistry registry = new RerankerRegistry(
                List.of(new HeuristicReranker()),
                properties);
        queryEngine = new QueryEngineImpl(embeddingService, vectorStore, keywordIndex, properties, registry);
        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class)))
                .thenThrow(VectorDependencyException.unavailable("search", new IllegalStateException("raw-marker")));
        when(keywordIndex.search(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(List.of(new RetrievedContext(
                        "spring security authentication best practices", "keyword-doc", 0.8f, Map.of())));

        RetrievalResult result = queryEngine.retrieveWithDiagnostics(
                "spring security auth",
                new RetrieveOptions("kb_test", 5, 0.0f, Map.of(), true));

        assertEquals("keyword_only", result.diagnostics().get("retrievalMode"));
        assertEquals(true, result.diagnostics().get("retrievalDegraded"));
        assertEquals("nvidia", result.diagnostics().get("rerankRequestedProvider"));
        assertEquals("heuristic", result.diagnostics().get("rerankEffectiveProvider"));
        assertEquals("not_configured", result.diagnostics().get("rerankFallbackReason"));
        assertEquals(0, result.diagnostics().get("rerankModelCallCount"));
    }

    @Test
    void shouldKeepStableVectorFailureWhenKeywordRouteReturnsNoEvidence() {
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        RetrievalProperties properties = new RetrievalProperties();
        queryEngine = new QueryEngineImpl(embeddingService, vectorStore, keywordIndex, properties);
        VectorDependencyException failure = VectorDependencyException.unavailable(
                "search", new IllegalStateException("raw-marker"));
        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class))).thenThrow(failure);
        when(keywordIndex.search(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), any()))
                .thenReturn(List.of());

        VectorDependencyException actual = assertThrows(
                VectorDependencyException.class,
                () -> queryEngine.retrieveWithDiagnostics(
                        "什么是JWT？",
                        new RetrieveOptions("kb_test", 5, 0.0f, Map.of(), false)));

        assertEquals(VectorDependencyException.ERROR_CODE_UNAVAILABLE, actual.getErrorCode());
        verify(vectorStore, times(1)).search(anyString(), any(float[].class), any(SearchOptions.class));
        verify(keywordIndex, times(1)).search(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void shouldFallbackToHeuristicRerankerWhenModelProviderUnavailable() {
        RetrievalProperties properties = new RetrievalProperties();
        properties.getRerank().setProvider("model");
        properties.getHybrid().setEnabled(false);
        RerankerRegistry registry = new RerankerRegistry(
                List.of(new HeuristicReranker(), new ModelReranker(properties)),
                properties);
        queryEngine = new QueryEngineImpl(embeddingService, vectorStore, new com.enterprise.rag.core.rag.keyword.NoOpKeywordIndex(),
                properties, registry);

        when(vectorStore.search(anyString(), any(float[].class), any(SearchOptions.class))).thenReturn(List.of(
                new SearchResult("a", "mysql index tuning guide", 0.88f, Map.of()),
                new SearchResult("b", "spring security authentication best practices", 0.72f, Map.of())));

        RetrievalResult result = queryEngine.retrieveWithDiagnostics(
                "spring security auth",
                new RetrieveOptions("kb_test", 5, 0.0f, Map.of(), true));

        assertEquals("b", result.contexts().get(0).source());
        assertEquals("model", result.diagnostics().get("rerankRequestedProvider"));
        assertEquals("heuristic", result.diagnostics().get("rerankEffectiveProvider"));
        assertEquals(1, result.diagnostics().get("rerankFallbackCount"));
        assertEquals("not_configured", result.diagnostics().get("rerankFallbackReason"));
        assertEquals(0, result.diagnostics().get("rerankModelCallCount"));
    }
}

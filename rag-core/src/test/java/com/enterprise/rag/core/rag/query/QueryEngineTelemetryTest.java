package com.enterprise.rag.core.rag.query;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.model.RetrieveOptions;
import com.enterprise.rag.core.rag.rerank.HeuristicReranker;
import com.enterprise.rag.core.rag.rerank.RerankerRegistry;
import com.enterprise.rag.core.vectorstore.SearchOptions;
import com.enterprise.rag.core.vectorstore.SearchResult;
import com.enterprise.rag.core.vectorstore.VectorStore;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryEngineTelemetryTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    private final OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(provider)
            .build();

    @AfterEach
    void cleanup() {
        provider.close();
        exporter.reset();
    }

    @Test
    void hybridRouteEmitsOnlyActuallyExecutedFixedStages() {
        EmbeddingService embedding = mock(EmbeddingService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        RetrievalProperties properties = new RetrievalProperties();
        RerankerRegistry registry = new RerankerRegistry(List.of(new HeuristicReranker()), properties);
        when(embedding.embed(any())).thenReturn(new float[] {0.1f});
        when(vectorStore.search(any(), any(float[].class), any(SearchOptions.class)))
                .thenReturn(List.of(new SearchResult("shared", "content", 0.8f, Map.of())));
        when(keywordIndex.search(any(), any(), any(Integer.class), any()))
                .thenReturn(List.of(new RetrievedContext("content", "shared", 0.9f, Map.of())));
        QueryEngineImpl engine = new QueryEngineImpl(
                embedding, vectorStore, keywordIndex, properties, registry,
                new GenAiTelemetry(openTelemetry));

        engine.retrieveWithDiagnostics("RAG", new RetrieveOptions("kb", 2, 0.0f, Map.of(), false));

        Set<String> names = exporter.getFinishedSpanItems().stream()
                .map(span -> span.getName())
                .collect(Collectors.toSet());
        assertTrue(names.containsAll(Set.of(
                GenAiTelemetry.SpanNames.QUERY_EMBEDDING,
                GenAiTelemetry.SpanNames.VECTOR_SEARCH,
                GenAiTelemetry.SpanNames.KEYWORD_SEARCH,
                GenAiTelemetry.SpanNames.RETRIEVAL_FUSION)));
        assertFalse(names.contains(GenAiTelemetry.SpanNames.RERANK));
    }

    @Test
    void rerankStageReusesRequestedEffectiveProviderAndFallbackDiagnostics() {
        EmbeddingService embedding = mock(EmbeddingService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        RetrievalProperties properties = new RetrievalProperties();
        properties.getHybrid().setEnabled(false);
        properties.getRerank().setProvider("nvidia");
        RerankerRegistry registry = new RerankerRegistry(List.of(new HeuristicReranker()), properties);
        when(embedding.embed(any())).thenReturn(new float[] {0.1f});
        when(vectorStore.search(any(), any(float[].class), any(SearchOptions.class)))
                .thenReturn(List.of(new SearchResult("doc", "content", 0.8f, Map.of())));
        QueryEngineImpl engine = new QueryEngineImpl(
                embedding, vectorStore, mock(KeywordIndex.class), properties, registry,
                new GenAiTelemetry(openTelemetry));

        engine.retrieveWithDiagnostics("RAG", new RetrieveOptions("kb", 2, 0.0f, Map.of(), true));

        var rerank = exporter.getFinishedSpanItems().stream()
                .filter(span -> GenAiTelemetry.SpanNames.RERANK.equals(span.getName()))
                .findFirst().orElseThrow();
        assertEquals("nvidia", rerank.getAttributes().get(GenAiTelemetry.Attributes.PROVIDER_REQUESTED));
        assertEquals("heuristic", rerank.getAttributes().get(GenAiTelemetry.Attributes.PROVIDER_EFFECTIVE));
        assertEquals("not_configured", rerank.getAttributes().get(GenAiTelemetry.Attributes.FALLBACK_REASON));
        assertEquals(1L, rerank.getAttributes().get(GenAiTelemetry.Attributes.FALLBACK_COUNT));
    }
}

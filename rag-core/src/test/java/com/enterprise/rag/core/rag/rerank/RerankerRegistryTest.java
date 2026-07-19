package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RerankerRegistryTest {

    @Test
    void shouldReportDefaultHeuristicWithoutFallbackOrModelCall() {
        RetrievalProperties properties = new RetrievalProperties();
        RerankerRegistry registry = new RerankerRegistry(List.of(new HeuristicReranker()), properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("spring security authentication", "security", 0.8f, Map.of()));

        RerankOutcome outcome = registry.rerankWithDiagnostics("spring security", contexts);

        assertEquals("heuristic", outcome.diagnostics().requestedProvider());
        assertEquals("heuristic", outcome.diagnostics().effectiveProvider());
        assertEquals(0, outcome.diagnostics().fallbackCount());
        assertEquals(0, outcome.diagnostics().modelCallCount());
        assertEquals(1.0d, outcome.diagnostics().coverage());
    }

    @Test
    void shouldFallbackToHeuristicWhenModelRerankerThrows() {
        RetrievalProperties properties = new RetrievalProperties();
        properties.getRerank().setProvider("model");
        RerankerRegistry registry = new RerankerRegistry(
                List.of(new HeuristicReranker(), new ThrowingModelReranker()),
                properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("mysql index tuning guide", "mysql", 0.9f, Map.of()),
                new RetrievedContext("spring security authentication best practices", "security", 0.8f, Map.of()));

        List<RetrievedContext> reranked = registry.rerank("spring security auth", contexts);

        assertEquals("security", reranked.get(0).source());
    }

    @Test
    void shouldReturnSanitizedAttributionWhenRequestedProviderFallsBack() {
        RetrievalProperties properties = new RetrievalProperties();
        properties.getRerank().setProvider("model");
        properties.getRerank().getModel().setModel("configured-model");
        RerankerRegistry registry = new RerankerRegistry(
                List.of(new HeuristicReranker(), new ThrowingModelReranker()),
                properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("mysql index tuning guide", "mysql", 0.9f, Map.of()),
                new RetrievedContext("spring security authentication best practices", "security", 0.8f, Map.of()));

        RerankOutcome outcome = registry.rerankWithDiagnostics("spring security auth", contexts);

        assertEquals("security", outcome.contexts().get(0).source());
        assertEquals("model", outcome.diagnostics().requestedProvider());
        assertEquals("heuristic", outcome.diagnostics().effectiveProvider());
        assertEquals(1, outcome.diagnostics().fallbackCount());
        assertEquals("provider_failure", outcome.diagnostics().fallbackReason());
        assertEquals(1, outcome.diagnostics().modelCallCount());
        assertEquals(2, outcome.diagnostics().candidateCount());
        assertEquals(2, outcome.diagnostics().scoredCount());
        assertEquals(1.0d, outcome.diagnostics().coverage());
        assertEquals("configured-model", outcome.diagnostics().model());
        assertEquals("generic-rerank-v1", outcome.diagnostics().protocol());
    }

    @Test
    void shouldPreserveStableProviderFailureTaxonomyWithoutRawMessage() {
        RetrievalProperties properties = new RetrievalProperties();
        properties.getRerank().setProvider("nvidia");
        RerankerRegistry registry = new RerankerRegistry(
                List.of(new HeuristicReranker(), new TimeoutNvidiaReranker()),
                properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first", "first", 0.9f, Map.of()),
                new RetrievedContext("second", "second", 0.8f, Map.of()));

        RerankOutcome outcome = registry.rerankWithDiagnostics("query", contexts);

        assertEquals("heuristic", outcome.diagnostics().effectiveProvider());
        assertEquals("timeout", outcome.diagnostics().fallbackReason());
        assertEquals(1, outcome.diagnostics().modelCallCount());
        assertFalse(outcome.diagnostics().toMap().toString().contains("secret raw provider message"));
    }

    private static class ThrowingModelReranker implements Reranker {
        @Override
        public String provider() {
            return "model";
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public List<RetrievedContext> rerank(String query, List<RetrievedContext> contexts) {
            throw new IllegalStateException("provider failed");
        }
    }

    private static class TimeoutNvidiaReranker implements Reranker {
        @Override
        public String provider() {
            return "nvidia";
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public List<RetrievedContext> rerank(String query, List<RetrievedContext> contexts) {
            throw new RerankProviderException("timeout", 1, "secret raw provider message");
        }
    }
}

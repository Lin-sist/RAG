package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RerankerRegistryTest {

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
}

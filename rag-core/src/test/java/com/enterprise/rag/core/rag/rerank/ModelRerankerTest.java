package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModelRerankerTest {

    @Test
    void shouldStayUnavailableUntilConcreteAdapterIsImplemented() {
        RetrievalProperties properties = new RetrievalProperties();
        properties.getRerank().getModel().setEnabled(true);
        properties.getRerank().getModel().setBaseUrl("https://rerank.example.test");
        properties.getRerank().getModel().setApiKey("test-key");
        properties.getRerank().getModel().setModel("test-reranker");

        ModelReranker reranker = new ModelReranker(properties);

        assertFalse(reranker.available());
    }

    @Test
    void shouldKeepInputOrderWhenCalledWithoutConcreteAdapter() {
        RetrievalProperties properties = new RetrievalProperties();
        ModelReranker reranker = new ModelReranker(properties);
        List<RetrievedContext> contexts = List.of(
                new RetrievedContext("first", "a", 0.8f, Map.of()),
                new RetrievedContext("second", "b", 0.7f, Map.of()));

        assertEquals(contexts, reranker.rerank("query", contexts));
    }
}

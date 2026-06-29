package com.enterprise.rag.core.rag.keyword;

import com.enterprise.rag.core.rag.model.RetrievedContext;

import java.util.List;
import java.util.Map;

/**
 * Disabled keyword index fallback for tests and emergency degradation.
 */
public class NoOpKeywordIndex implements KeywordIndex {
    @Override
    public void upsert(String collectionName, List<KeywordDocument> documents) {
    }

    @Override
    public void rebuildCollection(String collectionName, List<KeywordDocument> documents) {
    }

    @Override
    public void delete(String collectionName, List<String> ids) {
    }

    @Override
    public void dropCollection(String collectionName) {
    }

    @Override
    public List<RetrievedContext> search(String collectionName, String query, int topK, Map<String, Object> filter) {
        return List.of();
    }
}

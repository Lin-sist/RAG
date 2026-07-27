package com.enterprise.rag.core.rag.keyword;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;

import java.util.List;
import java.util.Map;

/**
 * Disabled keyword index fallback for tests and emergency degradation.
 */
public class NoOpKeywordIndex implements KeywordIndex {
    @Override
    public void upsert(TenantVectorScope scope, List<KeywordDocument> documents) {
    }

    @Override
    public void rebuildCollection(TenantVectorScope scope, List<KeywordDocument> documents) {
    }

    @Override
    public void delete(TenantVectorScope scope, List<String> ids) {
    }

    @Override
    public void dropCollection(TenantVectorScope scope) {
    }

    @Override
    public List<RetrievedContext> search(TenantVectorScope scope, String query, int topK, Map<String, Object> filter) {
        return List.of();
    }
}

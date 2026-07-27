package com.enterprise.rag.core.rag.keyword;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;

import java.util.List;
import java.util.Map;

/**
 * Keyword index used as the sparse/BM25 route in hybrid retrieval.
 */
public interface KeywordIndex {

    void upsert(TenantVectorScope scope, List<KeywordDocument> documents);

    void rebuildCollection(TenantVectorScope scope, List<KeywordDocument> documents);

    void delete(TenantVectorScope scope, List<String> ids);

    void dropCollection(TenantVectorScope scope);

    List<RetrievedContext> search(TenantVectorScope scope, String query, int topK, Map<String, Object> filter);

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    default void upsert(String collectionName, List<KeywordDocument> documents) {
        throw new IllegalArgumentException("Tenant vector scope is required");
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    default void rebuildCollection(String collectionName, List<KeywordDocument> documents) {
        throw new IllegalArgumentException("Tenant vector scope is required");
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    default void delete(String collectionName, List<String> ids) {
        throw new IllegalArgumentException("Tenant vector scope is required");
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    default void dropCollection(String collectionName) {
        throw new IllegalArgumentException("Tenant vector scope is required");
    }

    /** @deprecated Raw collection names cannot establish tenant scope. */
    @Deprecated(since = "C13b", forRemoval = false)
    default List<RetrievedContext> search(
            String collectionName, String query, int topK, Map<String, Object> filter) {
        throw new IllegalArgumentException("Tenant vector scope is required");
    }
}

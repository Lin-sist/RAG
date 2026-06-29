package com.enterprise.rag.core.rag.keyword;

import com.enterprise.rag.core.rag.model.RetrievedContext;

import java.util.List;
import java.util.Map;

/**
 * Keyword index used as the sparse/BM25 route in hybrid retrieval.
 */
public interface KeywordIndex {

    void upsert(String collectionName, List<KeywordDocument> documents);

    void rebuildCollection(String collectionName, List<KeywordDocument> documents);

    void delete(String collectionName, List<String> ids);

    void dropCollection(String collectionName);

    List<RetrievedContext> search(String collectionName, String query, int topK, Map<String, Object> filter);
}

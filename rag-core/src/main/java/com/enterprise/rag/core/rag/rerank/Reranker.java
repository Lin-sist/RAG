package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;

import java.util.List;

public interface Reranker {

    String provider();

    boolean available();

    default String unavailableReason() {
        return "not_configured";
    }

    List<RetrievedContext> rerank(String query, List<RetrievedContext> contexts);
}

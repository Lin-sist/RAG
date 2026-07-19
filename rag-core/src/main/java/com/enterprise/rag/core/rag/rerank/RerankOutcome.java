package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;

import java.util.List;

public record RerankOutcome(List<RetrievedContext> contexts, RerankDiagnostics diagnostics) {

    public RerankOutcome {
        contexts = contexts == null ? List.of() : List.copyOf(contexts);
    }
}

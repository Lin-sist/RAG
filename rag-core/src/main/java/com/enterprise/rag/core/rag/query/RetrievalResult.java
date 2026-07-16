package com.enterprise.rag.core.rag.query;

import com.enterprise.rag.core.rag.model.RetrievedContext;

import java.util.List;
import java.util.Map;

/** Internal retrieval result with route diagnostics; not part of the public QA DTO. */
public record RetrievalResult(List<RetrievedContext> contexts, Map<String, Object> diagnostics) {

    public RetrievalResult {
        contexts = contexts == null ? List.of() : List.copyOf(contexts);
        diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    public static RetrievalResult complete(List<RetrievedContext> contexts) {
        return new RetrievalResult(contexts, Map.of());
    }

    public static RetrievalResult keywordOnly(List<RetrievedContext> contexts) {
        return new RetrievalResult(contexts, Map.of(
                "retrievalMode", "keyword_only",
                "retrievalDegraded", true,
                "degradedDependency", "milvus"));
    }

    public boolean degraded() {
        return Boolean.TRUE.equals(diagnostics.get("retrievalDegraded"));
    }
}

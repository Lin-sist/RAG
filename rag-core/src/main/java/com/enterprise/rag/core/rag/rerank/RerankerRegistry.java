package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RerankerRegistry {

    private final List<Reranker> rerankers;
    private final RetrievalProperties retrievalProperties;

    public RerankerRegistry(List<Reranker> rerankers, RetrievalProperties retrievalProperties) {
        this.rerankers = rerankers;
        this.retrievalProperties = retrievalProperties;
    }

    public Reranker activeReranker() {
        String provider = retrievalProperties.getRerank().getProvider();
        Reranker requested = find(provider);
        if (requested != null && requested.available()) {
            return requested;
        }

        if (requested != null) {
            log.warn("Reranker provider '{}' is unavailable; falling back to heuristic", provider);
        }
        return find("heuristic");
    }

    public List<RetrievedContext> rerank(
            String query,
            List<RetrievedContext> contexts) {
        Reranker requested = activeReranker();
        Reranker heuristic = find("heuristic");

        try {
            return requested.rerank(query, contexts);
        } catch (Exception e) {
            if (!"heuristic".equalsIgnoreCase(requested.provider())) {
                log.warn("Reranker provider '{}' failed; falling back to heuristic: {}",
                        requested.provider(), e.getMessage());
                return heuristic.rerank(query, contexts);
            }
            throw e;
        }
    }

    private Reranker find(String provider) {
        String normalized = provider == null || provider.isBlank() ? "heuristic" : provider;
        return rerankers.stream()
                .filter(reranker -> reranker.provider().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseGet(() -> rerankers.stream()
                        .filter(reranker -> "heuristic".equalsIgnoreCase(reranker.provider()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No heuristic reranker configured")));
    }
}

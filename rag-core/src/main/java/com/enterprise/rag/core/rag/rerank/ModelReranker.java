package com.enterprise.rag.core.rag.rerank;

import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Model reranker extension point. It stays unavailable until a concrete API
 * adapter is added and explicitly enabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelReranker implements Reranker {

    private final RetrievalProperties retrievalProperties;

    @Override
    public String provider() {
        return "model";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public List<RetrievedContext> rerank(String query, List<RetrievedContext> contexts) {
        RetrievalProperties.ModelReranker model = retrievalProperties.getRerank().getModel();
        log.warn("Model reranker requested but unavailable; adapterEnabled={}, baseUrlConfigured={}, modelConfigured={}",
                model.isEnabled(),
                model.getBaseUrl() != null && !model.getBaseUrl().isBlank(),
                model.getModel() != null && !model.getModel().isBlank());
        return contexts == null ? List.of() : contexts;
    }
}

package com.enterprise.rag.document.chunker;

import org.springframework.stereotype.Component;

/**
 * Conservative token estimator used when no model-specific tokenizer is
 * available. It intentionally overestimates by counting UTF-16 characters.
 */
@Component
public class ConservativeTokenCounter implements TokenCounter {

    @Override
    public int count(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length();
    }
}

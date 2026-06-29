package com.enterprise.rag.core.rag.keyword;

import java.util.Map;

/**
 * Keyword retrieval document indexed alongside vector chunks.
 */
public record KeywordDocument(
        String id,
        String content,
        Map<String, Object> metadata) {
    public KeywordDocument {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public boolean isValid() {
        return id != null && !id.isBlank() && content != null && !content.isBlank();
    }
}

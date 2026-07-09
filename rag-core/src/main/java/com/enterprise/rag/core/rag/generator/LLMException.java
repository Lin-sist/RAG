package com.enterprise.rag.core.rag.generator;

import java.util.Map;

/**
 * LLM 服务异常
 */
public class LLMException extends RuntimeException {
    private final Map<String, Object> diagnostics;

    public LLMException(String message) {
        this(message, null, Map.of());
    }

    public LLMException(String message, Map<String, Object> diagnostics) {
        this(message, null, diagnostics);
    }

    public LLMException(String message, Throwable cause) {
        this(message, cause, Map.of());
    }

    public LLMException(String message, Throwable cause, Map<String, Object> diagnostics) {
        super(message, cause);
        this.diagnostics = diagnostics == null ? Map.of() : Map.copyOf(diagnostics);
    }

    public Map<String, Object> diagnostics() {
        return diagnostics;
    }
}

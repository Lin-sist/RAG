package com.enterprise.rag.core.rag.rerank;

/** Provider failure carrying only stable attribution facts; the raw message is never exported. */
public class RerankProviderException extends RuntimeException {

    private final String reason;
    private final int modelCallCount;

    public RerankProviderException(String reason, int modelCallCount, String message) {
        super(message);
        this.reason = reason;
        this.modelCallCount = Math.max(0, Math.min(1, modelCallCount));
    }

    public RerankProviderException(String reason, int modelCallCount, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.modelCallCount = Math.max(0, Math.min(1, modelCallCount));
    }

    public String reason() {
        return reason;
    }

    public int modelCallCount() {
        return modelCallCount;
    }
}

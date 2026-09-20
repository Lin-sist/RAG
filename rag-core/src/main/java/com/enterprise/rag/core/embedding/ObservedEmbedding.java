package com.enterprise.rag.core.embedding;

/**
 * One logical embedding request and the provider work it actually caused.
 */
public record ObservedEmbedding(
        float[] vector,
        boolean cacheHit,
        int providerCallCount,
        int providerFallbackCount) {

    public ObservedEmbedding {
        if (vector == null) {
            throw new IllegalArgumentException("vector cannot be null");
        }
        if (providerCallCount < 0 || providerFallbackCount < 0
                || providerFallbackCount > providerCallCount) {
            throw new IllegalArgumentException("embedding observation counts are invalid");
        }
    }
}

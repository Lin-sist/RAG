package com.enterprise.rag.core.embedding;

/**
 * 嵌入服务异常
 * 当向量嵌入生成失败时抛出
 */
public class EmbeddingException extends RuntimeException {

    private final String providerName;
    private final boolean retryable;

    public EmbeddingException(String message) {
        super(message);
        this.providerName = null;
        this.retryable = false;
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
        this.providerName = null;
        this.retryable = false;
    }

    public EmbeddingException(String message, String providerName, boolean retryable) {
        super(message);
        this.providerName = providerName;
        this.retryable = retryable;
    }

    public EmbeddingException(String message, Throwable cause, String providerName, boolean retryable) {
        super(message, cause);
        this.providerName = providerName;
        this.retryable = retryable;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isRetryable() {
        return retryable;
    }
}

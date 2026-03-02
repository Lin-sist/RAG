package com.enterprise.rag.core.vectorstore;

/**
 * 向量存储异常
 * 当向量存储操作失败时抛出
 */
public class VectorStoreException extends RuntimeException {

    public VectorStoreException(String message) {
        super(message);
    }

    public VectorStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}

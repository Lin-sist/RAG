package com.enterprise.rag.core.rag.generator;

/**
 * LLM 服务异常
 */
public class LLMException extends RuntimeException {

    public LLMException(String message) {
        super(message);
    }

    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }
}

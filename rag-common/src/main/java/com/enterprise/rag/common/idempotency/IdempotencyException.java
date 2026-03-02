package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 幂等性处理异常
 */
public class IdempotencyException extends BusinessException {

    public static final String ERROR_CODE_PROCESSING = "IDEMPOTENCY_001";
    public static final String ERROR_CODE_STORAGE = "IDEMPOTENCY_002";

    public IdempotencyException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }

    public IdempotencyException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 创建请求正在处理中的异常
     */
    public static IdempotencyException processing(String idempotencyKey) {
        return new IdempotencyException(
            ERROR_CODE_PROCESSING,
            "请求正在处理中，请稍后重试。幂等性Key: " + idempotencyKey
        );
    }

    /**
     * 创建存储失败的异常
     */
    public static IdempotencyException storageFailed(String idempotencyKey, Throwable cause) {
        return new IdempotencyException(
            ERROR_CODE_STORAGE,
            "幂等性结果存储失败。幂等性Key: " + idempotencyKey,
            cause
        );
    }
}

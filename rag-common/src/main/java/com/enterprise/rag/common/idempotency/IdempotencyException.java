package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 幂等性处理异常
 */
public class IdempotencyException extends BusinessException {

    public static final String ERROR_CODE_PROCESSING = "IDEMPOTENCY_001";
    public static final String ERROR_CODE_STORAGE = "IDEMPOTENCY_002";
    public static final String ERROR_CODE_IDENTITY_REQUIRED = "IDEMPOTENCY_004";
    public static final String ERROR_CODE_SCOPE_MISMATCH = "IDEMPOTENCY_005";

    public IdempotencyException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }

    public IdempotencyException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public IdempotencyException(String errorCode, String message, HttpStatus httpStatus) {
        super(errorCode, message, httpStatus);
    }

    /**
     * 创建请求正在处理中的异常
     */
    public static IdempotencyException processing(String idempotencyKey) {
        return new IdempotencyException(
            ERROR_CODE_PROCESSING,
            "请求正在处理中，请稍后重试"
        );
    }

    /**
     * 创建存储失败的异常
     */
    public static IdempotencyException storageFailed(String idempotencyKey, Throwable cause) {
        return new IdempotencyException(
            ERROR_CODE_STORAGE,
            "幂等性结果存储失败",
            cause
        );
    }

    public static IdempotencyException identityRequired() {
        return new IdempotencyException(
                ERROR_CODE_IDENTITY_REQUIRED,
                "幂等请求缺少有效的租户用户身份",
                HttpStatus.UNAUTHORIZED);
    }

    public static IdempotencyException scopeMismatch() {
        return new IdempotencyException(
                ERROR_CODE_SCOPE_MISMATCH,
                "幂等请求身份范围与已存储记录不一致",
                HttpStatus.CONFLICT);
    }
}

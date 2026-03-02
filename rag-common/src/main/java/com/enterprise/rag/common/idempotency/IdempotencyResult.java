package com.enterprise.rag.common.idempotency;

import java.time.Instant;

/**
 * 幂等性处理结果
 *
 * @param <T>         结果类型
 * @param isNew       是否为新请求（首次处理）
 * @param result      处理结果
 * @param processedAt 处理时间
 */
public record IdempotencyResult<T>(
    boolean isNew,
    T result,
    Instant processedAt
) {
    /**
     * 创建新请求的结果
     */
    public static <T> IdempotencyResult<T> newRequest(T result) {
        return new IdempotencyResult<>(true, result, Instant.now());
    }

    /**
     * 创建重复请求的结果
     */
    public static <T> IdempotencyResult<T> duplicate(T result, Instant processedAt) {
        return new IdempotencyResult<>(false, result, processedAt);
    }
}

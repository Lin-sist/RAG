package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.constant.RedisKeyConstants;

import java.util.function.Supplier;

/**
 * 幂等性处理器接口
 * <p>
 * 用于确保写操作的幂等性，防止重复请求产生副作用。
 * 使用 Redis 存储幂等性 Key 和处理结果。
 */
public interface IdempotencyHandler {

    /**
     * 使用已认证业务范围执行幂等操作。
     */
    default <T> IdempotencyResult<T> execute(
            IdempotencyScope scope,
            String endpoint,
            String requestKey,
            Supplier<T> operation,
            Class<T> resultType) {
        return execute(scope, endpoint, requestKey, operation, resultType,
                RedisKeyConstants.IDEMPOTENCY_TTL);
    }

    /**
     * 使用已认证业务范围和自定义 TTL 执行幂等操作。
     */
    <T> IdempotencyResult<T> execute(
            IdempotencyScope scope,
            String endpoint,
            String requestKey,
            Supplier<T> operation,
            Class<T> resultType,
            long ttlSeconds);

    boolean exists(IdempotencyScope scope, String endpoint, String requestKey);

    <T> IdempotencyResult<T> getStoredResult(
            IdempotencyScope scope,
            String endpoint,
            String requestKey,
            Class<T> resultType);

    void remove(IdempotencyScope scope, String endpoint, String requestKey);

    /**
     * 执行幂等性操作
     * <p>
     * 如果幂等性 Key 不存在，执行操作并存储结果；
     * 如果幂等性 Key 已存在，直接返回之前的处理结果。
     *
     * @param idempotencyKey 幂等性 Key
     * @param operation      要执行的操作
     * @param resultType     结果类型
     * @param <T>            结果类型
     * @return 幂等性处理结果
     */
    @Deprecated(since = "C13b", forRemoval = false)
    <T> IdempotencyResult<T> execute(String idempotencyKey, Supplier<T> operation, Class<T> resultType);

    /**
     * 执行幂等性操作（使用自定义过期时间）
     *
     * @param idempotencyKey 幂等性 Key
     * @param operation      要执行的操作
     * @param resultType     结果类型
     * @param ttlSeconds     过期时间（秒）
     * @param <T>            结果类型
     * @return 幂等性处理结果
     */
    @Deprecated(since = "C13b", forRemoval = false)
    <T> IdempotencyResult<T> execute(String idempotencyKey, Supplier<T> operation, Class<T> resultType, long ttlSeconds);

    /**
     * 检查幂等性 Key 是否已存在
     *
     * @param idempotencyKey 幂等性 Key
     * @return 是否存在
     */
    @Deprecated(since = "C13b", forRemoval = false)
    boolean exists(String idempotencyKey);

    /**
     * 获取已存储的结果
     *
     * @param idempotencyKey 幂等性 Key
     * @param resultType     结果类型
     * @param <T>            结果类型
     * @return 存储的结果，如果不存在返回 null
     */
    @Deprecated(since = "C13b", forRemoval = false)
    <T> IdempotencyResult<T> getStoredResult(String idempotencyKey, Class<T> resultType);

    /**
     * 删除幂等性 Key（用于测试或手动清理）
     *
     * @param idempotencyKey 幂等性 Key
     */
    @Deprecated(since = "C13b", forRemoval = false)
    void remove(String idempotencyKey);
}

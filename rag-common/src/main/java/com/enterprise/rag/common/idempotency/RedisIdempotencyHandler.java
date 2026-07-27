package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.exception.RedisDependencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis 的幂等性处理器实现
 * <p>
 * 使用 Redis 存储幂等性 Key 和处理结果，支持：
 * - 首次请求：执行操作并存储结果
 * - 重复请求：直接返回之前的处理结果
 * - 并发控制：使用 PROCESSING 状态防止并发执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyHandler implements IdempotencyHandler {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 处理中状态的短暂过期时间（秒）
     * 用于防止处理中断导致的死锁
     */
    private static final long PROCESSING_TTL_SECONDS = 60;

    @Override
    public <T> IdempotencyResult<T> execute(
            IdempotencyScope scope,
            String endpoint,
            String requestKey,
            Supplier<T> operation,
            Class<T> resultType,
            long ttlSeconds) {
        String redisKey = buildScopedKey(scope, endpoint, requestKey);
        return executeInternal(scope, redisKey, requestKey, operation, resultType, ttlSeconds);
    }

    @Override
    public boolean exists(IdempotencyScope scope, String endpoint, String requestKey) {
        String redisKey = buildScopedKey(scope, endpoint, requestKey);
        return readStoredData(redisKey, scope) != null;
    }

    @Override
    public <T> IdempotencyResult<T> getStoredResult(
            IdempotencyScope scope,
            String endpoint,
            String requestKey,
            Class<T> resultType) {
        String redisKey = buildScopedKey(scope, endpoint, requestKey);
        return getStoredResultInternal(redisKey, resultType, scope);
    }

    @Override
    public void remove(IdempotencyScope scope, String endpoint, String requestKey) {
        String redisKey = buildScopedKey(scope, endpoint, requestKey);
        if (readStoredData(redisKey, scope) != null) {
            stringRedisTemplate.delete(redisKey);
            log.debug("Removed scoped idempotency key");
        }
    }

    @Deprecated(since = "C13b", forRemoval = false)
    @Override
    public <T> IdempotencyResult<T> execute(String idempotencyKey, Supplier<T> operation, Class<T> resultType) {
        return execute(idempotencyKey, operation, resultType, RedisKeyConstants.IDEMPOTENCY_TTL);
    }

    @Deprecated(since = "C13b", forRemoval = false)
    @Override
    public <T> IdempotencyResult<T> execute(String idempotencyKey, Supplier<T> operation, 
                                            Class<T> resultType, long ttlSeconds) {
        String redisKey = buildKey(idempotencyKey);
        return executeInternal(null, redisKey, idempotencyKey, operation, resultType, ttlSeconds);
    }

    private <T> IdempotencyResult<T> executeInternal(
            IdempotencyScope scope,
            String redisKey,
            String requestKey,
            Supplier<T> operation,
            Class<T> resultType,
            long ttlSeconds) {
        // 1. 尝试获取已存储的结果
        IdempotencyResult<T> existingResult = getStoredResultInternal(redisKey, resultType, scope);
        if (existingResult != null) {
            log.debug("Idempotency key exists, returning cached result");
            return existingResult;
        }

        // 2. 尝试设置 PROCESSING 状态（原子操作）
        IdempotencyData processingData = scope == null
                ? IdempotencyData.processing()
                : IdempotencyData.processing(scope);
        boolean acquired = trySetProcessing(redisKey, processingData);

        if (!acquired) {
            // 可能是并发请求，再次检查是否已完成
            existingResult = getStoredResultInternal(redisKey, resultType, scope);
            if (existingResult != null) {
                return existingResult;
            }
            // 仍在处理中
            throw IdempotencyException.processing(requestKey);
        }

        // 3. 执行操作
        T result;
        try {
            result = operation.get();
        } catch (Exception e) {
            // 操作失败，存储失败状态
            storeFailedResult(redisKey, scope, e.getMessage(), ttlSeconds);
            throw e;
        }

        // 4. 存储成功结果
        storeCompletedResult(redisKey, scope, result, resultType, ttlSeconds);

        return IdempotencyResult.newRequest(result);
    }

    @Deprecated(since = "C13b", forRemoval = false)
    @Override
    public boolean exists(String idempotencyKey) {
        String redisKey = buildKey(idempotencyKey);
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        return Boolean.TRUE.equals(exists);
    }

    @Deprecated(since = "C13b", forRemoval = false)
    @Override
    public <T> IdempotencyResult<T> getStoredResult(String idempotencyKey, Class<T> resultType) {
        String redisKey = buildKey(idempotencyKey);
        return getStoredResultInternal(redisKey, resultType, null);
    }

    @Deprecated(since = "C13b", forRemoval = false)
    @Override
    public void remove(String idempotencyKey) {
        String redisKey = buildKey(idempotencyKey);
        stringRedisTemplate.delete(redisKey);
        log.debug("Removed idempotency key");
    }

    /**
     * 构建 Redis key
     */
    private String buildKey(String idempotencyKey) {
        return RedisKeyConstants.idempotencyKey(idempotencyKey);
    }

    private String buildScopedKey(IdempotencyScope scope, String endpoint, String requestKey) {
        if (scope == null) {
            throw IdempotencyException.identityRequired();
        }
        return RedisKeyConstants.idempotencyV2Key(
                scope.tenantId(), scope.userId(), endpoint, requestKey);
    }

    /**
     * 尝试设置 PROCESSING 状态
     */
    private boolean trySetProcessing(String redisKey, IdempotencyData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(redisKey, json, PROCESSING_TTL_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Idempotency lock failed closed: dependency=redis, subsystem=idempotency, "
                            + "operation=lock, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("idempotency", "lock", e);
        }
    }

    /**
     * 存储成功结果
     */
    private <T> void storeCompletedResult(
            String redisKey,
            IdempotencyScope scope,
            T result,
            Class<T> resultType,
            long ttlSeconds) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            IdempotencyData data = scope == null
                    ? IdempotencyData.completed(resultJson, resultType.getName())
                    : IdempotencyData.completed(scope, resultJson, resultType.getName());
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(redisKey, json, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Stored completed idempotency result");
        } catch (Exception e) {
            log.error("Idempotency result write has unknown outcome: dependency=redis, "
                            + "subsystem=idempotency, operation=write_result, "
                            + "failMode=outcome_unknown, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.outcomeUnknown("idempotency", "write_result", e);
        }
    }

    /**
     * 存储失败结果
     */
    private void storeFailedResult(
            String redisKey,
            IdempotencyScope scope,
            String errorMessage,
            long ttlSeconds) {
        try {
            IdempotencyData data = scope == null
                    ? IdempotencyData.failed(errorMessage)
                    : IdempotencyData.failed(scope, errorMessage);
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(redisKey, json, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Stored failed idempotency result");
        } catch (Exception e) {
            log.error("Failed to persist idempotency failure state: dependency=redis, "
                            + "subsystem=idempotency, operation=write_failure, "
                            + "failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
        }
    }

    /**
     * 获取已存储的结果（内部方法）
     */
    private <T> IdempotencyResult<T> getStoredResultInternal(
            String redisKey,
            Class<T> resultType,
            IdempotencyScope expectedScope) {
        IdempotencyData data = readStoredData(redisKey, expectedScope);
        if (data == null) {
            return null;
        }

        switch (data.getStatus()) {
            case PROCESSING:
                // 仍在处理中
                return null;
            case COMPLETED:
                try {
                    T result = objectMapper.readValue(data.getResultJson(), resultType);
                    return IdempotencyResult.duplicate(result, data.getProcessedAt());
                } catch (Exception e) {
                    throw deserializeFailure(e);
                }
            case FAILED:
                // 之前失败了，允许重试；身份校验已在删除前完成。
                try {
                    stringRedisTemplate.delete(redisKey);
                    return null;
                } catch (Exception e) {
                    throw RedisDependencyException.unavailable("idempotency", "delete_failed_state", e);
                }
            default:
                return null;
        }
    }

    private IdempotencyData readStoredData(String redisKey, IdempotencyScope expectedScope) {
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("Idempotency read failed closed: dependency=redis, subsystem=idempotency, "
                            + "operation=read, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("idempotency", "read", e);
        }
        if (json == null) {
            return null;
        }

        try {
            IdempotencyData data = objectMapper.readValue(json, IdempotencyData.class);
            validateScope(data, expectedScope);
            return data;
        } catch (IdempotencyException e) {
            throw e;
        } catch (Exception e) {
            throw deserializeFailure(e);
        }
    }

    private void validateScope(IdempotencyData data, IdempotencyScope expectedScope) {
        if (expectedScope == null) {
            return;
        }
        if (!Long.valueOf(expectedScope.tenantId()).equals(data.getTenantId())
                || !Long.valueOf(expectedScope.userId()).equals(data.getUserId())) {
            log.warn("Idempotency payload scope mismatch, failing closed");
            throw IdempotencyException.scopeMismatch();
        }
    }

    private RedisDependencyException deserializeFailure(Exception cause) {
        log.error("Failed to read idempotency state: dependency=redis, subsystem=idempotency, "
                        + "operation=deserialize, failMode=closed, errorType={}",
                cause.getClass().getSimpleName());
        return RedisDependencyException.unavailable("idempotency", "deserialize", cause);
    }
}

package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    public <T> IdempotencyResult<T> execute(String idempotencyKey, Supplier<T> operation, Class<T> resultType) {
        return execute(idempotencyKey, operation, resultType, RedisKeyConstants.IDEMPOTENCY_TTL);
    }

    @Override
    public <T> IdempotencyResult<T> execute(String idempotencyKey, Supplier<T> operation, 
                                            Class<T> resultType, long ttlSeconds) {
        String redisKey = buildKey(idempotencyKey);

        // 1. 尝试获取已存储的结果
        IdempotencyResult<T> existingResult = getStoredResultInternal(redisKey, resultType);
        if (existingResult != null) {
            log.debug("Idempotency key exists, returning cached result: {}", idempotencyKey);
            return existingResult;
        }

        // 2. 尝试设置 PROCESSING 状态（原子操作）
        IdempotencyData processingData = IdempotencyData.processing();
        boolean acquired = trySetProcessing(redisKey, processingData);

        if (!acquired) {
            // 可能是并发请求，再次检查是否已完成
            existingResult = getStoredResultInternal(redisKey, resultType);
            if (existingResult != null) {
                return existingResult;
            }
            // 仍在处理中
            throw IdempotencyException.processing(idempotencyKey);
        }

        // 3. 执行操作
        T result;
        try {
            result = operation.get();
        } catch (Exception e) {
            // 操作失败，存储失败状态
            storeFailedResult(redisKey, e.getMessage(), ttlSeconds);
            throw e;
        }

        // 4. 存储成功结果
        storeCompletedResult(redisKey, result, resultType, ttlSeconds);

        return IdempotencyResult.newRequest(result);
    }

    @Override
    public boolean exists(String idempotencyKey) {
        String redisKey = buildKey(idempotencyKey);
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public <T> IdempotencyResult<T> getStoredResult(String idempotencyKey, Class<T> resultType) {
        String redisKey = buildKey(idempotencyKey);
        return getStoredResultInternal(redisKey, resultType);
    }

    @Override
    public void remove(String idempotencyKey) {
        String redisKey = buildKey(idempotencyKey);
        stringRedisTemplate.delete(redisKey);
        log.debug("Removed idempotency key: {}", idempotencyKey);
    }

    /**
     * 构建 Redis key
     */
    private String buildKey(String idempotencyKey) {
        return RedisKeyConstants.idempotencyKey(idempotencyKey);
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
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize processing data", e);
            return false;
        }
    }

    /**
     * 存储成功结果
     */
    private <T> void storeCompletedResult(String redisKey, T result, Class<T> resultType, long ttlSeconds) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            IdempotencyData data = IdempotencyData.completed(resultJson, resultType.getName());
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(redisKey, json, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Stored completed result for key: {}", redisKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize completed result", e);
            throw IdempotencyException.storageFailed(redisKey, e);
        }
    }

    /**
     * 存储失败结果
     */
    private void storeFailedResult(String redisKey, String errorMessage, long ttlSeconds) {
        try {
            IdempotencyData data = IdempotencyData.failed(errorMessage);
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(redisKey, json, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Stored failed result for key: {}", redisKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize failed result", e);
        }
    }

    /**
     * 获取已存储的结果（内部方法）
     */
    private <T> IdempotencyResult<T> getStoredResultInternal(String redisKey, Class<T> resultType) {
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            return null;
        }

        try {
            IdempotencyData data = objectMapper.readValue(json, IdempotencyData.class);

            switch (data.getStatus()) {
                case PROCESSING:
                    // 仍在处理中
                    return null;
                case COMPLETED:
                    T result = objectMapper.readValue(data.getResultJson(), resultType);
                    return IdempotencyResult.duplicate(result, data.getProcessedAt());
                case FAILED:
                    // 之前失败了，允许重试
                    stringRedisTemplate.delete(redisKey);
                    return null;
                default:
                    return null;
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize idempotency data: {}", redisKey, e);
            // 数据损坏，删除并允许重试
            stringRedisTemplate.delete(redisKey);
            return null;
        }
    }
}

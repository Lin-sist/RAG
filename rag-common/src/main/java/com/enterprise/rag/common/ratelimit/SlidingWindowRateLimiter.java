package com.enterprise.rag.common.ratelimit;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis + Lua 的滑动窗口限流器实现
 * <p>
 * 使用 Redis ZSET 实现滑动窗口算法：
 * - member: 请求唯一标识（时间戳+随机数）
 * - score: 请求时间戳
 * <p>
 * 每次请求时：
 * 1. 移除窗口外的过期请求
 * 2. 统计当前窗口内的请求数
 * 3. 如果未超过阈值，添加新请求
 */
@Slf4j
@Component
public class SlidingWindowRateLimiter implements RateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 滑动窗口限流 Lua 脚本
     * <p>
     * KEYS[1]: 限流 key
     * ARGV[1]: 当前时间戳（毫秒）
     * ARGV[2]: 窗口大小（毫秒）
     * ARGV[3]: 最大请求数
     * ARGV[4]: 请求唯一标识
     * <p>
     * 返回值: [是否允许(0/1), 剩余配额, 重置时间戳(秒)]
     */
    private static final String SLIDING_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local max_requests = tonumber(ARGV[3])
        local request_id = ARGV[4]
        
        -- 计算窗口起始时间
        local window_start = now - window
        
        -- 移除窗口外的过期请求
        redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)
        
        -- 统计当前窗口内的请求数
        local current_count = redis.call('ZCARD', key)
        
        -- 计算重置时间（当前时间 + 窗口大小）
        local reset_time = math.floor((now + window) / 1000)
        
        -- 判断是否允许请求
        if current_count < max_requests then
            -- 添加新请求
            redis.call('ZADD', key, now, request_id)
            -- 设置 key 过期时间（窗口大小 + 1秒缓冲）
            redis.call('PEXPIRE', key, window + 1000)
            -- 返回：允许, 剩余配额, 重置时间
            return {1, max_requests - current_count - 1, reset_time}
        else
            -- 获取最早的请求时间，计算重试等待时间
            local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
            local retry_after = 0
            if oldest and #oldest >= 2 then
                local oldest_time = tonumber(oldest[2])
                retry_after = math.ceil((oldest_time + window - now) / 1000)
                if retry_after < 0 then
                    retry_after = 0
                end
            end
            -- 返回：拒绝, 剩余配额(0), 重置时间, 重试等待时间
            return {0, 0, reset_time, retry_after}
        end
        """;

    private final DefaultRedisScript<List> slidingWindowScript;

    public SlidingWindowRateLimiter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.slidingWindowScript = new DefaultRedisScript<>();
        this.slidingWindowScript.setScriptText(SLIDING_WINDOW_SCRIPT);
        this.slidingWindowScript.setResultType(List.class);
    }

    @Override
    public RateLimitResult tryAcquire(String key, RateLimitConfig config) {
        return tryAcquire(RateLimitDimension.GLOBAL, key, config);
    }

    @Override
    public RateLimitResult tryAcquire(RateLimitDimension dimension, String key, RateLimitConfig config) {
        String redisKey = buildKey(dimension, key);
        long now = System.currentTimeMillis();
        long windowMillis = config.windowSeconds() * 1000;
        String requestId = now + ":" + Math.random();

        try {
            @SuppressWarnings("unchecked")
            List<Long> result = stringRedisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList(redisKey),
                String.valueOf(now),
                String.valueOf(windowMillis),
                String.valueOf(config.maxRequests()),
                requestId
            );

            if (result == null || result.isEmpty()) {
                log.warn("Rate limit script returned null or empty result for key: {}", redisKey);
                // 默认允许请求，避免限流器故障影响业务
                return RateLimitResult.allowed(config.maxRequests(), now / 1000 + config.windowSeconds());
            }

            boolean allowed = result.get(0) == 1L;
            long remaining = result.get(1);
            long resetTime = result.get(2);
            long retryAfter = result.size() > 3 ? result.get(3) : 0;

            if (allowed) {
                return RateLimitResult.allowed(remaining, resetTime);
            } else {
                return RateLimitResult.denied(resetTime, retryAfter);
            }
        } catch (Exception e) {
            log.error("Rate limit error for key: {}", redisKey, e);
            // 限流器故障时默认允许请求
            return RateLimitResult.allowed(config.maxRequests(), now / 1000 + config.windowSeconds());
        }
    }

    @Override
    public void reset(String key) {
        reset(RateLimitDimension.GLOBAL, key);
    }

    @Override
    public void reset(RateLimitDimension dimension, String key) {
        String redisKey = buildKey(dimension, key);
        try {
            stringRedisTemplate.delete(redisKey);
            log.debug("Reset rate limit for key: {}", redisKey);
        } catch (Exception e) {
            log.error("Failed to reset rate limit for key: {}", redisKey, e);
        }
    }

    /**
     * 构建 Redis key
     */
    private String buildKey(RateLimitDimension dimension, String key) {
        return RedisKeyConstants.rateLimitKey(dimension.getCode(), key);
    }
}

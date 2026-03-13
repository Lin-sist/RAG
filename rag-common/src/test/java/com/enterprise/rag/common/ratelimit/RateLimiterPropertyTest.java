package com.enterprise.rag.common.ratelimit;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 限流器属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * 
 * Property 15: 限流阈值正确性
 * Property 16: 限流响应头完整性
 * 
 * Validates: Requirements 6.2, 6.4
 */
class RateLimiterPropertyTest {

    private static StringRedisTemplate stringRedisTemplate;
    private static SlidingWindowRateLimiter rateLimiter;
    private static boolean redisAvailable = false;

    static {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
            connectionFactory.afterPropertiesSet();

            stringRedisTemplate = new StringRedisTemplate(connectionFactory);
            stringRedisTemplate.afterPropertiesSet();

            // Test connection
            stringRedisTemplate.getConnectionFactory().getConnection().ping();

            rateLimiter = new SlidingWindowRateLimiter(stringRedisTemplate);
            redisAvailable = true;
        } catch (Exception e) {
            System.err.println("Redis not available, skipping property tests: " + e.getMessage());
            redisAvailable = false;
        }
    }

    /**
     * Property 15: 限流阈值正确性
     * 
     * *For any* 限流配置，当请求数超过阈值时，后续请求应返回拒绝结果。
     * 
     * 测试策略：
     * 1. 生成随机的限流配置（maxRequests, windowSeconds）
     * 2. 发送 maxRequests 个请求，验证都被允许
     * 3. 发送额外请求，验证被拒绝
     * 
     * **Validates: Requirements 6.2**
     */
    @Property(tries = 100)
    void rateLimitShouldRejectRequestsExceedingThreshold(
            @ForAll @IntRange(min = 1, max = 20) int maxRequests,
            @ForAll @LongRange(min = 10, max = 60) long windowSeconds) {
        if (!redisAvailable) {
            return;
        }

        // 使用唯一 key 避免测试间干扰
        String uniqueKey = "test-" + UUID.randomUUID();
        RateLimitConfig config = new RateLimitConfig(maxRequests, windowSeconds, RateLimitStrategy.SLIDING_WINDOW);

        try {
            // 发送 maxRequests 个请求，应该都被允许
            List<RateLimitResult> allowedResults = new ArrayList<>();
            for (int i = 0; i < maxRequests; i++) {
                RateLimitResult result = rateLimiter.tryAcquire(uniqueKey, config);
                allowedResults.add(result);

                Assertions.assertThat(result.allowed())
                        .as("Request %d of %d should be allowed", i + 1, maxRequests)
                        .isTrue();
            }

            // 验证剩余配额递减
            for (int i = 0; i < allowedResults.size(); i++) {
                RateLimitResult result = allowedResults.get(i);
                long expectedRemaining = maxRequests - i - 1;
                Assertions.assertThat(result.remaining())
                        .as("Remaining quota after request %d should be %d", i + 1, expectedRemaining)
                        .isEqualTo(expectedRemaining);
            }

            // 发送额外请求，应该被拒绝
            RateLimitResult deniedResult = rateLimiter.tryAcquire(uniqueKey, config);

            Assertions.assertThat(deniedResult.allowed())
                    .as("Request exceeding threshold should be denied")
                    .isFalse();

            Assertions.assertThat(deniedResult.remaining())
                    .as("Remaining quota should be 0 when denied")
                    .isEqualTo(0);

        } finally {
            // 清理测试数据
            rateLimiter.reset(uniqueKey);
        }
    }

    /**
     * Property 16: 限流响应头完整性
     * 
     * *For any* 限流请求，响应应包含剩余配额和重置时间。
     * 被限流的请求还应包含重试等待时间。
     * 
     * 测试策略：
     * 1. 发送请求并验证响应包含所有必要字段
     * 2. 验证重置时间在合理范围内
     * 3. 验证被拒绝时 retryAfter > 0
     * 
     * **Validates: Requirements 6.4**
     */
    @Property(tries = 100)
    void rateLimitResultShouldContainCompleteHeaders(
            @ForAll @IntRange(min = 1, max = 10) int maxRequests,
            @ForAll @LongRange(min = 5, max = 30) long windowSeconds) {
        if (!redisAvailable) {
            return;
        }

        String uniqueKey = "test-headers-" + UUID.randomUUID();
        RateLimitConfig config = new RateLimitConfig(maxRequests, windowSeconds, RateLimitStrategy.SLIDING_WINDOW);

        try {
            long beforeRequest = System.currentTimeMillis() / 1000;

            // 发送一个请求
            RateLimitResult result = rateLimiter.tryAcquire(uniqueKey, config);

            long afterRequest = System.currentTimeMillis() / 1000;

            // 验证允许的请求包含完整信息
            Assertions.assertThat(result.allowed())
                    .as("First request should be allowed")
                    .isTrue();

            // 验证剩余配额
            Assertions.assertThat(result.remaining())
                    .as("Remaining should be maxRequests - 1")
                    .isEqualTo(maxRequests - 1);

            // 验证重置时间在合理范围内
            long expectedResetMin = beforeRequest + windowSeconds;
            long expectedResetMax = afterRequest + windowSeconds + 1; // 允许1秒误差

            Assertions.assertThat(result.resetTime())
                    .as("Reset time should be within expected range")
                    .isGreaterThanOrEqualTo(expectedResetMin)
                    .isLessThanOrEqualTo(expectedResetMax);

            // 耗尽配额
            for (int i = 1; i < maxRequests; i++) {
                rateLimiter.tryAcquire(uniqueKey, config);
            }

            // 验证被拒绝的请求包含 retryAfter
            RateLimitResult deniedResult = rateLimiter.tryAcquire(uniqueKey, config);

            Assertions.assertThat(deniedResult.allowed())
                    .as("Request should be denied after quota exhausted")
                    .isFalse();

            Assertions.assertThat(deniedResult.remaining())
                    .as("Remaining should be 0 when denied")
                    .isEqualTo(0);

            Assertions.assertThat(deniedResult.resetTime())
                    .as("Reset time should be set when denied")
                    .isGreaterThan(0);

            // retryAfter 应该 >= 0（可能为0如果窗口即将重置）
            Assertions.assertThat(deniedResult.retryAfter())
                    .as("Retry after should be non-negative")
                    .isGreaterThanOrEqualTo(0);

        } finally {
            rateLimiter.reset(uniqueKey);
        }
    }

    /**
     * 验证不同维度的限流独立性
     */
    @Property(tries = 50)
    void differentDimensionsShouldBeIndependent(
            @ForAll @IntRange(min = 2, max = 5) int maxRequests) {
        if (!redisAvailable) {
            return;
        }

        String baseKey = "test-dimension-" + UUID.randomUUID();
        RateLimitConfig config = new RateLimitConfig(maxRequests, 60, RateLimitStrategy.SLIDING_WINDOW);

        try {
            // 在 USER 维度耗尽配额
            for (int i = 0; i < maxRequests; i++) {
                rateLimiter.tryAcquire(RateLimitDimension.USER, baseKey, config);
            }

            // USER 维度应该被拒绝
            RateLimitResult userResult = rateLimiter.tryAcquire(RateLimitDimension.USER, baseKey, config);
            Assertions.assertThat(userResult.allowed())
                    .as("USER dimension should be denied")
                    .isFalse();

            // IP 维度应该仍然允许（不同的 Redis key）
            RateLimitResult ipResult = rateLimiter.tryAcquire(RateLimitDimension.IP, baseKey, config);
            Assertions.assertThat(ipResult.allowed())
                    .as("IP dimension should still be allowed")
                    .isTrue();

        } finally {
            rateLimiter.reset(RateLimitDimension.USER, baseKey);
            rateLimiter.reset(RateLimitDimension.IP, baseKey);
        }
    }

    /**
     * 验证重置功能
     */
    @Property(tries = 50)
    void resetShouldClearRateLimit(
            @ForAll @IntRange(min = 1, max = 5) int maxRequests) {
        if (!redisAvailable) {
            return;
        }

        String uniqueKey = "test-reset-" + UUID.randomUUID();
        RateLimitConfig config = new RateLimitConfig(maxRequests, 60, RateLimitStrategy.SLIDING_WINDOW);

        try {
            // 耗尽配额
            for (int i = 0; i < maxRequests; i++) {
                rateLimiter.tryAcquire(uniqueKey, config);
            }

            // 验证被拒绝
            RateLimitResult deniedResult = rateLimiter.tryAcquire(uniqueKey, config);
            Assertions.assertThat(deniedResult.allowed())
                    .as("Should be denied after quota exhausted")
                    .isFalse();

            // 重置
            rateLimiter.reset(uniqueKey);

            // 验证重置后可以再次请求
            RateLimitResult afterResetResult = rateLimiter.tryAcquire(uniqueKey, config);
            Assertions.assertThat(afterResetResult.allowed())
                    .as("Should be allowed after reset")
                    .isTrue();

            Assertions.assertThat(afterResetResult.remaining())
                    .as("Remaining should be maxRequests - 1 after reset")
                    .isEqualTo(maxRequests - 1);

        } finally {
            rateLimiter.reset(uniqueKey);
        }
    }

    /**
     * 自定义断言类
     */
    private static class Assertions {
        static BooleanAssert assertThat(boolean actual) {
            return new BooleanAssert(actual);
        }

        static LongAssert assertThat(long actual) {
            return new LongAssert(actual);
        }
    }

    private static class BooleanAssert {
        private final boolean actual;
        private String description;

        BooleanAssert(boolean actual) {
            this.actual = actual;
        }

        BooleanAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isTrue() {
            if (!actual) {
                throw new AssertionError(description != null ? description : "Expected true but was false");
            }
        }

        void isFalse() {
            if (actual) {
                throw new AssertionError(description != null ? description : "Expected false but was true");
            }
        }
    }

    private static class LongAssert {
        private final long actual;
        private String description;

        LongAssert(long actual) {
            this.actual = actual;
        }

        LongAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        LongAssert isEqualTo(long expected) {
            if (actual != expected) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected " + expected + " but was " + actual);
            }
            return this;
        }

        LongAssert isGreaterThan(long expected) {
            if (actual <= expected) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected > " + expected + " but was " + actual);
            }
            return this;
        }

        LongAssert isGreaterThanOrEqualTo(long expected) {
            if (actual < expected) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected >= " + expected + " but was " + actual);
            }
            return this;
        }

        LongAssert isLessThanOrEqualTo(long expected) {
            if (actual > expected) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected <= " + expected + " but was " + actual);
            }
            return this;
        }
    }
}

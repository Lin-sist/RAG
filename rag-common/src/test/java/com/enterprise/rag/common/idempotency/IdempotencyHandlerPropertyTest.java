package com.enterprise.rag.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 幂等性处理器属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * 
 * Property 17: 幂等性处理正确性
 * 
 * Validates: Requirements 7.2, 7.3
 */
class IdempotencyHandlerPropertyTest {

    private static StringRedisTemplate stringRedisTemplate;
    private static RedisIdempotencyHandler idempotencyHandler;
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

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            idempotencyHandler = new RedisIdempotencyHandler(stringRedisTemplate, objectMapper);
            redisAvailable = true;
        } catch (Exception e) {
            System.err.println("Redis not available, skipping property tests: " + e.getMessage());
            redisAvailable = false;
        }
    }

    /**
     * Property 17: 幂等性处理正确性
     * 
     * *For any* 带幂等性 Key 的写操作请求，使用相同 Key 重复请求应返回与首次请求相同的结果。
     * 
     * 测试策略：
     * 1. 生成随机的幂等性 Key 和 payload
     * 2. 首次执行操作，记录结果
     * 3. 使用相同 Key 再次执行，验证返回相同结果
     * 4. 验证操作只执行了一次
     * 
     * **Validates: Requirements 7.2, 7.3**
     */
    @Property(tries = 100)
    void idempotencyKeyShouldReturnSameResult(
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String idempotencyKey,
            @ForAll @AlphaChars @StringLength(min = 1, max = 100) String payload) {
        if (!redisAvailable) {
            return;
        }

        // 使用唯一前缀避免测试间干扰
        String uniqueKey = "test-" + UUID.randomUUID() + "-" + idempotencyKey;

        // 计数器用于验证操作只执行一次
        AtomicInteger executionCount = new AtomicInteger(0);

        try {
            // 首次请求
            IdempotencyResult<TestResult> firstResult = idempotencyHandler.execute(
                    uniqueKey,
                    () -> {
                        executionCount.incrementAndGet();
                        return new TestResult(payload, System.currentTimeMillis());
                    },
                    TestResult.class,
                    60 // 60秒过期
            );

            // 验证首次请求是新请求
            Assertions.assertThat(firstResult.isNew())
                    .as("First request should be marked as new")
                    .isTrue();

            Assertions.assertThat(firstResult.result())
                    .as("First result should not be null")
                    .isNotNull();

            Assertions.assertThat(firstResult.result().payload())
                    .as("First result payload should match input")
                    .isEqualTo(payload);

            // 验证操作执行了一次
            Assertions.assertThat(executionCount.get())
                    .as("Operation should be executed once")
                    .isEqualTo(1);

            // 重复请求
            IdempotencyResult<TestResult> secondResult = idempotencyHandler.execute(
                    uniqueKey,
                    () -> {
                        executionCount.incrementAndGet();
                        return new TestResult("different-" + payload, System.currentTimeMillis());
                    },
                    TestResult.class,
                    60);

            // 验证重复请求不是新请求
            Assertions.assertThat(secondResult.isNew())
                    .as("Second request should not be marked as new")
                    .isFalse();

            // 验证返回相同结果
            Assertions.assertThat(secondResult.result())
                    .as("Second result should not be null")
                    .isNotNull();

            Assertions.assertThat(secondResult.result().payload())
                    .as("Second result should return same payload as first")
                    .isEqualTo(firstResult.result().payload());

            Assertions.assertThat(secondResult.result().timestamp())
                    .as("Second result should return same timestamp as first")
                    .isEqualTo(firstResult.result().timestamp());

            // 验证操作仍然只执行了一次
            Assertions.assertThat(executionCount.get())
                    .as("Operation should still be executed only once")
                    .isEqualTo(1);

        } finally {
            // 清理测试数据
            idempotencyHandler.remove(uniqueKey);
        }
    }

    /**
     * 验证 exists 方法正确性
     */
    @Property(tries = 100)
    void existsShouldReturnCorrectStatus(
            @ForAll @AlphaChars @StringLength(min = 10, max = 30) String idempotencyKey) {
        if (!redisAvailable) {
            return;
        }

        String uniqueKey = "test-exists-" + UUID.randomUUID() + "-" + idempotencyKey;

        try {
            // 初始状态应该不存在
            Assertions.assertThat(idempotencyHandler.exists(uniqueKey))
                    .as("Key should not exist initially")
                    .isFalse();

            // 执行操作
            idempotencyHandler.execute(
                    uniqueKey,
                    () -> "test-result",
                    String.class,
                    60);

            // 执行后应该存在
            Assertions.assertThat(idempotencyHandler.exists(uniqueKey))
                    .as("Key should exist after execution")
                    .isTrue();

            // 删除后应该不存在
            idempotencyHandler.remove(uniqueKey);

            Assertions.assertThat(idempotencyHandler.exists(uniqueKey))
                    .as("Key should not exist after removal")
                    .isFalse();

        } finally {
            idempotencyHandler.remove(uniqueKey);
        }
    }

    /**
     * 验证 getStoredResult 方法正确性
     */
    @Property(tries = 100)
    void getStoredResultShouldReturnCachedResult(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String payload) {
        if (!redisAvailable) {
            return;
        }

        String uniqueKey = "test-stored-" + UUID.randomUUID();

        try {
            // 初始状态应该返回 null
            IdempotencyResult<String> initialResult = idempotencyHandler.getStoredResult(uniqueKey, String.class);
            Assertions.assertThat(initialResult)
                    .as("Stored result should be null initially")
                    .isNull();

            // 执行操作
            idempotencyHandler.execute(
                    uniqueKey,
                    () -> payload,
                    String.class,
                    60);

            // 获取存储的结果
            IdempotencyResult<String> storedResult = idempotencyHandler.getStoredResult(uniqueKey, String.class);

            Assertions.assertThat(storedResult)
                    .as("Stored result should not be null after execution")
                    .isNotNull();

            Assertions.assertThat(storedResult.isNew())
                    .as("Stored result should not be marked as new")
                    .isFalse();

            Assertions.assertThat(storedResult.result())
                    .as("Stored result should match original payload")
                    .isEqualTo(payload);

        } finally {
            idempotencyHandler.remove(uniqueKey);
        }
    }

    /**
     * 验证不同 Key 的独立性
     */
    @Property(tries = 50)
    void differentKeysShouldBeIndependent(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String payload1,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String payload2) {
        if (!redisAvailable) {
            return;
        }

        String key1 = "test-independent-1-" + UUID.randomUUID();
        String key2 = "test-independent-2-" + UUID.randomUUID();

        try {
            // 使用 key1 执行操作
            IdempotencyResult<String> result1 = idempotencyHandler.execute(
                    key1,
                    () -> payload1,
                    String.class,
                    60);

            // 使用 key2 执行操作
            IdempotencyResult<String> result2 = idempotencyHandler.execute(
                    key2,
                    () -> payload2,
                    String.class,
                    60);

            // 两个结果都应该是新请求
            Assertions.assertThat(result1.isNew())
                    .as("First key result should be new")
                    .isTrue();

            Assertions.assertThat(result2.isNew())
                    .as("Second key result should be new")
                    .isTrue();

            // 结果应该各自独立
            Assertions.assertThat(result1.result())
                    .as("First result should match payload1")
                    .isEqualTo(payload1);

            Assertions.assertThat(result2.result())
                    .as("Second result should match payload2")
                    .isEqualTo(payload2);

        } finally {
            idempotencyHandler.remove(key1);
            idempotencyHandler.remove(key2);
        }
    }

    /**
     * 测试结果类
     */
    public record TestResult(String payload, long timestamp) {
    }

    /**
     * 自定义断言类
     */
    private static class Assertions {
        static BooleanAssert assertThat(boolean actual) {
            return new BooleanAssert(actual);
        }

        static IntAssert assertThat(int actual) {
            return new IntAssert(actual);
        }

        static <T> ObjectAssert<T> assertThat(T actual) {
            return new ObjectAssert<>(actual);
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

    private static class IntAssert {
        private final int actual;
        private String description;

        IntAssert(int actual) {
            this.actual = actual;
        }

        IntAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isEqualTo(int expected) {
            if (actual != expected) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected " + expected + " but was " + actual);
            }
        }
    }

    private static class ObjectAssert<T> {
        private final T actual;
        private String description;

        ObjectAssert(T actual) {
            this.actual = actual;
        }

        ObjectAssert<T> as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isNull() {
            if (actual != null) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected null but was " + actual);
            }
        }

        void isNotNull() {
            if (actual == null) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected non-null but was null");
            }
        }

        void isEqualTo(T expected) {
            if (actual == null && expected == null) {
                return;
            }
            if (actual == null || !actual.equals(expected)) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected " + expected + " but was " + actual);
            }
        }
    }
}

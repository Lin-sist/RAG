package com.enterprise.rag.common.trace;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.HashSet;
import java.util.Set;

/**
 * TraceId 生成器属性测试
 * 
 * Feature: enterprise-rag-qa-system, Property 18: TraceId 唯一性
 * Validates: Requirements 8.1
 */
class TraceIdGeneratorPropertyTest {

    /**
     * Property 18: TraceId 唯一性
     * 
     * *For any* 进入系统的请求，生成的 TraceId 应是唯一的（在合理时间窗口内不重复）。
     * 
     * 测试策略：生成大量 TraceId，验证它们都是唯一的
     * 
     * **Validates: Requirements 8.1**
     */
    @Property(tries = 100)
    void traceIdsShouldBeUnique(@ForAll @IntRange(min = 100, max = 1000) int count) {
        Set<String> generatedIds = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            String traceId = TraceIdGenerator.generate();
            
            // 验证 TraceId 格式正确
            Assertions.assertThat(traceId)
                .isNotNull()
                .hasSize(32)
                .matches("[0-9a-f]{32}");
            
            // 验证 TraceId 唯一性
            boolean isUnique = generatedIds.add(traceId);
            Assertions.assertThat(isUnique)
                .as("TraceId should be unique, but found duplicate: %s", traceId)
                .isTrue();
        }
        
        // 验证生成了预期数量的唯一 ID
        Assertions.assertThat(generatedIds).hasSize(count);
    }

    /**
     * 验证 TraceId 格式有效性
     */
    @Property(tries = 100)
    void generatedTraceIdShouldBeValid() {
        String traceId = TraceIdGenerator.generate();
        
        Assertions.assertThat(TraceIdGenerator.isValid(traceId))
            .as("Generated TraceId should be valid: %s", traceId)
            .isTrue();
    }

    /**
     * 验证 UUID 格式的 TraceId 也是唯一的
     */
    @Property(tries = 100)
    void uuidTraceIdsShouldBeUnique(@ForAll @IntRange(min = 100, max = 500) int count) {
        Set<String> generatedIds = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            String traceId = TraceIdGenerator.generateUUID();
            
            // 验证格式
            Assertions.assertThat(traceId)
                .isNotNull()
                .hasSize(32)
                .matches("[0-9a-f]{32}");
            
            // 验证唯一性
            boolean isUnique = generatedIds.add(traceId);
            Assertions.assertThat(isUnique)
                .as("UUID TraceId should be unique, but found duplicate: %s", traceId)
                .isTrue();
        }
    }

    /**
     * 验证无效 TraceId 被正确识别
     */
    @Property(tries = 100)
    void invalidTraceIdsShouldBeRejected(@ForAll("invalidTraceIds") String invalidId) {
        Assertions.assertThat(TraceIdGenerator.isValid(invalidId))
            .as("Invalid TraceId should be rejected: %s", invalidId)
            .isFalse();
    }

    @Provide
    Arbitrary<String> invalidTraceIds() {
        return Arbitraries.oneOf(
            // null 值
            Arbitraries.just(null),
            // 空字符串
            Arbitraries.just(""),
            // 长度不正确
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(31),
            Arbitraries.strings().alpha().ofMinLength(33).ofMaxLength(50),
            // 包含非十六进制字符
            Arbitraries.strings().withChars("ghijklmnopqrstuvwxyz!@#$%").ofLength(32),
            // 混合有效和无效字符
            Arbitraries.strings().withChars("0123456789abcdefGHIJKL").ofLength(32)
                .filter(s -> s.chars().anyMatch(c -> c > 'f' && c <= 'z' || c > 'F' && c <= 'Z'))
        );
    }

    /**
     * AssertJ 风格的断言辅助类
     */
    private static class Assertions {
        static StringAssert assertThat(String actual) {
            return new StringAssert(actual);
        }

        static BooleanAssert assertThat(boolean actual) {
            return new BooleanAssert(actual);
        }

        static <T> CollectionAssert<T> assertThat(Set<T> actual) {
            return new CollectionAssert<>(actual);
        }
    }

    private static class StringAssert {
        private final String actual;
        private String description;

        StringAssert(String actual) {
            this.actual = actual;
        }

        StringAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        StringAssert isNotNull() {
            if (actual == null) {
                throw new AssertionError(description != null ? description : "Expected non-null value");
            }
            return this;
        }

        StringAssert hasSize(int expected) {
            if (actual.length() != expected) {
                throw new AssertionError(
                    (description != null ? description + ": " : "") +
                    "Expected size " + expected + " but was " + actual.length());
            }
            return this;
        }

        StringAssert matches(String regex) {
            if (!actual.matches(regex)) {
                throw new AssertionError(
                    (description != null ? description + ": " : "") +
                    "Expected to match " + regex + " but was " + actual);
            }
            return this;
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

    private static class CollectionAssert<T> {
        private final Set<T> actual;

        CollectionAssert(Set<T> actual) {
            this.actual = actual;
        }

        void hasSize(int expected) {
            if (actual.size() != expected) {
                throw new AssertionError("Expected size " + expected + " but was " + actual.size());
            }
        }
    }
}

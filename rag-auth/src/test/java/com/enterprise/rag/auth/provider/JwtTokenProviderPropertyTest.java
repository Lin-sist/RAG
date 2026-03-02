package com.enterprise.rag.auth.provider;

import com.enterprise.rag.auth.config.JwtProperties;
import com.enterprise.rag.auth.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.HashSet;
import java.util.Set;

/**
 * JWT Token 属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5
 */
class JwtTokenProviderPropertyTest {

    private final JwtTokenProvider jwtTokenProvider;

    JwtTokenProviderPropertyTest() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        jwtProperties.setAccessTokenExpiration(3600L);
        jwtProperties.setRefreshTokenExpiration(86400L);
        jwtProperties.setIssuer("test-issuer");
        this.jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    /**
     * Property 1: JWT Token 往返一致性
     * 
     * *For any* 有效的用户凭证，生成 JWT Token 后再验证该 Token，
     * 应该能正确识别用户身份并允许访问。
     * 
     * **Validates: Requirements 1.1, 1.2**
     */
    @Property(tries = 100)
    void jwtTokenRoundTripConsistency(
            @ForAll @IntRange(min = 1, max = 10000) long userId,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll("validRoles") Set<String> roles) {
        
        // 创建用户
        UserPrincipal user = UserPrincipal.builder()
                .id(userId)
                .username(username)
                .email(username + "@test.com")
                .enabled(true)
                .roles(roles)
                .build();

        // 生成 Access Token
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        // 验证 Token 有效
        Assertions.assertThat(jwtTokenProvider.isTokenValid(accessToken))
                .as("Generated token should be valid")
                .isTrue();

        // 从 Token 中恢复用户信息
        UserPrincipal recoveredUser = jwtTokenProvider.getUserPrincipalFromToken(accessToken);

        // 验证用户信息一致
        Assertions.assertThat(recoveredUser.getId())
                .as("User ID should match")
                .isEqualTo(userId);
        Assertions.assertThat(recoveredUser.getUsername())
                .as("Username should match")
                .isEqualTo(username);
        Assertions.assertThat(recoveredUser.getRoles())
                .as("Roles should match")
                .isEqualTo(roles);
    }

    /**
     * Property 2: 无效 Token 拒绝
     * 
     * *For any* 无效的 JWT Token（过期、篡改、格式错误），
     * 验证时应返回 false。
     * 
     * **Validates: Requirements 1.3**
     */
    @Property(tries = 100)
    void invalidTokensShouldBeRejected(@ForAll("invalidTokens") String invalidToken) {
        Assertions.assertThat(jwtTokenProvider.isTokenValid(invalidToken))
                .as("Invalid token should be rejected: %s", invalidToken)
                .isFalse();
    }

    /**
     * Property 3: Token 刷新有效性
     * 
     * *For any* 有效的 Refresh Token，刷新后生成的新 Access Token 
     * 应该是有效的且能通过验证。
     * 
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void refreshTokenShouldGenerateValidAccessToken(
            @ForAll @IntRange(min = 1, max = 10000) long userId,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll("validRoles") Set<String> roles) {
        
        // 创建用户
        UserPrincipal user = UserPrincipal.builder()
                .id(userId)
                .username(username)
                .email(username + "@test.com")
                .enabled(true)
                .roles(roles)
                .build();

        // 生成 Refresh Token
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // 验证 Refresh Token 有效
        Assertions.assertThat(jwtTokenProvider.isTokenValid(refreshToken))
                .as("Refresh token should be valid")
                .isTrue();

        // 验证 Token 类型
        Assertions.assertThat(jwtTokenProvider.getTokenType(refreshToken))
                .as("Token type should be 'refresh'")
                .isEqualTo("refresh");

        // 从 Refresh Token 获取用户信息并生成新的 Access Token
        UserPrincipal recoveredUser = jwtTokenProvider.getUserPrincipalFromToken(refreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(recoveredUser);

        // 验证新 Access Token 有效
        Assertions.assertThat(jwtTokenProvider.isTokenValid(newAccessToken))
                .as("New access token should be valid")
                .isTrue();

        // 验证新 Token 类型
        Assertions.assertThat(jwtTokenProvider.getTokenType(newAccessToken))
                .as("New token type should be 'access'")
                .isEqualTo("access");

        // 验证用户信息一致
        UserPrincipal newUser = jwtTokenProvider.getUserPrincipalFromToken(newAccessToken);
        Assertions.assertThat(newUser.getId())
                .as("User ID should match after refresh")
                .isEqualTo(userId);
        Assertions.assertThat(newUser.getUsername())
                .as("Username should match after refresh")
                .isEqualTo(username);
    }

    /**
     * 验证 Token 包含正确的 Claims
     */
    @Property(tries = 100)
    void tokenShouldContainCorrectClaims(
            @ForAll @IntRange(min = 1, max = 10000) long userId,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username,
            @ForAll("validRoles") Set<String> roles) {
        
        UserPrincipal user = UserPrincipal.builder()
                .id(userId)
                .username(username)
                .email(username + "@test.com")
                .enabled(true)
                .roles(roles)
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);
        Claims claims = jwtTokenProvider.parseToken(token);

        // 验证 Claims
        Assertions.assertThat(claims.getSubject())
                .as("Subject should be username")
                .isEqualTo(username);
        Assertions.assertThat(claims.get("userId", Long.class))
                .as("userId claim should match")
                .isEqualTo(userId);
        Assertions.assertThat(claims.get("tokenType", String.class))
                .as("tokenType claim should be 'access'")
                .isEqualTo("access");
        Assertions.assertThat(claims.getIssuer())
                .as("Issuer should match")
                .isEqualTo("test-issuer");
        Assertions.assertThat(claims.getExpiration())
                .as("Expiration should be set")
                .isNotNull();
        Assertions.assertThat(claims.getIssuedAt())
                .as("IssuedAt should be set")
                .isNotNull();
    }

    /**
     * 验证 Token Hash 唯一性
     */
    @Property(tries = 100)
    void tokenHashesShouldBeUnique(@ForAll @IntRange(min = 10, max = 100) int count) {
        Set<String> hashes = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            UserPrincipal user = UserPrincipal.builder()
                    .id((long) i)
                    .username("user" + i)
                    .email("user" + i + "@test.com")
                    .enabled(true)
                    .roles(Set.of("USER"))
                    .build();

            String token = jwtTokenProvider.generateAccessToken(user);
            String hash = jwtTokenProvider.getTokenHash(token);

            boolean isUnique = hashes.add(hash);
            Assertions.assertThat(isUnique)
                    .as("Token hash should be unique")
                    .isTrue();
        }
    }

    @Provide
    Arbitrary<Set<String>> validRoles() {
        return Arbitraries.of(
                Set.of("USER"),
                Set.of("ADMIN"),
                Set.of("USER", "ADMIN"),
                Set.of("USER", "MANAGER"),
                Set.of("ADMIN", "MANAGER", "USER")
        );
    }

    @Provide
    Arbitrary<String> invalidTokens() {
        return Arbitraries.oneOf(
                // null 值
                Arbitraries.just(null),
                // 空字符串
                Arbitraries.just(""),
                // 随机字符串
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100),
                // 格式错误的 JWT（缺少部分）
                Arbitraries.just("header.payload"),
                Arbitraries.just("header"),
                // 无效的 Base64
                Arbitraries.just("invalid.base64.token"),
                // 篡改的 Token（修改签名）
                Arbitraries.just("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.tampered_signature")
        );
    }

    /**
     * 断言辅助类
     */
    private static class Assertions {
        static BooleanAssert assertThat(boolean actual) {
            return new BooleanAssert(actual);
        }

        static StringAssert assertThat(String actual) {
            return new StringAssert(actual);
        }

        static LongAssert assertThat(Long actual) {
            return new LongAssert(actual);
        }

        static <T> ObjectAssert<T> assertThat(T actual) {
            return new ObjectAssert<>(actual);
        }

        static <T> SetAssert<T> assertThat(Set<T> actual) {
            return new SetAssert<>(actual);
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

        StringAssert isEqualTo(String expected) {
            if (actual == null && expected != null || actual != null && !actual.equals(expected)) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected '" + expected + "' but was '" + actual + "'");
            }
            return this;
        }

        StringAssert isNotNull() {
            if (actual == null) {
                throw new AssertionError(description != null ? description : "Expected non-null value");
            }
            return this;
        }
    }

    private static class LongAssert {
        private final Long actual;
        private String description;

        LongAssert(Long actual) {
            this.actual = actual;
        }

        LongAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        LongAssert isEqualTo(long expected) {
            if (actual == null || actual != expected) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected " + expected + " but was " + actual);
            }
            return this;
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

        ObjectAssert<T> isNotNull() {
            if (actual == null) {
                throw new AssertionError(description != null ? description : "Expected non-null value");
            }
            return this;
        }

        ObjectAssert<T> isEqualTo(T expected) {
            if (actual == null && expected != null || actual != null && !actual.equals(expected)) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected '" + expected + "' but was '" + actual + "'");
            }
            return this;
        }
    }

    private static class SetAssert<T> {
        private final Set<T> actual;
        private String description;

        SetAssert(Set<T> actual) {
            this.actual = actual;
        }

        SetAssert<T> as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        SetAssert<T> isEqualTo(Set<T> expected) {
            if (actual == null && expected != null || actual != null && !actual.equals(expected)) {
                throw new AssertionError(
                        (description != null ? description + ": " : "") +
                                "Expected " + expected + " but was " + actual);
            }
            return this;
        }
    }
}

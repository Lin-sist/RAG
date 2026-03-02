package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.config.JwtProperties;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.common.util.RedisUtil;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Token 黑名单服务属性测试
 * 
 * Feature: enterprise-rag-qa-system, Property 4: Token 黑名单有效性
 * Validates: Requirements 1.5
 */
class TokenBlacklistServicePropertyTest {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtil redisUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final Set<String> blacklistedKeys = new HashSet<>();

    TokenBlacklistServicePropertyTest() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-must-be-at-least-256-bits-long-for-hs256");
        jwtProperties.setAccessTokenExpiration(3600L);
        jwtProperties.setRefreshTokenExpiration(86400L);
        jwtProperties.setIssuer("test-issuer");
        
        this.jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        this.redisUtil = Mockito.mock(RedisUtil.class);
        this.tokenBlacklistService = new TokenBlacklistService(redisUtil, jwtTokenProvider);

        // 模拟 Redis 行为
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            blacklistedKeys.add(key);
            return null;
        }).when(redisUtil).setString(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return blacklistedKeys.contains(key);
        }).when(redisUtil).hasKey(anyString());

        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            blacklistedKeys.remove(key);
            return true;
        }).when(redisUtil).delete(anyString());
    }

    /**
     * Property 4: Token 黑名单有效性
     * 
     * *For any* 已登出的 Token，该 Token 应被加入黑名单，
     * 后续使用该 Token 的请求应被拒绝。
     * 
     * **Validates: Requirements 1.5**
     */
    @Property(tries = 100)
    void blacklistedTokenShouldBeRejected(
            @ForAll @IntRange(min = 1, max = 10000) long userId,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username) {
        
        // 清理之前的状态
        blacklistedKeys.clear();

        // 创建用户并生成 Token
        UserPrincipal user = UserPrincipal.builder()
                .id(userId)
                .username(username)
                .email(username + "@test.com")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        // 验证 Token 初始状态不在黑名单中
        Assertions.assertThat(tokenBlacklistService.isBlacklisted(token))
                .as("Token should not be blacklisted initially")
                .isFalse();

        // 将 Token 加入黑名单
        tokenBlacklistService.addToBlacklist(token);

        // 验证 Token 现在在黑名单中
        Assertions.assertThat(tokenBlacklistService.isBlacklisted(token))
                .as("Token should be blacklisted after adding")
                .isTrue();
    }

    /**
     * 验证从黑名单移除后 Token 不再被拒绝
     */
    @Property(tries = 100)
    void removedTokenShouldNotBeBlacklisted(
            @ForAll @IntRange(min = 1, max = 10000) long userId,
            @ForAll @AlphaChars @StringLength(min = 3, max = 20) String username) {
        
        // 清理之前的状态
        blacklistedKeys.clear();

        UserPrincipal user = UserPrincipal.builder()
                .id(userId)
                .username(username)
                .email(username + "@test.com")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);

        // 加入黑名单
        tokenBlacklistService.addToBlacklist(token);
        Assertions.assertThat(tokenBlacklistService.isBlacklisted(token))
                .as("Token should be blacklisted")
                .isTrue();

        // 从黑名单移除
        tokenBlacklistService.removeFromBlacklist(token);
        Assertions.assertThat(tokenBlacklistService.isBlacklisted(token))
                .as("Token should not be blacklisted after removal")
                .isFalse();
    }

    /**
     * 验证空或 null Token 不会导致异常
     */
    @Property(tries = 100)
    void nullOrEmptyTokenShouldNotCauseException(@ForAll("nullOrEmptyTokens") String token) {
        // 不应抛出异常
        tokenBlacklistService.addToBlacklist(token);
        
        // 空或 null Token 不应被视为在黑名单中
        Assertions.assertThat(tokenBlacklistService.isBlacklisted(token))
                .as("Null or empty token should not be blacklisted")
                .isFalse();
    }

    @Provide
    Arbitrary<String> nullOrEmptyTokens() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   ")
        );
    }

    /**
     * 断言辅助类
     */
    private static class Assertions {
        static BooleanAssert assertThat(boolean actual) {
            return new BooleanAssert(actual);
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
}

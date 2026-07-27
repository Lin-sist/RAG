package com.enterprise.rag.auth.service.impl;

import com.enterprise.rag.auth.dto.AuthResponse;
import com.enterprise.rag.auth.dto.LoginRequest;
import com.enterprise.rag.auth.exception.AuthException;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.auth.service.AuthService;
import com.enterprise.rag.auth.service.TokenBlacklistService;
import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.exception.RedisDependencyException;
import com.enterprise.rag.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserDetailsService userDetailsService;
    private final RedisUtil redisUtil;

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            // 使用 Spring Security 进行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // 生成 Token
            String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userPrincipal);

            // 保存用户会话到 Redis
            saveUserSession(userPrincipal, accessToken, refreshToken);

            log.info("用户登录成功");

            return buildAuthResponse(userPrincipal, accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            log.warn("登录失败：用户名或密码错误");
            throw AuthException.invalidCredentials();
        } catch (DisabledException e) {
            log.warn("登录失败：用户已禁用");
            throw AuthException.userDisabled();
        }
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return;
        }

        // 将 Token 加入黑名单；失败时不得报告登出成功
        tokenBlacklistService.addToBlacklist(accessToken);

        UserPrincipal userPrincipal = jwtTokenProvider.getUserPrincipalFromToken(accessToken);
        requireValidIdentity(userPrincipal, AuthException.invalidToken());
        String sessionKey = sessionKey(userPrincipal);
        try {
            Boolean sessionExists = redisUtil.hasKey(sessionKey);
            if (Boolean.TRUE.equals(sessionExists)) {
                Object storedTenantId = redisUtil.hGet(sessionKey, "tenantId");
                Object storedUserId = redisUtil.hGet(sessionKey, "userId");
                if (!matchesSessionIdentity(userPrincipal, storedTenantId, storedUserId)) {
                    throw AuthException.invalidToken();
                }
                Object refreshTokenInSession = redisUtil.hGet(sessionKey, "refreshToken");
                if (refreshTokenInSession instanceof String refreshToken && !refreshToken.isBlank()) {
                    tokenBlacklistService.addToBlacklist(refreshToken);
                }
                redisUtil.delete(sessionKey);
            }
        } catch (AuthException | RedisDependencyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Logout session revoke failed closed: dependency=redis, subsystem=auth_session, "
                            + "operation=revoke, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("auth_session", "revoke", e);
        }

        log.info("用户登出成功");
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // 验证 Refresh Token
        if (!jwtTokenProvider.isTokenValid(refreshToken)) {
            throw AuthException.invalidRefreshToken();
        }

        // 检查是否在黑名单中
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw AuthException.tokenBlacklisted();
        }

        // 检查 Token 类型
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw AuthException.invalidRefreshToken();
        }

        // 获取用户信息
        UserPrincipal userPrincipal = jwtTokenProvider.getUserPrincipalFromToken(refreshToken);
        requireValidIdentity(userPrincipal, AuthException.invalidRefreshToken());

        // 校验 refresh token 对应会话必须存在且 token 一致
        validateRefreshSession(userPrincipal, refreshToken);

        // 重新加载用户信息（确保用户状态最新）
        UserPrincipal freshUserPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(
                userPrincipal.getUsername());

        if (!sameIdentity(userPrincipal, freshUserPrincipal)) {
            throw AuthException.invalidRefreshToken();
        }

        if (!freshUserPrincipal.isEnabled()) {
            throw AuthException.userDisabled();
        }

        // 生成新的 Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(freshUserPrincipal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(freshUserPrincipal);

        // 将旧的 Refresh Token 加入黑名单
        tokenBlacklistService.addToBlacklist(refreshToken);

        // 更新用户会话
        saveUserSession(freshUserPrincipal, newAccessToken, newRefreshToken);

        log.info("用户 Token 刷新成功");

        return buildAuthResponse(freshUserPrincipal, newAccessToken, newRefreshToken);
    }

    @Override
    public UserPrincipal validateToken(String token) {
        // 验证 Token 格式和签名
        if (!jwtTokenProvider.isTokenValid(token)) {
            throw AuthException.invalidToken();
        }

        // 检查是否在黑名单中
        if (tokenBlacklistService.isBlacklisted(token)) {
            throw AuthException.tokenBlacklisted();
        }

        // 检查 Token 类型（只接受 access token）
        String tokenType = jwtTokenProvider.getTokenType(token);
        if (!"access".equals(tokenType)) {
            throw AuthException.invalidToken();
        }

        return jwtTokenProvider.getUserPrincipalFromToken(token);
    }

    /**
     * 保存用户会话到 Redis
     */
    private void saveUserSession(UserPrincipal userPrincipal, String accessToken, String refreshToken) {
        String sessionKey = sessionKey(userPrincipal);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("tenantId", userPrincipal.getTenantId());
        sessionData.put("userId", userPrincipal.getId());
        sessionData.put("accessToken", accessToken);
        sessionData.put("refreshToken", refreshToken);
        sessionData.put("loginTime", Instant.now().toString());
        sessionData.put("lastActiveTime", Instant.now().toString());

        try {
            redisUtil.hSetAll(sessionKey, sessionData);
            // 会话 TTL 与 refresh token 生命周期保持一致，避免出现 token 未过期但会话先失效
            redisUtil.expire(sessionKey, jwtTokenProvider.getRefreshTokenExpiration(), TimeUnit.SECONDS);
        } catch (Exception e) {
            try {
                redisUtil.delete(sessionKey);
            } catch (Exception cleanupError) {
                log.warn("Auth session cleanup degraded: dependency=redis, subsystem=auth_session, "
                                + "operation=delete, failMode=closed, errorType={}",
                        cleanupError.getClass().getSimpleName());
            }
            log.error("Auth session write failed closed: dependency=redis, subsystem=auth_session, "
                            + "operation=write, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("auth_session", "write", e);
        }
    }

    private void validateRefreshSession(UserPrincipal userPrincipal, String refreshToken) {
        String sessionKey = sessionKey(userPrincipal);
        Boolean sessionExists;
        Object storedTenantId;
        Object storedUserId;
        Object storedRefreshToken;
        try {
            sessionExists = redisUtil.hasKey(sessionKey);
            if (Boolean.TRUE.equals(sessionExists)) {
                storedTenantId = redisUtil.hGet(sessionKey, "tenantId");
                storedUserId = redisUtil.hGet(sessionKey, "userId");
                storedRefreshToken = redisUtil.hGet(sessionKey, "refreshToken");
            } else {
                storedTenantId = null;
                storedUserId = null;
                storedRefreshToken = null;
            }
        } catch (Exception e) {
            log.error("Auth session read failed closed: dependency=redis, subsystem=auth_session, "
                            + "operation=read, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("auth_session", "read", e);
        }

        if (!Boolean.TRUE.equals(sessionExists)) {
            throw AuthException.invalidRefreshToken();
        }

        if (!matchesSessionIdentity(userPrincipal, storedTenantId, storedUserId)) {
            throw AuthException.invalidRefreshToken();
        }

        if (!(storedRefreshToken instanceof String tokenInSession)
                || !Objects.equals(tokenInSession, refreshToken)) {
            throw AuthException.invalidRefreshToken();
        }
    }

    private String sessionKey(UserPrincipal userPrincipal) {
        if (!hasValidIdentity(userPrincipal)) {
            throw new IllegalArgumentException("User principal must contain a valid tenant and user identity");
        }
        return RedisKeyConstants.userSessionV2Key(
                userPrincipal.getTenantId(), userPrincipal.getId());
    }

    private void requireValidIdentity(UserPrincipal userPrincipal, AuthException failure) {
        if (!hasValidIdentity(userPrincipal)) {
            throw failure;
        }
    }

    private boolean hasValidIdentity(UserPrincipal userPrincipal) {
        return userPrincipal != null
                && userPrincipal.getTenantId() != null
                && userPrincipal.getTenantId() > 0
                && userPrincipal.getId() != null
                && userPrincipal.getId() > 0;
    }

    private boolean sameIdentity(UserPrincipal expected, UserPrincipal actual) {
        return hasValidIdentity(expected)
                && hasValidIdentity(actual)
                && Objects.equals(expected.getTenantId(), actual.getTenantId())
                && Objects.equals(expected.getId(), actual.getId());
    }

    private boolean matchesSessionIdentity(
            UserPrincipal expected,
            Object storedTenantId,
            Object storedUserId) {
        return matchesId(storedTenantId, expected.getTenantId())
                && matchesId(storedUserId, expected.getId());
    }

    private boolean matchesId(Object storedValue, Long expectedValue) {
        if (!(storedValue instanceof Number number) || expectedValue == null) {
            return false;
        }
        try {
            return new BigDecimal(number.toString()).longValueExact() == expectedValue;
        } catch (ArithmeticException | NumberFormatException e) {
            return false;
        }
    }

    /**
     * 构建认证响应
     */
    private AuthResponse buildAuthResponse(UserPrincipal user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiration())
                .tokenType("Bearer")
                .userInfo(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .build();
    }
}

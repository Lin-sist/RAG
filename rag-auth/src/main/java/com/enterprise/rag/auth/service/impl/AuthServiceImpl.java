package com.enterprise.rag.auth.service.impl;

import com.enterprise.rag.auth.dto.AuthResponse;
import com.enterprise.rag.auth.dto.LoginRequest;
import com.enterprise.rag.auth.exception.AuthException;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.auth.service.AuthService;
import com.enterprise.rag.auth.service.TokenBlacklistService;
import com.enterprise.rag.common.constant.RedisKeyConstants;
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
            saveUserSession(userPrincipal.getId(), accessToken, refreshToken);

            log.info("用户 {} 登录成功", userPrincipal.getUsername());

            return buildAuthResponse(userPrincipal, accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            log.warn("登录失败：用户名或密码错误 - {}", request.getUsername());
            throw AuthException.invalidCredentials();
        } catch (DisabledException e) {
            log.warn("登录失败：用户已禁用 - {}", request.getUsername());
            throw AuthException.userDisabled();
        }
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return;
        }

        try {
            // 将 Token 加入黑名单
            tokenBlacklistService.addToBlacklist(accessToken);

            // 获取用户 ID 并清除会话
            Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
            if (userId != null) {
                String sessionKey = RedisKeyConstants.userSessionKey(userId);

                // 同步失效当前会话里的 refresh token，防止 logout 后继续刷新
                Object refreshTokenInSession = redisUtil.hGet(sessionKey, "refreshToken");
                if (refreshTokenInSession instanceof String refreshToken && !refreshToken.isBlank()) {
                    tokenBlacklistService.addToBlacklist(refreshToken);
                }

                redisUtil.delete(sessionKey);
            }

            log.info("用户登出成功");
        } catch (Exception e) {
            log.error("登出处理失败", e);
        }
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

        // 校验 refresh token 对应会话必须存在且 token 一致
        validateRefreshSession(userPrincipal.getId(), refreshToken);

        // 重新加载用户信息（确保用户状态最新）
        UserPrincipal freshUserPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(
                userPrincipal.getUsername());

        if (!freshUserPrincipal.isEnabled()) {
            throw AuthException.userDisabled();
        }

        // 生成新的 Token
        String newAccessToken = jwtTokenProvider.generateAccessToken(freshUserPrincipal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(freshUserPrincipal);

        // 将旧的 Refresh Token 加入黑名单
        tokenBlacklistService.addToBlacklist(refreshToken);

        // 更新用户会话
        saveUserSession(freshUserPrincipal.getId(), newAccessToken, newRefreshToken);

        log.info("用户 {} Token 刷新成功", freshUserPrincipal.getUsername());

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
    private void saveUserSession(Long userId, String accessToken, String refreshToken) {
        String sessionKey = RedisKeyConstants.userSessionKey(userId);

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("accessToken", accessToken);
        sessionData.put("refreshToken", refreshToken);
        sessionData.put("loginTime", Instant.now().toString());
        sessionData.put("lastActiveTime", Instant.now().toString());

        redisUtil.hSetAll(sessionKey, sessionData);
        // 会话 TTL 与 refresh token 生命周期保持一致，避免出现 token 未过期但会话先失效
        redisUtil.expire(sessionKey, jwtTokenProvider.getRefreshTokenExpiration(), TimeUnit.SECONDS);
    }

    private void validateRefreshSession(Long userId, String refreshToken) {
        String sessionKey = RedisKeyConstants.userSessionKey(userId);
        if (!Boolean.TRUE.equals(redisUtil.hasKey(sessionKey))) {
            throw AuthException.invalidRefreshToken();
        }

        Object storedRefreshToken = redisUtil.hGet(sessionKey, "refreshToken");
        if (!(storedRefreshToken instanceof String tokenInSession)
                || !Objects.equals(tokenInSession, refreshToken)) {
            throw AuthException.invalidRefreshToken();
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

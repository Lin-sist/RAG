package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.exception.RedisDependencyException;
import com.enterprise.rag.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 使用 Redis SET 存储已失效的 Token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisUtil redisUtil;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 将 Token 加入黑名单
     * 
     * @param token JWT Token
     */
    public void addToBlacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        long remainingTime = jwtTokenProvider.getRemainingTimeInSeconds(token);
        if (remainingTime <= 0) {
            log.debug("Token 已过期，无需加入黑名单");
            return;
        }

        String tokenHash = jwtTokenProvider.getTokenHash(token);
        String blacklistKey = RedisKeyConstants.tokenBlacklistKey(tokenHash);

        try {
            redisUtil.setString(blacklistKey, "1", remainingTime, TimeUnit.SECONDS);
            log.info("Token 已加入黑名单，将在 {} 秒后自动移除", remainingTime);
        } catch (Exception e) {
            log.error("Token blacklist write failed closed: dependency=redis, "
                            + "subsystem=token_blacklist, operation=write, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("token_blacklist", "write", e);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     * 
     * @param token JWT Token
     * @return true 如果在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String tokenHash = jwtTokenProvider.getTokenHash(token);
        String blacklistKey = RedisKeyConstants.tokenBlacklistKey(tokenHash);
        try {
            Boolean exists = redisUtil.hasKey(blacklistKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Token blacklist lookup failed closed: dependency=redis, "
                            + "subsystem=token_blacklist, operation=read, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("token_blacklist", "read", e);
        }
    }

    /**
     * 从黑名单中移除 Token（通常不需要手动调用，Redis 会自动过期）
     * 
     * @param token JWT Token
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        String tokenHash = jwtTokenProvider.getTokenHash(token);
        String blacklistKey = RedisKeyConstants.tokenBlacklistKey(tokenHash);
        try {
            redisUtil.delete(blacklistKey);
            log.info("Token 已从黑名单中移除");
        } catch (Exception e) {
            log.error("Token blacklist removal failed closed: dependency=redis, "
                            + "subsystem=token_blacklist, operation=delete, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("token_blacklist", "delete", e);
        }
    }
}

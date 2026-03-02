package com.enterprise.rag.auth.service;

import com.enterprise.rag.auth.provider.JwtTokenProvider;
import com.enterprise.rag.common.constant.RedisKeyConstants;
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
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            // 获取 Token 剩余有效时间
            long remainingTime = jwtTokenProvider.getRemainingTimeInSeconds(token);
            
            if (remainingTime <= 0) {
                // Token 已过期，无需加入黑名单
                log.debug("Token 已过期，无需加入黑名单");
                return;
            }

            // 使用 Token Hash 作为 Key
            String tokenHash = jwtTokenProvider.getTokenHash(token);
            String blacklistKey = RedisKeyConstants.tokenBlacklistKey(tokenHash);

            // 存储到 Redis，过期时间与 Token 剩余有效时间一致
            redisUtil.setString(blacklistKey, "1", remainingTime, TimeUnit.SECONDS);
            
            log.info("Token 已加入黑名单，将在 {} 秒后自动移除", remainingTime);
        } catch (Exception e) {
            log.error("将 Token 加入黑名单失败", e);
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     * 
     * @param token JWT Token
     * @return true 如果在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            String tokenHash = jwtTokenProvider.getTokenHash(token);
            String blacklistKey = RedisKeyConstants.tokenBlacklistKey(tokenHash);
            
            Boolean exists = redisUtil.hasKey(blacklistKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("检查 Token 黑名单失败", e);
            // 出错时保守处理，认为不在黑名单中
            return false;
        }
    }

    /**
     * 从黑名单中移除 Token（通常不需要手动调用，Redis 会自动过期）
     * 
     * @param token JWT Token
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            String tokenHash = jwtTokenProvider.getTokenHash(token);
            String blacklistKey = RedisKeyConstants.tokenBlacklistKey(tokenHash);
            
            redisUtil.delete(blacklistKey);
            log.info("Token 已从黑名单中移除");
        } catch (Exception e) {
            log.error("从黑名单移除 Token 失败", e);
        }
    }
}

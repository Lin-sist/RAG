package com.enterprise.rag.auth.provider;

import com.enterprise.rag.auth.config.JwtProperties;
import com.enterprise.rag.auth.model.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT Token 提供者
 * 负责 Token 的生成、解析和验证
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 生成 Access Token
     */
    public String generateAccessToken(UserPrincipal user) {
        return generateToken(user, jwtProperties.getAccessTokenExpiration(), "access");
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(UserPrincipal user) {
        return generateToken(user, jwtProperties.getRefreshTokenExpiration(), "refresh");
    }

    /**
     * 生成 Token
     */
    private String generateToken(UserPrincipal user, long expirationSeconds, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("roles", user.getRoles());
        claims.put("tokenType", tokenType);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token 获取 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的 JWT Token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT Token 格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT Token 签名无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token 为空: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /**
     * 从 Token 中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        List<String> roles = parseToken(token).get("roles", List.class);
        return roles != null ? new HashSet<>(roles) : Collections.emptySet();
    }

    /**
     * 获取 Token 类型（access/refresh）
     */
    public String getTokenType(String token) {
        return parseToken(token).get("tokenType", String.class);
    }

    /**
     * 获取 Token 过期时间
     */
    public Date getExpirationFromToken(String token) {
        return parseToken(token).getExpiration();
    }

    /**
     * 获取 Token 剩余有效时间（秒）
     */
    public long getRemainingTimeInSeconds(String token) {
        Date expiration = getExpirationFromToken(token);
        long remaining = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * 计算 Token 的 Hash 值（用于黑名单）
     */
    public String getTokenHash(String token) {
        return Integer.toHexString(token.hashCode());
    }

    /**
     * 从 Token 构建 UserPrincipal
     */
    public UserPrincipal getUserPrincipalFromToken(String token) {
        Claims claims = parseToken(token);
        return UserPrincipal.builder()
                .id(claims.get("userId", Long.class))
                .username(claims.getSubject())
                .roles(getRolesFromToken(token))
                .enabled(true)
                .build();
    }

    /**
     * 获取 Access Token 过期时间（秒）
     */
    public long getAccessTokenExpiration() {
        return jwtProperties.getAccessTokenExpiration();
    }

    /**
     * 获取 Refresh Token 过期时间（秒）
     */
    public long getRefreshTokenExpiration() {
        return jwtProperties.getRefreshTokenExpiration();
    }
}

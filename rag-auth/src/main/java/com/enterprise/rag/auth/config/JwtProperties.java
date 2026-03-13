package com.enterprise.rag.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 密钥
     */
    private String secret;

    /**
     * Access Token 过期时间（秒），默认 2 小时
     */
    private long accessTokenExpiration = 7200L;

    /**
     * Refresh Token 过期时间（秒），默认 7 天
     */
    private long refreshTokenExpiration = 604800L;

    /**
     * Token 签发者
     */
    private String issuer = "enterprise-rag-qa-system";
}

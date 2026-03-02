package com.enterprise.rag.common.ratelimit;

/**
 * 限流维度枚举
 */
public enum RateLimitDimension {
    /**
     * 按用户限流
     */
    USER("user"),
    
    /**
     * 按 IP 限流
     */
    IP("ip"),
    
    /**
     * 按接口限流
     */
    API("api"),
    
    /**
     * 全局限流
     */
    GLOBAL("global");
    
    private final String code;
    
    RateLimitDimension(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}

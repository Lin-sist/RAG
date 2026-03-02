package com.enterprise.rag.common.ratelimit;

/**
 * 限流策略枚举
 */
public enum RateLimitStrategy {
    /**
     * 滑动窗口限流
     */
    SLIDING_WINDOW,
    
    /**
     * 令牌桶限流
     */
    TOKEN_BUCKET,
    
    /**
     * 固定窗口限流
     */
    FIXED_WINDOW
}

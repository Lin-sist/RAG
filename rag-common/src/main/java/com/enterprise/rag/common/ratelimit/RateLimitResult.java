package com.enterprise.rag.common.ratelimit;

/**
 * 限流结果
 *
 * @param allowed    是否允许请求
 * @param remaining  剩余配额
 * @param resetTime  重置时间（Unix 时间戳，秒）
 * @param retryAfter 重试等待时间（秒），仅当 allowed=false 时有意义
 */
public record RateLimitResult(
    boolean allowed,
    long remaining,
    long resetTime,
    long retryAfter
) {
    /**
     * 创建允许的结果
     */
    public static RateLimitResult allowed(long remaining, long resetTime) {
        return new RateLimitResult(true, remaining, resetTime, 0);
    }
    
    /**
     * 创建拒绝的结果
     */
    public static RateLimitResult denied(long resetTime, long retryAfter) {
        return new RateLimitResult(false, 0, resetTime, retryAfter);
    }
}

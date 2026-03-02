package com.enterprise.rag.common.ratelimit;

/**
 * 限流配置
 *
 * @param maxRequests   窗口内最大请求数
 * @param windowSeconds 窗口时间（秒）
 * @param strategy      限流策略
 */
public record RateLimitConfig(
    int maxRequests,
    long windowSeconds,
    RateLimitStrategy strategy
) {
    /**
     * 创建默认滑动窗口配置
     */
    public static RateLimitConfig slidingWindow(int maxRequests, long windowSeconds) {
        return new RateLimitConfig(maxRequests, windowSeconds, RateLimitStrategy.SLIDING_WINDOW);
    }
    
    /**
     * 创建默认配置：每分钟60次请求
     */
    public static RateLimitConfig defaultConfig() {
        return slidingWindow(60, 60);
    }
}

package com.enterprise.rag.common.ratelimit;

/**
 * 限流器接口
 */
public interface RateLimiter {
    
    /**
     * 尝试获取请求许可
     *
     * @param key    限流 key（如用户ID、IP地址、接口路径）
     * @param config 限流配置
     * @return 限流结果
     */
    RateLimitResult tryAcquire(String key, RateLimitConfig config);
    
    /**
     * 尝试获取请求许可（使用指定维度）
     *
     * @param dimension 限流维度
     * @param key       限流 key
     * @param config    限流配置
     * @return 限流结果
     */
    RateLimitResult tryAcquire(RateLimitDimension dimension, String key, RateLimitConfig config);
    
    /**
     * 重置限流计数
     *
     * @param key 限流 key
     */
    void reset(String key);
    
    /**
     * 重置限流计数（使用指定维度）
     *
     * @param dimension 限流维度
     * @param key       限流 key
     */
    void reset(RateLimitDimension dimension, String key);
}

package com.enterprise.rag.common.ratelimit;

import java.lang.annotation.*;

/**
 * 限流注解
 * <p>
 * 可用于方法或类级别，标记需要限流的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 窗口内最大请求数
     */
    int maxRequests() default 60;

    /**
     * 窗口时间（秒）
     */
    long windowSeconds() default 60;

    /**
     * 限流维度
     */
    RateLimitDimension dimension() default RateLimitDimension.USER;

    /**
     * 限流 key 前缀（可选）
     * <p>
     * 如果不指定，将根据维度自动生成：
     * - USER: 使用用户ID
     * - IP: 使用客户端IP
     * - API: 使用请求路径
     * - GLOBAL: 使用固定key
     */
    String keyPrefix() default "";

    /**
     * 限流策略
     */
    RateLimitStrategy strategy() default RateLimitStrategy.SLIDING_WINDOW;

    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后重试";
}

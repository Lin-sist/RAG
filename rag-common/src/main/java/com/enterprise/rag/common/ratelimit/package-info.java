/**
 * 限流模块
 * <p>
 * 提供基于 Redis + Lua 的分布式限流能力，支持多种限流策略和维度。
 * <p>
 * 主要组件：
 * <ul>
 *   <li>{@link com.enterprise.rag.common.ratelimit.RateLimiter} - 限流器接口</li>
 *   <li>{@link com.enterprise.rag.common.ratelimit.SlidingWindowRateLimiter} - 滑动窗口限流实现</li>
 *   <li>{@link com.enterprise.rag.common.ratelimit.RateLimitConfig} - 限流配置</li>
 *   <li>{@link com.enterprise.rag.common.ratelimit.RateLimitResult} - 限流结果</li>
 *   <li>{@link com.enterprise.rag.common.ratelimit.RateLimitDimension} - 限流维度</li>
 *   <li>{@link com.enterprise.rag.common.ratelimit.RateLimitStrategy} - 限流策略</li>
 * </ul>
 */
package com.enterprise.rag.common.ratelimit;

package com.enterprise.rag.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 限流拦截器
 * <p>
 * 拦截带有 @RateLimit 注解的请求，执行限流检查
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    /**
     * 限流响应头：剩余配额
     */
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";

    /**
     * 限流响应头：重置时间
     */
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";

    /**
     * 限流响应头：最大请求数
     */
    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";

    /**
     * 限流响应头：重试等待时间
     */
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    /**
     * 请求属性：用户ID（由认证模块设置）
     */
    public static final String REQUEST_ATTR_USER_ID = "rate_limit_user_id";

    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 获取方法或类上的 @RateLimit 注解
        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }

        if (rateLimit == null) {
            return true;
        }

        // 构建限流 key
        String key = buildKey(rateLimit, request);

        // 构建限流配置
        RateLimitConfig config = new RateLimitConfig(
            rateLimit.maxRequests(),
            rateLimit.windowSeconds(),
            rateLimit.strategy()
        );

        // 执行限流检查
        RateLimitResult result = rateLimiter.tryAcquire(rateLimit.dimension(), key, config);

        // 设置响应头
        setRateLimitHeaders(response, result, config);

        if (!result.allowed()) {
            log.warn("Rate limit exceeded for key: {}, dimension: {}", key, rateLimit.dimension());
            throw new RateLimitExceededException(rateLimit.message(), result.resetTime(), result.retryAfter());
        }

        return true;
    }

    /**
     * 构建限流 key
     */
    private String buildKey(RateLimit rateLimit, HttpServletRequest request) {
        String prefix = rateLimit.keyPrefix();
        if (!prefix.isEmpty()) {
            return prefix;
        }

        return switch (rateLimit.dimension()) {
            case USER -> getUserId(request);
            case IP -> getClientIp(request);
            case API -> request.getRequestURI();
            case GLOBAL -> "global";
        };
    }

    /**
     * 获取当前用户ID
     * <p>
     * 从请求属性中获取用户ID，该属性应由认证过滤器设置
     */
    private String getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(REQUEST_ATTR_USER_ID);
        if (userId != null) {
            return userId.toString();
        }
        // 尝试从 Principal 获取
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        return "anonymous";
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 设置限流响应头
     */
    private void setRateLimitHeaders(HttpServletResponse response, RateLimitResult result, RateLimitConfig config) {
        response.setHeader(HEADER_RATE_LIMIT_LIMIT, String.valueOf(config.maxRequests()));
        response.setHeader(HEADER_RATE_LIMIT_REMAINING, String.valueOf(result.remaining()));
        response.setHeader(HEADER_RATE_LIMIT_RESET, String.valueOf(result.resetTime()));

        if (!result.allowed()) {
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.retryAfter()));
        }
    }
}

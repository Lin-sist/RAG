package com.enterprise.rag.common.ratelimit;

import com.enterprise.rag.common.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 限流异常处理器
 * <p>
 * 处理 RateLimitExceededException，返回 429 状态码和限流响应头
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceededException(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header(RateLimitInterceptor.HEADER_RATE_LIMIT_REMAINING, "0")
            .header(RateLimitInterceptor.HEADER_RATE_LIMIT_RESET, String.valueOf(e.getResetTime()))
            .header(RateLimitInterceptor.HEADER_RETRY_AFTER, String.valueOf(e.getRetryAfter()))
            .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }
}

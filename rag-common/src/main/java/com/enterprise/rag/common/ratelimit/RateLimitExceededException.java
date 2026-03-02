package com.enterprise.rag.common.ratelimit;

import com.enterprise.rag.common.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 限流超限异常
 */
@Getter
public class RateLimitExceededException extends BusinessException {

    private final long resetTime;
    private final long retryAfter;

    public RateLimitExceededException(long resetTime, long retryAfter) {
        super("RATE_001", "请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS);
        this.resetTime = resetTime;
        this.retryAfter = retryAfter;
    }

    public RateLimitExceededException(String message, long resetTime, long retryAfter) {
        super("RATE_001", message, HttpStatus.TOO_MANY_REQUESTS);
        this.resetTime = resetTime;
        this.retryAfter = retryAfter;
    }
}

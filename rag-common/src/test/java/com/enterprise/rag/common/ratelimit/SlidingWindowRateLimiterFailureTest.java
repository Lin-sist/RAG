package com.enterprise.rag.common.ratelimit;

import com.enterprise.rag.common.exception.RedisDependencyException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlidingWindowRateLimiterFailureTest {

    @Test
    void redisFailureShouldFailClosedInsteadOfAllowingRequest() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)))
                .thenThrow(new RuntimeException("synthetic redis marker"));
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> limiter.tryAcquire(
                        RateLimitDimension.IP,
                        "127.0.0.1",
                        RateLimitConfig.slidingWindow(20, 60)));

        assertEquals("rate_limit", exception.getSubsystem());
        assertEquals("acquire", exception.getOperation());
        assertEquals(503, exception.getHttpStatus().value());
    }

    @Test
    void nullRedisResultShouldFailClosed() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)))
                .thenReturn(null);
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);

        assertThrows(RedisDependencyException.class,
                () -> limiter.tryAcquire("synthetic", RateLimitConfig.slidingWindow(20, 60)));
    }

    @Test
    void emptyRedisResultShouldFailClosed() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)))
                .thenReturn(List.of());
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(redisTemplate);

        assertThrows(RedisDependencyException.class,
                () -> limiter.tryAcquire("synthetic", RateLimitConfig.slidingWindow(20, 60)));
    }
}

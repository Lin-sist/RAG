package com.enterprise.rag.common.ratelimit;

import com.enterprise.rag.common.exception.GlobalExceptionHandler;
import com.enterprise.rag.common.exception.RedisDependencyException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimitInterceptorFailureMvcTest {

    @Test
    void redisFailureShouldReturn503BeforeControllerExecutes() throws Exception {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.tryAcquire(any(RateLimitDimension.class), anyString(), any(RateLimitConfig.class)))
                .thenThrow(RedisDependencyException.unavailable(
                        "rate_limit", "acquire", new RuntimeException("synthetic redis marker")));
        ProbeController controller = new ProbeController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new RateLimitInterceptor(rateLimiter))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/probe"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("REDIS_DEPENDENCY_UNAVAILABLE"));

        org.junit.jupiter.api.Assertions.assertEquals(0, controller.calls.get());
    }

    @RestController
    static class ProbeController {

        private final AtomicInteger calls = new AtomicInteger();

        @GetMapping("/probe")
        @RateLimit(dimension = RateLimitDimension.GLOBAL)
        String probe() {
            calls.incrementAndGet();
            return "ok";
        }
    }
}

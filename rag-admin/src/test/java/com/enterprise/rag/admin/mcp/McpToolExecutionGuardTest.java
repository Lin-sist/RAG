package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.ratelimit.RateLimitDimension;
import com.enterprise.rag.common.ratelimit.RateLimitResult;
import com.enterprise.rag.common.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpToolExecutionGuardTest {

    private static final RequestIdentity IDENTITY = new RequestIdentity(101L, 901L);

    @Test
    void rateLimitDenialStopsExecutionBeforeTheToolBody() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.tryAcquire(any(RateLimitDimension.class), anyString(), any()))
                .thenReturn(RateLimitResult.denied(100L, 1L));
        AtomicBoolean executed = new AtomicBoolean();
        try (McpToolExecutionGuard guard = guard(rateLimiter, Duration.ofSeconds(1))) {
            McpToolExecutionException error = assertThrows(
                    McpToolExecutionException.class,
                    () -> guard.execute(
                            IDENTITY,
                            McpToolSpecifications.SEARCH,
                            () -> {
                                executed.set(true);
                                return "unexpected";
                            }));

            assertEquals("MCP_RATE_LIMITED", error.category());
            assertFalse(executed.get());
            verify(rateLimiter).tryAcquire(
                    org.mockito.ArgumentMatchers.eq(RateLimitDimension.USER),
                    org.mockito.ArgumentMatchers.eq("901:101:rag.search"),
                    any());
        }
    }

    @Test
    void securityStateFailureFailsClosedBeforeExecution() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.tryAcquire(any(RateLimitDimension.class), anyString(), any()))
                .thenThrow(new IllegalStateException("redis-canary"));
        AtomicBoolean executed = new AtomicBoolean();
        try (McpToolExecutionGuard guard = guard(rateLimiter, Duration.ofSeconds(1))) {
            McpToolExecutionException error = assertThrows(
                    McpToolExecutionException.class,
                    () -> guard.execute(
                            IDENTITY,
                            McpToolSpecifications.ASK,
                            () -> {
                                executed.set(true);
                                return "unexpected";
                            }));

            assertEquals("MCP_DEPENDENCY_UNAVAILABLE", error.category());
            assertFalse(executed.get());
        }
    }

    @Test
    void deadlineCancelsTheWorkerAndReturnsAStableTimeout() throws Exception {
        RateLimiter rateLimiter = allowingRateLimiter();
        CountDownLatch interrupted = new CountDownLatch(1);
        try (McpToolExecutionGuard guard = guard(rateLimiter, Duration.ofMillis(20))) {
            McpToolExecutionException error = assertThrows(
                    McpToolExecutionException.class,
                    () -> guard.execute(
                            IDENTITY,
                            McpToolSpecifications.GET_CITATION,
                            () -> {
                                try {
                                    Thread.sleep(5_000L);
                                } catch (InterruptedException exception) {
                                    interrupted.countDown();
                                    Thread.currentThread().interrupt();
                                }
                                return "late";
                            }));

            assertEquals("MCP_TIMEOUT", error.category());
            assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void searchAndAskShareTheSamePerCallerConcurrencyBudget() throws Exception {
        RateLimiter rateLimiter = allowingRateLimiter();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (McpToolExecutionGuard guard = new McpToolExecutionGuard(
                rateLimiter,
                1,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(2))) {
            CompletableFuture<String> first = CompletableFuture.supplyAsync(() ->
                    guard.execute(IDENTITY, McpToolSpecifications.SEARCH, () -> {
                        entered.countDown();
                        try {
                            release.await(1, TimeUnit.SECONDS);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                        }
                        return "first";
                    }));
            assertTrue(entered.await(1, TimeUnit.SECONDS));

            McpToolExecutionException error = assertThrows(
                    McpToolExecutionException.class,
                    () -> guard.execute(
                            IDENTITY, McpToolSpecifications.ASK, () -> "second"));
            assertEquals("MCP_RATE_LIMITED", error.category());

            release.countDown();
            assertEquals("first", first.get(1, TimeUnit.SECONDS));
        } finally {
            release.countDown();
        }
    }

    private McpToolExecutionGuard guard(RateLimiter rateLimiter, Duration readTimeout) {
        return new McpToolExecutionGuard(
                rateLimiter,
                2,
                readTimeout,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1));
    }

    private RateLimiter allowingRateLimiter() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.tryAcquire(any(RateLimitDimension.class), anyString(), any()))
                .thenReturn(RateLimitResult.allowed(100L, 100L));
        return rateLimiter;
    }
}

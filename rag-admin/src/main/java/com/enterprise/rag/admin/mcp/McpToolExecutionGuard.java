package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.ratelimit.RateLimitConfig;
import com.enterprise.rag.common.ratelimit.RateLimitDimension;
import com.enterprise.rag.common.ratelimit.RateLimitResult;
import com.enterprise.rag.common.ratelimit.RateLimiter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Fail-closed rate, expensive-concurrency and per-tool deadline boundary. */
final class McpToolExecutionGuard implements AutoCloseable {

    private static final int MAX_WORKERS = 32;

    private final RateLimiter rateLimiter;
    private final int expensiveConcurrencyPerUser;
    private final Duration readTimeout;
    private final Duration searchTimeout;
    private final Duration askTimeout;
    private final ThreadPoolExecutor executor;
    private final ConcurrentHashMap<CallerKey, Integer> expensiveExecutions =
            new ConcurrentHashMap<>();

    McpToolExecutionGuard(
            RateLimiter rateLimiter,
            int expensiveConcurrencyPerUser,
            Duration readTimeout,
            Duration searchTimeout,
            Duration askTimeout) {
        this.rateLimiter = rateLimiter;
        this.expensiveConcurrencyPerUser = expensiveConcurrencyPerUser;
        this.readTimeout = readTimeout;
        this.searchTimeout = searchTimeout;
        this.askTimeout = askTimeout;
        AtomicInteger threadSequence = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(
                    runnable, "mcp-readonly-worker-" + threadSequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = new ThreadPoolExecutor(
                0,
                MAX_WORKERS,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    <T> T execute(
            RequestIdentity identity, String toolName, Supplier<T> action) {
        acquireRate(identity, toolName);
        CallerKey callerKey = new CallerKey(identity.tenantId(), identity.userId());
        boolean expensivePermit = false;
        if (McpToolSpecifications.isExternalTool(toolName)) {
            expensivePermit = tryAcquireExpensive(callerKey);
            if (!expensivePermit) {
                throw new McpToolExecutionException("MCP_RATE_LIMITED");
            }
        }

        Future<T> future;
        try {
            future = executor.submit(action::get);
        } catch (RejectedExecutionException exception) {
            release(callerKey, expensivePermit);
            throw new McpToolExecutionException("MCP_RATE_LIMITED");
        }
        try {
            return future.get(timeoutFor(toolName).toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new McpToolExecutionException("MCP_TIMEOUT");
        } catch (InterruptedException exception) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new McpToolExecutionException("MCP_TIMEOUT");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof McpToolExecutionException toolException) {
                throw toolException;
            }
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        } finally {
            release(callerKey, expensivePermit);
        }
    }

    private void acquireRate(RequestIdentity identity, String toolName) {
        RateLimitResult result;
        try {
            result = rateLimiter.tryAcquire(
                    RateLimitDimension.USER,
                    identity.tenantId() + ":" + identity.userId() + ":" + toolName,
                    rateConfig(toolName));
        } catch (RuntimeException exception) {
            throw new McpToolExecutionException("MCP_DEPENDENCY_UNAVAILABLE");
        }
        if (result == null) {
            throw new McpToolExecutionException("MCP_DEPENDENCY_UNAVAILABLE");
        }
        if (!result.allowed()) {
            throw new McpToolExecutionException("MCP_RATE_LIMITED");
        }
    }

    private RateLimitConfig rateConfig(String toolName) {
        int maxRequests = switch (toolName) {
            case McpToolSpecifications.ASK -> 30;
            case McpToolSpecifications.GET_CITATION -> 120;
            case McpToolSpecifications.SEARCH, McpToolSpecifications.COMPARE_SOURCES -> 60;
            default -> 1;
        };
        return RateLimitConfig.slidingWindow(maxRequests, 60L);
    }

    private Duration timeoutFor(String toolName) {
        return switch (toolName) {
            case McpToolSpecifications.SEARCH -> searchTimeout;
            case McpToolSpecifications.ASK -> askTimeout;
            default -> readTimeout;
        };
    }

    private boolean tryAcquireExpensive(CallerKey callerKey) {
        boolean[] acquired = {false};
        expensiveExecutions.compute(callerKey, (ignored, current) -> {
            int active = current == null ? 0 : current;
            if (active >= expensiveConcurrencyPerUser) {
                return active;
            }
            acquired[0] = true;
            return active + 1;
        });
        return acquired[0];
    }

    private void release(CallerKey callerKey, boolean expensivePermit) {
        if (!expensivePermit) {
            return;
        }
        expensiveExecutions.computeIfPresent(
                callerKey, (ignored, active) -> active <= 1 ? null : active - 1);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private record CallerKey(long tenantId, long userId) {
    }
}

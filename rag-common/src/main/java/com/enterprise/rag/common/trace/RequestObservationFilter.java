package com.enterprise.rag.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 统一请求观测过滤器。
 * <p>
 * 记录请求耗时、状态码和 traceId，支持慢请求告警。
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class RequestObservationFilter extends OncePerRequestFilter {

    private final long slowRequestThresholdMs;

    public RequestObservationFilter(
            @Value("${observability.slow-request-threshold-ms:1000}") long slowRequestThresholdMs) {
        this.slowRequestThresholdMs = slowRequestThresholdMs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNs = System.nanoTime();
        Exception chainException = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            chainException = ex;
            throw ex;
        } finally {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            String traceId = TraceContext.getTraceId();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            if (chainException != null) {
                log.error("request_failed traceId={} method={} uri={} status={} latencyMs={} error={}",
                        traceId, method, uri, status, latencyMs, chainException.getMessage());
            } else if (latencyMs >= slowRequestThresholdMs) {
                log.warn("slow_request traceId={} method={} uri={} status={} latencyMs={} thresholdMs={}",
                        traceId, method, uri, status, latencyMs, slowRequestThresholdMs);
            } else {
                log.info("request_done traceId={} method={} uri={} status={} latencyMs={}",
                        traceId, method, uri, status, latencyMs);
            }
        }
    }
}

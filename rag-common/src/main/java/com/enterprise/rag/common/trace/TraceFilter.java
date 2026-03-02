package com.enterprise.rag.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 链路追踪过滤器
 * 为每个请求生成或提取 TraceId，并设置到 MDC 上下文中
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String traceId = extractOrGenerateTraceId(request);
        String spanId = extractOrGenerateSpanId(request);
        
        try {
            // 设置 MDC 上下文
            TraceContext.setTraceId(traceId);
            TraceContext.setSpanId(spanId);
            
            // 设置响应头，便于客户端追踪
            response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
            response.setHeader(TraceContext.SPAN_ID_HEADER, spanId);
            
            if (log.isDebugEnabled()) {
                log.debug("Request started - URI: {}, Method: {}, TraceId: {}", 
                    request.getRequestURI(), request.getMethod(), traceId);
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            if (log.isDebugEnabled()) {
                log.debug("Request completed - URI: {}, Status: {}, TraceId: {}", 
                    request.getRequestURI(), response.getStatus(), traceId);
            }
            // 清除 MDC 上下文，防止线程复用时数据污染
            TraceContext.clear();
        }
    }

    /**
     * 从请求头提取 TraceId，如果不存在则生成新的
     */
    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TraceContext.TRACE_ID_HEADER);
        
        if (traceId != null && TraceIdGenerator.isValid(traceId)) {
            return traceId;
        }
        
        return TraceIdGenerator.generate();
    }

    /**
     * 从请求头提取 SpanId，如果不存在则生成新的
     */
    private String extractOrGenerateSpanId(HttpServletRequest request) {
        String spanId = request.getHeader(TraceContext.SPAN_ID_HEADER);
        
        if (spanId != null && !spanId.isEmpty()) {
            return spanId;
        }
        
        // 生成16位的 SpanId
        return TraceIdGenerator.generate().substring(0, 16);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 排除静态资源和健康检查端点
        return path.startsWith("/static/") 
            || path.startsWith("/favicon.ico")
            || path.equals("/actuator/health");
    }
}

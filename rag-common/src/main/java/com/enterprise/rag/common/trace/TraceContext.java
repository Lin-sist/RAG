package com.enterprise.rag.common.trace;

import org.slf4j.MDC;

/**
 * 追踪上下文
 * 管理当前请求的 TraceId 和 SpanId
 */
public final class TraceContext {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";

    private TraceContext() {
        // Utility class, prevent instantiation
    }

    /**
     * 获取当前 TraceId
     * 
     * @return 当前线程的 TraceId，如果不存在则返回 null
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 设置当前 TraceId
     * 
     * @param traceId 要设置的 TraceId
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 获取当前 SpanId
     * 
     * @return 当前线程的 SpanId，如果不存在则返回 null
     */
    public static String getSpanId() {
        return MDC.get(SPAN_ID_KEY);
    }

    /**
     * 设置当前 SpanId
     * 
     * @param spanId 要设置的 SpanId
     */
    public static void setSpanId(String spanId) {
        if (spanId != null && !spanId.isEmpty()) {
            MDC.put(SPAN_ID_KEY, spanId);
        }
    }

    /**
     * 清除当前线程的追踪上下文
     */
    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
    }

    /**
     * 清除所有 MDC 上下文
     */
    public static void clearAll() {
        MDC.clear();
    }

    /**
     * 生成新的 TraceId 并设置到上下文
     * 
     * @return 生成的 TraceId
     */
    public static String generateAndSetTraceId() {
        String traceId = TraceIdGenerator.generate();
        setTraceId(traceId);
        return traceId;
    }

    /**
     * 生成新的 SpanId 并设置到上下文
     * 
     * @return 生成的 SpanId
     */
    public static String generateAndSetSpanId() {
        String spanId = TraceIdGenerator.generate().substring(0, 16);
        setSpanId(spanId);
        return spanId;
    }
}

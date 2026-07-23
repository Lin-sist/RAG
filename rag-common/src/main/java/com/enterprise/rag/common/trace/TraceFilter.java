package com.enterprise.rag.common.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 链路追踪过滤器
 * 为每个请求生成或提取 TraceId，并设置到 MDC 上下文中
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {

    private static final TextMapGetter<HttpServletRequest> REQUEST_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return java.util.Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier == null ? null : carrier.getHeader(key);
        }
    };

    private final OpenTelemetry openTelemetry;
    private final boolean tracingEnabled;

    public TraceFilter(OpenTelemetry openTelemetry,
                       @Value("${rag.observability.tracing.enabled:false}") boolean tracingEnabled) {
        this.openTelemetry = openTelemetry;
        this.tracingEnabled = tracingEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (tracingEnabled && traceWithOpenTelemetry(request, response, filterChain)) {
            return;
        }

        filterWithLegacyContext(request, response, filterChain);
    }

    private boolean traceWithOpenTelemetry(HttpServletRequest request,
                                           HttpServletResponse response,
                                           FilterChain filterChain) throws ServletException, IOException {
        Context parent;
        Span span;
        try {
            parent = extractParent(request);
            span = openTelemetry.getTracer(GenAiTelemetry.INSTRUMENTATION_SCOPE)
                    .spanBuilder("rag.request")
                    .setSpanKind(SpanKind.SERVER)
                    .setParent(parent)
                    .startSpan();
        } catch (RuntimeException telemetryFailure) {
            log.warn("Tracing initialization failed; continuing with legacy trace context");
            return false;
        }

        SpanContext spanContext = span.getSpanContext();
        String previousTraceId = TraceContext.getTraceId();
        String previousSpanId = TraceContext.getSpanId();
        boolean asyncStarted = false;
        boolean failed = false;
        try (Scope ignored = span.makeCurrent()) {
            setTraceContext(response, spanContext.getTraceId(), spanContext.getSpanId());
            filterChain.doFilter(request, response);
            asyncStarted = request.isAsyncStarted();
            if (asyncStarted) {
                registerAsyncCompletion(request, span);
            } else {
                span.setAttribute(GenAiTelemetry.Attributes.OUTCOME, "success");
            }
        } catch (ServletException | IOException | RuntimeException failure) {
            failed = true;
            markError(span, failure);
            span.end();
            throw failure;
        } finally {
            restoreTraceContext(previousTraceId, previousSpanId);
            if (!asyncStarted && !failed) {
                span.end();
            }
        }
        return true;
    }

    private void registerAsyncCompletion(HttpServletRequest request, Span span) {
        AtomicBoolean ended = new AtomicBoolean();
        AsyncListener listener = new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                if (ended.compareAndSet(false, true)) {
                    span.setAttribute(GenAiTelemetry.Attributes.OUTCOME, "success");
                    span.end();
                }
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                endWithError("AsyncTimeout");
            }

            @Override
            public void onError(AsyncEvent event) {
                Throwable failure = event.getThrowable();
                endWithError(failure == null ? "AsyncError" : failure.getClass().getSimpleName());
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                event.getAsyncContext().addListener(this);
            }

            private void endWithError(String errorType) {
                if (ended.compareAndSet(false, true)) {
                    span.setStatus(StatusCode.ERROR);
                    span.setAttribute(GenAiTelemetry.Attributes.OUTCOME, "error");
                    span.setAttribute(GenAiTelemetry.Attributes.ERROR_TYPE, errorType);
                    span.end();
                }
            }
        };
        request.getAsyncContext().addListener(listener);
    }

    private void markError(Span span, Throwable failure) {
        span.setStatus(StatusCode.ERROR);
        span.setAttribute(GenAiTelemetry.Attributes.OUTCOME, "error");
        span.setAttribute(GenAiTelemetry.Attributes.ERROR_TYPE, failure.getClass().getSimpleName());
    }

    private Context extractParent(HttpServletRequest request) {
        Context extracted = openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), request, REQUEST_GETTER);
        if (Span.fromContext(extracted).getSpanContext().isValid()) {
            return extracted;
        }

        String traceId = request.getHeader(TraceContext.TRACE_ID_HEADER);
        String spanId = request.getHeader(TraceContext.SPAN_ID_HEADER);
        if (TraceId.isValid(traceId) && SpanId.isValid(spanId)) {
            SpanContext remoteParent = SpanContext.createFromRemoteParent(
                    traceId,
                    spanId,
                    TraceFlags.getSampled(),
                    TraceState.getDefault());
            return Context.root().with(Span.wrap(remoteParent));
        }
        return Context.root();
    }

    private void filterWithLegacyContext(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain) throws ServletException, IOException {
        String traceId = extractOrGenerateTraceId(request);
        String spanId = extractOrGenerateSpanId(request);
        String previousTraceId = TraceContext.getTraceId();
        String previousSpanId = TraceContext.getSpanId();

        try {
            setTraceContext(response, traceId, spanId);
            
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
            restoreTraceContext(previousTraceId, previousSpanId);
        }
    }

    private void setTraceContext(HttpServletResponse response, String traceId, String spanId) {
        TraceContext.setTraceId(traceId);
        TraceContext.setSpanId(spanId);
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);
        response.setHeader(TraceContext.SPAN_ID_HEADER, spanId);
    }

    private void restoreTraceContext(String traceId, String spanId) {
        TraceContext.clear();
        TraceContext.setTraceId(traceId);
        TraceContext.setSpanId(spanId);
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

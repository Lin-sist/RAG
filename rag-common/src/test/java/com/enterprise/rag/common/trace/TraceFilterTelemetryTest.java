package com.enterprise.rag.common.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.AsyncContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class TraceFilterTelemetryTest {

    private static final String W3C_TRACE_ID = "11111111111111111111111111111111";
    private static final String W3C_PARENT_SPAN_ID = "2222222222222222";

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    private final OpenTelemetry telemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(provider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();

    @AfterEach
    void cleanup() {
        TraceContext.clearAll();
        provider.close();
        exporter.reset();
    }

    @Test
    void w3cParentWinsAndMdcIsClearedAfterRequest() throws Exception {
        TraceFilter filter = new TraceFilter(telemetry, true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/qa/ask");
        request.addHeader("traceparent", "00-" + W3C_TRACE_ID + "-" + W3C_PARENT_SPAN_ID + "-01");
        request.addHeader(TraceContext.TRACE_ID_HEADER, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        request.addHeader(TraceContext.SPAN_ID_HEADER, "bbbbbbbbbbbbbbbb");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertEquals(W3C_TRACE_ID, TraceContext.getTraceId());
            assertEquals(W3C_TRACE_ID, response.getHeader(TraceContext.TRACE_ID_HEADER));
        };

        filter.doFilter(request, response, chain);

        var span = exporter.getFinishedSpanItems().get(0);
        assertEquals("rag.request", span.getName());
        assertEquals(W3C_TRACE_ID, span.getTraceId());
        assertEquals(W3C_PARENT_SPAN_ID, span.getParentSpanId());
        assertNull(TraceContext.getTraceId());
        assertNull(TraceContext.getSpanId());
    }

    @Test
    void asyncRequestSpanEndsOnlyAfterAsyncCompletion() throws Exception {
        TraceFilter filter = new TraceFilter(telemetry, true);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/qa/stream");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AsyncContext[] asyncContext = new AsyncContext[1];

        filter.doFilter(request, response, (req, res) -> asyncContext[0] = req.startAsync());

        assertTrue(exporter.getFinishedSpanItems().isEmpty());
        assertNull(TraceContext.getTraceId());
        assertNull(TraceContext.getSpanId());

        asyncContext[0].complete();

        assertEquals(1, exporter.getFinishedSpanItems().size());
        assertEquals("rag.request", exporter.getFinishedSpanItems().get(0).getName());
    }

    @Test
    void validCustomPairIsUsedOnlyWhenW3cParentIsAbsent() throws Exception {
        TraceFilter filter = new TraceFilter(telemetry, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/qa/ask");
        request.addHeader(TraceContext.TRACE_ID_HEADER, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        request.addHeader(TraceContext.SPAN_ID_HEADER, "bbbbbbbbbbbbbbbb");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        var span = exporter.getFinishedSpanItems().get(0);
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", span.getTraceId());
        assertEquals("bbbbbbbbbbbbbbbb", span.getParentSpanId());
    }

    @Test
    void incompleteCustomPairIsIgnoredAsRemoteParent() throws Exception {
        TraceFilter filter = new TraceFilter(telemetry, true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/qa/ask");
        request.addHeader(TraceContext.TRACE_ID_HEADER, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertFalse(exporter.getFinishedSpanItems().get(0).getParentSpanContext().isValid());
    }
}

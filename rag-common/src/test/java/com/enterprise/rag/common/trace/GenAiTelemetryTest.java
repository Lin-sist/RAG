package com.enterprise.rag.common.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenAiTelemetryTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    private final OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build();

    @AfterEach
    void closeProvider() {
        tracerProvider.close();
        exporter.reset();
    }

    @Test
    void safeErrorExportsOnlyAllowlistedFacts() {
        GenAiTelemetry telemetry = new GenAiTelemetry(openTelemetry);

        try (GenAiTelemetry.SpanScope span = telemetry.startRoot(
                GenAiTelemetry.SpanNames.ASK,
                Map.of(GenAiTelemetry.Attributes.OPERATION, "ask"),
                null)) {
            span.safeError(
                    new IllegalStateException("raw-sensitive-provider-message"),
                    "timeout",
                    "LLM_TIMEOUT");
        }

        SpanData exported = exporter.getFinishedSpanItems().get(0);
        assertEquals("rag.ask", exported.getName());
        assertEquals("IllegalStateException",
                exported.getAttributes().get(GenAiTelemetry.Attributes.ERROR_TYPE));
        assertEquals("timeout",
                exported.getAttributes().get(GenAiTelemetry.Attributes.ERROR_CATEGORY));
        assertEquals("LLM_TIMEOUT",
                exported.getAttributes().get(GenAiTelemetry.Attributes.ERROR_CODE));
        assertFalse(exported.toString().contains("raw-sensitive-provider-message"));
    }

    @Test
    void rejectsDynamicNamesAndNonAllowlistedOrUnsafeAttributes() {
        GenAiTelemetry telemetry = new GenAiTelemetry(openTelemetry);

        try (GenAiTelemetry.SpanScope ignored = telemetry.startRoot(
                "rag.ask.raw-sensitive-question",
                Map.of(AttributeKey.stringKey("question"), "raw-sensitive-question"),
                null)) {
            // no-op
        }
        try (GenAiTelemetry.SpanScope ignored = telemetry.startRoot(
                GenAiTelemetry.SpanNames.ASK,
                Map.of(
                        AttributeKey.stringKey("question"), "raw-sensitive-question",
                        GenAiTelemetry.Attributes.OPERATION, "raw sensitive question"),
                null)) {
            // no-op
        }

        assertEquals(1, exporter.getFinishedSpanItems().size());
        assertFalse(exporter.getFinishedSpanItems().get(0).toString().contains("raw-sensitive-question"));
        assertFalse(exporter.getFinishedSpanItems().get(0).toString().contains("raw sensitive question"));
    }

    @Test
    void telemetryFailureReturnsNoopScopeInsteadOfBreakingBusinessFlow() {
        OpenTelemetry brokenOpenTelemetry = mock(OpenTelemetry.class);
        Tracer brokenTracer = mock(Tracer.class);
        when(brokenOpenTelemetry.getTracer(
                GenAiTelemetry.INSTRUMENTATION_SCOPE, "1.0.0")).thenReturn(brokenTracer);
        when(brokenTracer.spanBuilder(GenAiTelemetry.SpanNames.ASK))
                .thenThrow(new IllegalStateException("telemetry-internal-sensitive-message"));

        GenAiTelemetry telemetry = new GenAiTelemetry(brokenOpenTelemetry);
        boolean businessCompleted = false;
        try (GenAiTelemetry.SpanScope span = telemetry.startRoot(
                GenAiTelemetry.SpanNames.ASK, Map.of(), null)) {
            span.outcome("SUCCESS");
            businessCompleted = true;
        }

        assertTrue(businessCompleted);
    }
}

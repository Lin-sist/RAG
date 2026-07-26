package com.enterprise.rag.admin.config;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GenAiExportDiagnosticsTest {

    @Test
    void traceExporterExceptionsAndLifecycleFailuresBecomeFixedSafeCounters() {
        GenAiExportDiagnostics diagnostics = new GenAiExportDiagnostics();
        SpanExporter exporter = diagnostics.observe(new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                throw new IllegalStateException("raw endpoint and exception must not escape");
            }

            @Override
            public CompletableResultCode flush() {
                return CompletableResultCode.ofFailure();
            }

            @Override
            public CompletableResultCode shutdown() {
                return CompletableResultCode.ofFailure();
            }
        });

        assertFalse(exporter.export(List.of()).isSuccess());
        assertFalse(exporter.flush().isSuccess());
        assertFalse(exporter.shutdown().isSuccess());
        assertEquals(1L, diagnostics.failureCount("trace", "export"));
        assertEquals(1L, diagnostics.failureCount("trace", "flush"));
        assertEquals(1L, diagnostics.failureCount("trace", "shutdown"));
        assertEquals(0L, diagnostics.failureCount("trace", "raw endpoint and exception"));
    }

    @Test
    void metricExporterFailuresUseTheSameBoundedSignalAndPhaseTaxonomy() {
        GenAiExportDiagnostics diagnostics = new GenAiExportDiagnostics();
        MetricExporter exporter = diagnostics.observe(new MetricExporter() {
            @Override
            public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
                return AggregationTemporality.CUMULATIVE;
            }

            @Override
            public CompletableResultCode export(Collection<MetricData> metrics) {
                return CompletableResultCode.ofFailure();
            }

            @Override
            public CompletableResultCode flush() {
                throw new IllegalStateException("raw failure");
            }

            @Override
            public CompletableResultCode shutdown() {
                return CompletableResultCode.ofFailure();
            }
        });

        assertFalse(exporter.export(List.of()).isSuccess());
        assertFalse(exporter.flush().isSuccess());
        assertFalse(exporter.shutdown().isSuccess());
        assertEquals(1L, diagnostics.failureCount("metric", "export"));
        assertEquals(1L, diagnostics.failureCount("metric", "flush"));
        assertEquals(1L, diagnostics.failureCount("metric", "shutdown"));
        assertEquals(0L, diagnostics.failureCount("unknown", "export"));
    }
}

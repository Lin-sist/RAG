package com.enterprise.rag.admin.config;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

class GenAiTracingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GenAiTracingConfiguration.class);

    @Test
    void tracingIsNoopByDefaultAndRecordsOnlyWhenExplicitlyEnabled() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("genAiTracerProvider"));
            assertFalse(context.containsBean("genAiMeterProvider"));
            OpenTelemetry telemetry = context.getBean(OpenTelemetry.class);
            Span span = telemetry.getTracer("test").spanBuilder("default-off").startSpan();
            assertFalse(span.getSpanContext().isValid());
            span.end();
        });

        contextRunner
                .withPropertyValues("rag.observability.tracing.enabled=true")
                .run(context -> {
                    OpenTelemetry telemetry = context.getBean(OpenTelemetry.class);
                    Span span = telemetry.getTracer("test").spanBuilder("explicit-on").startSpan();
                    assertTrue(span.getSpanContext().isValid());
                    span.end();
                });
    }

    @Test
    void metricsCanBeEnabledWithoutEnablingTracing() {
        contextRunner
                .withPropertyValues("rag.observability.metrics.enabled=true")
                .run(context -> {
                    assertTrue(context.getBeansOfType(SdkMeterProvider.class).size() == 1);
                    assertTrue(context.getBeansOfType(SdkTracerProvider.class).isEmpty());

                    OpenTelemetry telemetry = context.getBean(OpenTelemetry.class);
                    Span span = telemetry.getTracer("test").spanBuilder("metrics-only").startSpan();
                    assertFalse(span.getSpanContext().isValid());
                    span.end();
                });
    }

    @Test
    void localMetricExportIsRegisteredOnlyWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues(
                        "rag.observability.metrics.enabled=true",
                        "rag.observability.export.enabled=true")
                .run(context -> assertTrue(
                        context.getBeansOfType(MetricExporter.class).size() == 1));
    }

    @Test
    void nonLocalOtlpEndpointIsRejectedWithoutBreakingTheApplicationContext() {
        contextRunner
                .withPropertyValues(
                        "rag.observability.metrics.enabled=true",
                        "rag.observability.export.enabled=true",
                        "rag.observability.export.endpoint=https://collector.example.com:4317")
                .run(context -> {
                    assertTrue(context.isRunning());
                    assertTrue(context.getBeansOfType(MetricExporter.class).isEmpty());
                    assertTrue(context.getBeansOfType(SdkMeterProvider.class).size() == 1);
                });
    }

    @Test
    void localTraceExportIsRegisteredOnlyWhenExplicitlyEnabled() {
        contextRunner
                .withPropertyValues(
                        "rag.observability.tracing.enabled=true",
                        "rag.observability.export.enabled=true")
                .run(context -> assertTrue(
                        context.getBeansOfType(SpanExporter.class).size() == 1));
    }

    @Test
    void durationMetricsUseTheFixedC12HistogramBoundaries() {
        InMemoryMetricReader reader = InMemoryMetricReader.create();
        contextRunner
                .withBean(MetricReader.class, () -> reader)
                .withPropertyValues("rag.observability.metrics.enabled=true")
                .run(context -> {
                    GenAiTelemetry telemetry = context.getBean(GenAiTelemetry.class);
                    try (GenAiTelemetry.SpanScope span = telemetry.startRoot(
                            GenAiTelemetry.SpanNames.ASK,
                            Map.of(GenAiTelemetry.Attributes.OPERATION, "ask"),
                            null)) {
                        span.outcome("SUCCESS");
                    }

                    List<Double> boundaries = reader.collectAllMetrics().stream()
                            .filter(metric -> "rag.operation.duration".equals(metric.getName()))
                            .findFirst()
                            .orElseThrow()
                            .getHistogramData().getPoints().iterator().next().getBoundaries();
                    assertEquals(GenAiTracingConfiguration.DURATION_BUCKETS, boundaries);
                });
    }

    @Test
    void unavailableCollectorIsBoundedFailOpenAndRecordsOnlySafeFailureFacts() {
        Logger grpcExporterLogger = Logger.getLogger(
                "io.opentelemetry.exporter.internal.grpc.GrpcExporter");
        List<String> capturedMessages = new CopyOnWriteArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                capturedMessages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        grpcExporterLogger.addHandler(capture);
        try {
            contextRunner
                    .withPropertyValues(
                            "rag.observability.tracing.enabled=true",
                            "rag.observability.metrics.enabled=true",
                            "rag.observability.export.enabled=true",
                            "rag.observability.export.endpoint=http://127.0.0.1:65534",
                            "rag.observability.export.timeout-millis=100",
                            "rag.observability.export.schedule-delay-millis=100",
                            "rag.observability.export.metric-interval-millis=1000")
                    .run(context -> {
                        GenAiTelemetry telemetry = context.getBean(GenAiTelemetry.class);
                        assertDoesNotThrow(() -> {
                            try (GenAiTelemetry.SpanScope span = telemetry.startRoot(
                                    GenAiTelemetry.SpanNames.ASK,
                                    Map.of(GenAiTelemetry.Attributes.OPERATION, "ask"),
                                    null)) {
                                span.outcome("SUCCESS");
                            }
                        });

                        assertTrue(context.getBean(SdkTracerProvider.class)
                                .forceFlush().join(2, TimeUnit.SECONDS).isDone());
                        assertTrue(context.getBean(SdkMeterProvider.class)
                                .forceFlush().join(2, TimeUnit.SECONDS).isDone());

                        GenAiExportDiagnostics diagnostics =
                                context.getBean(GenAiExportDiagnostics.class);
                        awaitExportFailures(diagnostics);
                        assertTrue(diagnostics.failureCount("trace", "export") >= 1L);
                        assertTrue(diagnostics.failureCount("metric", "export") >= 1L);
                        assertEquals(0L, diagnostics.failureCount("trace", "raw-exception"));
                    });
        } finally {
            grpcExporterLogger.removeHandler(capture);
        }
        String joinedMessages = String.join("\n", capturedMessages);
        assertFalse(joinedMessages.contains("65534"));
        assertFalse(joinedMessages.contains("Failed to connect"));
    }

    @Test
    void invalidAndOutOfRangePropertiesAreSafelyDisabledOrClamped() {
        GenAiObservabilityProperties properties = new GenAiObservabilityProperties();
        properties.getExport().setEnabled(true);
        properties.getExport().setEndpoint(null);
        properties.getExport().setTimeoutMillis(-1);
        properties.getExport().setScheduleDelayMillis(Integer.MAX_VALUE);
        properties.getExport().setMetricIntervalMillis(1);
        properties.getExport().setMaxQueueSize(1);
        properties.getExport().setMaxExportBatchSize(Integer.MAX_VALUE);

        assertTrue(properties.localOtlpEndpoint().isEmpty());
        assertEquals(100L, properties.exportTimeout().toMillis());
        assertEquals(30_000L, properties.scheduleDelay().toMillis());
        assertEquals(1_000L, properties.metricInterval().toMillis());
        assertEquals(64, properties.maxQueueSize());
        assertEquals(64, properties.maxExportBatchSize());
    }

    @Test
    void boundedBatchQueueDoesNotBlockBusinessLifecycleUnderPressure() {
        CompletableResultCode pendingExport = new CompletableResultCode();
        GenAiExportDiagnostics diagnostics = new GenAiExportDiagnostics();
        SpanExporter blockingExporter = diagnostics.observe(new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                return pendingExport;
            }

            @Override
            public CompletableResultCode flush() {
                return CompletableResultCode.ofSuccess();
            }

            @Override
            public CompletableResultCode shutdown() {
                pendingExport.fail();
                return CompletableResultCode.ofSuccess();
            }
        });

        contextRunner
                .withBean(SpanExporter.class, () -> blockingExporter)
                .withPropertyValues(
                        "rag.observability.tracing.enabled=true",
                        "rag.observability.export.max-queue-size=64",
                        "rag.observability.export.max-export-batch-size=1",
                        "rag.observability.export.schedule-delay-millis=100",
                        "rag.observability.export.timeout-millis=100")
                .run(context -> {
                    GenAiTelemetry telemetry = context.getBean(GenAiTelemetry.class);
                    long started = System.nanoTime();
                    assertDoesNotThrow(() -> {
                        for (int i = 0; i < 5_000; i++) {
                            try (GenAiTelemetry.SpanScope span = telemetry.startRoot(
                                    GenAiTelemetry.SpanNames.ASK,
                                    Map.of(GenAiTelemetry.Attributes.OPERATION, "ask"),
                                    null)) {
                                span.outcome("SUCCESS");
                            }
                        }
                    });
                    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(
                            System.nanoTime() - started);
                    assertTrue(elapsedMillis < 5_000L);
                    pendingExport.fail();
                    awaitExportFailures(diagnostics);
                    assertTrue(diagnostics.failureCount("trace", "export") >= 1L);
                });
    }

    private static void awaitExportFailures(GenAiExportDiagnostics diagnostics) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline
                && (diagnostics.failureCount("trace", "export") < 1L
                || diagnostics.failureCount("metric", "export") < 1L)) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
    }
}

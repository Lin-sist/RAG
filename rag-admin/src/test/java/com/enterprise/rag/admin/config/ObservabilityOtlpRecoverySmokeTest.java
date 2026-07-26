package com.enterprise.rag.admin.config;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityOtlpRecoverySmokeTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "RAG_OBSERVABILITY_RECOVERY_SMOKE", matches = "true")
    void recordsFailureWhileCollectorIsStoppedAndRecoversWithoutRestartingTheSdk() {
        new ApplicationContextRunner()
                .withUserConfiguration(GenAiTracingConfiguration.class)
                .withPropertyValues(
                        "rag.observability.tracing.enabled=true",
                        "rag.observability.metrics.enabled=true",
                        "rag.observability.export.enabled=true",
                        "rag.observability.export.endpoint=http://127.0.0.1:4317",
                        "rag.observability.export.timeout-millis=200",
                        "rag.observability.export.schedule-delay-millis=100",
                        "rag.observability.export.metric-interval-millis=1000")
                .run(context -> {
                    GenAiTelemetry telemetry = context.getBean(GenAiTelemetry.class);
                    SdkTracerProvider traces = context.getBean(SdkTracerProvider.class);
                    SdkMeterProvider metrics = context.getBean(SdkMeterProvider.class);
                    GenAiExportDiagnostics diagnostics =
                            context.getBean(GenAiExportDiagnostics.class);

                    emitSyntheticTimeout(telemetry);
                    traces.forceFlush().join(2, TimeUnit.SECONDS);
                    metrics.forceFlush().join(2, TimeUnit.SECONDS);
                    await(() -> diagnostics.failureCount("trace", "export") >= 1L
                            && diagnostics.failureCount("metric", "export") >= 1L, 5);
                    assertTrue(diagnostics.failureCount("trace", "export") >= 1L);
                    assertTrue(diagnostics.failureCount("metric", "export") >= 1L);

                    await(() -> {
                        emitSyntheticTimeout(telemetry);
                        traces.forceFlush().join(2, TimeUnit.SECONDS);
                        metrics.forceFlush().join(2, TimeUnit.SECONDS);
                        return diagnostics.successCount("trace", "export") >= 1L
                                && diagnostics.successCount("metric", "export") >= 1L;
                    }, 20);
                    assertTrue(diagnostics.successCount("trace", "export") >= 1L);
                    assertTrue(diagnostics.successCount("metric", "export") >= 1L);
                });
    }

    private static void emitSyntheticTimeout(GenAiTelemetry telemetry) {
        try (GenAiTelemetry.SpanScope span = telemetry.startRoot(
                GenAiTelemetry.SpanNames.ASK,
                Map.of(GenAiTelemetry.Attributes.OPERATION, "ask"),
                null)) {
            telemetry.currentTokenUsage(null, null);
            span.safeError(new IllegalStateException("synthetic"),
                    "timeout", "SYNTHETIC_TIMEOUT").outcome("TIMEOUT");
        }
    }

    private static void await(java.util.function.BooleanSupplier condition, int timeoutSeconds) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        do {
            if (condition.getAsBoolean()) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(250));
        } while (System.nanoTime() < deadline);
    }
}

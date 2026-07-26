package com.enterprise.rag.admin.config;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityOtlpSmokeTest {

    private static final String SENSITIVE_SENTINEL = "C12_SECRET_SENTINEL_DO_NOT_EXPORT";

    @Test
    @EnabledIfEnvironmentVariable(named = "RAG_OBSERVABILITY_SMOKE", matches = "true")
    void exportsOnlySyntheticTraceAndMetricsToTheLocalCollector() {
        new ApplicationContextRunner()
                .withUserConfiguration(GenAiTracingConfiguration.class)
                .withPropertyValues(
                        "rag.observability.tracing.enabled=true",
                        "rag.observability.metrics.enabled=true",
                        "rag.observability.export.enabled=true",
                        "rag.observability.export.endpoint=http://127.0.0.1:4317",
                        "rag.observability.export.timeout-millis=3000",
                        "rag.observability.export.metric-interval-millis=1000")
                .run(context -> {
                    GenAiTelemetry telemetry = context.getBean(GenAiTelemetry.class);
                    for (int i = 0; i < 100; i++) {
                        try (GenAiTelemetry.SpanScope success = telemetry.startRoot(
                                GenAiTelemetry.SpanNames.ASK,
                                Map.of(GenAiTelemetry.Attributes.OPERATION, "ask"),
                                null)) {
                            success.outcome("SUCCESS");
                        }
                    }

                    Map<AttributeKey<?>, Object> rootAttributes = new LinkedHashMap<>();
                    rootAttributes.put(GenAiTelemetry.Attributes.OPERATION, "ask");
                    rootAttributes.put(AttributeKey.stringKey("rag.question"), SENSITIVE_SENTINEL);

                    try (GenAiTelemetry.SpanScope ask = telemetry.startRoot(
                            GenAiTelemetry.SpanNames.ASK, rootAttributes, null)) {
                        try (GenAiTelemetry.SpanScope generation = telemetry.startSpan(
                                GenAiTelemetry.SpanNames.LLM_REQUEST,
                                Map.of(GenAiTelemetry.Attributes.STAGE,
                                        GenAiTelemetry.SpanNames.LLM_REQUEST))) {
                            generation.diagnostics(Map.of(
                                    "requestedProvider", "openai",
                                    "effectiveProvider", "heuristic",
                                    "modelCallCount", 1L,
                                    "fallbackCount", 1L,
                                    "fallbackReason", "timeout"));
                            telemetry.currentTokenUsage(11L, 7L);
                            generation.outcome("FALLBACK_SUCCESS");
                        }
                        ask.safeError(
                                new IllegalStateException(SENSITIVE_SENTINEL),
                                "timeout",
                                "SYNTHETIC_TIMEOUT")
                                .outcome("TIMEOUT");
                    }

                    assertTrue(context.getBean(SdkTracerProvider.class)
                            .forceFlush().join(5, TimeUnit.SECONDS).isSuccess());
                    assertTrue(context.getBean(SdkMeterProvider.class)
                            .forceFlush().join(5, TimeUnit.SECONDS).isSuccess());
                });
    }
}

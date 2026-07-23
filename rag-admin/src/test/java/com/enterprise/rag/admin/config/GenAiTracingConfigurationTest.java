package com.enterprise.rag.admin.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenAiTracingConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GenAiTracingConfiguration.class);

    @Test
    void tracingIsNoopByDefaultAndRecordsOnlyWhenExplicitlyEnabled() {
        contextRunner.run(context -> {
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
}

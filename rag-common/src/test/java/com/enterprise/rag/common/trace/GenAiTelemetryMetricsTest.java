package com.enterprise.rag.common.trace;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GenAiTelemetryMetricsTest {

    private static final AttributeKey<String> OPERATION = AttributeKey.stringKey("rag.operation");
    private static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("rag.outcome");

    @Test
    void recordsAskLifecycleMetricsWithoutDependingOnTraceSampling() {
        InMemoryMetricReader reader = InMemoryMetricReader.create();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(reader)
                .build();
        try {
            GenAiTelemetry telemetry = new GenAiTelemetry(OpenTelemetrySdk.builder()
                    .setMeterProvider(meterProvider)
                    .build());

            try (GenAiTelemetry.SpanScope span = telemetry.startRoot(
                    GenAiTelemetry.SpanNames.ASK,
                    Map.of(GenAiTelemetry.Attributes.OPERATION, "ask"),
                    null)) {
                span.outcome("SUCCESS");
            }

            Collection<MetricData> metrics = reader.collectAllMetrics();
            MetricData count = metric(metrics, "rag.operation.count");
            assertEquals(1L, count.getLongSumData().getPoints().iterator().next().getValue());
            assertEquals("ask", count.getLongSumData().getPoints().iterator().next()
                    .getAttributes().get(OPERATION));
            assertEquals("success", count.getLongSumData().getPoints().iterator().next()
                    .getAttributes().get(OUTCOME));

            MetricData duration = metric(metrics, "rag.operation.duration");
            assertEquals("s", duration.getUnit());
            assertEquals(1L, duration.getHistogramData().getPoints().iterator().next().getCount());

            MetricData inflight = metric(metrics, "rag.operation.inflight");
            assertEquals(0L, inflight.getLongSumData().getPoints().iterator().next().getValue());
        } finally {
            meterProvider.close();
        }
    }

    @Test
    void recordsProviderFallbackAndActualTokenMetricsWithoutHighCardinalityLabels() {
        InMemoryMetricReader reader = InMemoryMetricReader.create();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(reader)
                .build();
        try {
            GenAiTelemetry telemetry = new GenAiTelemetry(OpenTelemetrySdk.builder()
                    .setMeterProvider(meterProvider)
                    .build());

            try (GenAiTelemetry.SpanScope llm = telemetry.startRoot(
                    GenAiTelemetry.SpanNames.LLM_REQUEST, Map.of(), null)) {
                llm.providerCall("openai", "openai", "sensitive-model-7b", "http_json", 2, 1);
                telemetry.currentTokenUsage(11L, 7L);
                llm.outcome("SUCCESS");
            }
            try (GenAiTelemetry.SpanScope rerank = telemetry.startRoot(
                    GenAiTelemetry.SpanNames.RERANK, Map.of(), null)) {
                rerank.diagnostics(Map.of(
                        "rerankRequestedProvider", "unbounded-provider-sentinel",
                        "rerankEffectiveProvider", "heuristic",
                        "rerankFallbackCount", 1L,
                        "rerankFallbackReason", "timeout",
                        "modelCallCount", 1L));
                rerank.outcome("FALLBACK_SUCCESS");
            }

            Collection<MetricData> metrics = reader.collectAllMetrics();
            assertEquals(3L, metric(metrics, "rag.provider.call.count")
                    .getLongSumData().getPoints().stream()
                    .mapToLong(point -> point.getValue()).sum());
            assertEquals(1L, metric(metrics, "rag.fallback.count")
                    .getLongSumData().getPoints().stream()
                    .mapToLong(point -> point.getValue()).sum());
            assertEquals(18L, metric(metrics, "rag.token.usage")
                    .getLongSumData().getPoints().stream()
                    .mapToLong(point -> point.getValue()).sum());
            assertEquals(1L, metric(metrics, "rag.token.usage_coverage")
                    .getLongSumData().getPoints().stream()
                    .mapToLong(point -> point.getValue()).sum());

            String exported = metrics.toString();
            assertFalse(exported.contains("sensitive-model-7b"));
            assertFalse(exported.contains("unbounded-provider-sentinel"));
        } finally {
            meterProvider.close();
        }
    }

    @Test
    void randomProviderAndFallbackInputsCollapseToBoundedSeries() {
        InMemoryMetricReader reader = InMemoryMetricReader.create();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(reader)
                .build();
        try {
            GenAiTelemetry telemetry = new GenAiTelemetry(OpenTelemetrySdk.builder()
                    .setMeterProvider(meterProvider)
                    .build());

            Set<String> sentinels = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                String sentinel = "random-provider-" + i;
                sentinels.add(sentinel);
                try (GenAiTelemetry.SpanScope rerank = telemetry.startRoot(
                        GenAiTelemetry.SpanNames.RERANK, Map.of(), null)) {
                    rerank.diagnostics(Map.of(
                            "requestedProvider", sentinel,
                            "effectiveProvider", "heuristic",
                            "modelCallCount", 1L,
                            "fallbackCount", 1L,
                            "fallbackReason", "random-reason-" + i));
                    rerank.outcome("FALLBACK_SUCCESS");
                }
            }

            Collection<MetricData> metrics = reader.collectAllMetrics();
            MetricData providerCalls = metric(metrics, "rag.provider.call.count");
            MetricData fallbacks = metric(metrics, "rag.fallback.count");
            assertEquals(1, providerCalls.getLongSumData().getPoints().size());
            assertEquals(1, fallbacks.getLongSumData().getPoints().size());
            assertEquals(100L, providerCalls.getLongSumData().getPoints()
                    .iterator().next().getValue());
            assertEquals(100L, fallbacks.getLongSumData().getPoints()
                    .iterator().next().getValue());
            String exported = metrics.toString();
            sentinels.forEach(sentinel -> assertFalse(exported.contains(sentinel)));
        } finally {
            meterProvider.close();
        }
    }

    @Test
    void missingTokenUsageRecordsCoverageWithoutInventingActualTokens() {
        InMemoryMetricReader reader = InMemoryMetricReader.create();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(reader)
                .build();
        try {
            GenAiTelemetry telemetry = new GenAiTelemetry(OpenTelemetrySdk.builder()
                    .setMeterProvider(meterProvider)
                    .build());
            telemetry.currentTokenUsage(null, null);

            Collection<MetricData> metrics = reader.collectAllMetrics();
            assertFalse(metrics.stream().anyMatch(metric -> "rag.token.usage".equals(metric.getName())));
            MetricData coverage = metric(metrics, "rag.token.usage_coverage");
            assertEquals(1L, coverage.getLongSumData().getPoints()
                    .iterator().next().getValue());
            assertEquals("missing", coverage.getLongSumData().getPoints()
                    .iterator().next().getAttributes()
                    .get(AttributeKey.stringKey("rag.token.coverage")));
        } finally {
            meterProvider.close();
        }
    }

    private MetricData metric(Collection<MetricData> metrics, String name) {
        return metrics.stream()
                .filter(metric -> name.equals(metric.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing metric " + name));
    }
}

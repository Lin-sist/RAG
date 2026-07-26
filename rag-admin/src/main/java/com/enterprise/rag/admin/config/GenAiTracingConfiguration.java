package com.enterprise.rag.admin.config;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * C11/C12 GenAI tracing、metrics 与本机 OTLP export wiring。
 */
@Configuration
@EnableConfigurationProperties(GenAiObservabilityProperties.class)
public class GenAiTracingConfiguration {

    private static final AttributeKey<String> SERVICE_NAME = AttributeKey.stringKey("service.name");
    static final List<Double> DURATION_BUCKETS = List.of(
            0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0,
            2.5, 5.0, 10.0, 30.0, 60.0, 120.0, 300.0);

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "rag.observability.tracing.enabled", havingValue = "true")
    public SdkTracerProvider genAiTracerProvider(ObjectProvider<SpanExporter> spanExporter,
            GenAiObservabilityProperties properties) {
        SdkTracerProviderBuilder builder = SdkTracerProvider.builder()
                .setResource(genAiResource())
                .setSampler(Sampler.parentBased(Sampler.alwaysOn()));
        SpanExporter exporter = spanExporter.getIfAvailable();
        if (exporter != null) {
            builder.addSpanProcessor(BatchSpanProcessor.builder(exporter)
                    .setMaxQueueSize(properties.maxQueueSize())
                    .setMaxExportBatchSize(properties.maxExportBatchSize())
                    .setScheduleDelay(properties.scheduleDelay())
                    .setExporterTimeout(properties.exportTimeout())
                    .build());
        }
        return builder.build();
    }

    @Bean(destroyMethod = "")
    @ConditionalOnExpression("'${rag.observability.tracing.enabled:false}' == 'true'"
            + " && '${rag.observability.export.enabled:false}' == 'true'")
    public SpanExporter genAiSpanExporter(GenAiObservabilityProperties properties,
            GenAiExportDiagnostics diagnostics) {
        GenAiExportDiagnostics.installSafeGrpcLogFilter();
        return properties.localOtlpEndpoint()
                .<SpanExporter>map(endpoint -> OtlpGrpcSpanExporter.builder()
                        .setEndpoint(endpoint)
                        .setTimeout(properties.exportTimeout())
                        .build())
                .map(diagnostics::observe)
                .orElse(null);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "rag.observability.metrics.enabled", havingValue = "true")
    public SdkMeterProvider genAiMeterProvider(ObjectProvider<MetricReader> metricReaders) {
        SdkMeterProviderBuilder builder = SdkMeterProvider.builder()
                .setResource(genAiResource());
        registerDurationView(builder, "rag.operation.duration");
        registerDurationView(builder, "rag.stage.duration");
        metricReaders.orderedStream().forEach(builder::registerMetricReader);
        return builder.build();
    }

    @Bean(destroyMethod = "")
    @ConditionalOnExpression("'${rag.observability.metrics.enabled:false}' == 'true'"
            + " && '${rag.observability.export.enabled:false}' == 'true'")
    public MetricReader genAiMetricReader(ObjectProvider<MetricExporter> metricExporter,
            GenAiObservabilityProperties properties) {
        MetricExporter exporter = metricExporter.getIfAvailable();
        return exporter == null ? null : PeriodicMetricReader.builder(exporter)
                .setInterval(properties.metricInterval())
                .build();
    }

    @Bean(destroyMethod = "")
    @ConditionalOnExpression("'${rag.observability.metrics.enabled:false}' == 'true'"
            + " && '${rag.observability.export.enabled:false}' == 'true'")
    public MetricExporter genAiMetricExporter(GenAiObservabilityProperties properties,
            GenAiExportDiagnostics diagnostics) {
        GenAiExportDiagnostics.installSafeGrpcLogFilter();
        return properties.localOtlpEndpoint()
                .<MetricExporter>map(endpoint -> OtlpGrpcMetricExporter.builder()
                        .setEndpoint(endpoint)
                        .setTimeout(properties.exportTimeout())
                        .build())
                .map(diagnostics::observe)
                .orElse(null);
    }

    @Bean
    public GenAiExportDiagnostics genAiExportDiagnostics() {
        return new GenAiExportDiagnostics();
    }

    @Bean(destroyMethod = "")
    public OpenTelemetry openTelemetry(ObjectProvider<SdkTracerProvider> tracerProvider,
            ObjectProvider<SdkMeterProvider> meterProvider) {
        SdkTracerProvider provider = tracerProvider.getIfAvailable();
        SdkMeterProvider meters = meterProvider.getIfAvailable();
        if (provider == null && meters == null) {
            return OpenTelemetry.noop();
        }
        return new SignalOpenTelemetry(
                provider == null ? TracerProvider.noop() : provider,
                meters == null ? MeterProvider.noop() : meters,
                ContextPropagators.create(W3CTraceContextPropagator.getInstance()));
    }

    @Bean
    public GenAiTelemetry genAiTelemetry(OpenTelemetry openTelemetry) {
        return new GenAiTelemetry(openTelemetry);
    }

    private Resource genAiResource() {
        return Resource.getDefault().merge(Resource.create(
                Attributes.of(SERVICE_NAME, "enterprise-rag-qa")));
    }

    private void registerDurationView(SdkMeterProviderBuilder builder, String instrumentName) {
        builder.registerView(
                InstrumentSelector.builder().setName(instrumentName).build(),
                View.builder()
                        .setAggregation(Aggregation.explicitBucketHistogram(DURATION_BUCKETS))
                        .build());
    }

    private record SignalOpenTelemetry(
            TracerProvider tracerProvider,
            MeterProvider meterProvider,
            ContextPropagators propagators) implements OpenTelemetry {

        @Override
        public TracerProvider getTracerProvider() {
            return tracerProvider;
        }

        @Override
        public MeterProvider getMeterProvider() {
            return meterProvider;
        }

        @Override
        public ContextPropagators getPropagators() {
            return propagators;
        }
    }
}

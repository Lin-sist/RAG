package com.enterprise.rag.admin.config;

import com.enterprise.rag.common.trace.GenAiTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * C11 只建立进程内 OTel SDK。网络 exporter、metrics 与部署配置属于 C12。
 */
@Configuration
public class GenAiTracingConfiguration {

    private static final AttributeKey<String> SERVICE_NAME = AttributeKey.stringKey("service.name");

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "rag.observability.tracing.enabled", havingValue = "true")
    public SdkTracerProvider genAiTracerProvider() {
        Resource resource = Resource.getDefault().merge(Resource.create(
                Attributes.of(SERVICE_NAME, "enterprise-rag-qa")));
        return SdkTracerProvider.builder()
                .setResource(resource)
                .setSampler(Sampler.parentBased(Sampler.alwaysOn()))
                .build();
    }

    @Bean(destroyMethod = "")
    public OpenTelemetry openTelemetry(ObjectProvider<SdkTracerProvider> tracerProvider) {
        SdkTracerProvider provider = tracerProvider.getIfAvailable();
        if (provider == null) {
            return OpenTelemetry.noop();
        }
        return OpenTelemetrySdk.builder()
                .setTracerProvider(provider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @Bean
    public GenAiTelemetry genAiTelemetry(OpenTelemetry openTelemetry) {
        return new GenAiTelemetry(openTelemetry);
    }
}

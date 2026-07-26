package com.enterprise.rag.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * C12 本机 reference observability 配置。
 *
 * <p>这里只允许 loopback OTLP endpoint。远程/SaaS endpoint 属于后续独立变更，
 * 非法或越界配置会被安全禁用或收敛，不扩大业务故障域。</p>
 */
@ConfigurationProperties(prefix = "rag.observability")
public class GenAiObservabilityProperties {

    private final Signal tracing = new Signal();
    private final Signal metrics = new Signal();
    private final Export export = new Export();

    public Signal getTracing() {
        return tracing;
    }

    public Signal getMetrics() {
        return metrics;
    }

    public Export getExport() {
        return export;
    }

    public Optional<String> localOtlpEndpoint() {
        if (!export.enabled) {
            return Optional.empty();
        }
        if (export.endpoint == null || export.endpoint.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(export.endpoint);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean loopback = Set.of("localhost", "127.0.0.1", "::1").contains(host);
            boolean safePath = uri.getPath() == null || uri.getPath().isEmpty() || "/".equals(uri.getPath());
            boolean safePort = uri.getPort() >= 1 && uri.getPort() <= 65535;
            if (!"http".equals(scheme) || !loopback || !safePath || !safePort
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                return Optional.empty();
            }
            return Optional.of(uri.toString());
        } catch (IllegalArgumentException invalidEndpoint) {
            return Optional.empty();
        }
    }

    public Duration exportTimeout() {
        return Duration.ofMillis(clamp(export.timeoutMillis, 100, 10_000));
    }

    public Duration scheduleDelay() {
        return Duration.ofMillis(clamp(export.scheduleDelayMillis, 100, 30_000));
    }

    public Duration metricInterval() {
        return Duration.ofMillis(clamp(export.metricIntervalMillis, 1_000, 60_000));
    }

    public int maxQueueSize() {
        return clamp(export.maxQueueSize, 64, 8_192);
    }

    public int maxExportBatchSize() {
        return Math.min(clamp(export.maxExportBatchSize, 1, 2_048), maxQueueSize());
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public static final class Signal {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static final class Export {
        private boolean enabled;
        private String endpoint = "http://127.0.0.1:4317";
        private int timeoutMillis = 3_000;
        private int scheduleDelayMillis = 5_000;
        private int metricIntervalMillis = 30_000;
        private int maxQueueSize = 2_048;
        private int maxExportBatchSize = 512;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public int getScheduleDelayMillis() {
            return scheduleDelayMillis;
        }

        public void setScheduleDelayMillis(int scheduleDelayMillis) {
            this.scheduleDelayMillis = scheduleDelayMillis;
        }

        public int getMetricIntervalMillis() {
            return metricIntervalMillis;
        }

        public void setMetricIntervalMillis(int metricIntervalMillis) {
            this.metricIntervalMillis = metricIntervalMillis;
        }

        public int getMaxQueueSize() {
            return maxQueueSize;
        }

        public void setMaxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
        }

        public int getMaxExportBatchSize() {
            return maxExportBatchSize;
        }

        public void setMaxExportBatchSize(int maxExportBatchSize) {
            this.maxExportBatchSize = maxExportBatchSize;
        }
    }
}

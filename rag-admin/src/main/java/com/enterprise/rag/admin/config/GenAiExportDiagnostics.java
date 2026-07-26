package com.enterprise.rag.admin.config;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * C12 exporter 的进程内安全诊断。
 *
 * <p>只保留固定 signal/phase 组合的计数，不接收 endpoint、header、异常或业务内容。</p>
 */
public final class GenAiExportDiagnostics {

    private static final String GRPC_EXPORTER_LOGGER =
            "io.opentelemetry.exporter.internal.grpc.GrpcExporter";
    private static final AtomicBoolean SAFE_LOG_FILTER_INSTALLED = new AtomicBoolean();
    private final LongAdder[][] successes = new LongAdder[Signal.values().length][Phase.values().length];
    private final LongAdder[][] failures = new LongAdder[Signal.values().length][Phase.values().length];

    public GenAiExportDiagnostics() {
        for (Signal signal : Signal.values()) {
            for (Phase phase : Phase.values()) {
                successes[signal.ordinal()][phase.ordinal()] = new LongAdder();
                failures[signal.ordinal()][phase.ordinal()] = new LongAdder();
            }
        }
    }

    static void installSafeGrpcLogFilter() {
        if (!SAFE_LOG_FILTER_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        Logger logger = Logger.getLogger(GRPC_EXPORTER_LOGGER);
        Filter existing = logger.getFilter();
        logger.setFilter(record -> {
            if (existing != null && !existing.isLoggable(record)) {
                return false;
            }
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                record.setMessage("OTLP export failed; see bounded GenAiExportDiagnostics counters");
                record.setParameters(null);
                record.setThrown(null);
            }
            return true;
        });
    }

    SpanExporter observe(SpanExporter delegate) {
        return new SpanExporter() {
            @Override
            public CompletableResultCode export(Collection<SpanData> spans) {
                return observeResult(Signal.TRACE, Phase.EXPORT, () -> delegate.export(spans));
            }

            @Override
            public CompletableResultCode flush() {
                return observeResult(Signal.TRACE, Phase.FLUSH, delegate::flush);
            }

            @Override
            public CompletableResultCode shutdown() {
                return observeResult(Signal.TRACE, Phase.SHUTDOWN, delegate::shutdown);
            }
        };
    }

    MetricExporter observe(MetricExporter delegate) {
        return new MetricExporter() {
            @Override
            public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
                return delegate.getAggregationTemporality(instrumentType);
            }

            @Override
            public Aggregation getDefaultAggregation(InstrumentType instrumentType) {
                return delegate.getDefaultAggregation(instrumentType);
            }

            @Override
            public MemoryMode getMemoryMode() {
                return delegate.getMemoryMode();
            }

            @Override
            public CompletableResultCode export(Collection<MetricData> metrics) {
                return observeResult(Signal.METRIC, Phase.EXPORT, () -> delegate.export(metrics));
            }

            @Override
            public CompletableResultCode flush() {
                return observeResult(Signal.METRIC, Phase.FLUSH, delegate::flush);
            }

            @Override
            public CompletableResultCode shutdown() {
                return observeResult(Signal.METRIC, Phase.SHUTDOWN, delegate::shutdown);
            }
        };
    }

    public long failureCount(String signal, String phase) {
        Signal safeSignal = Signal.parse(signal);
        Phase safePhase = Phase.parse(phase);
        if (safeSignal == null || safePhase == null) {
            return 0L;
        }
        return failures[safeSignal.ordinal()][safePhase.ordinal()].sum();
    }

    public long successCount(String signal, String phase) {
        Signal safeSignal = Signal.parse(signal);
        Phase safePhase = Phase.parse(phase);
        if (safeSignal == null || safePhase == null) {
            return 0L;
        }
        return successes[safeSignal.ordinal()][safePhase.ordinal()].sum();
    }

    private CompletableResultCode observeResult(
            Signal signal,
            Phase phase,
            Supplier<CompletableResultCode> action) {
        final CompletableResultCode result;
        try {
            result = action.get();
        } catch (RuntimeException exporterFailure) {
            recordFailure(signal, phase);
            return CompletableResultCode.ofFailure();
        }
        if (result == null) {
            recordFailure(signal, phase);
            return CompletableResultCode.ofFailure();
        }
        result.whenComplete(() -> {
            if (result.isSuccess()) {
                successes[signal.ordinal()][phase.ordinal()].increment();
            } else {
                recordFailure(signal, phase);
            }
        });
        return result;
    }

    private void recordFailure(Signal signal, Phase phase) {
        failures[signal.ordinal()][phase.ordinal()].increment();
    }

    private enum Signal {
        TRACE,
        METRIC;

        private static Signal parse(String value) {
            try {
                return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalidValue) {
                return null;
            }
        }
    }

    private enum Phase {
        EXPORT,
        FLUSH,
        SHUTDOWN;

        private static Phase parse(String value) {
            try {
                return value == null ? null : valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException invalidValue) {
                return null;
            }
        }
    }
}

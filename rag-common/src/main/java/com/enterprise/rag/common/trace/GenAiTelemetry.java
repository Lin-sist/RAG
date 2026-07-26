package com.enterprise.rag.common.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * C11 GenAI tracing 的小型安全门面。
 *
 * <p>业务代码只通过固定 span/attribute contract 写入遥测；本类不会调用
 * {@link Span#recordException(Throwable)}，避免异常 message/stack 进入普通 telemetry。</p>
 */
public final class GenAiTelemetry {

    public static final String INSTRUMENTATION_SCOPE = "com.enterprise.rag.genai";
    private static final String INSTRUMENTATION_VERSION = "1.0.0";
    private static final Pattern SAFE_STRING = Pattern.compile("[A-Za-z0-9_./:@-]{1,128}");

    private final Tracer tracer;
    private final MetricInstruments metrics;

    public GenAiTelemetry(OpenTelemetry openTelemetry) {
        OpenTelemetry effective = openTelemetry == null ? OpenTelemetry.noop() : openTelemetry;
        Tracer resolved;
        try {
            resolved = effective.getTracer(INSTRUMENTATION_SCOPE, INSTRUMENTATION_VERSION);
        } catch (RuntimeException telemetryFailure) {
            resolved = OpenTelemetry.noop().getTracer(INSTRUMENTATION_SCOPE, INSTRUMENTATION_VERSION);
        }
        this.tracer = resolved;
        this.metrics = MetricInstruments.create(effective);
    }

    public static GenAiTelemetry noop() {
        return new GenAiTelemetry(OpenTelemetry.noop());
    }

    public SpanScope startRoot(String spanName,
            Map<? extends AttributeKey<?>, ?> attributes,
            Context linkedContext) {
        if (!SpanNames.ALLOWED.contains(spanName)) {
            return SpanScope.noop();
        }
        try {
            SpanBuilder builder = tracer.spanBuilder(spanName).setNoParent();
            if (linkedContext != null && Span.fromContext(linkedContext).getSpanContext().isValid()) {
                builder.addLink(Span.fromContext(linkedContext).getSpanContext());
            }
            return start(builder, attributes, spanName);
        } catch (RuntimeException telemetryFailure) {
            return SpanScope.noop();
        }
    }

    public SpanScope startSpan(String spanName, Map<? extends AttributeKey<?>, ?> attributes) {
        if (!SpanNames.ALLOWED.contains(spanName)) {
            return SpanScope.noop();
        }
        try {
            return start(tracer.spanBuilder(spanName), attributes, spanName);
        } catch (RuntimeException telemetryFailure) {
            return SpanScope.noop();
        }
    }

    public Context captureContext() {
        try {
            return Context.current();
        } catch (RuntimeException telemetryFailure) {
            return Context.root();
        }
    }

    public void currentProviderCall(String requestedProvider,
            String effectiveProvider,
            String model,
            String protocol,
            long attemptCount,
            long retryCount) {
        try {
            Span current = Span.current();
            setSafeString(current, Attributes.PROVIDER_REQUESTED, requestedProvider);
            setSafeString(current, Attributes.PROVIDER_EFFECTIVE, effectiveProvider);
            setSafeString(current, Attributes.MODEL, model);
            setSafeString(current, Attributes.PROTOCOL, protocol);
            current.setAttribute(Attributes.ATTEMPT_COUNT, Math.max(0L, attemptCount));
            current.setAttribute(Attributes.RETRY_COUNT, Math.max(0L, retryCount));
            if (retryCount > 0L) {
                current.addEvent("rag.retry", io.opentelemetry.api.common.Attributes.of(
                        Attributes.RETRY_COUNT, Math.max(0L, retryCount)));
            }
            metrics.providerCalls(SpanNames.LLM_REQUEST, requestedProvider, effectiveProvider,
                    "success", attemptCount);
        } catch (RuntimeException telemetryFailure) {
            // telemetry 必须 fail open
        }
    }

    public void currentTokenUsage(Long inputTokens, Long outputTokens) {
        try {
            Span current = Span.current();
            if (inputTokens != null && inputTokens >= 0L) {
                current.setAttribute(Attributes.TOKEN_INPUT, inputTokens);
            }
            if (outputTokens != null && outputTokens >= 0L) {
                current.setAttribute(Attributes.TOKEN_OUTPUT, outputTokens);
            }
            metrics.tokenUsage(inputTokens, outputTokens);
        } catch (RuntimeException telemetryFailure) {
            // telemetry 必须 fail open
        }
    }

    private static void setSafeString(Span span, AttributeKey<String> key, String value) {
        if (value != null && SAFE_STRING.matcher(value).matches()) {
            span.setAttribute(key, value);
        }
    }

    private SpanScope start(SpanBuilder builder,
            Map<? extends AttributeKey<?>, ?> attributes,
            String spanName) {
        Span span = null;
        Scope scope = null;
        try {
            span = builder.startSpan();
            applyAttributes(span, attributes);
            scope = span.makeCurrent();
            return new SpanScope(span, scope, metrics, spanName);
        } catch (RuntimeException telemetryFailure) {
            if (scope != null) {
                try {
                    scope.close();
                } catch (RuntimeException ignored) {
                    // best effort cleanup
                }
            }
            if (span != null) {
                try {
                    span.end();
                } catch (RuntimeException ignored) {
                    // best effort cleanup
                }
            }
            return SpanScope.noop();
        }
    }

    @SuppressWarnings("unchecked")
    private void applyAttributes(Span span, Map<? extends AttributeKey<?>, ?> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }
        attributes.forEach((key, value) -> {
            if (key == null || value == null || !Attributes.ALLOWED.contains(key)) {
                return;
            }
            if (value instanceof String stringValue) {
                if (SAFE_STRING.matcher(stringValue).matches()) {
                    span.setAttribute((AttributeKey<String>) key, stringValue);
                }
            } else if (value instanceof Long longValue) {
                span.setAttribute((AttributeKey<Long>) key, longValue);
            } else if (value instanceof Integer intValue) {
                span.setAttribute((AttributeKey<Long>) key, intValue.longValue());
            } else if (value instanceof Double doubleValue) {
                span.setAttribute((AttributeKey<Double>) key, doubleValue);
            } else if (value instanceof Float floatValue) {
                span.setAttribute((AttributeKey<Double>) key, floatValue.doubleValue());
            } else if (value instanceof Boolean booleanValue) {
                span.setAttribute((AttributeKey<Boolean>) key, booleanValue);
            }
        });
    }

    public static final class SpanScope implements AutoCloseable {
        private final Span span;
        private final Scope scope;
        private final MetricInstruments metrics;
        private final String spanName;
        private final long startedNanos;
        private final AtomicBoolean scopeClosed = new AtomicBoolean();
        private final AtomicBoolean ended = new AtomicBoolean();
        private volatile String finalOutcome;

        private SpanScope(Span span, Scope scope, MetricInstruments metrics, String spanName) {
            this.span = Objects.requireNonNull(span, "span");
            this.scope = Objects.requireNonNull(scope, "scope");
            this.metrics = Objects.requireNonNull(metrics, "metrics");
            this.spanName = spanName;
            this.startedNanos = System.nanoTime();
            this.metrics.started(spanName);
        }

        private static SpanScope noop() {
            return new SpanScope(Span.getInvalid(), () -> { }, MetricInstruments.noop(), null);
        }

        public Span span() {
            return span;
        }

        public SpanScope safeError(Throwable error, String category, String code) {
            try {
                setSafeString(Attributes.ERROR_TYPE, error == null ? null : error.getClass().getSimpleName());
                setSafeString(Attributes.ERROR_CATEGORY, category);
                setSafeString(Attributes.ERROR_CODE, code);
                span.setStatus(StatusCode.ERROR);
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
            return this;
        }

        public SpanScope outcome(String outcome) {
            try {
                this.finalOutcome = outcome;
                setSafeString(Attributes.OUTCOME, outcome);
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
            return this;
        }

        public SpanScope lineageContext(String taskId,
                Long documentId,
                String chunkId,
                long rank,
                double score,
                String status) {
            try {
                io.opentelemetry.api.common.AttributesBuilder event =
                        io.opentelemetry.api.common.Attributes.builder();
                if (taskId != null && SAFE_STRING.matcher(taskId).matches()) {
                    event.put(Attributes.LINEAGE_TASK_ID, taskId);
                }
                if (documentId != null) {
                    event.put(Attributes.LINEAGE_DOCUMENT_ID, documentId);
                }
                if (chunkId != null && SAFE_STRING.matcher(chunkId).matches()) {
                    event.put(Attributes.LINEAGE_CHUNK_ID, chunkId);
                }
                event.put(Attributes.LINEAGE_RANK, rank);
                event.put(Attributes.LINEAGE_SCORE, score);
                if (status != null && SAFE_STRING.matcher(status).matches()) {
                    event.put(Attributes.LINEAGE_STATUS, status);
                }
                span.addEvent("rag.lineage.context", event.build());
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
            return this;
        }

        public SpanScope diagnostics(Map<String, Object> diagnostics) {
            if (diagnostics == null || diagnostics.isEmpty()) {
                return this;
            }
            try {
                String requestedProvider =
                        first(diagnostics, "rerankRequestedProvider", "requestedProvider", "provider");
                String effectiveProvider =
                        first(diagnostics, "rerankEffectiveProvider", "effectiveProvider", "provider");
                stringFact(Attributes.PROVIDER_REQUESTED, requestedProvider);
                stringFact(Attributes.PROVIDER_EFFECTIVE, effectiveProvider);
                stringFact(Attributes.MODEL, first(diagnostics, "rerankModel", "model"));
                stringFact(Attributes.PROTOCOL, first(diagnostics, "rerankProtocol", "protocol"));
                String fallbackReason = first(diagnostics, "rerankFallbackReason", "fallbackReason");
                stringFact(Attributes.FALLBACK_REASON, fallbackReason);
                long fallbackCount = number(diagnostics, "rerankFallbackCount", "fallbackCount");
                longFact(Attributes.FALLBACK_COUNT, fallbackCount);
                longFact(Attributes.ATTEMPT_COUNT, number(diagnostics, "attemptCount"));
                long retryCount = number(diagnostics, "retryCount");
                longFact(Attributes.RETRY_COUNT, retryCount);
                if (retryCount > 0L) {
                    span.addEvent("rag.retry", io.opentelemetry.api.common.Attributes.of(
                            Attributes.RETRY_COUNT, retryCount));
                }
                long modelCallCount = number(diagnostics, "modelCallCount", "rerankModelCallCount");
                metrics.providerCalls(spanName, requestedProvider, effectiveProvider,
                        fallbackCount > 0L ? "fallback_success" : "success", modelCallCount);
                metrics.fallbacks(spanName, requestedProvider, effectiveProvider,
                        fallbackReason, fallbackCount);
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
            return this;
        }

        public SpanScope providerCall(String requestedProvider,
                String effectiveProvider,
                String model,
                String protocol,
                long attemptCount,
                long retryCount) {
            stringFact(Attributes.PROVIDER_REQUESTED, requestedProvider)
                    .stringFact(Attributes.PROVIDER_EFFECTIVE, effectiveProvider)
                    .stringFact(Attributes.MODEL, model)
                    .stringFact(Attributes.PROTOCOL, protocol)
                    .longFact(Attributes.ATTEMPT_COUNT, attemptCount)
                    .longFact(Attributes.RETRY_COUNT, retryCount);
            if (retryCount > 0L) {
                try {
                    span.addEvent("rag.retry", io.opentelemetry.api.common.Attributes.of(
                            Attributes.RETRY_COUNT, Math.max(0L, retryCount)));
                } catch (RuntimeException telemetryFailure) {
                    // telemetry 必须 fail open
                }
            }
            metrics.providerCalls(spanName, requestedProvider, effectiveProvider,
                    "success", attemptCount);
            return this;
        }

        private String first(Map<String, Object> diagnostics, String... keys) {
            for (String key : keys) {
                Object value = diagnostics.get(key);
                if (value != null) {
                    return String.valueOf(value);
                }
            }
            return null;
        }

        private long number(Map<String, Object> diagnostics, String... keys) {
            for (String key : keys) {
                Object value = diagnostics.get(key);
                if (value instanceof Number number) {
                    return Math.max(0L, number.longValue());
                }
            }
            return 0L;
        }

        public SpanScope stringFact(AttributeKey<String> key, String value) {
            try {
                if (Attributes.ALLOWED.contains(key)) {
                    setSafeString(key, value);
                }
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
            return this;
        }

        public SpanScope longFact(AttributeKey<Long> key, long value) {
            try {
                if (Attributes.ALLOWED.contains(key)) {
                    span.setAttribute(key, Math.max(0L, value));
                }
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
            return this;
        }

        public SpanScope booleanFact(AttributeKey<Boolean> key, boolean value) {
            try {
                if (Attributes.ALLOWED.contains(key)) {
                    span.setAttribute(key, value);
                }
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
            return this;
        }

        private void setSafeString(AttributeKey<String> key, String value) {
            if (value != null && SAFE_STRING.matcher(value).matches()) {
                span.setAttribute(key, value);
            }
        }

        public SpanScope detach() {
            if (scopeClosed.compareAndSet(false, true)) {
                try {
                    scope.close();
                } catch (RuntimeException telemetryFailure) {
                    // telemetry 必须 fail open
                }
            }
            return this;
        }

        public void finish(String finalOutcome) {
            if (finalOutcome != null) {
                outcome(finalOutcome);
            }
            detach();
            if (ended.compareAndSet(false, true)) {
                try {
                    span.end();
                } catch (RuntimeException telemetryFailure) {
                    // telemetry 必须 fail open
                }
                metrics.finished(spanName, this.finalOutcome,
                        Math.max(0L, System.nanoTime() - startedNanos));
            }
        }

        @Override
        public void close() {
            finish(null);
        }
    }

    private static final class MetricInstruments {
        private static final AttributeKey<String> OPERATION = AttributeKey.stringKey("rag.operation");
        private static final AttributeKey<String> STAGE = AttributeKey.stringKey("rag.stage");
        private static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("rag.outcome");
        private static final AttributeKey<String> PROVIDER_REQUESTED =
                AttributeKey.stringKey("rag.provider.requested");
        private static final AttributeKey<String> PROVIDER_EFFECTIVE =
                AttributeKey.stringKey("rag.provider.effective");
        private static final AttributeKey<String> FALLBACK_REASON =
                AttributeKey.stringKey("rag.fallback.reason");
        private static final AttributeKey<String> TOKEN_DIRECTION =
                AttributeKey.stringKey("rag.token.direction");
        private static final AttributeKey<String> TOKEN_COVERAGE =
                AttributeKey.stringKey("rag.token.coverage");
        private static final Set<String> OUTCOMES = Set.of(
                "success", "cache_hit", "no_result", "error", "cancelled", "timeout",
                "fallback_success", "invalid_request", "unknown");
        private static final Set<String> PROVIDERS = Set.of(
                "openai", "qwen", "nvidia", "heuristic", "bge", "none", "disabled", "unknown");
        private static final Set<String> FALLBACK_REASONS = Set.of(
                "not_configured", "health_check_failed", "timeout", "http_4xx", "http_5xx",
                "network", "invalid_response", "incomplete_rankings", "invalid_input",
                "provider_failure", "none", "unknown");
        private final LongCounter operationCount;
        private final DoubleHistogram operationDuration;
        private final LongUpDownCounter operationInflight;
        private final DoubleHistogram stageDuration;
        private final LongCounter providerCallCount;
        private final LongCounter fallbackCount;
        private final LongCounter tokenUsage;
        private final LongCounter tokenUsageCoverage;

        private MetricInstruments(Meter meter) {
            this.operationCount = meter.counterBuilder("rag.operation.count")
                    .setDescription("Completed RAG operations")
                    .setUnit("{operation}")
                    .build();
            this.operationDuration = meter.histogramBuilder("rag.operation.duration")
                    .setDescription("RAG operation lifecycle duration")
                    .setUnit("s")
                    .build();
            this.operationInflight = meter.upDownCounterBuilder("rag.operation.inflight")
                    .setDescription("In-flight RAG operations")
                    .setUnit("{operation}")
                    .build();
            this.stageDuration = meter.histogramBuilder("rag.stage.duration")
                    .setDescription("RAG fixed-stage duration")
                    .setUnit("s")
                    .build();
            this.providerCallCount = meter.counterBuilder("rag.provider.call.count")
                    .setDescription("Actual provider calls")
                    .setUnit("{call}")
                    .build();
            this.fallbackCount = meter.counterBuilder("rag.fallback.count")
                    .setDescription("Provider fallbacks")
                    .setUnit("{fallback}")
                    .build();
            this.tokenUsage = meter.counterBuilder("rag.token.usage")
                    .setDescription("Provider-reported actual token usage")
                    .setUnit("{token}")
                    .build();
            this.tokenUsageCoverage = meter.counterBuilder("rag.token.usage_coverage")
                    .setDescription("Provider token usage observation coverage")
                    .setUnit("{observation}")
                    .build();
        }

        private static MetricInstruments create(OpenTelemetry openTelemetry) {
            try {
                return new MetricInstruments(openTelemetry.meterBuilder(INSTRUMENTATION_SCOPE)
                        .setInstrumentationVersion(INSTRUMENTATION_VERSION)
                        .build());
            } catch (RuntimeException telemetryFailure) {
                return noop();
            }
        }

        private static MetricInstruments noop() {
            return new MetricInstruments(OpenTelemetry.noop().meterBuilder(INSTRUMENTATION_SCOPE)
                    .setInstrumentationVersion(INSTRUMENTATION_VERSION)
                    .build());
        }

        private void started(String spanName) {
            String operation = operation(spanName);
            if (operation == null) {
                return;
            }
            try {
                operationInflight.add(1L, io.opentelemetry.api.common.Attributes.of(
                        OPERATION, operation));
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
        }

        private void finished(String spanName, String rawOutcome, long durationNanos) {
            String operation = operation(spanName);
            String outcome = normalizeOutcome(rawOutcome);
            double seconds = durationNanos / 1_000_000_000.0d;
            try {
                if (operation != null) {
                    io.opentelemetry.api.common.Attributes labels =
                            io.opentelemetry.api.common.Attributes.of(
                                    OPERATION, operation,
                                    OUTCOME, outcome);
                    operationCount.add(1L, labels);
                    operationDuration.record(seconds, labels);
                    operationInflight.add(-1L, io.opentelemetry.api.common.Attributes.of(
                            OPERATION, operation));
                } else if (spanName != null && SpanNames.ALLOWED.contains(spanName)) {
                    stageDuration.record(seconds, io.opentelemetry.api.common.Attributes.of(
                            STAGE, spanName,
                            OUTCOME, outcome));
                }
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
        }

        private void providerCalls(String spanName,
                String requestedProvider,
                String effectiveProvider,
                String outcome,
                long callCount) {
            if (callCount <= 0L) {
                return;
            }
            try {
                providerCallCount.add(callCount, io.opentelemetry.api.common.Attributes.builder()
                        .put(STAGE, normalizeStage(spanName))
                        .put(PROVIDER_REQUESTED, normalizeProvider(requestedProvider))
                        .put(PROVIDER_EFFECTIVE, normalizeProvider(effectiveProvider))
                        .put(OUTCOME, normalizeOutcome(outcome))
                        .build());
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
        }

        private void fallbacks(String spanName,
                String requestedProvider,
                String effectiveProvider,
                String fallbackReason,
                long count) {
            if (count <= 0L) {
                return;
            }
            try {
                fallbackCount.add(count, io.opentelemetry.api.common.Attributes.builder()
                        .put(STAGE, normalizeStage(spanName))
                        .put(PROVIDER_REQUESTED, normalizeProvider(requestedProvider))
                        .put(PROVIDER_EFFECTIVE, normalizeProvider(effectiveProvider))
                        .put(FALLBACK_REASON, normalizeFallbackReason(fallbackReason))
                        .build());
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
        }

        private void tokenUsage(Long inputTokens, Long outputTokens) {
            try {
                boolean inputPresent = inputTokens != null && inputTokens >= 0L;
                boolean outputPresent = outputTokens != null && outputTokens >= 0L;
                if (inputPresent) {
                    tokenUsage.add(inputTokens, io.opentelemetry.api.common.Attributes.of(
                            TOKEN_DIRECTION, "input"));
                }
                if (outputPresent) {
                    tokenUsage.add(outputTokens, io.opentelemetry.api.common.Attributes.of(
                            TOKEN_DIRECTION, "output"));
                }
                tokenUsageCoverage.add(1L, io.opentelemetry.api.common.Attributes.of(
                        TOKEN_COVERAGE, inputPresent && outputPresent ? "present" : "missing"));
            } catch (RuntimeException telemetryFailure) {
                // telemetry 必须 fail open
            }
        }

        private static String operation(String spanName) {
            if (SpanNames.ASK.equals(spanName)) {
                return "ask";
            }
            if (SpanNames.INGEST.equals(spanName)) {
                return "ingest";
            }
            return null;
        }

        private static String normalizeOutcome(String rawOutcome) {
            if (rawOutcome == null) {
                return "unknown";
            }
            String normalized = rawOutcome.toLowerCase(Locale.ROOT);
            return OUTCOMES.contains(normalized) ? normalized : "unknown";
        }

        private static String normalizeProvider(String rawProvider) {
            if (rawProvider == null) {
                return "unknown";
            }
            String normalized = rawProvider.toLowerCase(Locale.ROOT);
            return PROVIDERS.contains(normalized) ? normalized : "other";
        }

        private static String normalizeFallbackReason(String rawReason) {
            if (rawReason == null) {
                return "unknown";
            }
            String normalized = rawReason.toLowerCase(Locale.ROOT);
            return FALLBACK_REASONS.contains(normalized) ? normalized : "other";
        }

        private static String normalizeStage(String spanName) {
            return spanName != null && SpanNames.ALLOWED.contains(spanName) ? spanName : "unknown";
        }
    }

    public static final class SpanNames {
        public static final String ASK = "rag.ask";
        public static final String CACHE_LOOKUP = "rag.cache.lookup";
        public static final String RETRIEVAL = "rag.retrieval";
        public static final String QUERY_EMBEDDING = "rag.embedding.query";
        public static final String VECTOR_SEARCH = "rag.vector.search";
        public static final String KEYWORD_SEARCH = "rag.keyword.search";
        public static final String RETRIEVAL_FUSION = "rag.retrieval.fusion";
        public static final String RERANK = "rag.rerank";
        public static final String GENERATION = "rag.generation";
        public static final String PROMPT_BUILD = "rag.prompt.build";
        public static final String LLM_REQUEST = "rag.llm.request";
        public static final String CITATION_VALIDATE = "rag.citation.validate";
        public static final String INGEST = "rag.ingest";
        public static final String INGEST_INPUT_OPEN = "rag.ingest.input.open";
        public static final String INGEST_PARSE_CHUNK = "rag.ingest.parse-and-chunk";
        public static final String DOCUMENT_EMBEDDING = "rag.embedding.document";
        public static final String VECTOR_UPSERT = "rag.vector.upsert";
        public static final String KEYWORD_UPSERT = "rag.keyword.upsert";
        public static final String INDEX_FINALIZE = "rag.index.finalize";

        private static final java.util.Set<String> ALLOWED = java.util.Set.of(
                ASK, CACHE_LOOKUP, RETRIEVAL, QUERY_EMBEDDING, VECTOR_SEARCH,
                KEYWORD_SEARCH, RETRIEVAL_FUSION, RERANK, GENERATION, PROMPT_BUILD,
                LLM_REQUEST, CITATION_VALIDATE, INGEST, INGEST_INPUT_OPEN,
                INGEST_PARSE_CHUNK, DOCUMENT_EMBEDDING, VECTOR_UPSERT,
                KEYWORD_UPSERT, INDEX_FINALIZE);

        private SpanNames() {
        }
    }

    public static final class Attributes {
        public static final AttributeKey<String> OPERATION = AttributeKey.stringKey("rag.operation");
        public static final AttributeKey<String> STAGE = AttributeKey.stringKey("rag.stage");
        public static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("rag.outcome");
        public static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("rag.error.type");
        public static final AttributeKey<String> ERROR_CATEGORY = AttributeKey.stringKey("rag.error.category");
        public static final AttributeKey<String> ERROR_CODE = AttributeKey.stringKey("rag.error.code");
        public static final AttributeKey<String> TASK_ID = AttributeKey.stringKey("rag.ingest.task.id");
        public static final AttributeKey<Long> DOCUMENT_ID = AttributeKey.longKey("rag.document.id");
        public static final AttributeKey<Boolean> RESUME = AttributeKey.booleanKey("rag.ingest.resume");
        public static final AttributeKey<String> LINEAGE_TASK_ID = AttributeKey.stringKey("rag.lineage.task.id");
        public static final AttributeKey<Long> LINEAGE_DOCUMENT_ID = AttributeKey.longKey("rag.lineage.document.id");
        public static final AttributeKey<String> LINEAGE_CHUNK_ID = AttributeKey.stringKey("rag.lineage.chunk.id");
        public static final AttributeKey<Long> LINEAGE_RANK = AttributeKey.longKey("rag.lineage.rank");
        public static final AttributeKey<Double> LINEAGE_SCORE = AttributeKey.doubleKey("rag.lineage.score");
        public static final AttributeKey<String> LINEAGE_STATUS = AttributeKey.stringKey("rag.lineage.status");
        public static final AttributeKey<Boolean> CACHE_ENABLED = AttributeKey.booleanKey("rag.cache.enabled");
        public static final AttributeKey<Boolean> CACHE_HIT = AttributeKey.booleanKey("rag.cache.hit");
        public static final AttributeKey<String> RETRIEVAL_ROUTE = AttributeKey.stringKey("rag.retrieval.route");
        public static final AttributeKey<Long> CANDIDATE_COUNT = AttributeKey.longKey("rag.candidate.count");
        public static final AttributeKey<Long> SELECTED_COUNT = AttributeKey.longKey("rag.selected.count");
        public static final AttributeKey<Long> TOP_K = AttributeKey.longKey("rag.top_k");
        public static final AttributeKey<String> PROVIDER_REQUESTED = AttributeKey.stringKey("rag.provider.requested");
        public static final AttributeKey<String> PROVIDER_EFFECTIVE = AttributeKey.stringKey("rag.provider.effective");
        public static final AttributeKey<String> MODEL = AttributeKey.stringKey("rag.model");
        public static final AttributeKey<String> PROTOCOL = AttributeKey.stringKey("rag.protocol");
        public static final AttributeKey<Long> FALLBACK_COUNT = AttributeKey.longKey("rag.fallback.count");
        public static final AttributeKey<String> FALLBACK_REASON = AttributeKey.stringKey("rag.fallback.reason");
        public static final AttributeKey<Long> ATTEMPT_COUNT = AttributeKey.longKey("rag.attempt.count");
        public static final AttributeKey<Long> RETRY_COUNT = AttributeKey.longKey("rag.retry.count");
        public static final AttributeKey<String> INGEST_PHASE = AttributeKey.stringKey("rag.ingest.phase");
        public static final AttributeKey<Long> INGEST_CHUNK_COUNT = AttributeKey.longKey("rag.ingest.chunk_count");
        public static final AttributeKey<Long> TOKEN_INPUT = AttributeKey.longKey("rag.token.input");
        public static final AttributeKey<Long> TOKEN_OUTPUT = AttributeKey.longKey("rag.token.output");
        public static final AttributeKey<Long> PROMPT_ESTIMATED_TOKENS =
                AttributeKey.longKey("rag.prompt.estimated_tokens");

        private static final Set<AttributeKey<?>> ALLOWED = Set.of(
                OPERATION, STAGE, OUTCOME, ERROR_TYPE, ERROR_CATEGORY, ERROR_CODE,
                TASK_ID, DOCUMENT_ID, RESUME, LINEAGE_TASK_ID, LINEAGE_DOCUMENT_ID,
                LINEAGE_CHUNK_ID, LINEAGE_RANK, LINEAGE_SCORE, LINEAGE_STATUS,
                CACHE_ENABLED, CACHE_HIT, RETRIEVAL_ROUTE, CANDIDATE_COUNT, SELECTED_COUNT,
                TOP_K, PROVIDER_REQUESTED, PROVIDER_EFFECTIVE, MODEL, PROTOCOL,
                FALLBACK_COUNT, FALLBACK_REASON, ATTEMPT_COUNT, RETRY_COUNT,
                INGEST_PHASE, INGEST_CHUNK_COUNT, TOKEN_INPUT, TOKEN_OUTPUT,
                PROMPT_ESTIMATED_TOKENS);

        private Attributes() {
        }
    }
}

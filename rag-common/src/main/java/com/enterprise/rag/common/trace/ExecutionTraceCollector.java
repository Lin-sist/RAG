package com.enterprise.rag.common.trace;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import java.util.*;

/** Request-local UI projection, independent of OTel recording/export. No source text is accepted. */
public final class ExecutionTraceCollector {
    public static final String REACTOR_CONTEXT_KEY = "rag.execution-trace.collector";
    private static final ContextKey<ExecutionTraceCollector> COLLECTOR = ContextKey.named(REACTOR_CONTEXT_KEY);
    private static final ContextKey<Handle> PARENT = ContextKey.named("rag.execution-trace.parent");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> BUSINESS_STATES = Set.of("ANSWER", "NO_ANSWER", "UNSUPPORTED", "INVALID", "ERROR", "CANCELLED");
    private static final Map<String, String> NUMBERS = Map.ofEntries(
            Map.entry("rag.attempt.count", "attemptCount"), Map.entry("rag.retry.count", "retryCount"),
            Map.entry("rag.fallback.count", "fallbackCount"), Map.entry("rag.selected.count", "selectedContextCount"),
            Map.entry("rag.candidate.count", "candidateCount"), Map.entry("rag.top_k", "topK"),
            Map.entry("rag.token.input", "inputTokens"), Map.entry("rag.token.output", "outputTokens"));
    private static final Map<String, String> ENUMS = Map.of(
            "rag.provider.requested", "requestedProvider", "rag.provider.effective", "effectiveProvider",
            "rag.fallback.reason", "fallbackReason", "rag.error.category", "errorCategory",
            "rag.retrieval.route", "retrievalMode");
    private static final Set<String> SAFE_ENUMS = Set.of("none", "unknown", "heuristic", "nvidia", "openai",
            "ollama", "anthropic", "mock", "local", "hybrid", "dense_only", "keyword_only", "disabled",
            "other", "timeout", "network", "unavailable", "invalid_response", "incomplete_rankings",
            "retry_exhausted", "provider_error", "dependency_unavailable", "rate_limited", "error",
            "cancelled", "not_configured", "provider_unavailable", "execution_error", "milvus", "fallback");
    private final String runId = UUID.randomUUID().toString();
    private final String traceId;
    private final long started = System.nanoTime();
    private final List<Handle> stages = new ArrayList<>();
    private final ArrayDeque<Map<String, Object>> pending = new ArrayDeque<>();
    private long seq;
    private int eventCount;
    private int totalBytes;
    private Long frozenElapsed;
    private boolean partial;
    private boolean closed;
    private String reason;

    public ExecutionTraceCollector(String traceId) {
        this.traceId = traceId != null && traceId.matches("[0-9a-f]{32}") ? traceId : null;
        enqueue("execution.start", Map.of());
    }
    public Scope activate() { return Context.current().with(COLLECTOR, this).makeCurrent(); }
    public static Handle startCurrent(String stage) {
        ExecutionTraceCollector collector = Context.current().get(COLLECTOR);
        return collector == null ? null : collector.start(stage);
    }
    public static void currentFact(String key, Object value) {
        Handle handle = Context.current().get(PARENT);
        if (handle != null) handle.fact(key, value);
    }
    private synchronized Handle start(String stage) {
        if (closed) return null;
        if (stages.size() >= 128) { markPartial("TRUNCATED"); return null; }
        Handle parent = Context.current().get(PARENT);
        Handle handle = new Handle("s" + (stages.size() + 1),
                parent != null && parent.owner() == this ? parent.id : null, stage, elapsed());
        stages.add(handle);
        enqueue("stage.upsert", Map.of("stage", handle.view()));
        return handle;
    }
    private long elapsed() { return frozenElapsed != null ? frozenElapsed : Math.min(86_400_000L, Math.max(0, (System.nanoTime() - started) / 1_000_000)); }
    public synchronized void markPartial(String code) { partial = true; reason = "TRUNCATED".equals(code) ? code : "INCOMPLETE"; }
    private Map<String, Object> envelope(String kind, long eventSeq) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("schemaVersion", "trace-v1"); e.put("runId", runId); e.put("seq", eventSeq); e.put("kind", kind);
        e.put("elapsedMs", elapsed());
        return e;
    }
    private void enqueue(String kind, Map<String, Object> data) {
        if (closed) return;
        try {
            Map<String, Object> event = envelope(kind, seq + 1); event.putAll(data);
            int bytes = JSON.writeValueAsBytes(event).length;
            if (eventCount >= 255 || bytes > 2048 || totalBytes + bytes > 524288) { markPartial("TRUNCATED"); return; }
            seq++; eventCount++; totalBytes += bytes; pending.add(event);
        } catch (Exception ignored) { markPartial("INCOMPLETE"); }
    }
    public synchronized List<Map<String, Object>> drain() {
        List<Map<String, Object>> batch = new ArrayList<>(pending); pending.clear(); return batch;
    }
    public synchronized Map<String, Object> snapshot(String businessState) {
        if (stages.isEmpty() || stages.stream().anyMatch(s -> s.end == null)) markPartial("INCOMPLETE");
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("schemaVersion", "trace-v1"); view.put("runId", runId); view.put("seq", Math.max(1, seq));
        view.put("traceId", traceId); view.put("captureStatus", partial ? "PARTIAL" : "COMPLETE");
        view.put("elapsedMs", elapsed()); view.put("reasonCode", reason);
        view.put("businessFinalState", businessState != null && BUSINESS_STATES.contains(businessState) ? businessState : null);
        List<Map<String, Object>> rows = new ArrayList<>(stages.stream().map(Handle::view).toList());
        view.put("stages", rows);
        try {
            while (JSON.writeValueAsBytes(view).length > 65536 && !rows.isEmpty()) {
                rows.remove(rows.size() - 1); view.put("captureStatus", "PARTIAL"); view.put("reasonCode", "TRUNCATED");
            }
        } catch (Exception ignored) { view.put("stages", List.of()); view.put("captureStatus", "PARTIAL"); view.put("reasonCode", "INCOMPLETE"); }
        return view;
    }
    public synchronized Map<String, Object> finalEvent(String businessState) {
        seq++; Map<String, Object> event = envelope("execution.snapshot", seq);
        event.put("snapshot", snapshot(businessState)); closed = true; return event;
    }
    public synchronized void release() { closed = true; pending.clear(); stages.clear(); }

    public final class Handle {
        private final String id, parent, stage;
        private final long start;
        private Long end;
        private String status = "RUNNING";
        private final Map<String, Object> facts = new LinkedHashMap<>();
        private Handle(String id, String parent, String stage, long start) { this.id = id; this.parent = parent; this.stage = stage; this.start = start; }
        private ExecutionTraceCollector owner() { return ExecutionTraceCollector.this; }
        public Scope makeCurrent() { return Context.current().with(COLLECTOR, owner()).with(PARENT, this).makeCurrent(); }
        public void fact(String key, Object value) {
            synchronized (owner()) {
                if (closed) return;
                String mapped = NUMBERS.get(key);
                if (mapped != null && value instanceof Number n && Double.isFinite(n.doubleValue()) && n.doubleValue() >= 0 && n.doubleValue() <= 1e12) {
                    if (n instanceof Double || n instanceof Float) facts.put(mapped, n.doubleValue());
                    else facts.put(mapped, n.longValue());
                }
                else if (ENUMS.containsKey(key) && value instanceof String s) facts.put(ENUMS.get(key), SAFE_ENUMS.contains(s.toLowerCase(Locale.ROOT)) ? s.toLowerCase(Locale.ROOT) : "other");
                else if ("rag.cache.enabled".equals(key) && value instanceof Boolean) facts.put("enabled", value);
            }
        }
        public void finish(String outcome) {
            synchronized (owner()) {
                if (closed || end != null) return;
                end = Math.max(start, elapsed());
                String normalized = outcome == null ? "SUCCESS" : outcome.toUpperCase(Locale.ROOT);
                status = switch (normalized) { case "ERROR", "TIMEOUT", "FAILED" -> "FAILED"; case "CANCELLED" -> "CANCELLED"; case "SKIPPED" -> "SKIPPED"; default -> "SUCCEEDED"; };
                if (parent == null && "rag.ask".equals(stage)) frozenElapsed = end;
                enqueue("stage.upsert", Map.of("stage", view()));
            }
        }
        private Map<String, Object> view() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stageId", id); row.put("parentStageId", parent); row.put("stage", stage); row.put("status", status);
            row.put("startOffsetMs", start); row.put("endOffsetMs", end); row.put("facts", new LinkedHashMap<>(facts)); return row;
        }
    }
}

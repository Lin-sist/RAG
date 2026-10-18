package com.enterprise.rag.core.rag.generator;

import com.enterprise.rag.common.trace.TraceContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-attempt, bounded scalar observations only; never retains response text or arbitrary labels. */
@Slf4j
final class StreamResponseDiagnostics {
    private final String traceId;
    private final String streamId;
    private final int attempt;
    private final int retryCount;
    private final int maxOutputTokens;
    private final int timeoutSeconds;
    private final int maxRetries;
    private final long startedNanos = System.nanoTime();
    private boolean logged;
    private boolean errorObserved;
    private int httpStatus = -1;
    private String contentType = "missing";
    private String finishReason = "missing";
    private long events, jsonEvents, jsonParseFailures, extractionFailures, doneEvents;
    private long missingChoicesEvents, emptyChoicesEvents, maxChoices;
    private long deltaEvents, messageEvents, contentEvents, contentChars, reasoningEvents, reasoningChars;
    private long visibleChunks, visibleChars, usageEvents;
    private long promptTokens = -1, completionTokens = -1, totalTokens = -1, reasoningTokens = -1;

    StreamResponseDiagnostics(String traceId, String streamId, int attempt, int retryCount,
            int maxOutputTokens, int timeoutSeconds, int maxRetries) {
        this.traceId = traceId;
        this.streamId = streamId;
        this.attempt = attempt;
        this.retryCount = retryCount;
        this.maxOutputTokens = maxOutputTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.maxRetries = maxRetries;
    }

    static String captureTraceId() {
        String value = TraceContext.getTraceId();
        return value != null && value.matches("[a-fA-F0-9]{16,32}") ? value : "missing";
    }

    synchronized void response(int status, List<String> types) {
        httpStatus = status;
        String type = types.isEmpty() ? "" : types.get(0).split(";", 2)[0].trim();
        contentType = type.isEmpty() ? "missing" : "text/event-stream".equalsIgnoreCase(type)
                ? "sse" : "application/json".equalsIgnoreCase(type) ? "json" : "other";
    }

    synchronized void event() { events++; }
    synchronized void done() { doneEvents++; }
    synchronized void parseFailure(boolean jsonParsed) {
        if (jsonParsed) extractionFailures++; else jsonParseFailures++;
    }

    synchronized void parsed(JsonNode root) {
        jsonEvents++;
        if (root == null) { missingChoicesEvents++; return; }
        JsonNode choices = root.path("choices");
        if (!choices.isArray()) missingChoicesEvents++;
        else if (choices.isEmpty()) emptyChoicesEvents++;
        else {
            maxChoices = Math.max(maxChoices, choices.size());
            // The existing parser consumes only the first choice. Do not scan or retain other text.
            JsonNode choice = choices.path(0);
            JsonNode delta = choice.path("delta");
            if (delta.isObject()) deltaEvents++;
            if (choice.path("message").isObject()) messageEvents++;
            long chars = textLength(delta.path("content"));
            if (chars > 0) { contentEvents++; contentChars += chars; }
            long reasoning = textLength(delta.path("reasoning_content")) + textLength(delta.path("reasoning"));
            if (reasoning > 0) { reasoningEvents++; reasoningChars += reasoning; }
            JsonNode reason = choice.path("finish_reason");
            if (!reason.isMissingNode() && !reason.isNull()) {
                finishReason = switch (reason.asText()) {
                    case "stop" -> "stop";
                    case "length" -> "length";
                    case "tool_calls" -> "tool_calls";
                    case "function_call" -> "function_call";
                    case "content_filter" -> "content_filter";
                    default -> "other";
                };
            }
        }
        JsonNode usage = root.path("usage");
        if (usage.isObject()) {
            usageEvents++;
            promptTokens = numeric(usage.path("prompt_tokens"), promptTokens);
            completionTokens = numeric(usage.path("completion_tokens"), completionTokens);
            totalTokens = numeric(usage.path("total_tokens"), totalTokens);
            reasoningTokens = numeric(usage.path("completion_tokens_details").path("reasoning_tokens"), reasoningTokens);
        }
    }

    synchronized void visible(String value) {
        visibleChunks++;
        visibleChars += value.length();
    }

    synchronized void error() { errorObserved = true; }

    synchronized void finish(reactor.core.publisher.SignalType signal) {
        if (logged) return;
        logged = true;
        Map<String, Object> fields = new LinkedHashMap<>();
        // retryWhen may cancel the source while propagating an already observed error.
        fields.put("outcome", errorObserved ? "error" : switch (signal) {
            case ON_COMPLETE -> "complete";
            case ON_ERROR -> "error";
            case CANCEL -> "cancel";
            default -> "other";
        });
        fields.put("httpStatus", httpStatus); fields.put("contentType", contentType);
        fields.put("elapsedMs", (System.nanoTime() - startedNanos) / 1_000_000);
        fields.put("retryCount", retryCount); fields.put("maxRetries", maxRetries);
        fields.put("maxOutputTokens", maxOutputTokens); fields.put("timeoutSeconds", timeoutSeconds);
        fields.put("events", events); fields.put("jsonEvents", jsonEvents);
        fields.put("jsonParseFailures", jsonParseFailures); fields.put("extractionFailures", extractionFailures);
        fields.put("doneEvents", doneEvents); fields.put("missingChoicesEvents", missingChoicesEvents);
        fields.put("emptyChoicesEvents", emptyChoicesEvents); fields.put("maxChoices", maxChoices);
        fields.put("deltaEvents", deltaEvents); fields.put("messageEvents", messageEvents);
        fields.put("contentEvents", contentEvents); fields.put("contentChars", contentChars);
        fields.put("reasoningEvents", reasoningEvents); fields.put("reasoningChars", reasoningChars);
        fields.put("visibleChunks", visibleChunks); fields.put("visibleChars", visibleChars);
        fields.put("finishReason", finishReason); fields.put("usageEvents", usageEvents);
        fields.put("promptTokens", promptTokens); fields.put("completionTokens", completionTokens);
        fields.put("totalTokens", totalTokens); fields.put("reasoningTokens", reasoningTokens);
        log.info("llm_stream_attempt traceId={} streamId={} provider=openai attempt={} diagnostics={}",
                traceId, streamId, attempt, fields);
    }

    private static long textLength(JsonNode value) { return value.isTextual() ? value.textValue().length() : 0; }
    private static long numeric(JsonNode value, long previous) {
        return value.isIntegralNumber() && value.canConvertToLong() && value.longValue() >= 0
                ? value.longValue() : previous;
    }
}

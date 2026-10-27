package com.enterprise.rag.common.trace;

import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionTraceCollectorTest {
    @Test
    void brokenOtelBuilderStillCollectsAndArbitraryReasonCannotLeak() {
        var otel = org.mockito.Mockito.mock(io.opentelemetry.api.OpenTelemetry.class);
        var tracer = org.mockito.Mockito.mock(io.opentelemetry.api.trace.Tracer.class);
        org.mockito.Mockito.when(otel.getTracer(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString())).thenReturn(tracer);
        org.mockito.Mockito.when(tracer.spanBuilder("rag.ask")).thenThrow(new IllegalStateException("private-otel-error"));
        var collector = new ExecutionTraceCollector(null);
        try (Scope ignored = collector.activate(); var span = new GenAiTelemetry(otel).startRoot("rag.ask", Map.of(), null)) { span.outcome("SUCCESS"); }
        collector.markPartial("private-reason");
        String snapshot = collector.snapshot("private-state").toString();
        assertTrue(snapshot.contains("SUCCEEDED"));
        assertFalse(snapshot.contains("private-"));
    }
    @Test
    void noopTelemetryCollectsActualHierarchyAndExcludesPrivateFacts() {
        ExecutionTraceCollector collector = new ExecutionTraceCollector("a".repeat(32));
        try (Scope ignored = collector.activate();
             var ask = GenAiTelemetry.noop().startRoot("rag.ask", Map.of(), null)) {
            try (var child = GenAiTelemetry.noop().startSpan("rag.llm.request", Map.of())) {
                child.providerCall("openai", "openai", "private-model-name", "private-protocol", 2, 1);
                child.stringFact(GenAiTelemetry.Attributes.TASK_ID, "private-task-id");
                child.safeError(new IllegalStateException("private-error-message"), "timeout", "private-code");
                child.outcome("ERROR");
            }
            ask.outcome("ERROR");
        }
        Map<String, Object> snapshot = collector.snapshot("ERROR");
        String text = snapshot.toString();
        assertEquals("COMPLETE", snapshot.get("captureStatus"));
        assertTrue(text.contains("parentStageId=s1"));
        assertTrue(text.contains("status=FAILED"));
        assertTrue(text.contains("retryCount=1"));
        assertFalse(text.contains("private-"));
        assertFalse(text.contains("RUNNING"));
    }

    @Test
    void boundsStagesEventsAndReleasesIdempotently() {
        ExecutionTraceCollector collector = new ExecutionTraceCollector(null);
        try (Scope ignored = collector.activate()) {
            for (int i = 0; i < 180; i++) {
                var handle = ExecutionTraceCollector.startCurrent("rag.vector.search");
                if (handle != null) handle.finish("SUCCESS");
            }
        }
        assertEquals(128, ((List<?>) collector.snapshot("ANSWER").get("stages")).size());
        assertEquals("PARTIAL", collector.snapshot("ANSWER").get("captureStatus"));
        assertTrue(collector.drain().size() <= 255);
        var event = collector.finalEvent("ANSWER");
        assertEquals(event.get("seq"), ((Map<?, ?>) event.get("snapshot")).get("seq"));
        collector.release(); collector.release();
        assertTrue(collector.drain().isEmpty());
    }

    @Test
    void projectionEndsBeforeTransportAndUnfinishedSpansStayPartial() {
        ExecutionTraceCollector collector = new ExecutionTraceCollector(null);
        try (Scope ignored = collector.activate()) {
            var ask = GenAiTelemetry.noop().startRoot("rag.ask", Map.of(), null);
            ask.detach(); ask.finishProjection("CANCELLED");
            var before = collector.snapshot("CANCELLED");
            ask.finish("SUCCESS");
            assertEquals(before, collector.snapshot("CANCELLED"));
            assertTrue(before.toString().contains("CANCELLED"));
        }
        ExecutionTraceCollector incomplete = new ExecutionTraceCollector(null);
        assertEquals("PARTIAL", incomplete.snapshot("ANSWER").get("captureStatus"));
    }
}

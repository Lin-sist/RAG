package com.enterprise.rag.admin.qa.dto;

import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.service.RAGService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamTerminalEventTest {
    @Test
    void terminalOnlyExposesAllowedMetadataAndUnknownUsage() throws Exception {
        RAGService.StreamTerminalSignal signal = new RAGService.StreamTerminalSignal();
        Citation citation = Citation.of("chunk-1", "validated snippet");
        signal.recordExecutionResult("ANSWER", "NONE", "answer", List.of(citation), Map.of(
                "validCitations", 1,
                "estimatedOutputTokens", 5,
                "providerBody", "must-not-leak"));

        StreamTerminalEvent event = StreamTerminalEvent.completed(signal);
        String json = new ObjectMapper().writeValueAsString(event);

        assertEquals("structured-v1", event.schemaVersion());
        assertEquals("ANSWER", event.finalState());
        assertEquals(List.of(citation), event.citations());
        assertNull(event.usage());
        assertNull(event.budgetOutcome());
        assertEquals("legacy", event.classifierVersion());
        assertTrue(json.contains("\"usage\":null"));
        assertFalse(json.contains("must-not-leak"));
        assertFalse(json.contains("providerBody"));
    }

    @Test
    void missingCoreResultFailsClosed() {
        StreamTerminalEvent event = StreamTerminalEvent.completed(new RAGService.StreamTerminalSignal());

        assertEquals("ERROR", event.finalState());
        assertEquals("RESULT_UNAVAILABLE", event.reason());
        assertEquals(List.of(), event.citations());
        assertNull(event.usage());
    }

    @Test
    void nonAnswerTerminalCannotExposeCitations() {
        RAGService.StreamTerminalSignal signal = new RAGService.StreamTerminalSignal();
        signal.recordExecutionResult("NO_ANSWER", "NO_EVIDENCE", "",
                List.of(Citation.of("chunk-1", "not valid for this result")), Map.of());

        StreamTerminalEvent event = StreamTerminalEvent.completed(signal);

        assertEquals("NO_ANSWER", event.finalState());
        assertEquals(List.of(), event.citations());
    }
}

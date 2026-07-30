package com.enterprise.rag.admin.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpResultMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void structuredAndTextRepresentTheSameValidatedPayload() throws Exception {
        McpResultMapper mapper = new McpResultMapper(objectMapper, 4_096);
        Map<String, Object> payload = citationPayload();

        McpSchema.CallToolResult result = mapper.success(
                McpToolSpecifications.GET_CITATION, payload);

        assertFalse(result.isError());
        JsonNode structured = objectMapper.readTree(
                objectMapper.writeValueAsString(result.structuredContent()));
        McpSchema.TextContent text = (McpSchema.TextContent) result.content().get(0);
        assertEquals(structured, objectMapper.readTree(text.text()));
        assertEquals(2, result.content().size());
        McpSchema.ResourceLink link = (McpSchema.ResourceLink) result.content().get(1);
        assertEquals("rag://knowledge-bases/1/documents/2/chunks/0", link.uri());
    }

    @Test
    void outputSchemaDriftAndDuplicatedResultOverflowFailClosed() {
        Map<String, Object> drifted = new LinkedHashMap<>(citationPayload());
        drifted.put("rawMetadata", "private-canary");
        McpToolExecutionException schemaError = assertThrows(
                McpToolExecutionException.class,
                () -> new McpResultMapper(objectMapper, 4_096).success(
                        McpToolSpecifications.GET_CITATION, drifted));
        assertEquals("MCP_INTERNAL_ERROR", schemaError.category());

        McpToolExecutionException sizeError = assertThrows(
                McpToolExecutionException.class,
                () -> new McpResultMapper(objectMapper, 32).success(
                        McpToolSpecifications.GET_CITATION, citationPayload()));
        assertEquals("MCP_RESULT_TOO_LARGE", sizeError.category());
    }

    private Map<String, Object> citationPayload() {
        return Map.ofEntries(
                Map.entry("status", "ok"),
                Map.entry("kbId", 1L),
                Map.entry("documentId", 2L),
                Map.entry("chunkId", "chunk-a"),
                Map.entry("chunkIndex", 0),
                Map.entry("title", "document"),
                Map.entry("startPos", 0),
                Map.entry("endPos", 5),
                Map.entry("boundedContent", "alpha"),
                Map.entry("truncated", false),
                Map.entry("resourceUri", "rag://knowledge-bases/1/documents/2/chunks/0"));
    }
}

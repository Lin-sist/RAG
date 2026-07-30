package com.enterprise.rag.admin.mcp;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolRequestValidatorTest {

    @Test
    void searchAndAskRejectUnknownSelectorsBlankTextAndNumericBounds() {
        McpProperties properties = new McpProperties();
        properties.setExternalToolsEnabled(true);
        properties.setMaxQueryChars(5);
        McpToolRequestValidator validator = new McpToolRequestValidator(properties);

        List<Map<String, Object>> invalidArguments = List.of(
                Map.of("kbId", 1, "query", "ok", "tenantId", 2),
                Map.of("kbId", 1, "query", "ok", "collectionName", "private"),
                Map.of("kbId", 1, "query", "ok", "filter", Map.of()),
                Map.of("kbId", 1, "query", "ok", "provider", "client-choice"),
                Map.of("kbId", 1, "query", "   "),
                Map.of("kbId", 1, "query", "123456"),
                Map.of("kbId", 1, "query", "ok", "topK", 0),
                Map.of("kbId", 1, "query", "ok", "topK", 21),
                Map.of("kbId", 1, "query", "ok", "minScore", -0.1),
                Map.of("kbId", 1, "query", "ok", "minScore", 1.1),
                Map.of("kbId", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE),
                        "query", "ok"));
        for (Map<String, Object> arguments : invalidArguments) {
            assertEquals(
                    McpToolRequestValidator.Outcome.INVALID,
                    validator.validate(call(McpToolSpecifications.SEARCH, arguments)));
        }
        assertEquals(
                McpToolRequestValidator.Outcome.VALID,
                validator.validate(call(
                        McpToolSpecifications.ASK,
                        Map.of("kbId", 1, "question", "valid", "topK", 20,
                                "minScore", 1.0))));
    }

    @Test
    void citationReferencesAreExactAndDisabledExternalToolsStayDoubleGuarded() {
        McpProperties properties = new McpProperties();
        McpToolRequestValidator validator = new McpToolRequestValidator(properties);

        assertEquals(
                McpToolRequestValidator.Outcome.INVALID,
                validator.validate(call(
                        McpToolSpecifications.GET_CITATION,
                        Map.of("kbId", 1, "documentId", 2, "chunkId", "../chunk"))));
        assertEquals(
                McpToolRequestValidator.Outcome.INVALID,
                validator.validate(call(
                        McpToolSpecifications.COMPARE_SOURCES,
                        Map.of(
                                "left", Map.of("kbId", 1, "documentId", 2, "chunkId", "a"),
                                "right", Map.of("kbId", 1, "documentId", 2,
                                        "chunkId", "b", "tenantId", 2)))));
        assertEquals(
                McpToolRequestValidator.Outcome.EXTERNAL_TOOLS_DISABLED,
                validator.validate(call(
                        McpToolSpecifications.SEARCH,
                        Map.of("kbId", 1, "query", "valid"))));
        assertEquals(
                McpToolRequestValidator.Outcome.INVALID,
                validator.validate(call(
                        McpToolSpecifications.SEARCH,
                        Map.of("kbId", 1, "query", "valid", "tenantId", 2))));
    }

    private Map<String, Object> call(String name, Map<String, Object> arguments) {
        return Map.of("name", name, "arguments", arguments);
    }
}

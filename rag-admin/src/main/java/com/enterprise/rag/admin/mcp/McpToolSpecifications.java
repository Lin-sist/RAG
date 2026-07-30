package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fixed C15 tool identities, schemas and read-only protocol hints. */
final class McpToolSpecifications {

    static final String SEARCH = "rag.search";
    static final String ASK = "rag.ask";
    static final String GET_CITATION = "rag.get-citation";
    static final String COMPARE_SOURCES = "rag.compare-sources";

    private static final String SCHEMA_DIALECT =
            "https://json-schema.org/draft/2020-12/schema";
    private static final int MAX_CHUNK_ID_CHARS = 100;

    private McpToolSpecifications() {
    }

    static boolean isExternalTool(String toolName) {
        return SEARCH.equals(toolName) || ASK.equals(toolName);
    }

    static Map<String, Object> inputSchema(String toolName) {
        return inputSchema(toolName, McpProperties.DEFAULT_MAX_QUERY_CHARS);
    }

    static Map<String, Object> inputSchema(String toolName, int maxQueryChars) {
        return switch (toolName) {
            case SEARCH -> searchInputSchema("query", maxQueryChars);
            case ASK -> searchInputSchema("question", maxQueryChars);
            case GET_CITATION -> citationInputSchema();
            case COMPARE_SOURCES -> compareInputSchema();
            default -> null;
        };
    }

    static Map<String, Object> outputSchema(String toolName) {
        return switch (toolName) {
            case SEARCH -> searchOutputSchema();
            case ASK -> askOutputSchema();
            case GET_CITATION -> citationOutputSchema();
            case COMPARE_SOURCES -> compareOutputSchema();
            default -> null;
        };
    }

    static List<McpStatelessServerFeatures.SyncToolSpecification> specifications(
            McpReadOnlyToolService toolService,
            McpRequestIdentityResolver identityResolver,
            McpProperties properties) {
        List<McpStatelessServerFeatures.SyncToolSpecification> specifications =
                new ArrayList<>();
        if (properties.isExternalToolsEnabled()) {
            specifications.add(specification(
                    SEARCH,
                    "Search authorized knowledge-base evidence",
                    searchInputSchema("query", properties.getMaxQueryChars()),
                    searchOutputSchema(),
                    toolService,
                    identityResolver));
            specifications.add(specification(
                    ASK,
                    "Ask an authorized knowledge base without history/count side effects",
                    searchInputSchema("question", properties.getMaxQueryChars()),
                    askOutputSchema(),
                    toolService,
                    identityResolver));
        }
        specifications.add(specification(
                GET_CITATION,
                "Read one exact authorized citation",
                citationInputSchema(),
                citationOutputSchema(),
                toolService,
                identityResolver));
        specifications.add(specification(
                COMPARE_SOURCES,
                "Compare two exact authorized sources without semantic inference",
                compareInputSchema(),
                compareOutputSchema(),
                toolService,
                identityResolver));
        return List.copyOf(specifications);
    }

    private static McpStatelessServerFeatures.SyncToolSpecification specification(
            String name,
            String description,
            Map<String, Object> inputSchema,
            Map<String, Object> outputSchema,
            McpReadOnlyToolService toolService,
            McpRequestIdentityResolver identityResolver) {
        McpSchema.ToolAnnotations annotations = McpSchema.ToolAnnotations.builder()
                .readOnlyHint(true)
                .destructiveHint(false)
                .idempotentHint(true)
                .openWorldHint(false)
                .build();
        McpSchema.Tool tool = McpSchema.Tool.builder(name)
                .description(description)
                .inputSchema(inputSchema)
                .outputSchema(outputSchema)
                .annotations(annotations)
                .build();
        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((context, request) -> toolService.call(
                        identityResolver.requireContextIdentity(context),
                        request.name(),
                        request.arguments()))
                .build();
    }

    private static Map<String, Object> searchInputSchema(
            String textProperty, int maxQueryChars) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("kbId", positiveInteger());
        properties.put(textProperty, Map.of(
                "type", "string",
                "minLength", 1,
                "maxLength", maxQueryChars,
                "pattern", ".*\\S.*"));
        properties.put("topK", Map.of(
                "type", "integer",
                "minimum", 1,
                "maximum", 20,
                "default", 5));
        properties.put("minScore", Map.of(
                "type", "number",
                "minimum", 0,
                "maximum", 1,
                "default", 0.3));
        return objectSchema(properties, List.of("kbId", textProperty));
    }

    private static Map<String, Object> citationInputSchema() {
        return objectSchema(Map.of(
                "kbId", positiveInteger(),
                "documentId", positiveInteger(),
                "chunkId", chunkIdSchema()),
                List.of("kbId", "documentId", "chunkId"));
    }

    private static Map<String, Object> compareInputSchema() {
        Map<String, Object> reference = objectSchema(Map.of(
                "kbId", positiveInteger(),
                "documentId", positiveInteger(),
                "chunkId", chunkIdSchema()),
                List.of("kbId", "documentId", "chunkId"));
        return objectSchema(Map.of("left", reference, "right", reference),
                List.of("left", "right"));
    }

    private static Map<String, Object> searchOutputSchema() {
        Map<String, Object> item = objectSchema(Map.ofEntries(
                Map.entry("rank", Map.of("type", "integer", "minimum", 1, "maximum", 20)),
                Map.entry("score", Map.of("type", "number")),
                Map.entry("documentId", positiveInteger()),
                Map.entry("chunkId", chunkIdSchema()),
                Map.entry("documentTitle", boundedString(512)),
                Map.entry("sourceFileName", boundedString(512)),
                Map.entry("boundedExcerpt", boundedString(8_192)),
                Map.entry("resourceUri", boundedString(512))),
                List.of("rank", "score", "documentId", "chunkId", "documentTitle",
                        "sourceFileName", "boundedExcerpt", "resourceUri"));
        return objectSchema(Map.of(
                "status", Map.of("type", "string", "enum", List.of("ok", "no_result")),
                "resultCount", Map.of("type", "integer", "minimum", 0, "maximum", 20),
                "items", Map.of("type", "array", "maxItems", 20, "items", item),
                "diagnostics", diagnosticsSchema()),
                List.of("status", "resultCount", "items", "diagnostics"));
    }

    private static Map<String, Object> askOutputSchema() {
        Map<String, Object> citation = objectSchema(Map.of(
                "documentId", positiveInteger(),
                "chunkId", chunkIdSchema(),
                "title", boundedString(512),
                "boundedSnippet", boundedString(8_192),
                "score", Map.of("type", "number"),
                "resourceUri", boundedString(512)),
                List.of("documentId", "chunkId", "title", "boundedSnippet", "score",
                        "resourceUri"));
        return objectSchema(Map.of(
                "status", Map.of("type", "string", "enum", List.of("ok", "no_result", "error")),
                "answer", boundedString(65_536),
                "citations", Map.of("type", "array", "maxItems", 20, "items", citation),
                "errorCategory", boundedString(64),
                "diagnostics", diagnosticsSchema()),
                List.of("status", "answer", "citations", "diagnostics"));
    }

    private static Map<String, Object> citationOutputSchema() {
        return objectSchema(Map.ofEntries(
                Map.entry("status", Map.of("type", "string", "const", "ok")),
                Map.entry("kbId", positiveInteger()),
                Map.entry("documentId", positiveInteger()),
                Map.entry("chunkId", chunkIdSchema()),
                Map.entry("chunkIndex", Map.of("type", "integer", "minimum", 0)),
                Map.entry("title", boundedString(512)),
                Map.entry("startPos", Map.of("type", "integer")),
                Map.entry("endPos", Map.of("type", "integer")),
                Map.entry("boundedContent", boundedString(65_536)),
                Map.entry("truncated", Map.of("type", "boolean")),
                Map.entry("resourceUri", boundedString(512))),
                List.of("status", "kbId", "documentId", "chunkId", "chunkIndex", "title",
                        "startPos", "endPos", "boundedContent", "truncated", "resourceUri"));
    }

    private static Map<String, Object> compareOutputSchema() {
        Map<String, Object> source = new LinkedHashMap<>(citationOutputSchema());
        return objectSchema(Map.of(
                "status", Map.of("type", "string", "const", "ok"),
                "left", source,
                "right", source,
                "sameKnowledgeBase", Map.of("type", "boolean"),
                "sameDocument", Map.of("type", "boolean"),
                "sameChunk", Map.of("type", "boolean"),
                "semanticComparisonStatus", Map.of(
                        "type", "string", "const", "NOT_PERFORMED")),
                List.of("status", "left", "right", "sameKnowledgeBase", "sameDocument",
                        "sameChunk", "semanticComparisonStatus"));
    }

    private static Map<String, Object> diagnosticsSchema() {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (String key : List.of(
                "retrievalMode", "degradedDependency", "rerankRequestedProvider",
                "rerankEffectiveProvider", "rerankFallbackReason", "rerankModel",
                "rerankProtocol", "generationRequestedProvider", "generationEffectiveProvider",
                "generationFallbackReason", "generationModel")) {
            fields.put(key, boundedString(128));
        }
        for (String key : List.of(
                "rerankFallbackCount", "rerankModelCallCount", "rerankCandidateCount",
                "rerankScoredCount", "rerankLatencyMillis", "generationFallbackCount",
                "generationModelCallCount", "generationRetryCount")) {
            fields.put(key, Map.of("type", "integer", "minimum", 0));
        }
        for (String key : List.of(
                "retrievalDegraded", "generationTimedOut", "cacheHit")) {
            fields.put(key, Map.of("type", "boolean"));
        }
        for (String key : List.of("rerankCoverage", "citationCoverage")) {
            fields.put(key, Map.of("type", "number", "minimum", 0, "maximum", 1));
        }
        return objectSchema(fields, List.of());
    }

    private static Map<String, Object> positiveInteger() {
        return Map.of(
                "type", "integer",
                "minimum", 1,
                "maximum", Long.MAX_VALUE);
    }

    private static Map<String, Object> chunkIdSchema() {
        return Map.of(
                "type", "string",
                "minLength", 1,
                "maxLength", MAX_CHUNK_ID_CHARS,
                "pattern", "^[A-Za-z0-9:_-]+$");
    }

    private static Map<String, Object> boundedString(int maxLength) {
        return Map.of("type", "string", "maxLength", maxLength);
    }

    private static Map<String, Object> objectSchema(
            Map<String, Object> properties,
            List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", SCHEMA_DIALECT);
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        schema.put("additionalProperties", false);
        return Map.copyOf(schema);
    }
}

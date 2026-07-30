package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.security.RequestIdentity;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/** Fail-closed execution boundary while individual C15 tool handlers are wired vertically. */
final class McpReadOnlyToolService {

    private final McpCitationReader citationReader;
    private final McpExternalReadService externalReadService;
    private final McpResultMapper resultMapper;
    private final McpProperties properties;
    private final McpToolExecutionGuard executionGuard;

    McpReadOnlyToolService(
            McpCitationReader citationReader,
            McpExternalReadService externalReadService,
            McpResultMapper resultMapper,
            McpProperties properties,
            McpToolExecutionGuard executionGuard) {
        this.citationReader = citationReader;
        this.externalReadService = externalReadService;
        this.resultMapper = resultMapper;
        this.properties = properties;
        this.executionGuard = executionGuard;
    }

    McpSchema.CallToolResult call(
            RequestIdentity identity, String toolName, Map<String, Object> arguments) {
        if (McpToolSpecifications.inputSchema(toolName) == null) {
            return resultMapper.error("MCP_INVALID_ARGUMENT");
        }
        if (McpToolSpecifications.isExternalTool(toolName)
                && !properties.isExternalToolsEnabled()) {
            return resultMapper.error("MCP_EXTERNAL_TOOLS_DISABLED");
        }
        try {
            return executionGuard.execute(
                    identity, toolName, () -> dispatch(identity, toolName, arguments));
        } catch (McpToolExecutionException exception) {
            return resultMapper.error(exception.category());
        } catch (RuntimeException exception) {
            return resultMapper.error("MCP_INTERNAL_ERROR");
        }
    }

    private McpSchema.CallToolResult dispatch(
            RequestIdentity identity, String toolName, Map<String, Object> arguments) {
        try {
            return switch (toolName) {
                case McpToolSpecifications.GET_CITATION -> getCitation(identity, arguments);
                case McpToolSpecifications.COMPARE_SOURCES -> compareSources(identity, arguments);
                case McpToolSpecifications.SEARCH -> search(identity, arguments);
                case McpToolSpecifications.ASK -> ask(identity, arguments);
                default -> resultMapper.error("MCP_INVALID_ARGUMENT");
            };
        } catch (McpToolExecutionException exception) {
            return resultMapper.error(exception.category());
        } catch (RuntimeException exception) {
            return resultMapper.error("MCP_INTERNAL_ERROR");
        }
    }

    private McpSchema.CallToolResult search(
            RequestIdentity identity, Map<String, Object> arguments) {
        if (!properties.isExternalToolsEnabled()) {
            return resultMapper.error("MCP_EXTERNAL_TOOLS_DISABLED");
        }
        if (externalReadService == null) {
            return resultMapper.error("MCP_DEPENDENCY_UNAVAILABLE");
        }
        Map<String, Object> payload = externalReadService.search(
                identity,
                longArgument(arguments, "kbId"),
                stringArgument(arguments, "query"),
                intArgument(arguments, "topK", 5),
                floatArgument(arguments, "minScore", 0.3f));
        return resultMapper.success(McpToolSpecifications.SEARCH, payload);
    }

    private McpSchema.CallToolResult ask(
            RequestIdentity identity, Map<String, Object> arguments) {
        if (!properties.isExternalToolsEnabled()) {
            return resultMapper.error("MCP_EXTERNAL_TOOLS_DISABLED");
        }
        if (externalReadService == null) {
            return resultMapper.error("MCP_DEPENDENCY_UNAVAILABLE");
        }
        Map<String, Object> payload = externalReadService.ask(
                identity,
                longArgument(arguments, "kbId"),
                stringArgument(arguments, "question"),
                intArgument(arguments, "topK", 5),
                floatArgument(arguments, "minScore", 0.3f));
        return resultMapper.success(McpToolSpecifications.ASK, payload);
    }

    private McpSchema.CallToolResult getCitation(
            RequestIdentity identity, Map<String, Object> arguments) {
        McpCitationReader.CitationSource source = citationReader.read(
                identity,
                longArgument(arguments, "kbId"),
                longArgument(arguments, "documentId"),
                stringArgument(arguments, "chunkId"));
        return resultMapper.success(
                McpToolSpecifications.GET_CITATION, sourcePayload(source));
    }

    private McpSchema.CallToolResult compareSources(
            RequestIdentity identity, Map<String, Object> arguments) {
        SourceReference leftReference = sourceReference(arguments, "left");
        SourceReference rightReference = sourceReference(arguments, "right");
        McpCitationReader.CitationSource left = citationReader.read(
                identity,
                leftReference.kbId(),
                leftReference.documentId(),
                leftReference.chunkId());
        McpCitationReader.CitationSource right = citationReader.read(
                identity,
                rightReference.kbId(),
                rightReference.documentId(),
                rightReference.chunkId());
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("status", "ok");
        payload.put("left", sourcePayload(left));
        payload.put("right", sourcePayload(right));
        payload.put("sameKnowledgeBase", left.kbId() == right.kbId());
        payload.put("sameDocument", left.documentId() == right.documentId());
        payload.put("sameChunk", left.kbId() == right.kbId()
                && left.documentId() == right.documentId()
                && left.chunkId().equals(right.chunkId()));
        payload.put("semanticComparisonStatus", "NOT_PERFORMED");
        return resultMapper.success(McpToolSpecifications.COMPARE_SOURCES, payload);
    }

    private Map<String, Object> sourcePayload(McpCitationReader.CitationSource source) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("status", "ok");
        payload.put("kbId", source.kbId());
        payload.put("documentId", source.documentId());
        payload.put("chunkId", source.chunkId());
        payload.put("chunkIndex", source.chunkIndex());
        payload.put("title", source.title());
        payload.put("startPos", source.startPos());
        payload.put("endPos", source.endPos());
        payload.put("boundedContent", source.boundedContent());
        payload.put("truncated", source.truncated());
        payload.put("resourceUri", source.resourceUri());
        return Map.copyOf(payload);
    }

    private SourceReference sourceReference(
            Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof Map<?, ?> rawReference)) {
            throw new McpToolExecutionException("MCP_INVALID_ARGUMENT");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> reference = (Map<String, Object>) rawReference;
        return new SourceReference(
                longArgument(reference, "kbId"),
                longArgument(reference, "documentId"),
                stringArgument(reference, "chunkId"));
    }

    private long longArgument(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof Number number)) {
            throw new McpToolExecutionException("MCP_INVALID_ARGUMENT");
        }
        return number.longValue();
    }

    private int intArgument(
            Map<String, Object> arguments, String name, int defaultValue) {
        Object value = arguments.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)) {
            throw new McpToolExecutionException("MCP_INVALID_ARGUMENT");
        }
        return number.intValue();
    }

    private float floatArgument(
            Map<String, Object> arguments, String name, float defaultValue) {
        Object value = arguments.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Number number)) {
            throw new McpToolExecutionException("MCP_INVALID_ARGUMENT");
        }
        return number.floatValue();
    }

    private String stringArgument(Map<String, Object> arguments, String name) {
        Object value = arguments.get(name);
        if (!(value instanceof String string)) {
            throw new McpToolExecutionException("MCP_INVALID_ARGUMENT");
        }
        return string;
    }

    private record SourceReference(long kbId, long documentId, String chunkId) {
    }
}

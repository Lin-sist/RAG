package com.enterprise.rag.admin.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Validates, serializes and byte-bounds dual structured/text tool results. */
final class McpResultMapper {

    private static final int MAX_RESOURCE_LINKS = 40;
    private static final int RESOURCE_LINK_OVERHEAD_BYTES = 96;

    private final ObjectMapper objectMapper;
    private final JsonSchemaValidator schemaValidator;
    private final int maxResultBytes;

    McpResultMapper(ObjectMapper objectMapper, int maxResultBytes) {
        this(objectMapper, McpJsonDefaults.getSchemaValidator(), maxResultBytes);
    }

    McpResultMapper(
            ObjectMapper objectMapper,
            JsonSchemaValidator schemaValidator,
            int maxResultBytes) {
        this.objectMapper = objectMapper;
        this.schemaValidator = schemaValidator;
        this.maxResultBytes = maxResultBytes;
    }

    McpSchema.CallToolResult success(String toolName, Map<String, Object> payload) {
        Map<String, Object> outputSchema = McpToolSpecifications.outputSchema(toolName);
        if (outputSchema == null || !schemaValidator.validate(outputSchema, payload).valid()) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException | RuntimeException exception) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }
        Set<String> resourceUris = new TreeSet<>();
        collectResourceUris(payload, resourceUris);
        long resultBytes = (long) json.getBytes(StandardCharsets.UTF_8).length * 2L;
        for (String resourceUri : resourceUris) {
            resultBytes += resourceUri.getBytes(StandardCharsets.UTF_8).length
                    + RESOURCE_LINK_OVERHEAD_BYTES;
        }
        if (resultBytes > maxResultBytes) {
            throw new McpToolExecutionException("MCP_RESULT_TOO_LARGE");
        }
        McpSchema.CallToolResult.Builder result = McpSchema.CallToolResult.builder()
                .structuredContent(payload)
                .addTextContent(json)
                .isError(false);
        resourceUris.forEach(resourceUri -> result.addContent(
                McpSchema.ResourceLink.builder()
                        .name("authorized-source")
                        .uri(resourceUri)
                        .description("Authorized bounded source")
                        .mimeType("text/plain; charset=utf-8")
                        .build()));
        return result.build();
    }

    McpSchema.CallToolResult error(String category) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(category)
                .isError(true)
                .build();
    }

    private void collectResourceUris(Object value, Set<String> resourceUris) {
        if (resourceUris.size() > MAX_RESOURCE_LINKS) {
            throw new McpToolExecutionException("MCP_RESULT_TOO_LARGE");
        }
        if (value instanceof Map<?, ?> map) {
            Object resourceUri = map.get("resourceUri");
            if (resourceUri instanceof String uri) {
                McpResourceUri parsed;
                try {
                    parsed = McpResourceUri.parse(uri);
                } catch (McpResourceUri.InvalidResourceUriException exception) {
                    throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
                }
                if (!(parsed instanceof McpResourceUri.Chunk)
                        || !parsed.uri().equals(uri)) {
                    throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
                }
                resourceUris.add(uri);
            }
            map.values().forEach(item -> collectResourceUris(item, resourceUris));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectResourceUris(item, resourceUris));
        }
        if (resourceUris.size() > MAX_RESOURCE_LINKS) {
            throw new McpToolExecutionException("MCP_RESULT_TOO_LARGE");
        }
    }
}

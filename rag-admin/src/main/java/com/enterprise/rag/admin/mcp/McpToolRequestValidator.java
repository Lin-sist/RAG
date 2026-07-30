package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;

import java.util.Map;
import java.util.Set;

/** Sanitized pre-dispatch validation for tools/call request shape and arguments. */
final class McpToolRequestValidator {

    private static final Set<String> PARAMETER_NAMES = Set.of("name", "arguments", "_meta");

    enum Outcome {
        VALID,
        INVALID,
        EXTERNAL_TOOLS_DISABLED
    }

    private final McpProperties properties;
    private final JsonSchemaValidator schemaValidator;

    McpToolRequestValidator(McpProperties properties) {
        this(properties, McpJsonDefaults.getSchemaValidator());
    }

    McpToolRequestValidator(
            McpProperties properties, JsonSchemaValidator schemaValidator) {
        this.properties = properties;
        this.schemaValidator = schemaValidator;
    }

    Outcome validate(Object rawParameters) {
        if (!(rawParameters instanceof Map<?, ?> parameters)
                || !PARAMETER_NAMES.containsAll(parameters.keySet())
                || !(parameters.get("name") instanceof String toolName)
                || !(parameters.get("arguments") instanceof Map<?, ?> rawArguments)) {
            return Outcome.INVALID;
        }
        Map<String, Object> schema = McpToolSpecifications.inputSchema(
                toolName, properties.getMaxQueryChars());
        if (schema == null) {
            return Outcome.INVALID;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) rawArguments;
        try {
            if (!schemaValidator.validate(schema, arguments).valid()) {
                return Outcome.INVALID;
            }
            return McpToolSpecifications.isExternalTool(toolName)
                    && !properties.isExternalToolsEnabled()
                    ? Outcome.EXTERNAL_TOOLS_DISABLED
                    : Outcome.VALID;
        } catch (RuntimeException exception) {
            return Outcome.INVALID;
        }
    }
}

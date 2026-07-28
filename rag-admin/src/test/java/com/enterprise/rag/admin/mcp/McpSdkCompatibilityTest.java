package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpSdkCompatibilityTest {

    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "$schema", "https://json-schema.org/draft/2020-12/schema",
            "type", "object",
            "properties", Map.of(
                    "query", Map.of("type", "string", "minLength", 1)),
            "required", List.of("query"),
            "additionalProperties", false);

    @Test
    void jackson2SchemaValidationWorksWithTheBootManagedRuntime() {
        JsonSchemaValidator validator = McpJsonDefaults.getSchemaValidator();

        assertTrue(validator.validateSchema(INPUT_SCHEMA).valid());
        assertTrue(validator.validate(INPUT_SCHEMA, Map.of("query", "safe synthetic query")).valid());
        assertFalse(validator.validate(INPUT_SCHEMA, Map.of("query", "", "tenantId", 2)).valid());
    }
}

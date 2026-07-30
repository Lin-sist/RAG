package com.enterprise.rag.admin.mcp;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpToolSchemaIdentityTest {

    static final String EXPECTED_SCHEMA_SHA256 =
            "44ce4cc851551057bbabfbdf3735b964de300ff691b2e73a612856e9fa64213f";

    @Test
    void fixedFourToolSchemasHaveAStableCanonicalHash() throws Exception {
        Map<String, Object> identity = new LinkedHashMap<>();
        for (String toolName : List.of(
                McpToolSpecifications.SEARCH,
                McpToolSpecifications.ASK,
                McpToolSpecifications.GET_CITATION,
                McpToolSpecifications.COMPARE_SOURCES)) {
            identity.put(toolName, Map.of(
                    "input", McpToolSpecifications.inputSchema(toolName),
                    "output", McpToolSpecifications.outputSchema(toolName)));
        }
        ObjectMapper canonicalMapper = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        byte[] canonical = canonicalMapper.writeValueAsBytes(identity);
        String actual = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(canonical));

        assertEquals(EXPECTED_SCHEMA_SHA256, actual,
                () -> "canonical schema bytes="
                        + new String(canonical, StandardCharsets.UTF_8));
    }
}

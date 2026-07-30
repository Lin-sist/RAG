package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = McpProtocolMvcTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rag.mcp.enabled=true",
                "rag.mcp.local-only=false",
                "rag.mcp.external-tools-enabled=true",
                "rag.mcp.max-chunk-bytes=10"
        })
class McpIndependentClientMvcTest {

    @LocalServerPort
    private int port;

    @Test
    void officialIndependentClientTraversesAllReadOnlyCapabilities() {
        HttpClientStreamableHttpTransport transport =
                HttpClientStreamableHttpTransport.builder(
                                "http://127.0.0.1:" + port)
                        .endpoint("/mcp")
                        .connectTimeout(Duration.ofSeconds(5))
                        .httpRequestCustomizer((builder, method, uri, body, context) ->
                                builder.header("X-C15-Test-Identity", "tenant-a"))
                        .build();
        try (McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("c15-independent-client", "1.0"))
                .initializationTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(5))
                .build()) {
            McpSchema.InitializeResult initialized = client.initialize();
            assertEquals("2025-11-25", initialized.protocolVersion());
            assertEquals("enterprise-rag-readonly", initialized.serverInfo().name());

            assertEquals(3, client.listResourceTemplates().resourceTemplates().size());
            assertEquals(Set.of(
                            "rag://knowledge-bases/1",
                            "rag://knowledge-bases/3",
                            "rag://knowledge-bases/5"),
                    client.listResources().resources().stream()
                            .map(McpSchema.Resource::uri)
                            .collect(Collectors.toSet()));
            assertEquals("application/json", text(client.readResource(
                    new McpSchema.ReadResourceRequest(
                            "rag://knowledge-bases/1/documents/2"))).mimeType());
            McpSchema.TextResourceContents chunk = text(client.readResource(
                    new McpSchema.ReadResourceRequest(
                            "rag://knowledge-bases/1/documents/2/chunks/1")));
            assertEquals("甲乙丙", chunk.text());
            assertEquals(true, chunk.meta().get("truncated"));

            Set<String> toolNames = client.listTools().tools().stream()
                    .map(McpSchema.Tool::name)
                    .collect(Collectors.toSet());
            assertEquals(Set.of(
                    "rag.search",
                    "rag.ask",
                    "rag.get-citation",
                    "rag.compare-sources"), toolNames);

            assertSuccessful(client.callTool(new McpSchema.CallToolRequest(
                    "rag.search", Map.of("kbId", 1, "query", "safe"))));
            assertSuccessful(client.callTool(new McpSchema.CallToolRequest(
                    "rag.ask", Map.of("kbId", 1, "question", "safe"))));
            assertSuccessful(client.callTool(new McpSchema.CallToolRequest(
                    "rag.get-citation",
                    Map.of("kbId", 1, "documentId", 2, "chunkId", "chunk-a"))));
            assertSuccessful(client.callTool(new McpSchema.CallToolRequest(
                    "rag.compare-sources",
                    Map.of(
                            "left", Map.of(
                                    "kbId", 1, "documentId", 2, "chunkId", "chunk-a"),
                            "right", Map.of(
                                    "kbId", 1, "documentId", 2, "chunkId", "chunk-b")))));

            assertThrows(RuntimeException.class, () -> client.callTool(
                    new McpSchema.CallToolRequest(
                            "rag.delete-document", Map.of("documentId", 2))));
        }
    }

    private McpSchema.TextResourceContents text(McpSchema.ReadResourceResult result) {
        assertEquals(1, result.contents().size());
        assertTrue(result.contents().get(0) instanceof McpSchema.TextResourceContents);
        return (McpSchema.TextResourceContents) result.contents().get(0);
    }

    private void assertSuccessful(McpSchema.CallToolResult result) {
        assertFalse(result.isError());
        assertFalse(result.content().isEmpty());
        assertTrue(result.structuredContent() instanceof Map<?, ?>);
    }
}

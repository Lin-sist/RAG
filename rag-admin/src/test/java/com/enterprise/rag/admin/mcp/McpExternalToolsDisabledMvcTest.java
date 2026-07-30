package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.service.RAGService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(
        classes = McpProtocolMvcTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rag.mcp.enabled=true",
                "rag.mcp.local-only=false",
                "rag.mcp.external-tools-enabled=false"
        })
class McpExternalToolsDisabledMvcTest {

    @LocalServerPort
    private int port;

    @Autowired
    private QueryEngine queryEngine;

    @Autowired
    private RAGService ragService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void disabledExternalToolsAreHiddenAndCannotBeCalledByName() throws Exception {
        JsonNode list = objectMapper.readTree(post("""
                {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
                """).body()).path("result").path("tools");
        Set<String> names = new HashSet<>();
        list.forEach(tool -> names.add(tool.path("name").asText()));
        assertEquals(Set.of("rag.get-citation", "rag.compare-sources"), names);

        JsonNode callResult = objectMapper.readTree(post("""
                {
                  "jsonrpc":"2.0",
                  "id":2,
                  "method":"tools/call",
                  "params":{
                    "name":"rag.search",
                    "arguments":{"kbId":1,"query":"synthetic disabled call"}
                  }
                }
                """).body()).path("result");
        assertTrue(callResult.path("isError").asBoolean());
        assertEquals("MCP_EXTERNAL_TOOLS_DISABLED",
                callResult.path("content").get(0).path("text").asText());
        verifyNoInteractions(queryEngine, ragService);
    }

    private HttpResponse<String> post(String requestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .header("X-C15-Test-Identity", "tenant-a")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return HttpClient.newHttpClient().send(
                request, HttpResponse.BodyHandlers.ofString());
    }
}

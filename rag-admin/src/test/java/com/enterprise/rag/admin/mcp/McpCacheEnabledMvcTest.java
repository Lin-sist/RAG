package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.service.RAGService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = McpProtocolMvcTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rag.mcp.enabled=true",
                "rag.mcp.local-only=false",
                "rag.mcp.external-tools-enabled=true",
                "rag.mcp.cache-enabled=true"
        })
class McpCacheEnabledMvcTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RAGService ragService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private QAHistoryService qaHistoryService;

    @Test
    void serverOwnedCacheToggleIsPassedWithoutAuthoritativeSideEffects() throws Exception {
        clearInvocations(ragService, knowledgeBaseService, qaHistoryService);

        JsonNode result = post("""
                {
                  "jsonrpc":"2.0",
                  "id":1,
                  "method":"tools/call",
                  "params":{
                    "name":"rag.ask",
                    "arguments":{"kbId":1,"question":"safe synthetic question"}
                  }
                }
                """).path("result");

        assertFalse(result.path("isError").asBoolean(true), result.toString());
        assertTrue(result.path("structuredContent")
                .path("diagnostics").path("cacheHit").asBoolean());
        ArgumentCaptor<QARequest> request = ArgumentCaptor.forClass(QARequest.class);
        verify(ragService).ask(request.capture());
        assertTrue(request.getValue().enableCache());
        assertEquals(1L, request.getValue().scope().tenantId());
        assertEquals(1L, request.getValue().scope().knowledgeBaseId());
        assertTrue(request.getValue().filter().isEmpty());
        verify(knowledgeBaseService, never()).incrementQueryCount(anyLong(), anyLong());
        verify(qaHistoryService, never()).save(any(), any());
    }

    private JsonNode post(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return objectMapper.readTree(response.body());
    }
}

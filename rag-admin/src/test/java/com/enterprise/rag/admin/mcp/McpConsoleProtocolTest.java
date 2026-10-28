package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.service.RAGService;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = McpProtocolMvcTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"rag.mcp.enabled=true", "rag.mcp.local-only=true",
                "rag.mcp.allowed-origins=http://127.0.0.1:5188", "rag.mcp.external-tools-enabled=false"})
class McpConsoleProtocolTest {
    @LocalServerPort int port;
    @Autowired QueryEngine query;
    @Autowired RAGService rag;
    @Autowired KnowledgeBaseService knowledgeBases;
    @Autowired DocumentService documents;
    @Autowired QAHistoryService history;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private HttpResponse<String> send(String method, Integer id, Map<String, Object> params, String origin) throws Exception {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("jsonrpc", "2.0");
        if (id != null) body.put("id", id);
        body.put("method", method);
        if (params != null) body.put("params", params);
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream").header("Origin", origin)
                .header("MCP-Protocol-Version", McpCatalog.TARGET_PROTOCOL)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void fiveBrowserMethodsTraverseRealServletAndSdkWithoutBusinessServices() throws Exception {
        clearInvocations(query, rag, knowledgeBases, documents, history);
        String origin = "http://127.0.0.1:5188";
        var initialize = send("initialize", 1, Map.of("protocolVersion", McpCatalog.TARGET_PROTOCOL, "capabilities", Map.of(),
                "clientInfo", Map.of("name", "rag-browser-check", "version", "1.0")), origin);
        assertEquals(200, initialize.statusCode());
        assertTrue(initialize.headers().firstValue("MCP-Session-Id").isEmpty());
        assertEquals(McpCatalog.TARGET_PROTOCOL, mapper.readTree(initialize.body()).path("result").path("protocolVersion").asText());
        var notification = send("notifications/initialized", null, null, origin);
        assertEquals(202, notification.statusCode());
        assertTrue(notification.body().isEmpty());
        var ping = send("ping", 2, Map.of(), origin);
        assertEquals(200, ping.statusCode());
        assertTrue(mapper.readTree(ping.body()).has("result"));
        var tools = send("tools/list", 3, Map.of(), origin);
        assertEquals(2, mapper.readTree(tools.body()).path("result").path("tools").size());
        var templates = send("resources/templates/list", 4, Map.of(), origin);
        assertEquals(3, mapper.readTree(templates.body()).path("result").path("resourceTemplates").size());
        verifyNoInteractions(query, rag, knowledgeBases, documents, history);
    }

    @Test void untrustedBrowserOriginIsStillRejectedBeforeDiscovery() throws Exception {
        var response = send("tools/list", 1, Map.of(), "http://untrusted.invalid");
        assertEquals(403, response.statusCode());
        assertFalse(response.body().contains("rag.get-citation"));
    }
}

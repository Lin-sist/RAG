package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.ratelimit.RateLimitResult;
import com.enterprise.rag.common.ratelimit.RateLimiter;
import com.enterprise.rag.core.rag.model.QARequest;
import com.enterprise.rag.core.rag.model.QAResponse;
import com.enterprise.rag.core.rag.model.Citation;
import com.enterprise.rag.core.rag.model.RetrievedContext;
import com.enterprise.rag.core.rag.query.QueryEngine;
import com.enterprise.rag.core.rag.query.RetrievalResult;
import com.enterprise.rag.core.rag.service.RAGService;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = McpProtocolMvcTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "rag.mcp.enabled=true",
                "rag.mcp.local-only=false",
                "rag.mcp.allowed-origins=https://trusted.example",
                "rag.mcp.resource-page-size=2",
                "rag.mcp.max-chunk-bytes=10",
                "rag.mcp.external-tools-enabled=true"
        })
class McpProtocolMvcTest {

    private static final String INITIALIZE_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "method": "initialize",
              "params": {
                "protocolVersion": "2025-11-25",
                "capabilities": {},
                "clientInfo": {
                  "name": "c15-transport-test",
                  "version": "1.0"
                }
              }
            }
            """;

    private static final String LIST_RESOURCE_TEMPLATES_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 2,
              "method": "resources/templates/list",
              "params": {}
            }
            """;

    private static final String LIST_RESOURCES_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 3,
              "method": "resources/list",
              "params": {}
            }
            """;

    private static final String READ_KNOWLEDGE_BASE_RESOURCE_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 7,
              "method": "resources/read",
              "params": {"uri": "rag://knowledge-bases/1"}
            }
            """;

    private static final String LIST_TOOLS_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": 19,
              "method": "tools/list",
              "params": {}
            }
            """;

    @LocalServerPort
    private int port;

    @org.springframework.beans.factory.annotation.Autowired
    private RAGService syntheticRagService;

    @org.springframework.beans.factory.annotation.Autowired
    private KnowledgeBaseService syntheticKnowledgeBaseService;

    @org.springframework.beans.factory.annotation.Autowired
    private DocumentService syntheticDocumentService;

    @org.springframework.beans.factory.annotation.Autowired
    private QAHistoryService syntheticQaHistoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enabledSessionlessTransportNegotiatesTargetProtocol() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertFalse(response.headers().firstValue("MCP-Session-Id").isPresent());

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals("2.0", body.path("jsonrpc").asText());
        assertEquals(1, body.path("id").asInt());
        assertEquals("2025-11-25", body.path("result").path("protocolVersion").asText());
        assertEquals("enterprise-rag-readonly",
                body.path("result").path("serverInfo").path("name").asText());
        assertEquals("c15-v1",
                body.path("result").path("serverInfo").path("version").asText());
        assertFalse(body.path("result").path("capabilities").path("resources").isMissingNode());
        assertFalse(body.path("result").path("capabilities").path("tools").isMissingNode());
    }

    @Test
    void toolsListDeclaresExactlyFourStrictReadOnlyTools() throws Exception {
        HttpResponse<String> response = postJson(LIST_TOOLS_REQUEST, "tenant-a");

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.path("error").isMissingNode(), response.body());
        JsonNode tools = body.path("result").path("tools");
        assertEquals(4, tools.size());
        Set<String> names = new HashSet<>();
        tools.forEach(tool -> {
            names.add(tool.path("name").asText());
            JsonNode schema = tool.path("inputSchema");
            assertEquals("https://json-schema.org/draft/2020-12/schema",
                    schema.path("$schema").asText());
            assertEquals("object", schema.path("type").asText());
            assertFalse(schema.path("additionalProperties").asBoolean(true));
            assertTrue(tool.path("annotations").path("readOnlyHint").asBoolean());
            assertFalse(tool.path("annotations").path("destructiveHint").asBoolean(true));
            assertTrue(tool.path("annotations").path("idempotentHint").asBoolean());
            assertFalse(tool.path("annotations").path("openWorldHint").asBoolean(true));
            assertTrue(tool.path("execution").isMissingNode());
        });
        assertEquals(Set.of(
                "rag.search",
                "rag.ask",
                "rag.get-citation",
                "rag.compare-sources"), names);
    }

    @Test
    void toolsCallRejectsUnknownArgumentsWithAStableProtocolError() throws Exception {
        String marker = "client-tenant-selector-canary";
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 20,
                  "method": "tools/call",
                  "params": {
                    "name": "rag.search",
                    "arguments": {
                      "kbId": 1,
                      "query": "safe synthetic query",
                      "tenantId": "%s"
                    }
                  }
                }
                """.formatted(marker);

        JsonNode error = objectMapper.readTree(postJson(request, "tenant-a").body())
                .path("error");

        assertEquals(-32602, error.path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT", error.path("message").asText());
        assertFalse(error.toString().contains(marker));
        assertFalse(error.toString().contains("tenantId"));
    }

    @Test
    void getCitationReturnsAnExactBoundedReadOnlySource() throws Exception {
        String arguments = """
                {"kbId":1,"documentId":2,"chunkId":"chunk-a"}
                """;

        JsonNode result = objectMapper.readTree(postJson(
                callToolRequest(21, "rag.get-citation", arguments), "tenant-a").body())
                .path("result");

        assertFalse(result.path("isError").asBoolean(true), result.toString());
        JsonNode structured = result.path("structuredContent");
        assertEquals("ok", structured.path("status").asText());
        assertEquals(1L, structured.path("kbId").asLong());
        assertEquals(2L, structured.path("documentId").asLong());
        assertEquals("chunk-a", structured.path("chunkId").asText());
        assertEquals(0, structured.path("chunkIndex").asInt());
        assertEquals("tenant-a-document", structured.path("title").asText());
        assertEquals("alpha", structured.path("boundedContent").asText());
        assertFalse(structured.path("truncated").asBoolean());
        assertEquals("rag://knowledge-bases/1/documents/2/chunks/0",
                structured.path("resourceUri").asText());
        assertEquals(structured,
                objectMapper.readTree(result.path("content").get(0).path("text").asText()));
        assertFalse(result.toString().contains("tenantId"));
        assertFalse(result.toString().contains("uploaderId"));
        assertFalse(result.toString().contains("metadata-canary"));
    }

    @Test
    void compareSourcesAuthorizesAndReturnsBothSidesWithoutSemanticInference()
            throws Exception {
        String arguments = """
                {
                  "left":{"kbId":1,"documentId":2,"chunkId":"chunk-a"},
                  "right":{"kbId":1,"documentId":2,"chunkId":"chunk-b"}
                }
                """;

        JsonNode result = objectMapper.readTree(postJson(
                callToolRequest(22, "rag.compare-sources", arguments), "tenant-a").body())
                .path("result");

        assertFalse(result.path("isError").asBoolean(true), result.toString());
        JsonNode structured = result.path("structuredContent");
        assertEquals("chunk-a", structured.path("left").path("chunkId").asText());
        assertEquals("chunk-b", structured.path("right").path("chunkId").asText());
        assertTrue(structured.path("sameKnowledgeBase").asBoolean());
        assertTrue(structured.path("sameDocument").asBoolean());
        assertFalse(structured.path("sameChunk").asBoolean(true));
        assertEquals("NOT_PERFORMED",
                structured.path("semanticComparisonStatus").asText());
        assertFalse(result.toString().contains("similarity"));
        assertFalse(result.toString().contains("contradiction"));
    }

    @Test
    void citationAndCompareUseOpaqueMatchedControlsForForeignOrMismatchedSources()
            throws Exception {
        JsonNode foreignCitation = objectMapper.readTree(postJson(
                callToolRequest(
                        221,
                        "rag.get-citation",
                        "{\"kbId\":4,\"documentId\":44,\"chunkId\":\"foreign-canary\"}"),
                "tenant-a").body()).path("result");
        JsonNode nonexistentCitation = objectMapper.readTree(postJson(
                callToolRequest(
                        222,
                        "rag.get-citation",
                        "{\"kbId\":8,\"documentId\":88,\"chunkId\":\"missing-control\"}"),
                "tenant-a").body()).path("result");
        assertEquals(nonexistentCitation, foreignCitation);
        assertTrue(foreignCitation.path("isError").asBoolean());
        assertEquals("MCP_RESOURCE_NOT_FOUND",
                foreignCitation.path("content").get(0).path("text").asText());
        assertFalse(foreignCitation.toString().contains("foreign-canary"));

        JsonNode mismatchedDocument = objectMapper.readTree(postJson(
                callToolRequest(
                        223,
                        "rag.get-citation",
                        "{\"kbId\":1,\"documentId\":3,\"chunkId\":\"chunk-a\"}"),
                "tenant-a").body()).path("result");
        JsonNode missingDocument = objectMapper.readTree(postJson(
                callToolRequest(
                        224,
                        "rag.get-citation",
                        "{\"kbId\":1,\"documentId\":99,\"chunkId\":\"chunk-a\"}"),
                "tenant-a").body()).path("result");
        assertEquals(missingDocument, mismatchedDocument);

        JsonNode compareWithForeignRight = objectMapper.readTree(postJson(
                callToolRequest(
                        225,
                        "rag.compare-sources",
                        "{\"left\":{\"kbId\":1,\"documentId\":2,\"chunkId\":\"chunk-a\"},"
                                + "\"right\":{\"kbId\":4,\"documentId\":44,"
                                + "\"chunkId\":\"foreign-canary\"}}"),
                "tenant-a").body()).path("result");
        assertTrue(compareWithForeignRight.path("isError").asBoolean());
        assertEquals("MCP_RESOURCE_NOT_FOUND",
                compareWithForeignRight.path("content").get(0).path("text").asText());
        assertFalse(compareWithForeignRight.toString().contains("foreign-canary"));
        assertFalse(compareWithForeignRight.toString().contains("alpha"));
    }

    @Test
    void searchReturnsOnlyGroundedBoundedItemsAndDiagnosticsWhitelist() throws Exception {
        String arguments = """
                {"kbId":1,"query":"safe synthetic query","topK":2,"minScore":0.4}
                """;

        JsonNode result = objectMapper.readTree(postJson(
                callToolRequest(23, "rag.search", arguments), "tenant-a").body())
                .path("result");

        assertFalse(result.path("isError").asBoolean(true), result.toString());
        JsonNode structured = result.path("structuredContent");
        assertEquals("ok", structured.path("status").asText());
        assertEquals(1, structured.path("resultCount").asInt());
        JsonNode item = structured.path("items").get(0);
        assertEquals(1, item.path("rank").asInt());
        assertEquals(2L, item.path("documentId").asLong());
        assertEquals("chunk-a", item.path("chunkId").asText());
        assertEquals("tenant-a-document", item.path("documentTitle").asText());
        assertEquals("tenant-a.pdf", item.path("sourceFileName").asText());
        assertEquals("retrieved alpha", item.path("boundedExcerpt").asText());
        assertEquals("rag://knowledge-bases/1/documents/2/chunks/0",
                item.path("resourceUri").asText());
        assertEquals("deterministic",
                structured.path("diagnostics").path("rerankEffectiveProvider").asText());
        assertEquals(0,
                structured.path("diagnostics").path("rerankModelCallCount").asInt());
        assertFalse(structured.path("diagnostics").has("rawProviderCanary"));
        assertEquals(structured,
                objectMapper.readTree(result.path("content").get(0).path("text").asText()));
        assertFalse(result.toString().contains("safe synthetic query"));
        assertFalse(result.toString().contains("raw-metadata-canary"));
    }

    @Test
    void askUsesTheReadOnlyFacadeAndReturnsOnlyGroundedCitations() throws Exception {
        org.mockito.Mockito.clearInvocations(
                syntheticRagService,
                syntheticKnowledgeBaseService,
                syntheticQaHistoryService);
        String arguments = """
                {"kbId":1,"question":"safe synthetic question","topK":2,"minScore":0.4}
                """;

        JsonNode result = objectMapper.readTree(postJson(
                callToolRequest(24, "rag.ask", arguments), "tenant-a").body())
                .path("result");

        assertFalse(result.path("isError").asBoolean(true), result.toString());
        JsonNode structured = result.path("structuredContent");
        assertEquals("ok", structured.path("status").asText());
        assertEquals("synthetic grounded answer", structured.path("answer").asText());
        JsonNode citation = structured.path("citations").get(0);
        assertEquals(2L, citation.path("documentId").asLong());
        assertEquals("chunk-a", citation.path("chunkId").asText());
        assertEquals("tenant-a-document", citation.path("title").asText());
        assertEquals("bounded citation", citation.path("boundedSnippet").asText());
        assertEquals("rag://knowledge-bases/1/documents/2/chunks/0",
                citation.path("resourceUri").asText());
        assertFalse(structured.path("diagnostics").path("cacheHit").asBoolean(true));
        assertEquals(0,
                structured.path("diagnostics").path("generationModelCallCount").asInt());
        assertFalse(structured.path("diagnostics").has("rawGenerationCanary"));
        assertEquals(structured,
                objectMapper.readTree(result.path("content").get(0).path("text").asText()));
        assertFalse(result.toString().contains("safe synthetic question"));
        assertFalse(result.toString().contains("raw-context-canary"));

        org.mockito.ArgumentCaptor<QARequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(QARequest.class);
        org.mockito.Mockito.verify(syntheticRagService).ask(requestCaptor.capture());
        QARequest actualRequest = requestCaptor.getValue();
        assertEquals(1L, actualRequest.scope().tenantId());
        assertEquals(1L, actualRequest.scope().knowledgeBaseId());
        assertFalse(actualRequest.enableCache());
        assertFalse(actualRequest.stream());
        assertTrue(actualRequest.filter().isEmpty());
        org.mockito.Mockito.verify(syntheticKnowledgeBaseService,
                org.mockito.Mockito.never()).incrementQueryCount(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
        org.mockito.Mockito.verify(syntheticQaHistoryService,
                org.mockito.Mockito.never()).save(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void searchAndAskPreserveNoResultAndDependencyFailureSemantics() throws Exception {
        org.mockito.Mockito.clearInvocations(
                syntheticRagService,
                syntheticKnowledgeBaseService,
                syntheticQaHistoryService);

        JsonNode searchNoResult = objectMapper.readTree(postJson(
                callToolRequest(
                        25,
                        "rag.search",
                        "{\"kbId\":1,\"query\":\"no-result\"}"),
                "tenant-a").body()).path("result");
        assertFalse(searchNoResult.path("isError").asBoolean(true));
        assertEquals("no_result",
                searchNoResult.path("structuredContent").path("status").asText());
        assertEquals(0,
                searchNoResult.path("structuredContent").path("resultCount").asInt());

        JsonNode askNoResult = objectMapper.readTree(postJson(
                callToolRequest(
                        26,
                        "rag.ask",
                        "{\"kbId\":1,\"question\":\"no-result\"}"),
                "tenant-a").body()).path("result");
        assertFalse(askNoResult.path("isError").asBoolean(true));
        assertEquals("no_result",
                askNoResult.path("structuredContent").path("status").asText());
        assertEquals("NO_RESULT",
                askNoResult.path("structuredContent").path("errorCategory").asText());

        JsonNode searchFailure = objectMapper.readTree(postJson(
                callToolRequest(
                        27,
                        "rag.search",
                        "{\"kbId\":1,\"query\":\"dependency-failure\"}"),
                "tenant-a").body()).path("result");
        assertTrue(searchFailure.path("isError").asBoolean());
        assertEquals("MCP_DEPENDENCY_UNAVAILABLE",
                searchFailure.path("content").get(0).path("text").asText());

        JsonNode askFailure = objectMapper.readTree(postJson(
                callToolRequest(
                        28,
                        "rag.ask",
                        "{\"kbId\":1,\"question\":\"generation-error\"}"),
                "tenant-a").body()).path("result");
        assertTrue(askFailure.path("isError").asBoolean());
        assertEquals("MCP_DEPENDENCY_UNAVAILABLE",
                askFailure.path("content").get(0).path("text").asText());
        assertFalse(searchFailure.toString().contains("vector-provider-canary"));
        assertFalse(askFailure.toString().contains("generation-provider-canary"));

        org.mockito.Mockito.verify(syntheticKnowledgeBaseService,
                org.mockito.Mockito.never()).incrementQueryCount(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
        org.mockito.Mockito.verify(syntheticQaHistoryService,
                org.mockito.Mockito.never()).save(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resourcesAndAllToolsNeverInvokeAuthoritativeWriteMethods() throws Exception {
        org.mockito.Mockito.clearInvocations(
                syntheticKnowledgeBaseService,
                syntheticDocumentService,
                syntheticQaHistoryService,
                syntheticRagService);

        postJson(LIST_RESOURCES_REQUEST, "tenant-a");
        postJson(readKnowledgeBaseRequest(29, 1L), "tenant-a");
        postJson(readResourceRequest(30, "rag://knowledge-bases/1/documents/2"), "tenant-a");
        postJson(readResourceRequest(
                31, "rag://knowledge-bases/1/documents/2/chunks/0"), "tenant-a");
        postJson(callToolRequest(
                32,
                "rag.get-citation",
                "{\"kbId\":1,\"documentId\":2,\"chunkId\":\"chunk-a\"}"),
                "tenant-a");
        postJson(callToolRequest(
                33,
                "rag.compare-sources",
                "{\"left\":{\"kbId\":1,\"documentId\":2,\"chunkId\":\"chunk-a\"},"
                        + "\"right\":{\"kbId\":1,\"documentId\":2,\"chunkId\":\"chunk-b\"}}"),
                "tenant-a");
        postJson(callToolRequest(
                34, "rag.search", "{\"kbId\":1,\"query\":\"safe\"}"),
                "tenant-a");
        postJson(callToolRequest(
                35, "rag.ask", "{\"kbId\":1,\"question\":\"safe\"}"),
                "tenant-a");

        assertNoMethodsInvoked(syntheticKnowledgeBaseService, Set.of(
                "create", "update", "delete", "updateDocumentCount", "incrementQueryCount"));
        assertNoMethodsInvoked(syntheticDocumentService, Set.of(
                "create", "updateStatus", "updateChunkCount", "updateContentHash",
                "updateInputState", "delete", "deleteByKnowledgeBaseId", "saveChunks"));
        assertNoMethodsInvoked(syntheticQaHistoryService, Set.of(
                "save", "delete", "deleteAll"));
    }

    @Test
    void guessedWriteToolNameFailsAtTheProtocolBoundary() throws Exception {
        String marker = "delete-authoritative-canary";
        String request = callToolRequest(
                36,
                "rag.delete-document",
                "{\"documentId\":2,\"marker\":\"" + marker + "\"}");

        JsonNode error = objectMapper.readTree(postJson(request, "tenant-a").body())
                .path("error");

        assertEquals(-32602, error.path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT", error.path("message").asText());
        assertFalse(error.toString().contains(marker));
    }

    @Test
    void statelessTransportRejectsGetWithoutCreatingASession() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        assertFalse(response.headers().firstValue("MCP-Session-Id").isPresent());
    }

    @Test
    void invalidOriginIsRejectedBeforeProtocolHandling() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Origin", "https://attacker.example")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(INITIALIZE_REQUEST))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(403, response.statusCode());
        assertTrue(response.body().contains("MCP_FORBIDDEN"));
    }

    @Test
    void authenticatedClientDiscoversExactlyThreeCanonicalResourceTemplates() throws Exception {
        HttpResponse<String> response = postJson(LIST_RESOURCE_TEMPLATES_REQUEST);

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(2, body.path("id").asInt());
        List<String> uriTemplates = body.path("result").path("resourceTemplates")
                .findValuesAsText("uriTemplate");
        assertEquals(Set.of(
                "rag://knowledge-bases/{kbId}",
                "rag://knowledge-bases/{kbId}/documents/{documentId}",
                "rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}"),
                Set.copyOf(uriTemplates));
        assertEquals(3, uriTemplates.size());
        assertTrue(body.path("result").path("nextCursor").isMissingNode()
                || body.path("result").path("nextCursor").isNull());
    }

    @Test
    void resourcesListReauthorizesAnOpaqueCursorForEveryRequestIdentity() throws Exception {
        HttpResponse<String> tenantAResponse = postJson(
                LIST_RESOURCES_REQUEST, "tenant-a");

        assertEquals(200, tenantAResponse.statusCode());
        JsonNode tenantABody = objectMapper.readTree(tenantAResponse.body());
        assertEquals(List.of(
                        "rag://knowledge-bases/1",
                        "rag://knowledge-bases/3"),
                tenantABody.path("result").path("resources")
                        .findValuesAsText("uri"));
        String cursor = tenantABody.path("result").path("nextCursor").asText();
        assertFalse(cursor.isBlank());

        String tenantBRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": 4,
                  "method": "resources/list",
                  "params": {"cursor": "%s"}
                }
                """.formatted(cursor);
        HttpResponse<String> tenantBResponse = postJson(
                tenantBRequest, "tenant-b");

        assertEquals(200, tenantBResponse.statusCode());
        JsonNode tenantBBody = objectMapper.readTree(tenantBResponse.body());
        assertEquals(List.of(
                        "rag://knowledge-bases/4",
                        "rag://knowledge-bases/6"),
                tenantBBody.path("result").path("resources")
                        .findValuesAsText("uri"));
        assertFalse(tenantBResponse.body().contains("tenant-a-next-canary"));
    }

    @Test
    void resourcesListRejectsReservedOrMalformedPaginationInputWithoutEchoingIt() throws Exception {
        String marker = "foreign-cursor-marker";
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 5,
                  "method": "resources/list",
                  "params": {
                    "cursor": "%s",
                    "tenantId": 2
                  }
                }
                """.formatted(marker);

        HttpResponse<String> response = postJson(request, "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(-32602, body.path("error").path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT", body.path("error").path("message").asText());
        assertFalse(response.body().contains(marker));
        assertFalse(response.body().contains("tenantId"));
    }

    @Test
    void resourcesListRejectsANonCanonicalCursorAsInvalidArgument() throws Exception {
        String marker = "not-a-canonical-cursor";
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 6,
                  "method": "resources/list",
                  "params": {"cursor": "%s"}
                }
                """.formatted(marker);

        HttpResponse<String> response = postJson(request, "tenant-a");

        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(-32602, body.path("error").path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT", body.path("error").path("message").asText());
        assertFalse(response.body().contains(marker));
    }

    @Test
    void resourcesReadReturnsOnlyTheAuthorizedKnowledgeBaseJsonWhitelist() throws Exception {
        HttpResponse<String> response = postJson(
                READ_KNOWLEDGE_BASE_RESOURCE_REQUEST, "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(7, body.path("id").asInt());
        assertTrue(body.path("error").isMissingNode(), response.body());
        JsonNode resource = body.path("result").path("contents").get(0);
        assertEquals("rag://knowledge-bases/1", resource.path("uri").asText());
        assertEquals("application/json", resource.path("mimeType").asText());

        JsonNode content = objectMapper.readTree(resource.path("text").asText());
        Set<String> fieldNames = new HashSet<>();
        content.fieldNames().forEachRemaining(fieldNames::add);
        assertEquals(Set.of(
                "id",
                "name",
                "description",
                "documentCount",
                "isPublic",
                "createdAt",
                "updatedAt"), fieldNames);
        assertEquals(1L, content.path("id").asLong());
        assertEquals("tenant-a-one", content.path("name").asText());
        assertEquals("tenant-a-one description", content.path("description").asText());
        assertEquals(2, content.path("documentCount").asInt());
        assertFalse(content.path("isPublic").asBoolean());
        assertEquals("2026-07-28T09:15:00", content.path("createdAt").asText());
        assertEquals("2026-07-29T10:30:00", content.path("updatedAt").asText());
        assertFalse(response.body().contains("ownerId"));
        assertFalse(response.body().contains("vectorCollection"));
        assertFalse(response.body().contains("tenantId"));
    }

    @Test
    void resourcesReadMakesForeignAndNonexistentKnowledgeBasesIndistinguishable() throws Exception {
        HttpResponse<String> foreign = postJson(
                readKnowledgeBaseRequest(8, 1L), "tenant-b");
        HttpResponse<String> nonexistent = postJson(
                readKnowledgeBaseRequest(8, 99L), "tenant-a");

        assertEquals(200, foreign.statusCode());
        assertEquals(200, nonexistent.statusCode());
        JsonNode foreignError = objectMapper.readTree(foreign.body()).path("error");
        JsonNode nonexistentError = objectMapper.readTree(nonexistent.body()).path("error");
        assertEquals(nonexistentError, foreignError);
        assertEquals(-32603, foreignError.path("code").asInt());
        assertEquals("MCP_RESOURCE_NOT_FOUND", foreignError.path("message").asText());
        assertFalse(foreign.body().contains("tenant-a-one"));
        assertFalse(foreign.body().contains("知识库"));
        assertFalse(nonexistent.body().contains("知识库"));
    }

    @Test
    void resourcesReadReturnsAStableForbiddenErrorForSameTenantPrivateKnowledgeBase()
            throws Exception {
        HttpResponse<String> response = postJson(
                readKnowledgeBaseRequest(9, 9L), "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body()).path("error");
        assertEquals(-32603, error.path("code").asInt());
        assertEquals("MCP_FORBIDDEN", error.path("message").asText());
        assertFalse(response.body().contains("无权"));
        assertFalse(response.body().contains("ownerId"));
    }

    @Test
    void resourcesReadRejectsClientSuppliedTenantSelectors() throws Exception {
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 10,
                  "method": "resources/read",
                  "params": {
                    "uri": "rag://knowledge-bases/1",
                    "tenantId": 2
                  }
                }
                """;

        HttpResponse<String> response = postJson(request, "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertEquals(-32602, body.path("error").path("code").asInt());
        assertEquals("MCP_INVALID_ARGUMENT", body.path("error").path("message").asText());
        assertTrue(body.path("result").isMissingNode());
        assertFalse(response.body().contains("tenantId"));
    }

    @Test
    void resourcesReadSanitizesUnexpectedDependencyFailures() throws Exception {
        String canary = "sql-resource-read-canary";

        HttpResponse<String> response = postJson(
                readKnowledgeBaseRequest(11, 77L), "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body()).path("error");
        assertEquals(-32603, error.path("code").asInt());
        assertEquals("MCP_INTERNAL_ERROR", error.path("message").asText());
        assertFalse(response.body().contains(canary));
    }

    @Test
    void resourcesReadRejectsANonCanonicalUriWithoutEchoingIt() throws Exception {
        String marker = "query-resource-canary";
        String request = """
                {
                  "jsonrpc": "2.0",
                  "id": 12,
                  "method": "resources/read",
                  "params": {
                    "uri": "rag://knowledge-bases/1?marker=%s"
                  }
                }
                """.formatted(marker);

        HttpResponse<String> response = postJson(request, "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode error = objectMapper.readTree(response.body()).path("error");
        assertEquals(-32603, error.path("code").asInt());
        assertEquals("MCP_RESOURCE_NOT_FOUND", error.path("message").asText());
        assertFalse(response.body().contains(marker));
    }

    @Test
    void resourcesReadReturnsOnlyTheAuthorizedDocumentJsonWhitelist() throws Exception {
        HttpResponse<String> response = postJson(
                readResourceRequest(13, "rag://knowledge-bases/1/documents/2"),
                "tenant-a");

        assertEquals(200, response.statusCode());
        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.path("error").isMissingNode(), response.body());
        JsonNode resource = body.path("result").path("contents").get(0);
        assertEquals("rag://knowledge-bases/1/documents/2", resource.path("uri").asText());
        assertEquals("application/json", resource.path("mimeType").asText());

        JsonNode content = objectMapper.readTree(resource.path("text").asText());
        Set<String> fieldNames = new HashSet<>();
        content.fieldNames().forEachRemaining(fieldNames::add);
        assertEquals(Set.of(
                "id",
                "kbId",
                "title",
                "fileType",
                "status",
                "chunkCount",
                "createdAt",
                "updatedAt"), fieldNames);
        assertEquals(2L, content.path("id").asLong());
        assertEquals(1L, content.path("kbId").asLong());
        assertEquals("tenant-a-document", content.path("title").asText());
        assertEquals("pdf", content.path("fileType").asText());
        assertEquals("INDEXED", content.path("status").asText());
        assertEquals(3, content.path("chunkCount").asInt());
        assertEquals("2026-07-28T11:45:00", content.path("createdAt").asText());
        assertEquals("2026-07-29T12:30:00", content.path("updatedAt").asText());
        assertFalse(response.body().contains("tenantId"));
        assertFalse(response.body().contains("uploaderId"));
        assertFalse(response.body().contains("private-storage-canary"));
        assertFalse(response.body().contains("private-content-hash-canary"));
        assertFalse(response.body().contains("inputSha256"));
        assertFalse(response.body().contains("deleted"));
        assertFalse(response.body().contains("version"));
    }

    @Test
    void resourcesReadMakesForeignNonexistentAndMismatchedDocumentsIndistinguishable()
            throws Exception {
        HttpResponse<String> foreign = postJson(
                readResourceRequest(14, "rag://knowledge-bases/1/documents/2"),
                "tenant-b");
        HttpResponse<String> nonexistent = postJson(
                readResourceRequest(14, "rag://knowledge-bases/1/documents/99"),
                "tenant-a");
        HttpResponse<String> mismatched = postJson(
                readResourceRequest(14, "rag://knowledge-bases/1/documents/3"),
                "tenant-a");

        JsonNode foreignError = objectMapper.readTree(foreign.body()).path("error");
        JsonNode nonexistentError = objectMapper.readTree(nonexistent.body()).path("error");
        JsonNode mismatchedError = objectMapper.readTree(mismatched.body()).path("error");
        assertEquals(nonexistentError, foreignError);
        assertEquals(nonexistentError, mismatchedError);
        assertEquals(-32603, foreignError.path("code").asInt());
        assertEquals("MCP_RESOURCE_NOT_FOUND", foreignError.path("message").asText());
        assertFalse(foreign.body().contains("tenant-a-document"));
        assertFalse(mismatched.body().contains("tenant-a-private"));
    }

    @Test
    void resourcesReadAuthorizesTheKnowledgeBaseBeforeReadingItsDocument() throws Exception {
        HttpResponse<String> response = postJson(
                readResourceRequest(15, "rag://knowledge-bases/9/documents/2"),
                "tenant-a");

        JsonNode error = objectMapper.readTree(response.body()).path("error");
        assertEquals(-32603, error.path("code").asInt());
        assertEquals("MCP_FORBIDDEN", error.path("message").asText());
        assertFalse(response.body().contains("tenant-a-document"));
    }

    @Test
    void resourcesReadReturnsAnAuthorizedChunkAsBoundedUtf8Text() throws Exception {
        HttpResponse<String> response = postJson(
                readResourceRequest(16, "rag://knowledge-bases/1/documents/2/chunks/0"),
                "tenant-a");

        JsonNode body = objectMapper.readTree(response.body());
        assertTrue(body.path("error").isMissingNode(), response.body());
        JsonNode resource = body.path("result").path("contents").get(0);
        assertEquals(
                "rag://knowledge-bases/1/documents/2/chunks/0",
                resource.path("uri").asText());
        assertEquals("text/plain; charset=utf-8", resource.path("mimeType").asText());
        assertEquals("alpha", resource.path("text").asText());
        assertTrue(resource.path("_meta").isMissingNode());
        assertFalse(response.body().contains("vector-canary"));
        assertFalse(response.body().contains("metadata-canary"));
        assertFalse(response.body().contains("tenantId"));
    }

    @Test
    void resourcesReadTruncatesOnlyAtACompleteUtf8CodePoint() throws Exception {
        HttpResponse<String> response = postJson(
                readResourceRequest(17, "rag://knowledge-bases/1/documents/2/chunks/1"),
                "tenant-a");

        JsonNode resource = objectMapper.readTree(response.body())
                .path("result").path("contents").get(0);
        String text = resource.path("text").asText();
        assertEquals("甲乙丙", text);
        assertTrue(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 10);
        assertTrue(resource.path("_meta").path("truncated").asBoolean());
    }

    @Test
    void resourcesReadMakesForeignNonexistentAndMismatchedChunksIndistinguishable()
            throws Exception {
        HttpResponse<String> foreign = postJson(
                readResourceRequest(18, "rag://knowledge-bases/1/documents/2/chunks/0"),
                "tenant-b");
        HttpResponse<String> nonexistent = postJson(
                readResourceRequest(18, "rag://knowledge-bases/1/documents/2/chunks/99"),
                "tenant-a");
        HttpResponse<String> mismatched = postJson(
                readResourceRequest(18, "rag://knowledge-bases/1/documents/2/chunks/2"),
                "tenant-a");

        JsonNode foreignError = objectMapper.readTree(foreign.body()).path("error");
        JsonNode nonexistentError = objectMapper.readTree(nonexistent.body()).path("error");
        JsonNode mismatchedError = objectMapper.readTree(mismatched.body()).path("error");
        assertEquals(nonexistentError, foreignError);
        assertEquals(nonexistentError, mismatchedError);
        assertEquals(-32603, foreignError.path("code").asInt());
        assertEquals("MCP_RESOURCE_NOT_FOUND", foreignError.path("message").asText());
        assertFalse(foreign.body().contains("alpha"));
        assertFalse(mismatched.body().contains("vector-canary"));
    }

    private String readResourceRequest(int requestId, String uri) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "resources/read",
                  "params": {"uri": "%s"}
                }
                """.formatted(requestId, uri);
    }

    private String callToolRequest(int requestId, String toolName, String arguments) {
        return """
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "method": "tools/call",
                  "params": {"name":"%s","arguments":%s}
                }
                """.formatted(requestId, toolName, arguments);
    }

    private String readKnowledgeBaseRequest(int requestId, long knowledgeBaseId) {
        return readResourceRequest(
                requestId, "rag://knowledge-bases/" + knowledgeBaseId);
    }

    private HttpResponse<String> postJson(String requestBody) throws Exception {
        return postJson(requestBody, null);
    }

    private HttpResponse<String> postJson(String requestBody, String testIdentity) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/mcp"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream");
        if (testIdentity != null) {
            builder.header("X-C15-Test-Identity", testIdentity);
        }
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertNoMethodsInvoked(Object mock, Set<String> forbiddenMethodNames) {
        Set<String> invokedForbidden = new HashSet<>();
        org.mockito.Mockito.mockingDetails(mock).getInvocations().forEach(invocation -> {
            String methodName = invocation.getMethod().getName();
            if (forbiddenMethodNames.contains(methodName)) {
                invokedForbidden.add(methodName);
            }
        });
        assertTrue(invokedForbidden.isEmpty(), invokedForbidden.toString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            SecurityAutoConfiguration.class
    })
    @Import({McpServerConfiguration.class, CurrentUserService.class})
    static class TestApplication {

        @Bean
        FilterRegistrationBean<Filter> syntheticAuthenticatedPrincipalFilter() {
            Filter filter = (request, response, chain) -> {
                boolean tenantB = "tenant-b".equals(
                        ((HttpServletRequest) request).getHeader("X-C15-Test-Identity"));
                UserPrincipal principal = UserPrincipal.builder()
                        .id(tenantB ? 2L : 1L)
                        .tenantId(tenantB ? 2L : 1L)
                        .username(tenantB ? "tenant-b" : "tenant-a")
                        .enabled(true)
                        .roles(Set.of("USER"))
                        .build();
                Principal authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {
                        @Override
                        public Principal getUserPrincipal() {
                            return authentication;
                        }
                    }, response);
            };
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
            registration.setName("syntheticMcpPrincipalFilter");
            registration.addUrlPatterns(McpServerConfiguration.ENDPOINT);
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
            return registration;
        }

        @Bean
        KnowledgeBaseService syntheticKnowledgeBaseService() {
            KnowledgeBaseService service = org.mockito.Mockito.mock(KnowledgeBaseService.class);
            org.mockito.Mockito.when(service.getAccessibleByIdentity(org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(invocation -> {
                        RequestIdentity identity = invocation.getArgument(0, RequestIdentity.class);
                        if (identity.tenantId() == 2L) {
                            return List.of(
                                    knowledgeBase(2L, "tenant-b-before-cursor"),
                                    knowledgeBase(4L, "tenant-b-four"),
                                    knowledgeBase(6L, "tenant-b-six"));
                        }
                        return List.of(
                                knowledgeBase(1L, "tenant-a-one"),
                                knowledgeBase(3L, "tenant-a-three"),
                                knowledgeBase(5L, "tenant-a-next-canary"));
                    });
            org.mockito.Mockito.when(service.getById(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.any(RequestIdentity.class)))
                    .thenAnswer(invocation -> {
                        long id = invocation.getArgument(0, Long.class);
                        RequestIdentity identity = invocation.getArgument(1, RequestIdentity.class);
                        if (identity.tenantId() == 1L && id == 77L) {
                            throw new IllegalStateException("sql-resource-read-canary");
                        }
                        if (identity.tenantId() != 1L || (id != 1L && id != 9L)) {
                            return Optional.empty();
                        }
                        return Optional.of(id == 1L
                                ? knowledgeBase(1L, "tenant-a-one")
                                : knowledgeBase(9L, "tenant-a-private"));
                    });
            org.mockito.Mockito.when(service.requireReadyVectorScope(1L, 1L))
                    .thenReturn(new TenantVectorScope(1L, 1L, "tenant-a-vector"));
            return service;
        }

        @Bean
        AuthorizationService syntheticAuthorizationService(
                KnowledgeBaseService knowledgeBaseService,
                QAHistoryService qaHistoryService) {
            return new AuthorizationService(
                    knowledgeBaseService,
                    org.mockito.Mockito.mock(KBPermissionService.class),
                    qaHistoryService);
        }

        @Bean
        QAHistoryService syntheticQaHistoryService() {
            return org.mockito.Mockito.mock(QAHistoryService.class);
        }

        @Bean
        RateLimiter syntheticRateLimiter() {
            RateLimiter rateLimiter = org.mockito.Mockito.mock(RateLimiter.class);
            org.mockito.Mockito.when(rateLimiter.tryAcquire(
                            org.mockito.ArgumentMatchers.any(),
                            org.mockito.ArgumentMatchers.anyString(),
                            org.mockito.ArgumentMatchers.any()))
                    .thenReturn(RateLimitResult.allowed(100L, 100L));
            return rateLimiter;
        }

        @Bean
        DocumentService syntheticDocumentService() {
            DocumentService service = org.mockito.Mockito.mock(DocumentService.class);
            org.mockito.Mockito.when(service.getById(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong()))
                    .thenAnswer(invocation -> {
                        long tenantId = invocation.getArgument(0, Long.class);
                        long documentId = invocation.getArgument(1, Long.class);
                        if (tenantId != 1L || (documentId != 2L && documentId != 3L)) {
                            return Optional.empty();
                        }
                        Document document = new Document();
                        document.setId(2L);
                        document.setTenantId(1L);
                        document.setKbId(documentId == 2L ? 1L : 9L);
                        document.setUploaderId(1L);
                        document.setTitle("tenant-a-document");
                        document.setFileType("pdf");
                        document.setStatus("INDEXED");
                        document.setChunkCount(3);
                        document.setFilePath("private-storage-canary");
                        document.setInputSha256("private-input-sha-canary");
                        document.setContentHash("private-content-hash-canary");
                        document.setCreatedAt(LocalDateTime.of(2026, 7, 28, 11, 45));
                        document.setUpdatedAt(LocalDateTime.of(2026, 7, 29, 12, 30));
                        return Optional.of(document);
                    });
            org.mockito.Mockito.when(service.getChunkByIndex(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyInt()))
                    .thenAnswer(invocation -> {
                        long tenantId = invocation.getArgument(0, Long.class);
                        long documentId = invocation.getArgument(1, Long.class);
                        int chunkIndex = invocation.getArgument(2, Integer.class);
                        if (tenantId != 1L || documentId != 2L
                                || (chunkIndex != 0 && chunkIndex != 1 && chunkIndex != 2)) {
                            return Optional.empty();
                        }
                        DocumentChunk chunk = new DocumentChunk();
                        chunk.setTenantId(1L);
                        chunk.setDocumentId(2L);
                        chunk.setChunkIndex(chunkIndex == 2 ? 1 : chunkIndex);
                        chunk.setVectorId(chunkIndex == 0 ? "chunk-a" : "chunk-b");
                        chunk.setContent(chunkIndex == 0 ? "alpha" : "甲乙丙丁");
                        chunk.setStartPos(chunkIndex == 0 ? 0 : 5);
                        chunk.setEndPos(chunkIndex == 0 ? 5 : 9);
                        chunk.setMetadata("metadata-canary");
                        return Optional.of(chunk);
                    });
            org.mockito.Mockito.when(service.getChunkByVectorId(
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyLong(),
                            org.mockito.ArgumentMatchers.anyString()))
                    .thenAnswer(invocation -> {
                        long tenantId = invocation.getArgument(0, Long.class);
                        long documentId = invocation.getArgument(1, Long.class);
                        String chunkId = invocation.getArgument(2, String.class);
                        if (tenantId != 1L || documentId != 2L
                                || (!"chunk-a".equals(chunkId) && !"chunk-b".equals(chunkId))) {
                            return Optional.empty();
                        }
                        int chunkIndex = "chunk-a".equals(chunkId) ? 0 : 1;
                        DocumentChunk chunk = new DocumentChunk();
                        chunk.setTenantId(1L);
                        chunk.setDocumentId(2L);
                        chunk.setChunkIndex(chunkIndex);
                        chunk.setVectorId(chunkId);
                        chunk.setContent(chunkIndex == 0 ? "alpha" : "甲乙丙丁");
                        chunk.setStartPos(chunkIndex == 0 ? 0 : 5);
                        chunk.setEndPos(chunkIndex == 0 ? 5 : 9);
                        chunk.setMetadata("metadata-canary");
                        return Optional.of(chunk);
                    });
            return service;
        }

        @Bean
        QueryEngine syntheticQueryEngine() {
            QueryEngine queryEngine = org.mockito.Mockito.mock(QueryEngine.class);
            org.mockito.Mockito.when(queryEngine.retrieveWithDiagnostics(
                            org.mockito.ArgumentMatchers.anyString(),
                            org.mockito.ArgumentMatchers.any()))
                    .thenAnswer(invocation -> {
                        String query = invocation.getArgument(0, String.class);
                        if ("no-result".equals(query)) {
                            return RetrievalResult.complete(List.of());
                        }
                        if ("dependency-failure".equals(query)) {
                            throw VectorDependencyException.unavailable(
                                    "search",
                                    new IllegalStateException("vector-provider-canary"));
                        }
                        return new RetrievalResult(
                                List.of(new RetrievedContext(
                                        "retrieved alpha",
                                        "chunk-a",
                                        0.91f,
                                        Map.of(
                                                "documentId", 2L,
                                                "chunkId", "chunk-a",
                                                "sourceFileName", "C:\\private\\tenant-a.pdf",
                                                "rawMetadata", "raw-metadata-canary"))),
                                Map.of(
                                        "retrievalMode", "hybrid",
                                        "retrievalDegraded", false,
                                        "rerankRequestedProvider", "deterministic",
                                        "rerankEffectiveProvider", "deterministic",
                                        "rerankModelCallCount", 0,
                                        "rawProviderCanary", "private-provider-response"));
                    });
            return queryEngine;
        }

        @Bean
        RAGService syntheticRagService() {
            RAGService ragService = org.mockito.Mockito.mock(RAGService.class);
            org.mockito.Mockito.when(ragService.ask(org.mockito.ArgumentMatchers.any(QARequest.class)))
                    .thenAnswer(invocation -> {
                        QARequest request = invocation.getArgument(0, QARequest.class);
                        if ("no-result".equals(request.question())) {
                            return QAResponse.noResult(request.question());
                        }
                        if ("generation-error".equals(request.question())) {
                            return QAResponse.error(
                                    request.question(), "generation-provider-canary");
                        }
                        return QAResponse.success(
                            "safe synthetic question",
                            "synthetic grounded answer",
                            List.of(Citation.grounded(
                                    "chunk-a",
                                    "tenant-a.pdf",
                                    "untrusted-title-canary",
                                    2L,
                                    "chunk-a",
                                    0.91d,
                                    "bounded citation",
                                    0,
                                    5)),
                            List.of(new RetrievedContext(
                                    "raw-context-canary",
                                    "chunk-a",
                                    0.91f,
                                    Map.of("raw", "private"))),
                            Map.of(
                                    "status", "ok",
                                    "cacheHit", true,
                                    "citationCoverage", 1.0d,
                                    "generationRequestedProvider", "deterministic",
                                    "generationEffectiveProvider", "deterministic",
                                    "generationModelCallCount", 0,
                                    "rawGenerationCanary", "private-provider-body"));
                    });
            return ragService;
        }

        private KnowledgeBaseDTO knowledgeBase(long id, String name) {
            return KnowledgeBaseDTO.builder()
                    .id(id)
                    .name(name)
                    .description(name + " description")
                    .ownerId(id == 1L ? 1L : 9000L + id)
                    .vectorCollection("vector-canary-" + id)
                    .documentCount(id == 1L ? 2 : 0)
                    .isPublic(false)
                    .createdAt(LocalDateTime.of(2026, 7, 28, 9, 15))
                    .updatedAt(LocalDateTime.of(2026, 7, 29, 10, 30))
                    .build();
        }
    }
}

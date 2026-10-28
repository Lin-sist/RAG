package com.enterprise.rag.admin.mcp;

import java.util.List;
import java.util.Map;

/** Pure descriptions shared by integration metadata and the SDK registry. */
public final class McpCatalog {
    public static final String ENDPOINT = "/mcp";
    public static final String SERVER_NAME = "enterprise-rag-readonly";
    public static final String SERVER_VERSION = "c15-v1";
    public static final String TARGET_PROTOCOL = "2025-11-25";

    private McpCatalog() { }

    public record Tool(String name, String title, String description,
                       Map<String, Object> inputSchema, Map<String, Object> outputSchema,
                       boolean readOnly, boolean externalCallPossible) { }

    public record ResourceTemplate(String name, String title, String description,
                                   String uriTemplate, String mimeType) { }

    public static List<Tool> tools(int maxQueryChars) {
        return List.of(
                tool(McpToolSpecifications.SEARCH, "检索知识库",
                        "Search authorized knowledge-base evidence", maxQueryChars),
                tool(McpToolSpecifications.ASK, "基于知识库回答",
                        "Ask an authorized knowledge base without history/count side effects", maxQueryChars),
                tool(McpToolSpecifications.GET_CITATION, "读取指定引用",
                        "Read one exact authorized citation", maxQueryChars),
                tool(McpToolSpecifications.COMPARE_SOURCES, "并排查看两份来源",
                        "Compare two exact authorized sources without semantic inference", maxQueryChars));
    }

    private static Tool tool(String name, String title, String description, int maxQueryChars) {
        return new Tool(name, title, description, McpToolSpecifications.inputSchema(name, maxQueryChars),
                McpToolSpecifications.outputSchema(name), true, McpToolSpecifications.isExternalTool(name));
    }

    public static List<ResourceTemplate> resourceTemplates() {
        return List.of(
                new ResourceTemplate("knowledge-base", "Knowledge base", "Authorized knowledge-base metadata",
                        "rag://knowledge-bases/{kbId}", "application/json"),
                new ResourceTemplate("knowledge-base-document", "Knowledge-base document",
                        "Authorized document metadata within a knowledge base",
                        "rag://knowledge-bases/{kbId}/documents/{documentId}", "application/json"),
                new ResourceTemplate("knowledge-base-document-chunk", "Knowledge-base document chunk",
                        "Authorized bounded document chunk text",
                        "rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}",
                        "text/plain; charset=utf-8"));
    }
}

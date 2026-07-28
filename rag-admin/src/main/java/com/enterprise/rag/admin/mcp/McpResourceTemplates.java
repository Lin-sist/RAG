package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/** Fixed C15 resource discovery templates. */
final class McpResourceTemplates {

    private McpResourceTemplates() {
    }

    static List<McpStatelessServerFeatures.SyncResourceTemplateSpecification> specifications() {
        return List.of(
                specification(
                        "rag://knowledge-bases/{kbId}",
                        "knowledge-base",
                        "Knowledge base",
                        "Authorized knowledge-base metadata",
                        "application/json"),
                specification(
                        "rag://knowledge-bases/{kbId}/documents/{documentId}",
                        "knowledge-base-document",
                        "Knowledge-base document",
                        "Authorized document metadata within a knowledge base",
                        "application/json"),
                specification(
                        "rag://knowledge-bases/{kbId}/documents/{documentId}/chunks/{chunkIndex}",
                        "knowledge-base-document-chunk",
                        "Knowledge-base document chunk",
                        "Authorized bounded document chunk text",
                        "text/plain; charset=utf-8"));
    }

    private static McpStatelessServerFeatures.SyncResourceTemplateSpecification specification(
            String uriTemplate,
            String name,
            String title,
            String description,
            String mimeType) {
        McpSchema.ResourceTemplate template = McpSchema.ResourceTemplate.builder(uriTemplate, name)
                .title(title)
                .description(description)
                .mimeType(mimeType)
                .build();
        return new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                template,
                (context, request) -> {
                    throw new IllegalArgumentException("MCP_RESOURCE_NOT_FOUND");
                });
    }
}

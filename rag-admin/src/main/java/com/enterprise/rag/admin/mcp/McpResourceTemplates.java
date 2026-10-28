package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/** Fixed C15 resource discovery templates. */
final class McpResourceTemplates {

    private McpResourceTemplates() {
    }

    static List<McpStatelessServerFeatures.SyncResourceTemplateSpecification> specifications(
            McpKnowledgeResourceService resourceService,
            McpRequestIdentityResolver identityResolver) {
        return McpCatalog.resourceTemplates().stream()
                .map(definition -> specification(definition, resourceService, identityResolver)).toList();
    }

    private static McpStatelessServerFeatures.SyncResourceTemplateSpecification specification(
            McpCatalog.ResourceTemplate definition,
            McpKnowledgeResourceService resourceService,
            McpRequestIdentityResolver identityResolver) {
        McpSchema.ResourceTemplate template = McpSchema.ResourceTemplate.builder(definition.uriTemplate(), definition.name())
                .title(definition.title())
                .description(definition.description())
                .mimeType(definition.mimeType())
                .build();
        return new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                template,
                (context, request) -> resourceService.read(
                        identityResolver.requireContextIdentity(context), request.uri()));
    }
}

package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.security.RequestIdentity;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Base64;
import java.util.List;

/** Maps authorized knowledge bases to the bounded MCP Resource discovery shape. */
final class McpKnowledgeResourceService {

    private static final String CURSOR_VERSION = "v1:";

    private final KnowledgeBaseService knowledgeBaseService;
    private final int pageSize;

    McpKnowledgeResourceService(KnowledgeBaseService knowledgeBaseService, int pageSize) {
        if (pageSize <= 0 || pageSize > McpProperties.MAX_RESOURCE_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid MCP resource page size");
        }
        this.knowledgeBaseService = knowledgeBaseService;
        this.pageSize = pageSize;
    }

    McpSchema.ListResourcesResult list(RequestIdentity identity, String cursor) {
        long lastSeenKnowledgeBaseId = decodeCursor(cursor);
        List<KnowledgeBaseDTO> accessible = knowledgeBaseService.getAccessibleByIdentity(identity)
                .stream()
                .filter(knowledgeBase -> knowledgeBase.getId() > lastSeenKnowledgeBaseId)
                .sorted(Comparator.comparing(KnowledgeBaseDTO::getId))
                .limit((long) pageSize + 1)
                .toList();
        boolean hasNextPage = accessible.size() > pageSize;
        List<KnowledgeBaseDTO> page = hasNextPage
                ? accessible.subList(0, pageSize)
                : accessible;
        List<McpSchema.Resource> resources = page.stream()
                .map(this::toResource)
                .toList();
        String nextCursor = hasNextPage
                ? encodeCursor(page.get(page.size() - 1).getId())
                : null;
        return new McpSchema.ListResourcesResult(resources, nextCursor);
    }

    private String encodeCursor(long lastSeenKnowledgeBaseId) {
        byte[] value = (CURSOR_VERSION + lastSeenKnowledgeBaseId)
                .getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private long decodeCursor(String cursor) {
        if (cursor == null) {
            return 0;
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);
            if (!Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(cursor)) {
                throw new IllegalArgumentException();
            }
            String value = new String(decoded, StandardCharsets.UTF_8);
            if (!value.matches("v1:[1-9][0-9]*")) {
                throw new IllegalArgumentException();
            }
            return Long.parseLong(value.substring(CURSOR_VERSION.length()));
        } catch (IllegalArgumentException exception) {
            throw new InvalidResourceCursorException();
        }
    }

    private McpSchema.Resource toResource(KnowledgeBaseDTO knowledgeBase) {
        String uri = new McpResourceUri.KnowledgeBase(knowledgeBase.getId()).uri();
        return McpSchema.Resource.builder(uri, knowledgeBase.getName())
                .title(knowledgeBase.getName())
                .mimeType("application/json")
                .build();
    }

    static final class InvalidResourceCursorException extends IllegalArgumentException {

        private InvalidResourceCursorException() {
            super("Invalid MCP resource cursor");
        }
    }
}

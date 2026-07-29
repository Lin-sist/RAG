package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Base64;
import java.util.List;

/** Maps authorized knowledge bases to the bounded MCP Resource discovery shape. */
final class McpKnowledgeResourceService {

    private static final String CURSOR_VERSION = "v1:";
    private static final String JSON_MIME_TYPE = "application/json";

    private final KnowledgeBaseService knowledgeBaseService;
    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper;
    private final int pageSize;

    McpKnowledgeResourceService(
            KnowledgeBaseService knowledgeBaseService,
            AuthorizationService authorizationService,
            ObjectMapper objectMapper,
            int pageSize) {
        if (pageSize <= 0 || pageSize > McpProperties.MAX_RESOURCE_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid MCP resource page size");
        }
        this.knowledgeBaseService = knowledgeBaseService;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
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

    McpSchema.ReadResourceResult read(RequestIdentity identity, String rawUri) {
        McpResourceUri resourceUri;
        try {
            resourceUri = McpResourceUri.parse(rawUri);
        } catch (McpResourceUri.InvalidResourceUriException exception) {
            throw McpResourceReadException.notFound();
        }
        if (!(resourceUri instanceof McpResourceUri.KnowledgeBase knowledgeBaseUri)) {
            throw McpResourceReadException.notFound();
        }
        KnowledgeBaseDTO knowledgeBase = requireKnowledgeBaseReadAccess(
                knowledgeBaseUri.knowledgeBaseId(), identity);
        String content = writeJson(new KnowledgeBaseResourceContent(
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                knowledgeBase.getDocumentCount(),
                knowledgeBase.getIsPublic(),
                knowledgeBase.getCreatedAt(),
                knowledgeBase.getUpdatedAt()));
        McpSchema.TextResourceContents resourceContents = new McpSchema.TextResourceContents(
                knowledgeBaseUri.uri(), JSON_MIME_TYPE, content);
        return new McpSchema.ReadResourceResult(List.of(resourceContents));
    }

    private KnowledgeBaseDTO requireKnowledgeBaseReadAccess(
            long knowledgeBaseId, RequestIdentity identity) {
        try {
            return authorizationService.requireKnowledgeBaseReadAccess(
                    knowledgeBaseId, identity);
        } catch (BusinessException exception) {
            if (exception.getHttpStatus() == HttpStatus.NOT_FOUND) {
                throw McpResourceReadException.notFound();
            }
            if (exception.getHttpStatus() == HttpStatus.FORBIDDEN) {
                throw McpResourceReadException.forbidden();
            }
            throw McpResourceReadException.internalError();
        } catch (RuntimeException exception) {
            throw McpResourceReadException.internalError();
        }
    }

    private String writeJson(KnowledgeBaseResourceContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException | RuntimeException exception) {
            throw new IllegalStateException("MCP_INTERNAL_ERROR");
        }
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
                .mimeType(JSON_MIME_TYPE)
                .build();
    }

    private record KnowledgeBaseResourceContent(
            Long id,
            String name,
            String description,
            Integer documentCount,
            Boolean isPublic,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    static final class InvalidResourceCursorException extends IllegalArgumentException {

        private InvalidResourceCursorException() {
            super("Invalid MCP resource cursor");
        }
    }

    static final class McpResourceReadException extends IllegalArgumentException {

        private McpResourceReadException(String category) {
            super(category);
        }

        private static McpResourceReadException notFound() {
            return new McpResourceReadException("MCP_RESOURCE_NOT_FOUND");
        }

        private static McpResourceReadException forbidden() {
            return new McpResourceReadException("MCP_FORBIDDEN");
        }

        private static McpResourceReadException internalError() {
            return new McpResourceReadException("MCP_INTERNAL_ERROR");
        }
    }
}

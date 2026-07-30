package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.service.DocumentService;
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
import java.util.Map;

/** Maps authorized knowledge bases to the bounded MCP Resource discovery shape. */
final class McpKnowledgeResourceService {

    private static final String CURSOR_VERSION = "v1:";
    private static final String JSON_MIME_TYPE = "application/json";

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper;
    private final int pageSize;
    private final int maxChunkBytes;

    McpKnowledgeResourceService(
            KnowledgeBaseService knowledgeBaseService,
            DocumentService documentService,
            AuthorizationService authorizationService,
            ObjectMapper objectMapper,
            int pageSize,
            int maxChunkBytes) {
        if (pageSize <= 0 || pageSize > McpProperties.MAX_RESOURCE_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid MCP resource page size");
        }
        if (maxChunkBytes <= 0
                || maxChunkBytes > McpProperties.MAX_CONFIGURABLE_CHUNK_BYTES) {
            throw new IllegalArgumentException("Invalid MCP max chunk bytes");
        }
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.authorizationService = authorizationService;
        this.objectMapper = objectMapper;
        this.pageSize = pageSize;
        this.maxChunkBytes = maxChunkBytes;
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
        if (resourceUri instanceof McpResourceUri.KnowledgeBase knowledgeBaseUri) {
            return readKnowledgeBase(identity, knowledgeBaseUri);
        }
        if (resourceUri instanceof McpResourceUri.Document documentUri) {
            return readDocument(identity, documentUri);
        }
        if (resourceUri instanceof McpResourceUri.Chunk chunkUri) {
            return readChunk(identity, chunkUri);
        }
        throw McpResourceReadException.notFound();
    }

    private McpSchema.ReadResourceResult readKnowledgeBase(
            RequestIdentity identity, McpResourceUri.KnowledgeBase knowledgeBaseUri) {
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

    private McpSchema.ReadResourceResult readDocument(
            RequestIdentity identity, McpResourceUri.Document documentUri) {
        requireKnowledgeBaseReadAccess(documentUri.knowledgeBaseId(), identity);
        Document document = requireDocument(identity.tenantId(), documentUri);
        String content = writeJson(new DocumentResourceContent(
                document.getId(),
                document.getKbId(),
                document.getTitle(),
                document.getFileType(),
                document.getStatus(),
                document.getChunkCount(),
                document.getCreatedAt(),
                document.getUpdatedAt()));
        McpSchema.TextResourceContents resourceContents = new McpSchema.TextResourceContents(
                documentUri.uri(), JSON_MIME_TYPE, content);
        return new McpSchema.ReadResourceResult(List.of(resourceContents));
    }

    private Document requireDocument(
            long tenantId, McpResourceUri.Document documentUri) {
        try {
            return documentService.getById(tenantId, documentUri.documentId())
                    .filter(document -> document.getKbId() != null
                            && documentUri.knowledgeBaseId() == document.getKbId())
                    .orElseThrow(McpResourceReadException::notFound);
        } catch (McpResourceReadException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw McpResourceReadException.internalError();
        }
    }

    private McpSchema.ReadResourceResult readChunk(
            RequestIdentity identity, McpResourceUri.Chunk chunkUri) {
        requireKnowledgeBaseReadAccess(chunkUri.knowledgeBaseId(), identity);
        requireDocument(identity.tenantId(), new McpResourceUri.Document(
                chunkUri.knowledgeBaseId(), chunkUri.documentId()));
        DocumentChunk chunk = requireChunk(identity.tenantId(), chunkUri);
        McpUtf8Bounder.BoundedText bounded;
        try {
            bounded = McpUtf8Bounder.bound(chunk.getContent(), maxChunkBytes);
        } catch (IllegalArgumentException exception) {
            throw McpResourceReadException.internalError();
        }
        McpSchema.TextResourceContents resourceContents = bounded.truncated()
                ? new McpSchema.TextResourceContents(
                        chunkUri.uri(),
                        "text/plain; charset=utf-8",
                        bounded.text(),
                        Map.of("truncated", true))
                : new McpSchema.TextResourceContents(
                        chunkUri.uri(),
                        "text/plain; charset=utf-8",
                        bounded.text());
        return new McpSchema.ReadResourceResult(List.of(resourceContents));
    }

    private DocumentChunk requireChunk(
            long tenantId, McpResourceUri.Chunk chunkUri) {
        try {
            return documentService.getChunkByIndex(
                            tenantId, chunkUri.documentId(), chunkUri.chunkIndex())
                    .filter(chunk -> Long.valueOf(tenantId).equals(chunk.getTenantId()))
                    .filter(chunk -> Long.valueOf(chunkUri.documentId())
                            .equals(chunk.getDocumentId()))
                    .filter(chunk -> Integer.valueOf(chunkUri.chunkIndex())
                            .equals(chunk.getChunkIndex()))
                    .orElseThrow(McpResourceReadException::notFound);
        } catch (McpResourceReadException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw McpResourceReadException.internalError();
        }
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

    private String writeJson(Object content) {
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

    private record DocumentResourceContent(
            Long id,
            Long kbId,
            String title,
            String fileType,
            String status,
            Integer chunkCount,
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

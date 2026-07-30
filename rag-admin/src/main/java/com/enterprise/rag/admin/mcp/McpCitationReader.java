package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Exact tenant/KB/document/chunk citation reader shared by citation-oriented tools. */
final class McpCitationReader {

    private static final int MAX_TITLE_CHARS = 512;
    private static final int MAX_TOOL_CONTENT_BYTES = 8_192;

    private final AuthorizationService authorizationService;
    private final DocumentService documentService;
    private final int maxContentBytes;

    McpCitationReader(
            AuthorizationService authorizationService,
            DocumentService documentService,
            int maxChunkBytes) {
        this.authorizationService = authorizationService;
        this.documentService = documentService;
        this.maxContentBytes = Math.min(maxChunkBytes, MAX_TOOL_CONTENT_BYTES);
    }

    CitationSource read(
            RequestIdentity identity, long kbId, long documentId, String chunkId) {
        requireKnowledgeBaseReadAccess(kbId, identity);
        Document document = requireDocument(identity.tenantId(), kbId, documentId);
        DocumentChunk chunk = requireChunk(
                identity.tenantId(), documentId, chunkId);
        McpUtf8Bounder.BoundedText bounded;
        try {
            bounded = McpUtf8Bounder.bound(chunk.getContent(), maxContentBytes);
        } catch (IllegalArgumentException exception) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }
        return new CitationSource(
                kbId,
                documentId,
                chunkId,
                chunk.getChunkIndex(),
                boundChars(document.getTitle(), MAX_TITLE_CHARS),
                chunk.getStartPos() == null ? -1 : chunk.getStartPos(),
                chunk.getEndPos() == null ? -1 : chunk.getEndPos(),
                bounded.text(),
                bounded.truncated(),
                new McpResourceUri.Chunk(kbId, documentId, chunk.getChunkIndex()).uri());
    }

    private void requireKnowledgeBaseReadAccess(long kbId, RequestIdentity identity) {
        try {
            authorizationService.requireKnowledgeBaseReadAccess(kbId, identity);
        } catch (BusinessException exception) {
            if (exception.getHttpStatus() == HttpStatus.NOT_FOUND) {
                throw new McpToolExecutionException("MCP_RESOURCE_NOT_FOUND");
            }
            if (exception.getHttpStatus() == HttpStatus.FORBIDDEN) {
                throw new McpToolExecutionException("MCP_FORBIDDEN");
            }
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        } catch (RuntimeException exception) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }
    }

    private Document requireDocument(long tenantId, long kbId, long documentId) {
        try {
            return documentService.getById(tenantId, documentId)
                    .filter(document -> Long.valueOf(kbId).equals(document.getKbId()))
                    .orElseThrow(() -> new McpToolExecutionException(
                            "MCP_RESOURCE_NOT_FOUND"));
        } catch (McpToolExecutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }
    }

    private DocumentChunk requireChunk(long tenantId, long documentId, String chunkId) {
        try {
            return documentService.getChunkByVectorId(tenantId, documentId, chunkId)
                    .filter(chunk -> Long.valueOf(tenantId).equals(chunk.getTenantId()))
                    .filter(chunk -> Long.valueOf(documentId).equals(chunk.getDocumentId()))
                    .filter(chunk -> chunkId.equals(chunk.getVectorId()))
                    .filter(chunk -> chunk.getChunkIndex() != null && chunk.getChunkIndex() >= 0)
                    .orElseThrow(() -> new McpToolExecutionException(
                            "MCP_RESOURCE_NOT_FOUND"));
        } catch (McpToolExecutionException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new McpToolExecutionException("MCP_INTERNAL_ERROR");
        }
    }

    private String boundChars(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        int count = value.codePointCount(0, value.length());
        return count <= maxChars
                ? value
                : value.substring(0, value.offsetByCodePoints(0, maxChars));
    }

    record CitationSource(
            long kbId,
            long documentId,
            String chunkId,
            int chunkIndex,
            String title,
            int startPos,
            int endPos,
            String boundedContent,
            boolean truncated,
            String resourceUri) {
    }
}

package com.enterprise.rag.admin.mcp;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.RequestIdentity;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpCitationReaderTest {

    private static final RequestIdentity IDENTITY = new RequestIdentity(101L, 901L);

    @Test
    void exactReadAuthorizesKbBeforeTenantDocumentAndChunkIdentity() {
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        DocumentService documentService = mock(DocumentService.class);
        when(authorizationService.requireKnowledgeBaseReadAccess(7L, IDENTITY))
                .thenReturn(KnowledgeBaseDTO.builder().id(7L).build());
        Document document = new Document();
        document.setId(42L);
        document.setKbId(7L);
        document.setTitle("document");
        when(documentService.getById(901L, 42L)).thenReturn(Optional.of(document));
        DocumentChunk chunk = new DocumentChunk();
        chunk.setTenantId(901L);
        chunk.setDocumentId(42L);
        chunk.setVectorId("chunk-a");
        chunk.setChunkIndex(3);
        chunk.setContent("evidence");
        chunk.setStartPos(10);
        chunk.setEndPos(18);
        when(documentService.getChunkByVectorId(901L, 42L, "chunk-a"))
                .thenReturn(Optional.of(chunk));
        McpCitationReader reader = new McpCitationReader(
                authorizationService, documentService, 65_536);

        McpCitationReader.CitationSource source = reader.read(
                IDENTITY, 7L, 42L, "chunk-a");

        assertEquals("evidence", source.boundedContent());
        assertEquals("rag://knowledge-bases/7/documents/42/chunks/3",
                source.resourceUri());
        InOrder order = inOrder(authorizationService, documentService);
        order.verify(authorizationService).requireKnowledgeBaseReadAccess(7L, IDENTITY);
        order.verify(documentService).getById(901L, 42L);
        order.verify(documentService).getChunkByVectorId(901L, 42L, "chunk-a");
    }

    @Test
    void mismatchedDocumentAndMissingChunkShareTheNotFoundCategory() {
        AuthorizationService authorizationService = mock(AuthorizationService.class);
        DocumentService documentService = mock(DocumentService.class);
        when(authorizationService.requireKnowledgeBaseReadAccess(7L, IDENTITY))
                .thenReturn(KnowledgeBaseDTO.builder().id(7L).build());
        Document mismatch = new Document();
        mismatch.setId(42L);
        mismatch.setKbId(8L);
        when(documentService.getById(901L, 42L)).thenReturn(Optional.of(mismatch));
        when(documentService.getById(901L, 43L)).thenReturn(Optional.of(document(43L, 7L)));
        when(documentService.getChunkByVectorId(901L, 43L, "missing"))
                .thenReturn(Optional.empty());
        McpCitationReader reader = new McpCitationReader(
                authorizationService, documentService, 65_536);

        McpToolExecutionException mismatchError = assertThrows(
                McpToolExecutionException.class,
                () -> reader.read(IDENTITY, 7L, 42L, "chunk-a"));
        McpToolExecutionException missingError = assertThrows(
                McpToolExecutionException.class,
                () -> reader.read(IDENTITY, 7L, 43L, "missing"));

        assertEquals("MCP_RESOURCE_NOT_FOUND", mismatchError.category());
        assertEquals(mismatchError.category(), missingError.category());
    }

    private Document document(long id, long kbId) {
        Document document = new Document();
        document.setId(id);
        document.setKbId(kbId);
        return document;
    }
}

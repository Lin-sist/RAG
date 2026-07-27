package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.DocumentStatus;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.impl.KeywordIndexBootstrap;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KeywordIndexBootstrapTenantEnforcementTest {

    @Test
    void bootstrapLoadsDocumentsAndChunksThroughKnowledgeBaseTenantScope() {
        KnowledgeBaseMapper kbMapper = mock(KnowledgeBaseMapper.class);
        DocumentService documentService = mock(DocumentService.class);
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        KnowledgeBase kb = knowledgeBase(77L);
        Document document = document(77L);
        DocumentChunk chunk = chunk(77L);
        when(kbMapper.selectList(null)).thenReturn(List.of(kb));
        when(documentService.getByKnowledgeBaseId(77L, 10L)).thenReturn(List.of(document));
        when(documentService.getChunksByDocumentId(77L, 20L)).thenReturn(List.of(chunk));

        new KeywordIndexBootstrap(kbMapper, documentService, keywordIndex).rebuildKeywordIndex();

        verify(documentService).getByKnowledgeBaseId(77L, 10L);
        verify(documentService).getChunksByDocumentId(77L, 20L);
        verify(documentService, never()).getByKnowledgeBaseId(10L);
        verify(documentService, never()).getChunksByDocumentId(20L);
        verify(keywordIndex).rebuildCollection(eq("kb_vectors"), anyList());
    }

    @Test
    void bootstrapRejectsMapperReturnedDocumentFromAnotherTenant() {
        KnowledgeBaseMapper kbMapper = mock(KnowledgeBaseMapper.class);
        DocumentService documentService = mock(DocumentService.class);
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        when(kbMapper.selectList(null)).thenReturn(List.of(knowledgeBase(77L)));
        when(documentService.getByKnowledgeBaseId(77L, 10L)).thenReturn(List.of(document(88L)));

        new KeywordIndexBootstrap(kbMapper, documentService, keywordIndex).rebuildKeywordIndex();

        verify(keywordIndex, never()).rebuildCollection(eq("kb_vectors"), anyList());
    }

    private static KnowledgeBase knowledgeBase(long tenantId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(10L);
        kb.setTenantId(tenantId);
        kb.setVectorCollection("kb_vectors");
        return kb;
    }

    private static Document document(long tenantId) {
        Document document = new Document();
        document.setId(20L);
        document.setTenantId(tenantId);
        document.setKbId(10L);
        document.setStatus(DocumentStatus.COMPLETED.name());
        document.setTitle("tenant-document.md");
        return document;
    }

    private static DocumentChunk chunk(long tenantId) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setTenantId(tenantId);
        chunk.setDocumentId(20L);
        chunk.setVectorId("vector-1");
        chunk.setContent("content");
        chunk.setChunkIndex(0);
        chunk.setStartPos(0);
        chunk.setEndPos(7);
        return chunk;
    }
}

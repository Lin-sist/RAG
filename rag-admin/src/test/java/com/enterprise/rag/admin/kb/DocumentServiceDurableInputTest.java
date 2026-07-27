package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.impl.DocumentServiceImpl;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DocumentServiceDurableInputTest {

    @Test
    void canonicalDeleteRemovesDurableInputBeforeDeletingDocumentRecord() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentChunkMapper chunkMapper = mock(DocumentChunkMapper.class);
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        VectorStore vectorStore = mock(VectorStore.class);
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        IndexInputStore inputStore = mock(IndexInputStore.class);
        DocumentServiceImpl service = new DocumentServiceImpl(
                documentMapper, chunkMapper, knowledgeBaseMapper, vectorStore, keywordIndex, inputStore);
        Document document = new Document();
        document.setTenantId(77L);
        document.setId(9L);
        document.setKbId(10L);
        document.setFilePath("objects/v2/77/durable.bin");

        when(documentMapper.selectByTenantAndId(77L, 9L)).thenReturn(document);
        when(chunkMapper.selectByTenantAndDocumentId(77L, 9L)).thenReturn(List.of());
        when(inputStore.delete(77L, "objects/v2/77/durable.bin"))
                .thenReturn(IndexInputStore.DeleteResult.DELETED);
        when(documentMapper.deleteByTenantAndId(77L, 9L)).thenReturn(1);

        assertTrue(service.delete(77L, 9L));

        var order = inOrder(inputStore, documentMapper);
        order.verify(inputStore).delete(77L, "objects/v2/77/durable.bin");
        order.verify(documentMapper).deleteByTenantAndId(77L, 9L);
    }

    @Test
    void canonicalDeleteDoesNotDeleteDocumentRecordWhenInputCleanupFails() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentChunkMapper chunkMapper = mock(DocumentChunkMapper.class);
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        VectorStore vectorStore = mock(VectorStore.class);
        KeywordIndex keywordIndex = mock(KeywordIndex.class);
        IndexInputStore inputStore = mock(IndexInputStore.class);
        DocumentServiceImpl service = new DocumentServiceImpl(
                documentMapper, chunkMapper, knowledgeBaseMapper, vectorStore, keywordIndex, inputStore);
        Document document = new Document();
        document.setTenantId(77L);
        document.setId(9L);
        document.setKbId(10L);
        document.setFilePath("objects/v2/77/durable.bin");

        when(documentMapper.selectByTenantAndId(77L, 9L)).thenReturn(document);
        when(chunkMapper.selectByTenantAndDocumentId(77L, 9L)).thenReturn(List.of());
        when(inputStore.delete(77L, "objects/v2/77/durable.bin"))
                .thenReturn(IndexInputStore.DeleteResult.FAILED);

        assertThrows(com.enterprise.rag.common.exception.BusinessException.class, () -> service.delete(77L, 9L));

        verify(documentMapper, never()).deleteByTenantAndId(77L, 9L);
    }

    @Test
    void legacyUnscopedDeleteFailsClosedBeforeLoadingDocument() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentServiceImpl service = new DocumentServiceImpl(
                documentMapper,
                mock(DocumentChunkMapper.class),
                mock(KnowledgeBaseMapper.class),
                mock(VectorStore.class),
                mock(KeywordIndex.class),
                mock(IndexInputStore.class));

        assertThrows(IllegalStateException.class, () -> service.delete(9L));
        verify(documentMapper, never()).selectById(9L);
    }
}

package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.impl.DocumentServiceImpl;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTenantGuardTest {

    @Test
    void unscopedLookupShouldFailClosedWithoutMapperAccess() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentServiceImpl service = new DocumentServiceImpl(
                documentMapper,
                mock(DocumentChunkMapper.class),
                mock(KnowledgeBaseMapper.class),
                mock(VectorStore.class),
                mock(KeywordIndex.class),
                mock(IndexInputStore.class));

        assertThrows(IllegalStateException.class, () -> service.getById(7L));
        verify(documentMapper, never()).selectById(7L);
    }

    @Test
    void chunkLookupCarriesTenantDocumentAndIndexToTheMapper() {
        DocumentChunkMapper chunkMapper = mock(DocumentChunkMapper.class);
        DocumentChunk expected = new DocumentChunk();
        when(chunkMapper.selectByTenantAndDocumentIdAndIndex(901L, 42L, 3))
                .thenReturn(expected);
        DocumentServiceImpl service = new DocumentServiceImpl(
                mock(DocumentMapper.class),
                chunkMapper,
                mock(KnowledgeBaseMapper.class),
                mock(VectorStore.class),
                mock(KeywordIndex.class),
                mock(IndexInputStore.class));

        DocumentChunk actual = service.getChunkByIndex(901L, 42L, 3).orElseThrow();

        assertSame(expected, actual);
        verify(chunkMapper).selectByTenantAndDocumentIdAndIndex(901L, 42L, 3);
    }

    @Test
    void citationLookupCarriesTenantDocumentAndVectorIdentityToTheMapper() {
        DocumentChunkMapper chunkMapper = mock(DocumentChunkMapper.class);
        DocumentChunk expected = new DocumentChunk();
        when(chunkMapper.selectByTenantAndDocumentIdAndVectorId(
                901L, 42L, "chunk-a")).thenReturn(expected);
        DocumentServiceImpl service = new DocumentServiceImpl(
                mock(DocumentMapper.class),
                chunkMapper,
                mock(KnowledgeBaseMapper.class),
                mock(VectorStore.class),
                mock(KeywordIndex.class),
                mock(IndexInputStore.class));

        DocumentChunk actual = service.getChunkByVectorId(
                901L, 42L, "chunk-a").orElseThrow();

        assertSame(expected, actual);
        verify(chunkMapper).selectByTenantAndDocumentIdAndVectorId(
                901L, 42L, "chunk-a");
    }
}

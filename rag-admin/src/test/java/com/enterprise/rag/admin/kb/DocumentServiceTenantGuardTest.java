package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.impl.DocumentServiceImpl;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}

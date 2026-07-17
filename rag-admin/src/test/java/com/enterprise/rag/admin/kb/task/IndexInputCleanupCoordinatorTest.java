package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.storage.IndexInputState;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexInputCleanupCoordinatorTest {

    @Test
    void cleanupPendingDeletesOnlyDurableInputAndMarksCleaned() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentService documentService = mock(DocumentService.class);
        IndexInputStore inputStore = mock(IndexInputStore.class);
        Document document = new Document();
        document.setId(88L);
        document.setFilePath("objects/pending.bin");
        when(documentMapper.findCleanupPending(20)).thenReturn(List.of(document));
        when(inputStore.delete("objects/pending.bin")).thenReturn(IndexInputStore.DeleteResult.DELETED);

        new IndexInputCleanupCoordinator(documentMapper, documentService, inputStore, 20).reconcileOnce();

        verify(inputStore).delete("objects/pending.bin");
        verify(documentService).updateInputState(88L, IndexInputState.CLEANED.name());
    }
}

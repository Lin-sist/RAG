package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyIndexTaskReconciliationCoordinatorTest {

    @Test
    void legacyDocumentsWithoutLedgerAreAtomicallyQuarantined() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        Document pending = document(81L, "PENDING");
        Document failed = document(82L, "FAILED");
        when(documentMapper.findLegacyUnledgered(20)).thenReturn(List.of(pending, failed));

        new LegacyIndexTaskReconciliationCoordinator(documentMapper, 20).reconcileOnce();

        verify(documentMapper).quarantineLegacyUnledgered(81L);
        verify(documentMapper).quarantineLegacyUnledgered(82L);
    }

    private static Document document(long id, String status) {
        Document document = new Document();
        document.setId(id);
        document.setStatus(status);
        document.setInputState("AVAILABLE");
        return document;
    }
}

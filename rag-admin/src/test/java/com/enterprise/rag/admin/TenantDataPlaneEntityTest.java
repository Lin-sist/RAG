package com.enterprise.rag.admin;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.KBPermission;
import com.enterprise.rag.admin.kb.task.IndexTaskRecord;
import com.enterprise.rag.admin.qa.entity.QAFeedback;
import com.enterprise.rag.admin.qa.entity.QAHistory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantDataPlaneEntityTest {

    @Test
    void everyPersistedTenantDataPlaneEntityCarriesTenantId() {
        Document document = new Document();
        DocumentChunk chunk = new DocumentChunk();
        KBPermission permission = new KBPermission();
        QAHistory history = new QAHistory();
        QAFeedback feedback = new QAFeedback();
        IndexTaskRecord task = new IndexTaskRecord();

        document.setTenantId(41L);
        chunk.setTenantId(42L);
        permission.setTenantId(43L);
        history.setTenantId(44L);
        feedback.setTenantId(45L);
        task.setTenantId(46L);

        assertEquals(41L, document.getTenantId());
        assertEquals(42L, chunk.getTenantId());
        assertEquals(43L, permission.getTenantId());
        assertEquals(44L, history.getTenantId());
        assertEquals(45L, feedback.getTenantId());
        assertEquals(46L, task.getTenantId());
    }
}

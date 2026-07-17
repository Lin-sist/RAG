package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.mapper.IndexTaskMapper;
import com.enterprise.rag.document.chunker.DocumentChunkingProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlIndexTaskLedgerTest {

    @Test
    void onlyConditionalUpdateWinnerAcquiresLease() {
        IndexTaskMapper mapper = mock(IndexTaskMapper.class);
        MySqlIndexTaskLedger ledger = new MySqlIndexTaskLedger(mapper, new DocumentChunkingProperties());
        when(mapper.claim("task-1", "worker-a", 300, 3)).thenReturn(1);
        when(mapper.claim("task-1", "worker-b", 300, 3)).thenReturn(0);

        assertTrue(ledger.claim("task-1", "worker-a", 300, 3));
        assertFalse(ledger.claim("task-1", "worker-b", 300, 3));

        verify(mapper).claim("task-1", "worker-a", 300, 3);
        verify(mapper).claim("task-1", "worker-b", 300, 3);
    }
}

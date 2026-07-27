package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.mapper.IndexTaskMapper;
import com.enterprise.rag.document.chunker.DocumentChunkingProperties;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlIndexTaskLedgerTest {

    @Test
    void createAcceptedPersistsTenantScopeBeforeReturningTaskId() {
        IndexTaskMapper mapper = mock(IndexTaskMapper.class);
        DocumentChunkingProperties properties = new DocumentChunkingProperties();
        MySqlIndexTaskLedger ledger = new MySqlIndexTaskLedger(mapper, properties);

        String taskId = ledger.createAccepted(77L, 101L, 20L);

        ArgumentCaptor<IndexTaskRecord> recordCaptor = ArgumentCaptor.forClass(IndexTaskRecord.class);
        verify(mapper).insert(recordCaptor.capture());
        IndexTaskRecord record = recordCaptor.getValue();
        assertEquals(77L, record.getTenantId());
        assertEquals(101L, record.getDocumentId());
        assertEquals(20L, record.getOwnerId());
        assertEquals(taskId, record.getTaskId());
        assertEquals(IndexTaskStatus.ACCEPTED.name(), record.getStatus());
        assertEquals(IndexTaskPhase.ACCEPTED.name(), record.getExecutionPhase());
    }

    @Test
    void onlyConditionalUpdateWinnerAcquiresLease() {
        IndexTaskMapper mapper = mock(IndexTaskMapper.class);
        MySqlIndexTaskLedger ledger = new MySqlIndexTaskLedger(mapper, new DocumentChunkingProperties());
        when(mapper.claim(77L, "task-1", "worker-a", 300, 3)).thenReturn(1);
        when(mapper.claim(77L, "task-1", "worker-b", 300, 3)).thenReturn(0);

        assertTrue(ledger.claim(77L, "task-1", "worker-a", 300, 3));
        assertFalse(ledger.claim(77L, "task-1", "worker-b", 300, 3));

        verify(mapper).claim(77L, "task-1", "worker-a", 300, 3);
        verify(mapper).claim(77L, "task-1", "worker-b", 300, 3);
    }

    @Test
    void reconciliationTransitionIsNotRestrictedToVectorInFlight() {
        IndexTaskMapper mapper = mock(IndexTaskMapper.class);
        MySqlIndexTaskLedger ledger = new MySqlIndexTaskLedger(mapper, new DocumentChunkingProperties());
        when(mapper.markReconciliationRequired(
                77L, "task-contract-mismatch", "INDEX_CONTRACT_MISMATCH")).thenReturn(1);

        ledger.markReconciliationRequired(77L, "task-contract-mismatch", "INDEX_CONTRACT_MISMATCH");

        verify(mapper).markReconciliationRequired(
                77L, "task-contract-mismatch", "INDEX_CONTRACT_MISMATCH");
    }

    @Test
    void scanExcludesAttemptsAtConfiguredMaximum() {
        IndexTaskMapper mapper = mock(IndexTaskMapper.class);
        MySqlIndexTaskLedger ledger = new MySqlIndexTaskLedger(mapper, new DocumentChunkingProperties());
        when(mapper.scanClaimable(20, 3)).thenReturn(java.util.List.of());

        ledger.scanClaimable(20, 3);

        verify(mapper).scanClaimable(20, 3);
    }

    @Test
    void scanFailsClosedWhenMapperReturnsRecordWithoutTenantIdentity() {
        IndexTaskMapper mapper = mock(IndexTaskMapper.class);
        MySqlIndexTaskLedger ledger = new MySqlIndexTaskLedger(mapper, new DocumentChunkingProperties());
        IndexTaskRecord unsafe = new IndexTaskRecord();
        unsafe.setTaskId("task-missing-tenant");
        when(mapper.scanClaimable(20, 3)).thenReturn(java.util.List.of(unsafe));

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> ledger.scanClaimable(20, 3));

        assertEquals("TENANT_IDENTITY_REQUIRED", error.getMessage());
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedUnscopedFindFailsClosed() {
        IndexTaskMapper mapper = mock(IndexTaskMapper.class);
        MySqlIndexTaskLedger ledger = new MySqlIndexTaskLedger(mapper, new DocumentChunkingProperties());

        IllegalStateException error = assertThrows(
                IllegalStateException.class, () -> ledger.find("task-unscoped"));

        assertEquals("TENANT_IDENTITY_REQUIRED", error.getMessage());
    }

    @Test
    void tenantScopedLeaseAndRetryMutationsDelegateTenantIdentity() {
        IndexTaskMapper mapper = mock(IndexTaskMapper.class);
        MySqlIndexTaskLedger ledger = new MySqlIndexTaskLedger(mapper, new DocumentChunkingProperties());
        when(mapper.release(77L, "task-1", "worker-1")).thenReturn(1);
        when(mapper.heartbeat(77L, "task-1", "worker-1", 300)).thenReturn(1);
        when(mapper.markAttemptsExhausted(
                77L, "task-1", "worker-1", "INDEX_TASK_RECOVERY_FAILED")).thenReturn(1);
        when(mapper.scheduleRetry(
                77L, "task-1", "worker-1", "INDEX_TASK_RECOVERY_FAILED", 30)).thenReturn(1);

        assertTrue(ledger.release(77L, "task-1", "worker-1"));
        assertTrue(ledger.heartbeat(77L, "task-1", "worker-1", 300));
        assertTrue(ledger.markAttemptsExhausted(
                77L, "task-1", "worker-1", "INDEX_TASK_RECOVERY_FAILED"));
        assertTrue(ledger.scheduleRetry(
                77L, "task-1", "worker-1", "INDEX_TASK_RECOVERY_FAILED", 30));

        verify(mapper).release(77L, "task-1", "worker-1");
        verify(mapper).heartbeat(77L, "task-1", "worker-1", 300);
        verify(mapper).markAttemptsExhausted(
                77L, "task-1", "worker-1", "INDEX_TASK_RECOVERY_FAILED");
        verify(mapper).scheduleRetry(
                77L, "task-1", "worker-1", "INDEX_TASK_RECOVERY_FAILED", 30);
    }

    @Test
    void customMutationSqlAlwaysIncludesTenantPredicate() throws Exception {
        assertTenantPredicate("claim", long.class, String.class, String.class, int.class, int.class);
        assertTenantPredicate("release", long.class, String.class, String.class);
        assertTenantPredicate("heartbeat", long.class, String.class, String.class, int.class);
        assertTenantPredicate("markReconciliationRequired", long.class, String.class, String.class);
        assertTenantPredicate("markAttemptsExhausted", long.class, String.class, String.class, String.class);
        assertTenantPredicate("scheduleRetry",
                long.class, String.class, String.class, String.class, int.class);
    }

    private static void assertTenantPredicate(String methodName, Class<?>... parameterTypes)
            throws Exception {
        Update update = IndexTaskMapper.class.getMethod(methodName, parameterTypes)
                .getAnnotation(Update.class);
        assertTrue(String.join("\n", update.value()).contains("tenant_id = #{tenantId}"), methodName);
    }
}

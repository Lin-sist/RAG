package com.enterprise.rag.admin.kb.task;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexTaskReconciliationCoordinatorTest {

    @Test
    void vectorInFlightIsQuarantinedWithoutCallingRecoveryExecutor() {
        IndexTaskLedger ledger = mock(IndexTaskLedger.class);
        IndexTaskRecoveryExecutor recoveryExecutor = mock(IndexTaskRecoveryExecutor.class);
        IndexTaskReconciliationProperties properties = new IndexTaskReconciliationProperties();
        IndexTaskRecord record = new IndexTaskRecord();
        record.setTaskId("task-unknown");
        record.setExecutionPhase(IndexTaskPhase.VECTOR_IN_FLIGHT.name());
        when(ledger.scanClaimable(20)).thenReturn(List.of(record));
        when(ledger.claim("task-unknown", "worker-1", 300, 3)).thenReturn(true);

        IndexTaskReconciliationCoordinator coordinator = new IndexTaskReconciliationCoordinator(
                ledger, recoveryExecutor, properties, "worker-1");
        coordinator.reconcileOnce();

        verify(ledger).markReconciliationRequired(
                "task-unknown", "VECTOR_OPERATION_OUTCOME_UNKNOWN");
        verify(recoveryExecutor, never()).resume(record);
    }
}

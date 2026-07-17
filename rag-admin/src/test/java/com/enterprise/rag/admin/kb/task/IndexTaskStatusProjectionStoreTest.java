package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.common.async.TaskState;
import com.enterprise.rag.common.async.TaskStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexTaskStatusProjectionStoreTest {

    @Test
    void durableProjectionPreservesOwnerButExposesOnlySanitizedFailureCode() {
        IndexTaskLedger ledger = mock(IndexTaskLedger.class);
        IndexTaskRecord record = new IndexTaskRecord();
        record.setTaskId("task-1");
        record.setTaskType("DOCUMENT_INDEX");
        record.setOwnerId(42L);
        record.setStatus(IndexTaskStatus.RECONCILIATION_REQUIRED.name());
        record.setProgress(70);
        record.setFailureCode("VECTOR_OPERATION_OUTCOME_UNKNOWN");
        record.setErrorMessage("raw-provider-marker");
        when(ledger.find("task-1")).thenReturn(Optional.of(record));

        TaskStatus status = new IndexTaskStatusProjectionStore(ledger).find("task-1").orElseThrow();

        assertEquals(TaskState.FAILED, status.state());
        assertEquals(42L, status.ownerId());
        assertEquals("VECTOR_OPERATION_OUTCOME_UNKNOWN", status.error());
        assertFalse(status.toString().contains("raw-provider-marker"));
    }
}

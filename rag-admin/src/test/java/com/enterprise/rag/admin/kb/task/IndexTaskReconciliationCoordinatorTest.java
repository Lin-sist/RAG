package com.enterprise.rag.admin.kb.task;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        when(ledger.scanClaimable(20, 3)).thenReturn(List.of(record));
        when(ledger.claim("task-unknown", "worker-1", 300, 3)).thenReturn(true);

        IndexTaskReconciliationCoordinator coordinator = new IndexTaskReconciliationCoordinator(
                ledger, recoveryExecutor, properties, "worker-1");
        coordinator.reconcileOnce();

        verify(ledger).markReconciliationRequired(
                "task-unknown", "VECTOR_OPERATION_OUTCOME_UNKNOWN");
        verify(recoveryExecutor, never()).resume(eq(record), any(IndexTaskLeaseGuard.class));
    }

    @Test
    void resumeDisabledDoesNotClaimSafePhaseOrConsumeAttempt() {
        IndexTaskLedger ledger = mock(IndexTaskLedger.class);
        IndexTaskRecoveryExecutor recoveryExecutor = mock(IndexTaskRecoveryExecutor.class);
        IndexTaskReconciliationProperties properties = new IndexTaskReconciliationProperties();
        properties.setResumeEnabled(false);
        IndexTaskRecord record = record("task-disabled");
        when(ledger.scanClaimable(20, 3)).thenReturn(List.of(record));

        new IndexTaskReconciliationCoordinator(
                ledger, recoveryExecutor, properties, "worker-1").reconcileOnce();

        verify(ledger, never()).claim(anyString(), anyString(), eq(300), eq(3));
        verify(recoveryExecutor, never()).resume(any(), any());
    }

    @Test
    void failedRecoveryAtMaxAttemptBecomesTerminalInsteadOfBeingReleased() {
        IndexTaskLedger ledger = mock(IndexTaskLedger.class);
        IndexTaskRecoveryExecutor recoveryExecutor = mock(IndexTaskRecoveryExecutor.class);
        IndexTaskReconciliationProperties properties = new IndexTaskReconciliationProperties();
        properties.setResumeEnabled(true);
        properties.setMaxAttempts(3);
        IndexTaskRecord record = new IndexTaskRecord();
        record.setTaskId("task-exhausted");
        record.setExecutionPhase(IndexTaskPhase.SAFE_PRE_VECTOR.name());
        record.setAttemptCount(2);
        when(ledger.scanClaimable(20, 3)).thenReturn(List.of(record));
        when(ledger.claim("task-exhausted", "worker-1", 300, 3)).thenReturn(true);
        when(ledger.heartbeat("task-exhausted", "worker-1", 300)).thenReturn(true);
        doThrow(new IllegalStateException("raw provider detail"))
                .when(recoveryExecutor).resume(eq(record), any(IndexTaskLeaseGuard.class));

        new IndexTaskReconciliationCoordinator(
                ledger, recoveryExecutor, properties, "worker-1").reconcileOnce();

        verify(ledger).markAttemptsExhausted(
                "task-exhausted", "worker-1", "INDEX_TASK_RECOVERY_FAILED");
        verify(ledger, never()).release("task-exhausted", "worker-1");
    }

    @Test
    void retryableFailureUsesStableCodeAndDatabaseBackoff() {
        IndexTaskLedger ledger = mock(IndexTaskLedger.class);
        IndexTaskRecoveryExecutor recoveryExecutor = mock(IndexTaskRecoveryExecutor.class);
        IndexTaskReconciliationProperties properties = new IndexTaskReconciliationProperties();
        properties.setResumeEnabled(true);
        properties.setInitialBackoffSeconds(30);
        properties.setMaxBackoffSeconds(300);
        IndexTaskRecord record = new IndexTaskRecord();
        record.setTaskId("task-retry");
        record.setExecutionPhase(IndexTaskPhase.SAFE_PRE_VECTOR.name());
        record.setAttemptCount(0);
        when(ledger.scanClaimable(20, 3)).thenReturn(List.of(record));
        when(ledger.claim("task-retry", "worker-1", 300, 3)).thenReturn(true);
        when(ledger.heartbeat("task-retry", "worker-1", 300)).thenReturn(true);
        doThrow(new IllegalStateException("secret raw message"))
                .when(recoveryExecutor).resume(eq(record), any(IndexTaskLeaseGuard.class));

        new IndexTaskReconciliationCoordinator(
                ledger, recoveryExecutor, properties, "worker-1").reconcileOnce();

        verify(ledger).scheduleRetry(
                "task-retry", "worker-1", "INDEX_TASK_RECOVERY_FAILED", 30);
        verify(ledger, never()).release("task-retry", "worker-1");
    }

    @Test
    void longRecoveryRenewsLeaseAtConfiguredHeartbeatInterval() {
        IndexTaskLedger ledger = mock(IndexTaskLedger.class);
        IndexTaskRecoveryExecutor recoveryExecutor = mock(IndexTaskRecoveryExecutor.class);
        ScheduledExecutorService heartbeatScheduler = mock(ScheduledExecutorService.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> heartbeatFuture = mock(ScheduledFuture.class);
        AtomicReference<Runnable> heartbeatAction = new AtomicReference<>();
        IndexTaskReconciliationProperties properties = new IndexTaskReconciliationProperties();
        properties.setResumeEnabled(true);
        properties.setHeartbeatSeconds(60);
        IndexTaskRecord record = new IndexTaskRecord();
        record.setTaskId("task-long");
        record.setExecutionPhase(IndexTaskPhase.SAFE_PRE_VECTOR.name());
        record.setAttemptCount(0);
        when(ledger.scanClaimable(20, 3)).thenReturn(List.of(record));
        when(ledger.claim("task-long", "worker-1", 300, 3)).thenReturn(true);
        when(ledger.heartbeat("task-long", "worker-1", 300)).thenReturn(true);
        when(heartbeatScheduler.scheduleAtFixedRate(
                any(Runnable.class), eq(60L), eq(60L), eq(TimeUnit.SECONDS)))
                .thenAnswer(invocation -> {
                    heartbeatAction.set(invocation.getArgument(0));
                    return heartbeatFuture;
                });
        doAnswer(invocation -> {
            heartbeatAction.get().run();
            heartbeatAction.get().run();
            return null;
        }).when(recoveryExecutor).resume(eq(record), any(IndexTaskLeaseGuard.class));

        new IndexTaskReconciliationCoordinator(
                ledger, recoveryExecutor, properties, "worker-1",
                Runnable::run, heartbeatScheduler, false).reconcileOnce();

        verify(ledger, times(3)).heartbeat("task-long", "worker-1", 300);
        verify(heartbeatFuture).cancel(false);
    }

    @Test
    void productionExecutorNeverExceedsConfiguredConcurrency() throws Exception {
        IndexTaskLedger ledger = mock(IndexTaskLedger.class);
        IndexTaskRecoveryExecutor recoveryExecutor = mock(IndexTaskRecoveryExecutor.class);
        IndexTaskReconciliationProperties properties = new IndexTaskReconciliationProperties();
        properties.setResumeEnabled(true);
        properties.setConcurrency(2);
        properties.setBatchSize(3);
        List<IndexTaskRecord> records = List.of(
                record("task-a"), record("task-b"), record("task-c"));
        when(ledger.scanClaimable(3, 3)).thenReturn(records);
        when(ledger.claim(anyString(), anyString(), eq(300), eq(3))).thenReturn(true);
        when(ledger.heartbeat(anyString(), anyString(), eq(300))).thenReturn(true);
        CountDownLatch firstTwoStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(3);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximumActive = new AtomicInteger();
        doAnswer(invocation -> {
            int nowActive = active.incrementAndGet();
            maximumActive.accumulateAndGet(nowActive, Math::max);
            firstTwoStarted.countDown();
            assertTrue(release.await(2, TimeUnit.SECONDS));
            active.decrementAndGet();
            completed.countDown();
            return null;
        }).when(recoveryExecutor).resume(any(IndexTaskRecord.class), any(IndexTaskLeaseGuard.class));
        IndexTaskReconciliationCoordinator coordinator =
                new IndexTaskReconciliationCoordinator(ledger, recoveryExecutor, properties);

        try {
            coordinator.reconcileOnce();
            assertTrue(firstTwoStarted.await(2, TimeUnit.SECONDS));
            assertEquals(2, maximumActive.get());
            release.countDown();
            assertTrue(completed.await(2, TimeUnit.SECONDS));
            assertEquals(2, maximumActive.get());
        } finally {
            release.countDown();
            coordinator.shutdownExecutors();
        }
    }

    private static IndexTaskRecord record(String taskId) {
        IndexTaskRecord record = new IndexTaskRecord();
        record.setTaskId(taskId);
        record.setExecutionPhase(IndexTaskPhase.SAFE_PRE_VECTOR.name());
        record.setAttemptCount(0);
        return record;
    }
}

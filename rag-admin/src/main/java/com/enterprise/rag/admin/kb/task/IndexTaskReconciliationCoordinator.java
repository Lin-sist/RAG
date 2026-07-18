package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

/**
 * 有界扫描并分类 durable index tasks。分类与真正 resume 分开。
 */
@Slf4j
@Component
public class IndexTaskReconciliationCoordinator {

    static final String RECOVERY_FAILURE_CODE = "INDEX_TASK_RECOVERY_FAILED";

    private final IndexTaskLedger ledger;
    private final IndexTaskRecoveryExecutor recoveryExecutor;
    private final IndexTaskReconciliationProperties properties;
    private final String workerId;
    private final Executor taskExecutor;
    private final ScheduledExecutorService heartbeatScheduler;
    private final boolean ownsExecutors;
    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();

    @Autowired
    public IndexTaskReconciliationCoordinator(
            IndexTaskLedger ledger,
            IndexTaskRecoveryExecutor recoveryExecutor,
            IndexTaskReconciliationProperties properties) {
        this(ledger, recoveryExecutor, properties, "index-reconciler-" + UUID.randomUUID(),
                newBoundedExecutor(properties), newHeartbeatScheduler(), true);
    }

    IndexTaskReconciliationCoordinator(
            IndexTaskLedger ledger,
            IndexTaskRecoveryExecutor recoveryExecutor,
            IndexTaskReconciliationProperties properties,
            String workerId) {
        this(ledger, recoveryExecutor, properties, workerId, Runnable::run, null, false);
    }

    IndexTaskReconciliationCoordinator(
            IndexTaskLedger ledger,
            IndexTaskRecoveryExecutor recoveryExecutor,
            IndexTaskReconciliationProperties properties,
            String workerId,
            Executor taskExecutor,
            ScheduledExecutorService heartbeatScheduler,
            boolean ownsExecutors) {
        this.ledger = ledger;
        this.recoveryExecutor = recoveryExecutor;
        this.properties = properties;
        this.workerId = workerId;
        this.taskExecutor = taskExecutor;
        this.heartbeatScheduler = heartbeatScheduler;
        this.ownsExecutors = ownsExecutors;
    }

    @Scheduled(fixedDelayString = "${document.index-task-reconciliation.scan-interval-ms:30000}")
    public void reconcileOnce() {
        if (!properties.isEnabled()) {
            return;
        }
        Iterable<IndexTaskRecord> claimable;
        try {
            claimable = ledger.scanClaimable(properties.getBatchSize(), properties.getMaxAttempts());
        } catch (RuntimeException e) {
            log.warn("索引任务协调扫描暂不可用: errorType={}", e.getClass().getSimpleName());
            return;
        }
        for (IndexTaskRecord task : claimable) {
            IndexTaskPhase phase = IndexTaskPhase.valueOf(task.getExecutionPhase());
            if (!properties.isResumeEnabled() && phase != IndexTaskPhase.VECTOR_IN_FLIGHT) {
                continue;
            }
            if (!ledger.claim(task.getTaskId(), workerId,
                    properties.getLeaseSeconds(), properties.getMaxAttempts())) {
                continue;
            }
            try {
                taskExecutor.execute(() -> reconcileClaimed(task));
            } catch (RejectedExecutionException e) {
                ledger.release(task.getTaskId(), workerId);
                log.warn("索引恢复执行队列已满，已释放 lease: taskId={}", task.getTaskId());
            }
        }
    }

    private void reconcileClaimed(IndexTaskRecord task) {
        IndexTaskPhase phase = IndexTaskPhase.valueOf(task.getExecutionPhase());
        if (phase == IndexTaskPhase.VECTOR_IN_FLIGHT) {
            ledger.markReconciliationRequired(
                    task.getTaskId(), VectorDependencyException.ERROR_CODE_OUTCOME_UNKNOWN);
            return;
        }
        if (!properties.isResumeEnabled()) {
            ledger.release(task.getTaskId(), workerId);
            return;
        }
        AtomicBoolean leaseOwned = new AtomicBoolean(true);
        ScheduledFuture<?> heartbeatFuture = null;
        try {
            if (!ledger.heartbeat(task.getTaskId(), workerId, properties.getLeaseSeconds())) {
                log.warn("索引任务 lease 已丢失，跳过恢复: taskId={}", task.getTaskId());
                return;
            }
            if (heartbeatScheduler != null) {
                long interval = Math.max(1, properties.getHeartbeatSeconds());
                heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate(
                        () -> renewLease(task.getTaskId(), leaseOwned),
                        interval, interval, TimeUnit.SECONDS);
            }
            IndexTaskLeaseGuard leaseGuard = () -> {
                if (!leaseOwned.get()
                        || !ledger.heartbeat(task.getTaskId(), workerId, properties.getLeaseSeconds())) {
                    leaseOwned.set(false);
                    throw new IndexTaskLeaseLostException(task.getTaskId());
                }
            };
            recoveryExecutor.resume(task, leaseGuard);
        } catch (IndexTaskLeaseLostException e) {
            log.warn("索引任务恢复因 lease 丢失而停止: taskId={}", task.getTaskId());
        } catch (RuntimeException e) {
            log.warn("索引任务安全恢复未执行: taskId={}, phase={}, errorType={}",
                    task.getTaskId(), phase, e.getClass().getSimpleName());
            if (!leaseOwned.get()) {
                return;
            }
            int claimedAttempt = (task.getAttemptCount() == null ? 0 : task.getAttemptCount()) + 1;
            if (claimedAttempt >= properties.getMaxAttempts()) {
                ledger.markAttemptsExhausted(task.getTaskId(), workerId, RECOVERY_FAILURE_CODE);
            } else {
                ledger.scheduleRetry(task.getTaskId(), workerId, RECOVERY_FAILURE_CODE,
                        calculateBackoffSeconds(claimedAttempt));
            }
        } finally {
            if (heartbeatFuture != null) {
                heartbeatFuture.cancel(false);
            }
        }
    }

    private void renewLease(String taskId, AtomicBoolean leaseOwned) {
        if (!leaseOwned.get()) {
            return;
        }
        try {
            if (!ledger.heartbeat(taskId, workerId, properties.getLeaseSeconds())) {
                leaseOwned.set(false);
            }
        } catch (RuntimeException e) {
            leaseOwned.set(false);
            log.warn("索引任务 heartbeat 无法确认 lease: taskId={}, errorType={}",
                    taskId, e.getClass().getSimpleName());
        }
    }

    private int calculateBackoffSeconds(int claimedAttempt) {
        long delay = Math.max(1, properties.getInitialBackoffSeconds());
        long maximum = Math.max(delay, properties.getMaxBackoffSeconds());
        for (int attempt = 1; attempt < claimedAttempt && delay < maximum; attempt++) {
            delay = Math.min(maximum, delay * 2L);
        }
        return (int) Math.min(Integer.MAX_VALUE, delay);
    }

    @PreDestroy
    void shutdownExecutors() {
        if (!ownsExecutors) {
            return;
        }
        if (taskExecutor instanceof ExecutorService executorService) {
            executorService.shutdown();
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
        }
    }

    private static ExecutorService newBoundedExecutor(IndexTaskReconciliationProperties properties) {
        int concurrency = Math.max(1, properties.getConcurrency());
        int queueCapacity = Math.max(concurrency, properties.getBatchSize());
        return new ThreadPoolExecutor(
                concurrency,
                concurrency,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                namedDaemonThreadFactory("index-task-recovery-"),
                new ThreadPoolExecutor.AbortPolicy());
    }

    private static ScheduledExecutorService newHeartbeatScheduler() {
        return Executors.newSingleThreadScheduledExecutor(
                namedDaemonThreadFactory("index-task-heartbeat-"));
    }

    private static ThreadFactory namedDaemonThreadFactory(String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + THREAD_SEQUENCE.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}

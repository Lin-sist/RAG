package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 有界扫描并分类 durable index tasks。分类与真正 resume 分开。
 */
@Slf4j
@Component
public class IndexTaskReconciliationCoordinator {

    private final IndexTaskLedger ledger;
    private final IndexTaskRecoveryExecutor recoveryExecutor;
    private final IndexTaskReconciliationProperties properties;
    private final String workerId;

    @Autowired
    public IndexTaskReconciliationCoordinator(
            IndexTaskLedger ledger,
            IndexTaskRecoveryExecutor recoveryExecutor,
            IndexTaskReconciliationProperties properties) {
        this(ledger, recoveryExecutor, properties, "index-reconciler-" + UUID.randomUUID());
    }

    IndexTaskReconciliationCoordinator(
            IndexTaskLedger ledger,
            IndexTaskRecoveryExecutor recoveryExecutor,
            IndexTaskReconciliationProperties properties,
            String workerId) {
        this.ledger = ledger;
        this.recoveryExecutor = recoveryExecutor;
        this.properties = properties;
        this.workerId = workerId;
    }

    @Scheduled(fixedDelayString = "${document.index-task-reconciliation.scan-interval-ms:30000}")
    public void reconcileOnce() {
        if (!properties.isEnabled()) {
            return;
        }
        Iterable<IndexTaskRecord> claimable;
        try {
            claimable = ledger.scanClaimable(properties.getBatchSize());
        } catch (RuntimeException e) {
            log.warn("索引任务协调扫描暂不可用: errorType={}", e.getClass().getSimpleName());
            return;
        }
        for (IndexTaskRecord task : claimable) {
            if (!ledger.claim(task.getTaskId(), workerId,
                    properties.getLeaseSeconds(), properties.getMaxAttempts())) {
                continue;
            }
            reconcileClaimed(task);
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
        try {
            if (!ledger.heartbeat(task.getTaskId(), workerId, properties.getLeaseSeconds())) {
                log.warn("索引任务 lease 已丢失，跳过恢复: taskId={}", task.getTaskId());
                return;
            }
            recoveryExecutor.resume(task);
        } catch (RuntimeException e) {
            log.warn("索引任务安全恢复未执行: taskId={}, phase={}, errorType={}",
                    task.getTaskId(), phase, e.getClass().getSimpleName());
            ledger.release(task.getTaskId(), workerId);
        }
    }
}

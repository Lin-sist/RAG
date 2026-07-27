package com.enterprise.rag.admin.kb.task;

import java.util.Optional;
import java.util.List;

/**
 * Durable index-task ledger boundary.
 */
public interface IndexTaskLedger {

    /**
     * 在任务对客户端可见之前持久化接受事实，并返回其稳定 taskId。
     */
    String createAccepted(long tenantId, Long documentId, Long ownerId);

    /**
     * Redis 初始投影或调度已知失败时，将尚未启动的任务收敛到稳定失败态。
     */
    void markAcceptanceFailed(long tenantId, String taskId, String failureCode);

    void markSafePreVector(long tenantId, String taskId);

    /**
     * 在首次 vector mutation 前持久化 prepared facts 与 in-flight 边界。
     */
    void markVectorInFlight(long tenantId, String taskId, String contentHash, int chunkCount);

    void markVectorConfirmed(long tenantId, String taskId);

    void markFinalizing(long tenantId, String taskId);

    void markCompleted(long tenantId, String taskId);

    /**
     * vector mutation 结果未知时隔离任务，禁止自动 replay。
     */
    void markReconciliationRequired(long tenantId, String taskId, String failureCode);

    /**
     * 通过单条 DB 条件 UPDATE 竞争 lease；过期判断只使用数据库时间。
     */
    boolean claim(long tenantId, String taskId, String workerId, int leaseSeconds, int maxAttempts);

    Optional<IndexTaskRecord> find(long tenantId, String taskId);

    /**
     * C13b 迁移期 fail-closed 桥；projection 后续切片必须改为显式传入 tenantId。
     */
    @Deprecated
    default Optional<IndexTaskRecord> find(String taskId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    List<IndexTaskRecord> scanClaimable(int limit, int maxAttempts);

    boolean release(long tenantId, String taskId, String workerId);

    boolean heartbeat(long tenantId, String taskId, String workerId, int leaseSeconds);

    /**
     * 当前 owner 的最后一次恢复尝试已耗尽，收敛到不再扫描的稳定终态。
     */
    boolean markAttemptsExhausted(long tenantId, String taskId, String workerId, String failureCode);

    /**
     * 使用数据库当前时间设置下一次尝试时间并释放当前 lease。
     */
    boolean scheduleRetry(long tenantId, String taskId, String workerId,
            String failureCode, int backoffSeconds);
}

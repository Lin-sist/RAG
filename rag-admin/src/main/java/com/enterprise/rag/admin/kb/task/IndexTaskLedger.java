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
    String createAccepted(Long documentId, Long ownerId);

    /**
     * Redis 初始投影或调度已知失败时，将尚未启动的任务收敛到稳定失败态。
     */
    void markAcceptanceFailed(String taskId, String failureCode);

    void markSafePreVector(String taskId);

    /**
     * 在首次 vector mutation 前持久化 prepared facts 与 in-flight 边界。
     */
    void markVectorInFlight(String taskId, String contentHash, int chunkCount);

    void markVectorConfirmed(String taskId);

    void markFinalizing(String taskId);

    void markCompleted(String taskId);

    /**
     * vector mutation 结果未知时隔离任务，禁止自动 replay。
     */
    void markReconciliationRequired(String taskId, String failureCode);

    /**
     * 通过单条 DB 条件 UPDATE 竞争 lease；过期判断只使用数据库时间。
     */
    boolean claim(String taskId, String workerId, int leaseSeconds, int maxAttempts);

    Optional<IndexTaskRecord> find(String taskId);

    List<IndexTaskRecord> scanClaimable(int limit);

    boolean release(String taskId, String workerId);

    boolean heartbeat(String taskId, String workerId, int leaseSeconds);
}

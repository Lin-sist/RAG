package com.enterprise.rag.admin.kb.task;

/**
 * 持久化索引任务状态。
 */
public enum IndexTaskStatus {
    ACCEPTED,
    RUNNING,
    COMPLETED,
    FAILED,
    RECONCILIATION_REQUIRED,
    CLEANUP_PENDING
}

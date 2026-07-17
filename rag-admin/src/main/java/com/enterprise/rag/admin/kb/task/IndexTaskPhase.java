package com.enterprise.rag.admin.kb.task;

/**
 * 索引任务可恢复检查点。
 */
public enum IndexTaskPhase {
    ACCEPTED,
    SAFE_PRE_VECTOR,
    VECTOR_IN_FLIGHT,
    VECTOR_CONFIRMED,
    FINALIZING,
    TERMINAL
}

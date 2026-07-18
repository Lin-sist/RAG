package com.enterprise.rag.admin.kb.task;

/**
 * 可安全恢复 phase 的执行边界。Coordinator 分类本身不直接调用 provider/vector。
 */
public interface IndexTaskRecoveryExecutor {

    void resume(IndexTaskRecord task, IndexTaskLeaseGuard leaseGuard);
}

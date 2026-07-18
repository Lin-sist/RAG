package com.enterprise.rag.admin.kb.task;

/**
 * 恢复执行对当前数据库 lease 所有权的 fail-closed 观察点。
 */
@FunctionalInterface
public interface IndexTaskLeaseGuard {

    void assertOwned();
}

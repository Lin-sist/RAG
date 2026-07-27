package com.enterprise.rag.common.async;

import java.util.Optional;

/**
 * Redis task projection miss 时使用的 durable status boundary。
 */
public interface DurableTaskStatusStore {

    Optional<TaskStatus> find(long tenantId, String taskId);

    default Optional<TaskStatus> find(String taskId) {
        return Optional.empty();
    }

    static DurableTaskStatusStore empty() {
        return (tenantId, taskId) -> Optional.empty();
    }
}

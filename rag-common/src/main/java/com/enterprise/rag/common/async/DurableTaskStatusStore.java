package com.enterprise.rag.common.async;

import java.util.Optional;

/**
 * Redis task projection miss 时使用的 durable status boundary。
 */
public interface DurableTaskStatusStore {

    Optional<TaskStatus> find(String taskId);

    static DurableTaskStatusStore empty() {
        return taskId -> Optional.empty();
    }
}

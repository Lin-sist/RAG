package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.common.async.DurableTaskStatusStore;
import com.enterprise.rag.common.async.TaskState;
import com.enterprise.rag.common.async.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 将 durable ledger 安全投影为既有 task polling contract。
 */
@Component
@RequiredArgsConstructor
public class IndexTaskStatusProjectionStore implements DurableTaskStatusStore {

    private final IndexTaskLedger ledger;

    @Override
    public Optional<TaskStatus> find(String taskId) {
        return ledger.find(taskId).map(this::toTaskStatus);
    }

    private TaskStatus toTaskStatus(IndexTaskRecord record) {
        TaskState state = switch (IndexTaskStatus.valueOf(record.getStatus())) {
            case ACCEPTED -> TaskState.PENDING;
            case RUNNING, CLEANUP_PENDING -> TaskState.RUNNING;
            case COMPLETED -> TaskState.COMPLETED;
            case FAILED, RECONCILIATION_REQUIRED -> TaskState.FAILED;
        };
        String message = switch (state) {
            case PENDING -> "任务已提交，等待执行";
            case RUNNING -> "任务执行中";
            case COMPLETED -> "任务执行完成";
            case FAILED -> "任务执行失败";
            case CANCELLED -> "任务已取消";
        };
        return new TaskStatus(
                record.getTaskId(),
                record.getTaskType(),
                state,
                record.getProgress() == null ? 0 : record.getProgress(),
                message,
                null,
                state == TaskState.FAILED ? record.getFailureCode() : null,
                toInstant(record.getCreatedAt()),
                toInstant(record.getUpdatedAt()),
                record.getOwnerId());
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? Instant.now() : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}

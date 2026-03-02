package com.enterprise.rag.common.async;

import java.time.Instant;

/**
 * 任务状态响应 DTO
 * <p>
 * 用于 API 响应，包含任务的完整状态信息。
 */
public record TaskStatusResponse(
    String taskId,
    String taskType,
    String state,
    int progress,
    String message,
    Object result,
    String error,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * 从 TaskStatus 创建响应
     */
    public static TaskStatusResponse from(TaskStatus status) {
        return new TaskStatusResponse(
            status.taskId(),
            status.taskType(),
            status.state().name(),
            status.progress(),
            status.message(),
            null, // result 需要单独反序列化
            status.error(),
            status.createdAt(),
            status.updatedAt()
        );
    }

    /**
     * 从 TaskStatus 创建响应（包含结果）
     */
    public static TaskStatusResponse from(TaskStatus status, Object result) {
        return new TaskStatusResponse(
            status.taskId(),
            status.taskType(),
            status.state().name(),
            status.progress(),
            status.message(),
            result,
            status.error(),
            status.createdAt(),
            status.updatedAt()
        );
    }

    /**
     * 判断任务是否已完成
     */
    public boolean isTerminal() {
        return "COMPLETED".equals(state) || "FAILED".equals(state) || "CANCELLED".equals(state);
    }

    /**
     * 判断任务是否成功
     */
    public boolean isSuccessful() {
        return "COMPLETED".equals(state);
    }
}

package com.enterprise.rag.common.async;

import java.time.Instant;

/**
 * 任务状态记录
 * <p>
 * 包含任务的完整状态信息，用于查询和持久化。
 *
 * @param taskId    任务唯一标识
 * @param taskType  任务类型
 * @param state     任务状态
 * @param progress  任务进度（0-100）
 * @param message   状态消息
 * @param result    任务结果（JSON 格式）
 * @param error     错误信息
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record TaskStatus(
    String taskId,
    String taskType,
    TaskState state,
    int progress,
    String message,
    String result,
    String error,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * 创建待执行状态
     */
    public static TaskStatus pending(String taskId, String taskType) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.PENDING, 0, "任务已提交，等待执行", null, null, now, now);
    }

    /**
     * 创建运行中状态
     */
    public static TaskStatus running(String taskId, String taskType, int progress, String message) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.RUNNING, progress, message, null, null, now, now);
    }

    /**
     * 创建完成状态
     */
    public static TaskStatus completed(String taskId, String taskType, String result) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.COMPLETED, 100, "任务执行完成", result, null, now, now);
    }

    /**
     * 创建失败状态
     */
    public static TaskStatus failed(String taskId, String taskType, String error) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.FAILED, 0, "任务执行失败", null, error, now, now);
    }

    /**
     * 创建取消状态
     */
    public static TaskStatus cancelled(String taskId, String taskType) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.CANCELLED, 0, "任务已取消", null, null, now, now);
    }

    /**
     * 更新进度
     */
    public TaskStatus withProgress(int newProgress, String newMessage) {
        return new TaskStatus(taskId, taskType, TaskState.RUNNING, newProgress, newMessage, result, error, createdAt, Instant.now());
    }

    /**
     * 标记为完成
     */
    public TaskStatus withCompleted(String newResult) {
        return new TaskStatus(taskId, taskType, TaskState.COMPLETED, 100, "任务执行完成", newResult, null, createdAt, Instant.now());
    }

    /**
     * 标记为失败
     */
    public TaskStatus withFailed(String newError) {
        return new TaskStatus(taskId, taskType, TaskState.FAILED, progress, "任务执行失败", null, newError, createdAt, Instant.now());
    }

    /**
     * 判断任务是否已完成（成功、失败或取消）
     */
    public boolean isTerminal() {
        return state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.CANCELLED;
    }
}

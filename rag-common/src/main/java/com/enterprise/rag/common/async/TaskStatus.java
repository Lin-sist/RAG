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
 * @param ownerId   任务所有者用户 ID（可为空）
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
        Instant updatedAt,
        Long ownerId) {
    /**
     * 创建待执行状态
     */
    public static TaskStatus pending(String taskId, String taskType) {
        return pending(taskId, taskType, null);
    }

    /**
     * 创建待执行状态（带 owner）
     */
    public static TaskStatus pending(String taskId, String taskType, Long ownerId) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.PENDING, 0, "任务已提交，等待执行", null, null, now, now, ownerId);
    }

    /**
     * 创建运行中状态
     */
    public static TaskStatus running(String taskId, String taskType, int progress, String message) {
        return running(taskId, taskType, progress, message, null);
    }

    /**
     * 创建运行中状态（带 owner）
     */
    public static TaskStatus running(String taskId, String taskType, int progress, String message, Long ownerId) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.RUNNING, progress, message, null, null, now, now, ownerId);
    }

    /**
     * 创建完成状态
     */
    public static TaskStatus completed(String taskId, String taskType, String result) {
        return completed(taskId, taskType, result, null);
    }

    /**
     * 创建完成状态（带 owner）
     */
    public static TaskStatus completed(String taskId, String taskType, String result, Long ownerId) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.COMPLETED, 100, "任务执行完成", result, null, now, now, ownerId);
    }

    /**
     * 创建失败状态
     */
    public static TaskStatus failed(String taskId, String taskType, String error) {
        return failed(taskId, taskType, error, null);
    }

    /**
     * 创建失败状态（带 owner）
     */
    public static TaskStatus failed(String taskId, String taskType, String error, Long ownerId) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.FAILED, 0, "任务执行失败", null, error, now, now, ownerId);
    }

    /**
     * 创建取消状态
     */
    public static TaskStatus cancelled(String taskId, String taskType) {
        return cancelled(taskId, taskType, null);
    }

    /**
     * 创建取消状态（带 owner）
     */
    public static TaskStatus cancelled(String taskId, String taskType, Long ownerId) {
        Instant now = Instant.now();
        return new TaskStatus(taskId, taskType, TaskState.CANCELLED, 0, "任务已取消", null, null, now, now, ownerId);
    }

    /**
     * 更新进度
     */
    public TaskStatus withProgress(int newProgress, String newMessage) {
        return new TaskStatus(taskId, taskType, TaskState.RUNNING, newProgress, newMessage, result, error, createdAt,
                Instant.now(), ownerId);
    }

    /**
     * 标记为完成
     */
    public TaskStatus withCompleted(String newResult) {
        return new TaskStatus(taskId, taskType, TaskState.COMPLETED, 100, "任务执行完成", newResult, null, createdAt,
                Instant.now(), ownerId);
    }

    /**
     * 标记为失败
     */
    public TaskStatus withFailed(String newError) {
        return new TaskStatus(taskId, taskType, TaskState.FAILED, progress, "任务执行失败", null, newError, createdAt,
                Instant.now(), ownerId);
    }

    /**
     * 判断任务是否已完成（成功、失败或取消）
     */
    public boolean isTerminal() {
        return state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.CANCELLED;
    }
}

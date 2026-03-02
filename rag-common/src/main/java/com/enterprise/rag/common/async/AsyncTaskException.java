package com.enterprise.rag.common.async;

/**
 * 异步任务异常
 */
public class AsyncTaskException extends RuntimeException {

    private final String taskId;
    private final String errorCode;

    public AsyncTaskException(String message) {
        super(message);
        this.taskId = null;
        this.errorCode = "ASYNC_TASK_ERROR";
    }

    public AsyncTaskException(String message, Throwable cause) {
        super(message, cause);
        this.taskId = null;
        this.errorCode = "ASYNC_TASK_ERROR";
    }

    public AsyncTaskException(String taskId, String message) {
        super(message);
        this.taskId = taskId;
        this.errorCode = "ASYNC_TASK_ERROR";
    }

    public AsyncTaskException(String taskId, String message, Throwable cause) {
        super(message, cause);
        this.taskId = taskId;
        this.errorCode = "ASYNC_TASK_ERROR";
    }

    public AsyncTaskException(String taskId, String errorCode, String message) {
        super(message);
        this.taskId = taskId;
        this.errorCode = errorCode;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 任务不存在异常
     */
    public static AsyncTaskException notFound(String taskId) {
        return new AsyncTaskException(taskId, "TASK_NOT_FOUND", "任务不存在: " + taskId);
    }

    /**
     * 任务已取消异常
     */
    public static AsyncTaskException cancelled(String taskId) {
        return new AsyncTaskException(taskId, "TASK_CANCELLED", "任务已取消: " + taskId);
    }

    /**
     * 任务执行失败异常
     */
    public static AsyncTaskException executionFailed(String taskId, Throwable cause) {
        return new AsyncTaskException(taskId, "任务执行失败: " + taskId, cause);
    }

    /**
     * 任务状态存储失败异常
     */
    public static AsyncTaskException storageFailed(String taskId, Throwable cause) {
        return new AsyncTaskException(taskId, "STORAGE_FAILED", "任务状态存储失败: " + taskId);
    }
}

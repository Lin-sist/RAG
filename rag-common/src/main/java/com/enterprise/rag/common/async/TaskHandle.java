package com.enterprise.rag.common.async;

import java.util.concurrent.CompletableFuture;

/**
 * 任务句柄
 * <p>
 * 提交异步任务后返回的句柄，包含任务 ID 和 Future 对象。
 *
 * @param <T>    任务结果类型
 * @param taskId 任务唯一标识
 * @param future 异步执行的 Future 对象
 */
public record TaskHandle<T>(
    String taskId,
    CompletableFuture<T> future
) {
    /**
     * 判断任务是否已完成
     */
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * 判断任务是否被取消
     */
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * 判断任务是否异常完成
     */
    public boolean isCompletedExceptionally() {
        return future.isCompletedExceptionally();
    }

    /**
     * 取消任务
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }
}

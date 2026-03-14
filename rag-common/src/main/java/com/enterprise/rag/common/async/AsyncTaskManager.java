package com.enterprise.rag.common.async;

import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * 异步任务管理器接口
 * <p>
 * 提供异步任务的提交、状态查询和结果获取功能。
 * 支持 CompletableFuture 实现异步执行，任务状态持久化到 Redis。
 */
public interface AsyncTaskManager {

    /**
     * 提交异步任务
     * <p>
     * 任务将异步执行，立即返回任务句柄。
     *
     * @param task 异步任务
     * @param <T>  任务结果类型
     * @return 任务句柄，包含任务 ID 和 Future
     */
    <T> TaskHandle<T> submit(AsyncTask<T> task);

    /**
     * 提交异步任务（指定任务类型）
     *
     * @param taskType 任务类型
     * @param task     异步任务
     * @param <T>      任务结果类型
     * @return 任务句柄
     */
    <T> TaskHandle<T> submit(String taskType, AsyncTask<T> task);

    /**
     * 提交异步任务（指定任务类型和所有者）
     *
     * @param taskType 任务类型
     * @param ownerId  任务所有者用户 ID
     * @param task     异步任务
     * @param <T>      任务结果类型
     * @return 任务句柄
     */
    <T> TaskHandle<T> submit(String taskType, Long ownerId, AsyncTask<T> task);

    /**
     * 提交简单的异步任务（无进度回调）
     *
     * @param taskType 任务类型
     * @param callable 可调用任务
     * @param <T>      任务结果类型
     * @return 任务句柄
     */
    <T> TaskHandle<T> submit(String taskType, Callable<T> callable);

    /**
     * 提交简单的异步任务（指定任务所有者）
     *
     * @param taskType 任务类型
     * @param ownerId  任务所有者用户 ID
     * @param callable 可调用任务
     * @param <T>      任务结果类型
     * @return 任务句柄
     */
    <T> TaskHandle<T> submit(String taskType, Long ownerId, Callable<T> callable);

    /**
     * 获取任务状态
     *
     * @param taskId 任务 ID
     * @return 任务状态，如果任务不存在返回 empty
     */
    Optional<TaskStatus> getStatus(String taskId);

    /**
     * 获取任务结果
     * <p>
     * 如果任务未完成，返回 empty；
     * 如果任务失败，抛出 AsyncTaskException。
     *
     * @param taskId     任务 ID
     * @param resultType 结果类型
     * @param <T>        结果类型
     * @return 任务结果
     */
    <T> Optional<T> getResult(String taskId, Class<T> resultType);

    /**
     * 更新任务进度
     *
     * @param taskId   任务 ID
     * @param progress 进度（0-100）
     * @param message  进度消息
     */
    void updateProgress(String taskId, int progress, String message);

    /**
     * 取消任务
     *
     * @param taskId 任务 ID
     * @return 是否成功取消
     */
    boolean cancel(String taskId);

    /**
     * 判断任务是否存在
     *
     * @param taskId 任务 ID
     * @return 是否存在
     */
    boolean exists(String taskId);

    /**
     * 删除任务状态（用于清理）
     *
     * @param taskId 任务 ID
     */
    void remove(String taskId);
}

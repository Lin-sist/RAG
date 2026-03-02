package com.enterprise.rag.common.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 任务状态查询服务
 * <p>
 * 提供任务状态查询、进度更新和结果获取的便捷方法。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusService {

    private final AsyncTaskManager asyncTaskManager;

    /**
     * 获取任务状态
     *
     * @param taskId 任务 ID
     * @return 任务状态
     * @throws AsyncTaskException 如果任务不存在
     */
    public TaskStatus getStatus(String taskId) {
        return asyncTaskManager.getStatus(taskId)
            .orElseThrow(() -> AsyncTaskException.notFound(taskId));
    }

    /**
     * 获取任务状态（可选）
     *
     * @param taskId 任务 ID
     * @return 任务状态，如果不存在返回 empty
     */
    public Optional<TaskStatus> getStatusOptional(String taskId) {
        return asyncTaskManager.getStatus(taskId);
    }

    /**
     * 获取任务结果
     *
     * @param taskId     任务 ID
     * @param resultType 结果类型
     * @param <T>        结果类型
     * @return 任务结果
     * @throws AsyncTaskException 如果任务不存在、未完成或执行失败
     */
    public <T> T getResult(String taskId, Class<T> resultType) {
        TaskStatus status = getStatus(taskId);
        
        if (status.state() == TaskState.FAILED) {
            throw AsyncTaskException.executionFailed(taskId, new RuntimeException(status.error()));
        }
        
        if (status.state() == TaskState.CANCELLED) {
            throw AsyncTaskException.cancelled(taskId);
        }
        
        if (status.state() != TaskState.COMPLETED) {
            throw new AsyncTaskException(taskId, "TASK_NOT_COMPLETED", "任务尚未完成: " + taskId);
        }
        
        return asyncTaskManager.getResult(taskId, resultType)
            .orElseThrow(() -> new AsyncTaskException(taskId, "RESULT_NOT_FOUND", "任务结果不存在: " + taskId));
    }

    /**
     * 获取任务结果（可选）
     *
     * @param taskId     任务 ID
     * @param resultType 结果类型
     * @param <T>        结果类型
     * @return 任务结果，如果任务未完成返回 empty
     */
    public <T> Optional<T> getResultOptional(String taskId, Class<T> resultType) {
        return asyncTaskManager.getResult(taskId, resultType);
    }

    /**
     * 更新任务进度
     *
     * @param taskId   任务 ID
     * @param progress 进度（0-100）
     * @param message  进度消息
     */
    public void updateProgress(String taskId, int progress, String message) {
        asyncTaskManager.updateProgress(taskId, progress, message);
    }

    /**
     * 判断任务是否完成
     *
     * @param taskId 任务 ID
     * @return 是否完成（成功、失败或取消）
     */
    public boolean isCompleted(String taskId) {
        return asyncTaskManager.getStatus(taskId)
            .map(TaskStatus::isTerminal)
            .orElse(false);
    }

    /**
     * 判断任务是否成功完成
     *
     * @param taskId 任务 ID
     * @return 是否成功完成
     */
    public boolean isSuccessful(String taskId) {
        return asyncTaskManager.getStatus(taskId)
            .map(status -> status.state() == TaskState.COMPLETED)
            .orElse(false);
    }

    /**
     * 判断任务是否失败
     *
     * @param taskId 任务 ID
     * @return 是否失败
     */
    public boolean isFailed(String taskId) {
        return asyncTaskManager.getStatus(taskId)
            .map(status -> status.state() == TaskState.FAILED)
            .orElse(false);
    }

    /**
     * 判断任务是否正在运行
     *
     * @param taskId 任务 ID
     * @return 是否正在运行
     */
    public boolean isRunning(String taskId) {
        return asyncTaskManager.getStatus(taskId)
            .map(status -> status.state() == TaskState.RUNNING)
            .orElse(false);
    }

    /**
     * 取消任务
     *
     * @param taskId 任务 ID
     * @return 是否成功取消
     */
    public boolean cancel(String taskId) {
        return asyncTaskManager.cancel(taskId);
    }

    /**
     * 判断任务是否存在
     *
     * @param taskId 任务 ID
     * @return 是否存在
     */
    public boolean exists(String taskId) {
        return asyncTaskManager.exists(taskId);
    }
}

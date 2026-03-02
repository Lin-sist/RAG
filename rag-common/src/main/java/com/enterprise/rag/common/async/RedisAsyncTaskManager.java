package com.enterprise.rag.common.async;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的异步任务管理器实现
 * <p>
 * 使用 CompletableFuture 实现异步执行，任务状态持久化到 Redis。
 * 支持任务进度更新、状态查询和结果获取。
 */
@Slf4j
@Component
public class RedisAsyncTaskManager implements AsyncTaskManager {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 内存中的任务句柄缓存，用于取消任务
     */
    private final Map<String, CompletableFuture<?>> taskFutures = new ConcurrentHashMap<>();

    public RedisAsyncTaskManager(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> TaskHandle<T> submit(AsyncTask<T> task) {
        return submit(task.getTaskType(), task);
    }

    @Override
    public <T> TaskHandle<T> submit(String taskType, AsyncTask<T> task) {
        String taskId = generateTaskId();
        
        // 1. 创建初始状态并持久化
        TaskStatus initialStatus = TaskStatus.pending(taskId, taskType);
        saveStatus(taskId, initialStatus);
        
        // 2. 异步执行任务
        CompletableFuture<T> future = executeAsync(taskId, taskType, task);
        
        // 3. 缓存 Future 用于取消
        taskFutures.put(taskId, future);
        
        // 4. 任务完成后清理缓存
        future.whenComplete((result, ex) -> taskFutures.remove(taskId));
        
        log.info("Submitted async task: taskId={}, taskType={}", taskId, taskType);
        return new TaskHandle<>(taskId, future);
    }

    @Override
    public <T> TaskHandle<T> submit(String taskType, Callable<T> callable) {
        AsyncTask<T> task = progressCallback -> callable.call();
        return submit(taskType, task);
    }

    @Override
    public Optional<TaskStatus> getStatus(String taskId) {
        String redisKey = buildKey(taskId);
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        
        if (json == null) {
            return Optional.empty();
        }
        
        try {
            TaskStatusData data = objectMapper.readValue(json, TaskStatusData.class);
            return Optional.of(data.toTaskStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize task status: taskId={}", taskId, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> getResult(String taskId, Class<T> resultType) {
        Optional<TaskStatus> statusOpt = getStatus(taskId);
        
        if (statusOpt.isEmpty()) {
            return Optional.empty();
        }
        
        TaskStatus status = statusOpt.get();
        
        if (status.state() == TaskState.FAILED) {
            throw AsyncTaskException.executionFailed(taskId, new RuntimeException(status.error()));
        }
        
        if (status.state() == TaskState.CANCELLED) {
            throw AsyncTaskException.cancelled(taskId);
        }
        
        if (status.state() != TaskState.COMPLETED) {
            return Optional.empty();
        }
        
        if (status.result() == null) {
            return Optional.empty();
        }
        
        try {
            T result = objectMapper.readValue(status.result(), resultType);
            return Optional.of(result);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize task result: taskId={}", taskId, e);
            return Optional.empty();
        }
    }

    @Override
    public void updateProgress(String taskId, int progress, String message) {
        Optional<TaskStatus> statusOpt = getStatus(taskId);
        
        if (statusOpt.isEmpty()) {
            log.warn("Cannot update progress for non-existent task: {}", taskId);
            return;
        }
        
        TaskStatus currentStatus = statusOpt.get();
        if (currentStatus.isTerminal()) {
            log.warn("Cannot update progress for terminal task: taskId={}, state={}", taskId, currentStatus.state());
            return;
        }
        
        TaskStatus updatedStatus = currentStatus.withProgress(progress, message);
        saveStatus(taskId, updatedStatus);
        
        log.debug("Updated task progress: taskId={}, progress={}, message={}", taskId, progress, message);
    }

    @Override
    public boolean cancel(String taskId) {
        CompletableFuture<?> future = taskFutures.get(taskId);
        
        if (future == null) {
            // 任务可能已完成或不存在
            Optional<TaskStatus> statusOpt = getStatus(taskId);
            if (statusOpt.isEmpty()) {
                return false;
            }
            
            TaskStatus status = statusOpt.get();
            if (status.isTerminal()) {
                return false;
            }
            
            // 更新状态为已取消
            saveStatus(taskId, TaskStatus.cancelled(taskId, status.taskType()));
            return true;
        }
        
        boolean cancelled = future.cancel(true);
        if (cancelled) {
            Optional<TaskStatus> statusOpt = getStatus(taskId);
            statusOpt.ifPresent(status -> 
                saveStatus(taskId, TaskStatus.cancelled(taskId, status.taskType()))
            );
        }
        
        return cancelled;
    }

    @Override
    public boolean exists(String taskId) {
        String redisKey = buildKey(taskId);
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void remove(String taskId) {
        String redisKey = buildKey(taskId);
        stringRedisTemplate.delete(redisKey);
        taskFutures.remove(taskId);
        log.debug("Removed task: {}", taskId);
    }

    /**
     * 异步执行任务
     */
    @Async
    protected <T> CompletableFuture<T> executeAsync(String taskId, String taskType, AsyncTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 更新状态为运行中
                saveStatus(taskId, TaskStatus.running(taskId, taskType, 0, "任务开始执行"));
                
                // 执行任务，传入进度回调
                T result = task.execute(progress -> {
                    updateProgress(taskId, progress.progress(), progress.message());
                });
                
                // 更新状态为完成
                String resultJson = serializeResult(result);
                saveStatus(taskId, TaskStatus.completed(taskId, taskType, resultJson));
                
                log.info("Task completed successfully: taskId={}", taskId);
                return result;
                
            } catch (Exception e) {
                // 更新状态为失败
                log.error("Task execution failed: taskId={}", taskId, e);
                saveStatus(taskId, TaskStatus.failed(taskId, taskType, e.getMessage()));
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 生成任务 ID
     */
    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 构建 Redis key
     */
    private String buildKey(String taskId) {
        return RedisKeyConstants.taskStatusKey(taskId);
    }

    /**
     * 保存任务状态到 Redis
     */
    private void saveStatus(String taskId, TaskStatus status) {
        String redisKey = buildKey(taskId);
        try {
            TaskStatusData data = TaskStatusData.fromTaskStatus(status);
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(redisKey, json, RedisKeyConstants.TASK_STATUS_TTL, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task status: taskId={}", taskId, e);
            throw AsyncTaskException.storageFailed(taskId, e);
        }
    }

    /**
     * 序列化结果
     */
    private <T> String serializeResult(T result) {
        if (result == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task result", e);
            return null;
        }
    }

    /**
     * 任务状态数据（用于 JSON 序列化）
     */
    private record TaskStatusData(
        String taskId,
        String taskType,
        String state,
        int progress,
        String message,
        String result,
        String error,
        long createdAt,
        long updatedAt
    ) {
        static TaskStatusData fromTaskStatus(TaskStatus status) {
            return new TaskStatusData(
                status.taskId(),
                status.taskType(),
                status.state().name(),
                status.progress(),
                status.message(),
                status.result(),
                status.error(),
                status.createdAt().toEpochMilli(),
                status.updatedAt().toEpochMilli()
            );
        }

        TaskStatus toTaskStatus() {
            return new TaskStatus(
                taskId,
                taskType,
                TaskState.valueOf(state),
                progress,
                message,
                result,
                error,
                Instant.ofEpochMilli(createdAt),
                Instant.ofEpochMilli(updatedAt)
            );
        }
    }
}

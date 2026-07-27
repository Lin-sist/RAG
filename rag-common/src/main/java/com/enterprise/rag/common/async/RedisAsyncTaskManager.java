package com.enterprise.rag.common.async;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.enterprise.rag.common.exception.RedisDependencyException;
import com.enterprise.rag.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
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
    private final Executor asyncTaskExecutor;
    private final DurableTaskStatusStore durableTaskStatusStore;

    /**
     * 内存中的任务句柄缓存，用于取消任务
     */
    private final Map<String, CompletableFuture<?>> taskFutures = new ConcurrentHashMap<>();

    @Autowired
    public RedisAsyncTaskManager(StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Qualifier("asyncTaskExecutor") Executor asyncTaskExecutor,
            ObjectProvider<DurableTaskStatusStore> durableTaskStatusStoreProvider) {
        this(stringRedisTemplate, objectMapper, asyncTaskExecutor,
                durableTaskStatusStoreProvider.getIfAvailable(DurableTaskStatusStore::empty));
    }

    public RedisAsyncTaskManager(StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            Executor asyncTaskExecutor) {
        this(stringRedisTemplate, objectMapper, asyncTaskExecutor, DurableTaskStatusStore.empty());
    }

    public RedisAsyncTaskManager(StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            Executor asyncTaskExecutor,
            DurableTaskStatusStore durableTaskStatusStore) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.durableTaskStatusStore = durableTaskStatusStore;
    }

    RedisAsyncTaskManager(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this(stringRedisTemplate, objectMapper, ForkJoinPool.commonPool());
    }

    @Override
    public <T> TaskHandle<T> submit(AsyncTask<T> task) {
        throw tenantIdentityRequired();
    }

    @Override
    public <T> TaskHandle<T> submit(String taskType, AsyncTask<T> task) {
        throw tenantIdentityRequired();
    }

    @Override
    public <T> TaskHandle<T> submit(String taskType, Long ownerId, AsyncTask<T> task) {
        throw tenantIdentityRequired();
    }

    @Override
    public <T> TaskHandle<T> submit(String taskId, String taskType, Long ownerId, AsyncTask<T> task) {
        throw tenantIdentityRequired();
    }

    @Override
    public <T> TaskHandle<T> submit(long tenantId, String taskType, Long ownerId, AsyncTask<T> task) {
        return submit(tenantId, generateTaskId(), taskType, ownerId, task);
    }

    @Override
    public <T> TaskHandle<T> submit(
            long tenantId, String taskId, String taskType, Long ownerId, AsyncTask<T> task) {
        requireTenantId(tenantId);
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }

        // 1. 创建初始状态并持久化
        TaskStatus initialStatus = TaskStatus.pending(tenantId, taskId, taskType, ownerId);
        saveStatus(tenantId, taskId, initialStatus);

        // 2. 异步执行任务
        CompletableFuture<T> future = executeAsync(tenantId, taskId, taskType, ownerId, task);

        // 3. 缓存 Future 用于取消
        String futureKey = buildKey(tenantId, taskId);
        taskFutures.put(futureKey, future);

        // 4. 任务完成后清理缓存
        future.whenComplete((result, ex) -> taskFutures.remove(futureKey));

        log.info("Submitted async task: taskId={}, taskType={}, ownerId={}", taskId, taskType, ownerId);
        return new TaskHandle<>(taskId, future);
    }

    @Override
    public <T> TaskHandle<T> submit(String taskType, Callable<T> callable) {
        throw tenantIdentityRequired();
    }

    @Override
    public <T> TaskHandle<T> submit(String taskType, Long ownerId, Callable<T> callable) {
        throw tenantIdentityRequired();
    }

    @Override
    public Optional<TaskStatus> getStatus(String taskId) {
        throw tenantIdentityRequired();
    }

    @Override
    public Optional<TaskStatus> getStatus(long tenantId, String taskId) {
        requireTenantId(tenantId);
        String redisKey = buildKey(tenantId, taskId);
        String json;
        try {
            json = stringRedisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("Task status read failed closed: dependency=redis, subsystem=task_status, "
                            + "operation=read, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("task_status", "read", e);
        }

        if (json == null) {
            Optional<TaskStatus> durableStatus = durableTaskStatusStore.find(tenantId, taskId);
            durableStatus.ifPresent(status -> {
                requireMatchingScope(tenantId, taskId, status);
                saveStatus(tenantId, taskId, status);
            });
            return durableStatus;
        }

        try {
            TaskStatusData data = objectMapper.readValue(json, TaskStatusData.class);
            TaskStatus status = data.toTaskStatus();
            requireMatchingScope(tenantId, taskId, status);
            return Optional.of(status);
        } catch (Exception e) {
            log.error("Task status deserialize failed closed: dependency=redis, subsystem=task_status, "
                            + "operation=deserialize, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("task_status", "deserialize", e);
        }
    }

    @Override
    public <T> Optional<T> getResult(String taskId, Class<T> resultType) {
        throw tenantIdentityRequired();
    }

    @Override
    public <T> Optional<T> getResult(long tenantId, String taskId, Class<T> resultType) {
        Optional<TaskStatus> statusOpt = getStatus(tenantId, taskId);

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
        } catch (Exception e) {
            log.error("Task result deserialize failed closed: dependency=redis, subsystem=task_status, "
                            + "operation=deserialize_result, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("task_status", "deserialize_result", e);
        }
    }

    @Override
    public void updateProgress(String taskId, int progress, String message) {
        throw tenantIdentityRequired();
    }

    @Override
    public void updateProgress(long tenantId, String taskId, int progress, String message) {
        Optional<TaskStatus> statusOpt = getStatus(tenantId, taskId);

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
        saveStatus(tenantId, taskId, updatedStatus);

        log.debug("Updated task progress: taskId={}, progress={}", taskId, progress);
    }

    @Override
    public boolean cancel(String taskId) {
        throw tenantIdentityRequired();
    }

    @Override
    public boolean cancel(long tenantId, String taskId) {
        String futureKey = buildKey(tenantId, taskId);
        CompletableFuture<?> future = taskFutures.get(futureKey);

        if (future == null) {
            // 任务可能已完成或不存在
            Optional<TaskStatus> statusOpt = getStatus(tenantId, taskId);
            if (statusOpt.isEmpty()) {
                return false;
            }

            TaskStatus status = statusOpt.get();
            if (status.isTerminal()) {
                return false;
            }

            // 更新状态为已取消
            saveStatus(tenantId, taskId,
                    TaskStatus.cancelled(tenantId, taskId, status.taskType(), status.ownerId()));
            return true;
        }

        boolean cancelled = future.cancel(true);
        if (cancelled) {
            Optional<TaskStatus> statusOpt = getStatus(tenantId, taskId);
            statusOpt.ifPresent(
                    status -> saveStatus(tenantId, taskId,
                            TaskStatus.cancelled(tenantId, taskId, status.taskType(), status.ownerId())));
        }

        return cancelled;
    }

    @Override
    public boolean exists(String taskId) {
        throw tenantIdentityRequired();
    }

    @Override
    public boolean exists(long tenantId, String taskId) {
        String redisKey = buildKey(tenantId, taskId);
        try {
            Boolean exists = stringRedisTemplate.hasKey(redisKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Task status existence check failed closed: dependency=redis, subsystem=task_status, "
                            + "operation=exists, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("task_status", "exists", e);
        }
    }

    @Override
    public void remove(String taskId) {
        throw tenantIdentityRequired();
    }

    @Override
    public void remove(long tenantId, String taskId) {
        String redisKey = buildKey(tenantId, taskId);
        try {
            stringRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.error("Task status removal failed closed: dependency=redis, subsystem=task_status, "
                            + "operation=remove, failMode=closed, errorType={}",
                    e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("task_status", "remove", e);
        }
        taskFutures.remove(redisKey);
        log.debug("Removed task: {}", taskId);
    }

    /**
     * 异步执行任务
     */
    protected <T> CompletableFuture<T> executeAsync(
            long tenantId, String taskId, String taskType, Long ownerId, AsyncTask<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 更新状态为运行中
                saveStatus(tenantId, taskId,
                        TaskStatus.running(tenantId, taskId, taskType, 0, "任务开始执行", ownerId));

                // 执行任务，传入进度回调
                T result = task.execute(progress -> {
                    updateProgress(tenantId, taskId, progress.progress(), progress.message());
                });

                // 更新状态为完成
                String resultJson = serializeResult(result);
                saveStatus(tenantId, taskId,
                        TaskStatus.completed(tenantId, taskId, taskType, resultJson, ownerId));

                log.info("Task completed successfully: taskId={}", taskId);
                return result;

            } catch (RedisDependencyException e) {
                throw e;
            } catch (BusinessException e) {
                log.error("Task execution failed with stable business result: taskId={}, errorCode={}, errorType={}",
                        taskId, e.getErrorCode(), e.getClass().getSimpleName());
                try {
                    saveStatus(tenantId, taskId, TaskStatus.failed(
                            tenantId, taskId, taskType, e.getErrorCode() + ": " + e.getMessage(), ownerId));
                } catch (RedisDependencyException statusFailure) {
                    statusFailure.addSuppressed(e);
                    throw statusFailure;
                }
                throw e;
            } catch (Exception e) {
                // 更新状态为失败
                log.error("Task execution failed: taskId={}, errorType={}",
                        taskId, e.getClass().getSimpleName());
                try {
                    saveStatus(tenantId, taskId,
                            TaskStatus.failed(tenantId, taskId, taskType, e.getMessage(), ownerId));
                } catch (RedisDependencyException statusFailure) {
                    statusFailure.addSuppressed(e);
                    throw statusFailure;
                }
                throw new RuntimeException(e);
            }
        }, asyncTaskExecutor);
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
    private String buildKey(long tenantId, String taskId) {
        return RedisKeyConstants.taskStatusV2Key(requireTenantId(tenantId), taskId);
    }

    /**
     * 保存任务状态到 Redis
     */
    private void saveStatus(long tenantId, String taskId, TaskStatus status) {
        requireMatchingScope(tenantId, taskId, status);
        String redisKey = buildKey(tenantId, taskId);
        try {
            TaskStatusData data = TaskStatusData.fromTaskStatus(status);
            String json = objectMapper.writeValueAsString(data);
            stringRedisTemplate.opsForValue().set(redisKey, json, RedisKeyConstants.TASK_STATUS_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            String operation = switch (status.state()) {
                case PENDING -> "write_pending";
                case RUNNING -> "write_running";
                case COMPLETED -> "write_completed";
                case FAILED -> "write_failed";
                case CANCELLED -> "write_cancelled";
            };
            log.error("Task status write failed closed: dependency=redis, subsystem=task_status, "
                            + "operation={}, failMode=closed, errorType={}",
                    operation, e.getClass().getSimpleName());
            throw RedisDependencyException.unavailable("task_status", operation, e);
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
            log.error("Failed to serialize task result: errorType={}",
                    e.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 任务状态数据（用于 JSON 序列化）
     */
    private record TaskStatusData(
            Long tenantId,
            String taskId,
            String taskType,
            String state,
            int progress,
            String message,
            String result,
            String error,
            long createdAt,
            long updatedAt,
            Long ownerId) {
        static TaskStatusData fromTaskStatus(TaskStatus status) {
            return new TaskStatusData(
                    status.tenantId(),
                    status.taskId(),
                    status.taskType(),
                    status.state().name(),
                    status.progress(),
                    status.message(),
                    status.result(),
                    status.error(),
                    status.createdAt().toEpochMilli(),
                    status.updatedAt().toEpochMilli(),
                    status.ownerId());
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
                    Instant.ofEpochMilli(updatedAt),
                    ownerId,
                    tenantId);
        }
    }

    private static long requireTenantId(long tenantId) {
        if (tenantId <= 0L) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        return tenantId;
    }

    private static void requireMatchingScope(long tenantId, String taskId, TaskStatus status) {
        if (status == null
                || status.tenantId() == null
                || status.tenantId() != tenantId
                || !taskId.equals(status.taskId())) {
            throw new IllegalStateException("Task projection scope mismatch");
        }
    }

    private static IllegalStateException tenantIdentityRequired() {
        return new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }
}

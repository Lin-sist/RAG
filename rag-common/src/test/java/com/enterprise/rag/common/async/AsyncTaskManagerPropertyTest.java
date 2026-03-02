package com.enterprise.rag.common.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步任务管理器属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * 
 * Property 19: 异步任务提交即时性
 * Property 20: 任务状态查询完整性
 * 
 * Validates: Requirements 9.2, 9.3
 */
class AsyncTaskManagerPropertyTest {

    private static StringRedisTemplate stringRedisTemplate;
    private static RedisAsyncTaskManager asyncTaskManager;
    private static boolean redisAvailable = false;

    static {
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
            connectionFactory.afterPropertiesSet();
            
            stringRedisTemplate = new StringRedisTemplate(connectionFactory);
            stringRedisTemplate.afterPropertiesSet();
            
            // Test connection
            stringRedisTemplate.getConnectionFactory().getConnection().ping();
            
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            
            asyncTaskManager = new RedisAsyncTaskManager(stringRedisTemplate, objectMapper);
            redisAvailable = true;
        } catch (Exception e) {
            System.err.println("Redis not available, skipping property tests: " + e.getMessage());
            redisAvailable = false;
        }
    }

    /**
     * Property 19: 异步任务提交即时性
     * 
     * *For any* 提交的异步任务，应立即返回任务 ID，且任务 ID 可用于后续状态查询。
     * 
     * 测试策略：
     * 1. 生成随机的任务类型和 payload
     * 2. 提交异步任务
     * 3. 验证立即返回任务 ID（非空）
     * 4. 验证任务 ID 可用于状态查询
     * 5. 验证初始状态为 PENDING 或 RUNNING
     * 
     * **Validates: Requirements 9.2**
     */
    @Property(tries = 100)
    void asyncTaskSubmitShouldReturnTaskIdImmediately(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String taskType,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String payload) {
        
        Assume.that(redisAvailable);
        
        String uniqueTaskType = "test-" + taskType + "-" + UUID.randomUUID().toString().substring(0, 8);
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch taskCanComplete = new CountDownLatch(1);
        
        try {
            // 提交异步任务
            long startTime = System.currentTimeMillis();
            
            TaskHandle<String> handle = asyncTaskManager.submit(uniqueTaskType, progressCallback -> {
                taskStarted.countDown();
                try {
                    // 等待测试完成验证后再完成任务
                    taskCanComplete.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return payload;
            });
            
            long submitTime = System.currentTimeMillis() - startTime;
            
            // 验证立即返回（提交时间应该很短）
            Assertions.assertThat(submitTime)
                .as("Task submission should be immediate (< 1000ms)")
                .isLessThan(1000);
            
            // 验证任务 ID 非空
            Assertions.assertThat(handle.taskId())
                .as("Task ID should not be null")
                .isNotNull();
            
            Assertions.assertThat(handle.taskId())
                .as("Task ID should not be empty")
                .isNotEmpty();
            
            // 验证任务 ID 可用于状态查询
            Optional<TaskStatus> statusOpt = asyncTaskManager.getStatus(handle.taskId());
            
            Assertions.assertThat(statusOpt.isPresent())
                .as("Task status should be queryable by task ID")
                .isTrue();
            
            TaskStatus status = statusOpt.get();
            
            // 验证任务 ID 匹配
            Assertions.assertThat(status.taskId())
                .as("Status task ID should match handle task ID")
                .isEqualTo(handle.taskId());
            
            // 验证任务类型匹配
            Assertions.assertThat(status.taskType())
                .as("Status task type should match submitted task type")
                .isEqualTo(uniqueTaskType);
            
            // 验证初始状态为 PENDING 或 RUNNING
            Assertions.assertThat(status.state() == TaskState.PENDING || status.state() == TaskState.RUNNING)
                .as("Initial state should be PENDING or RUNNING, but was " + status.state())
                .isTrue();
            
            // 验证创建时间不为空
            Assertions.assertThat(status.createdAt())
                .as("Created time should not be null")
                .isNotNull();
            
        } finally {
            // 允许任务完成
            taskCanComplete.countDown();
            
            // 等待一小段时间让任务完成
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Property 20: 任务状态查询完整性
     * 
     * *For any* 已提交的任务 ID，查询状态应返回包含任务状态、进度和结果（如已完成）的完整信息。
     * 
     * 测试策略：
     * 1. 提交异步任务并等待完成
     * 2. 查询任务状态
     * 3. 验证状态包含所有必要字段
     * 4. 验证完成后可以获取结果
     * 
     * **Validates: Requirements 9.3**
     */
    @Property(tries = 100)
    void taskStatusQueryShouldReturnCompleteInfo(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String taskType,
            @ForAll @AlphaChars @StringLength(min = 1, max = 50) String payload) {
        
        Assume.that(redisAvailable);
        
        String uniqueTaskType = "test-complete-" + taskType + "-" + UUID.randomUUID().toString().substring(0, 8);
        AtomicBoolean progressUpdated = new AtomicBoolean(false);
        
        try {
            // 提交异步任务
            TaskHandle<String> handle = asyncTaskManager.submit(uniqueTaskType, progressCallback -> {
                // 更新进度
                progressCallback.accept(AsyncTask.TaskProgress.of(50, "处理中"));
                progressUpdated.set(true);
                
                // 模拟处理
                Thread.sleep(50);
                
                return payload;
            });
            
            // 等待任务完成
            try {
                handle.future().get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                // 忽略异常，继续验证状态
            }
            
            // 等待状态更新
            Thread.sleep(100);
            
            // 查询任务状态
            Optional<TaskStatus> statusOpt = asyncTaskManager.getStatus(handle.taskId());
            
            Assertions.assertThat(statusOpt.isPresent())
                .as("Task status should be present")
                .isTrue();
            
            TaskStatus status = statusOpt.get();
            
            // 验证状态包含所有必要字段
            Assertions.assertThat(status.taskId())
                .as("Task ID should not be null")
                .isNotNull();
            
            Assertions.assertThat(status.taskType())
                .as("Task type should not be null")
                .isNotNull();
            
            Assertions.assertThat(status.state())
                .as("Task state should not be null")
                .isNotNull();
            
            Assertions.assertThat(status.createdAt())
                .as("Created time should not be null")
                .isNotNull();
            
            Assertions.assertThat(status.updatedAt())
                .as("Updated time should not be null")
                .isNotNull();
            
            // 验证完成状态
            if (status.state() == TaskState.COMPLETED) {
                // 验证进度为 100
                Assertions.assertThat(status.progress())
                    .as("Completed task progress should be 100")
                    .isEqualTo(100);
                
                // 验证可以获取结果
                Optional<String> resultOpt = asyncTaskManager.getResult(handle.taskId(), String.class);
                
                Assertions.assertThat(resultOpt.isPresent())
                    .as("Completed task should have result")
                    .isTrue();
                
                Assertions.assertThat(resultOpt.get())
                    .as("Result should match expected payload")
                    .isEqualTo(payload);
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 清理
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 验证任务存在性检查
     */
    @Property(tries = 50)
    void existsShouldReturnCorrectStatus(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String taskType) {
        
        Assume.that(redisAvailable);
        
        String uniqueTaskType = "test-exists-" + taskType + "-" + UUID.randomUUID().toString().substring(0, 8);
        String nonExistentTaskId = "non-existent-" + UUID.randomUUID();
        
        // 不存在的任务 ID 应该返回 false
        Assertions.assertThat(asyncTaskManager.exists(nonExistentTaskId))
            .as("Non-existent task should return false")
            .isFalse();
        
        // 提交任务
        TaskHandle<String> handle = asyncTaskManager.submit(uniqueTaskType, progressCallback -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "result";
        });
        
        // 提交后应该存在
        Assertions.assertThat(asyncTaskManager.exists(handle.taskId()))
            .as("Submitted task should exist")
            .isTrue();
        
        // 等待任务完成
        try {
            handle.future().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 忽略
        }
        
        // 完成后仍然存在
        Assertions.assertThat(asyncTaskManager.exists(handle.taskId()))
            .as("Completed task should still exist")
            .isTrue();
        
        // 删除后不存在
        asyncTaskManager.remove(handle.taskId());
        
        Assertions.assertThat(asyncTaskManager.exists(handle.taskId()))
            .as("Removed task should not exist")
            .isFalse();
    }

    /**
     * 验证进度更新
     */
    @Property(tries = 50)
    void progressUpdateShouldBeReflectedInStatus(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String taskType) {
        
        Assume.that(redisAvailable);
        
        String uniqueTaskType = "test-progress-" + taskType + "-" + UUID.randomUUID().toString().substring(0, 8);
        CountDownLatch progressChecked = new CountDownLatch(1);
        
        // 提交任务
        TaskHandle<String> handle = asyncTaskManager.submit(uniqueTaskType, progressCallback -> {
            // 更新进度到 50%
            progressCallback.accept(AsyncTask.TaskProgress.of(50, "半程"));
            
            // 等待测试检查进度
            try {
                progressChecked.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return "done";
        });
        
        // 等待任务开始并更新进度
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        try {
            // 查询状态
            Optional<TaskStatus> statusOpt = asyncTaskManager.getStatus(handle.taskId());
            
            if (statusOpt.isPresent()) {
                TaskStatus status = statusOpt.get();
                
                // 如果任务还在运行，验证进度
                if (status.state() == TaskState.RUNNING) {
                    Assertions.assertThat(status.progress())
                        .as("Progress should be updated to 50")
                        .isEqualTo(50);
                    
                    Assertions.assertThat(status.message())
                        .as("Message should be updated")
                        .isEqualTo("半程");
                }
            }
        } finally {
            progressChecked.countDown();
        }
    }

    /**
     * 自定义断言类
     */
    private static class Assertions {
        static BooleanAssert assertThat(boolean actual) {
            return new BooleanAssert(actual);
        }

        static LongAssert assertThat(long actual) {
            return new LongAssert(actual);
        }

        static IntAssert assertThat(int actual) {
            return new IntAssert(actual);
        }

        static <T> ObjectAssert<T> assertThat(T actual) {
            return new ObjectAssert<>(actual);
        }
    }

    private static class BooleanAssert {
        private final boolean actual;
        private String description;

        BooleanAssert(boolean actual) {
            this.actual = actual;
        }

        BooleanAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isTrue() {
            if (!actual) {
                throw new AssertionError(description != null ? description : "Expected true but was false");
            }
        }

        void isFalse() {
            if (actual) {
                throw new AssertionError(description != null ? description : "Expected false but was true");
            }
        }
    }

    private static class LongAssert {
        private final long actual;
        private String description;

        LongAssert(long actual) {
            this.actual = actual;
        }

        LongAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isLessThan(long expected) {
            if (actual >= expected) {
                throw new AssertionError(
                    (description != null ? description + ": " : "") +
                    "Expected < " + expected + " but was " + actual);
            }
        }
    }

    private static class IntAssert {
        private final int actual;
        private String description;

        IntAssert(int actual) {
            this.actual = actual;
        }

        IntAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isEqualTo(int expected) {
            if (actual != expected) {
                throw new AssertionError(
                    (description != null ? description + ": " : "") +
                    "Expected " + expected + " but was " + actual);
            }
        }
    }

    private static class ObjectAssert<T> {
        private final T actual;
        private String description;

        ObjectAssert(T actual) {
            this.actual = actual;
        }

        ObjectAssert<T> as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isNull() {
            if (actual != null) {
                throw new AssertionError(
                    (description != null ? description + ": " : "") +
                    "Expected null but was " + actual);
            }
        }

        void isNotNull() {
            if (actual == null) {
                throw new AssertionError(
                    (description != null ? description + ": " : "") +
                    "Expected non-null but was null");
            }
        }

        void isNotEmpty() {
            if (actual == null || (actual instanceof String && ((String) actual).isEmpty())) {
                throw new AssertionError(
                    (description != null ? description + ": " : "") +
                    "Expected non-empty but was " + actual);
            }
        }

        void isEqualTo(T expected) {
            if (actual == null && expected == null) {
                return;
            }
            if (actual == null || !actual.equals(expected)) {
                throw new AssertionError(
                    (description != null ? description + ": " : "") +
                    "Expected " + expected + " but was " + actual);
            }
        }
    }
}

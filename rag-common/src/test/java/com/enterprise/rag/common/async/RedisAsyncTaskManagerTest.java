package com.enterprise.rag.common.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class RedisAsyncTaskManagerTest {

    @Test
    void redisMissFallsBackToDurableStatusAndRebuildsProjection() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        DurableTaskStatusStore durableStore = mock(DurableTaskStatusStore.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        TaskStatus durable = TaskStatus.pending(77L, "task-durable", "DOCUMENT_INDEX", 42L);
        when(durableStore.find(77L, "task-durable")).thenReturn(Optional.of(durable));

        RedisAsyncTaskManager manager = new RedisAsyncTaskManager(
                redisTemplate, new ObjectMapper(), Runnable::run, durableStore);

        TaskStatus restored = manager.getStatus(77L, "task-durable").orElseThrow();

        assertEquals(77L, restored.tenantId());
        assertEquals(42L, restored.ownerId());
        verify(valueOperations).get("task:status:v2:77:task-durable");
        verify(durableStore).find(77L, "task-durable");
        verify(valueOperations).set(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldRunSupplyAsyncOnConfiguredExecutor() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AtomicBoolean executorUsed = new AtomicBoolean(false);
        Executor executor = command -> {
            executorUsed.set(true);
            Thread thread = new Thread(command, "async-task-test-worker");
            thread.start();
        };
        RedisAsyncTaskManager manager = new RedisAsyncTaskManager(redisTemplate, new ObjectMapper(), executor);

        TaskHandle<String> handle = manager.submit(77L, "THREAD_POOL_TEST", 42L,
                ignored -> Thread.currentThread().getName());

        String threadName = handle.future().get(5, TimeUnit.SECONDS);

        assertTrue(executorUsed.get());
        assertEquals("async-task-test-worker", threadName);
    }

    @Test
    void tenantPayloadMismatchFailsClosed() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:status:v2:77:task-1")).thenReturn(
                "{\"tenantId\":88,\"taskId\":\"task-1\",\"taskType\":\"INDEX\","
                        + "\"state\":\"RUNNING\",\"progress\":10,\"message\":\"running\","
                        + "\"result\":null,\"error\":null,\"createdAt\":1,\"updatedAt\":1,"
                        + "\"ownerId\":42}");
        RedisAsyncTaskManager manager = new RedisAsyncTaskManager(
                redisTemplate, new ObjectMapper(), Runnable::run);

        com.enterprise.rag.common.exception.RedisDependencyException error =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.enterprise.rag.common.exception.RedisDependencyException.class,
                        () -> manager.getStatus(77L, "task-1"));

        assertEquals("deserialize", error.getOperation());
    }
}

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
        TaskStatus durable = TaskStatus.pending("task-durable", "DOCUMENT_INDEX", 42L);
        when(durableStore.find("task-durable")).thenReturn(Optional.of(durable));

        RedisAsyncTaskManager manager = new RedisAsyncTaskManager(
                redisTemplate, new ObjectMapper(), Runnable::run, durableStore);

        TaskStatus restored = manager.getStatus("task-durable").orElseThrow();

        assertEquals(42L, restored.ownerId());
        verify(durableStore).find("task-durable");
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

        TaskHandle<String> handle = manager.submit("THREAD_POOL_TEST",
                ignored -> Thread.currentThread().getName());

        String threadName = handle.future().get(5, TimeUnit.SECONDS);

        assertTrue(executorUsed.get());
        assertEquals("async-task-test-worker", threadName);
    }
}

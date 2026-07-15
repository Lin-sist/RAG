package com.enterprise.rag.common.async;

import com.enterprise.rag.common.exception.RedisDependencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisAsyncTaskManagerFailureTest {

    private ValueOperations<String, String> valueOperations;
    private RedisAsyncTaskManager manager;

    @BeforeEach
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        valueOperations = operations;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Executor directExecutor = Runnable::run;
        manager = new RedisAsyncTaskManager(redisTemplate, new ObjectMapper(), directExecutor);
    }

    @Test
    void initialPendingWriteFailureShouldNotStartOperation() {
        AtomicInteger operationCalls = new AtomicInteger();
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> manager.submit("INDEX", ignored -> {
                    operationCalls.incrementAndGet();
                    return "done";
                }));

        assertEquals(0, operationCalls.get());
        assertEquals("task_status", exception.getSubsystem());
        assertEquals("write_pending", exception.getOperation());
    }

    @Test
    void statusReadFailureShouldNotLookLikeMissingTask() {
        when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("synthetic redis marker"));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> manager.getStatus("task-id"));

        assertEquals("read", exception.getOperation());
    }

    @Test
    void completedStateWriteFailureShouldRemainObservable() {
        AtomicInteger operationCalls = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        doAnswer(invocation -> {
            if (writes.incrementAndGet() == 3) {
                throw new RuntimeException("synthetic redis marker");
            }
            return null;
        }).when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        TaskHandle<String> handle = manager.submit("INDEX", ignored -> {
            operationCalls.incrementAndGet();
            return "done";
        });

        CompletionException exception = assertThrows(CompletionException.class, handle.future()::join);
        assertEquals(1, operationCalls.get());
        assertTrue(hasCause(exception, RedisDependencyException.class));
    }

    @Test
    void progressWriteFailureShouldRemainObservable() {
        Instant now = Instant.now();
        String json = "{\"taskId\":\"task-id\",\"taskType\":\"INDEX\",\"state\":\"RUNNING\","
                + "\"progress\":10,\"message\":\"running\",\"result\":null,\"error\":null,"
                + "\"createdAt\":" + now.toEpochMilli() + ",\"updatedAt\":"
                + now.toEpochMilli() + ",\"ownerId\":null}";
        when(valueOperations.get(anyString())).thenReturn(json);
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> manager.updateProgress("task-id", 50, "synthetic progress"));

        assertEquals("write_running", exception.getOperation());
    }

    @Test
    void cancelledStateWriteFailureShouldNotReturnSuccess() throws Exception {
        TaskStatus running = new TaskStatus(
                "task-id", "INDEX", TaskState.RUNNING, 10, "running", null, null,
                Instant.now(), Instant.now(), null);
        String json = "{\"taskId\":\"task-id\",\"taskType\":\"INDEX\",\"state\":\"RUNNING\","
                + "\"progress\":10,\"message\":\"running\",\"result\":null,\"error\":null,"
                + "\"createdAt\":" + running.createdAt().toEpochMilli() + ",\"updatedAt\":"
                + running.updatedAt().toEpochMilli() + ",\"ownerId\":null}";
        when(valueOperations.get(anyString())).thenReturn(json);
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> manager.cancel("task-id"));

        assertEquals("write_cancelled", exception.getOperation());
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

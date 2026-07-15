package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.exception.RedisDependencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class RedisIdempotencyHandlerFailureTest {

    private ValueOperations<String, String> valueOperations;
    private RedisIdempotencyHandler handler;

    @BeforeEach
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        valueOperations = operations;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new RedisIdempotencyHandler(redisTemplate, objectMapper);
    }

    @Test
    void preOperationRedisFailureShouldNotInvokeOperation() {
        AtomicInteger operationCalls = new AtomicInteger();
        when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("synthetic redis marker"));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> handler.execute(
                        "request-key",
                        () -> {
                            operationCalls.incrementAndGet();
                            return "created";
                        },
                        String.class));

        assertEquals(0, operationCalls.get());
        assertEquals("idempotency", exception.getSubsystem());
        assertEquals("read", exception.getOperation());
    }

    @Test
    void processingLockFailureShouldNotInvokeOperation() {
        AtomicInteger operationCalls = new AtomicInteger();
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenThrow(new RuntimeException("synthetic redis marker"));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> handler.execute(
                        "request-key",
                        () -> {
                            operationCalls.incrementAndGet();
                            return "created";
                        },
                        String.class));

        assertEquals(0, operationCalls.get());
        assertEquals("lock", exception.getOperation());
    }

    @Test
    void completedStateWriteFailureShouldReportOutcomeUnknown() {
        AtomicInteger operationCalls = new AtomicInteger();
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        doThrow(new RuntimeException("synthetic redis marker"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        RedisDependencyException exception = assertThrows(RedisDependencyException.class,
                () -> handler.execute(
                        "request-key",
                        () -> {
                            operationCalls.incrementAndGet();
                            return "created";
                        },
                        String.class));

        assertEquals(1, operationCalls.get());
        assertEquals(RedisDependencyException.ERROR_CODE_OUTCOME_UNKNOWN, exception.getErrorCode());
        assertEquals("outcome_unknown", exception.getFailMode());
    }
}

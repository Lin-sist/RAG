package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.constant.RedisKeyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisIdempotencyHandlerTenantScopeTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private RedisIdempotencyHandler handler;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        valueOperations = operations;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        handler = new RedisIdempotencyHandler(redisTemplate, objectMapper);
    }

    @Test
    void shouldWriteTenantScopedKeyAndIdentityPayload() throws Exception {
        IdempotencyScope scope = new IdempotencyScope(11L, 21L);
        String redisKey = RedisKeyConstants.idempotencyV2Key(
                11L, 21L, "/api/kb:create", "request:key");
        when(valueOperations.get(redisKey)).thenReturn(null);
        when(valueOperations.setIfAbsent(
                eq(redisKey), anyString(), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        IdempotencyResult<String> result = handler.execute(
                scope,
                "/api/kb:create",
                "request:key",
                () -> "created",
                String.class,
                120L);

        assertTrue(result.isNew());
        assertEquals("created", result.result());

        ArgumentCaptor<String> processingPayload = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(
                eq(redisKey), processingPayload.capture(), eq(60L), eq(TimeUnit.SECONDS));
        IdempotencyData processing = objectMapper.readValue(
                processingPayload.getValue(), IdempotencyData.class);
        assertEquals(11L, processing.getTenantId());
        assertEquals(21L, processing.getUserId());

        ArgumentCaptor<String> completedPayload = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                eq(redisKey), completedPayload.capture(), eq(120L), eq(TimeUnit.SECONDS));
        IdempotencyData completed = objectMapper.readValue(
                completedPayload.getValue(), IdempotencyData.class);
        assertEquals(11L, completed.getTenantId());
        assertEquals(21L, completed.getUserId());
    }

    @Test
    void shouldFailClosedBeforeOperationWhenPayloadScopeMismatches() throws Exception {
        IdempotencyScope requestedScope = new IdempotencyScope(11L, 21L);
        String redisKey = RedisKeyConstants.idempotencyV2Key(
                11L, 21L, "kb:create", "request-key");
        IdempotencyData otherTenantData = IdempotencyData.completed(
                new IdempotencyScope(12L, 21L),
                objectMapper.writeValueAsString("other-tenant-result"),
                String.class.getName());
        otherTenantData.setProcessedAt(Instant.now());
        when(valueOperations.get(redisKey)).thenReturn(objectMapper.writeValueAsString(otherTenantData));
        AtomicInteger operationCalls = new AtomicInteger();

        IdempotencyException exception = assertThrows(IdempotencyException.class,
                () -> handler.execute(
                        requestedScope,
                        "kb:create",
                        "request-key",
                        () -> {
                            operationCalls.incrementAndGet();
                            return "created";
                        },
                        String.class,
                        120L));

        assertEquals(IdempotencyException.ERROR_CODE_SCOPE_MISMATCH, exception.getErrorCode());
        assertEquals(0, operationCalls.get());
        verify(valueOperations, never()).setIfAbsent(
                anyString(), anyString(), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void legacyHandlerMethodsRemainDeprecatedCompatibilityOnly() throws Exception {
        assertDeprecated("execute", String.class, Supplier.class, Class.class);
        assertDeprecated("execute", String.class, Supplier.class, Class.class, long.class);
        assertDeprecated("exists", String.class);
        assertDeprecated("getStoredResult", String.class, Class.class);
        assertDeprecated("remove", String.class);
    }

    private void assertDeprecated(String methodName, Class<?>... parameterTypes) throws Exception {
        assertTrue(IdempotencyHandler.class.getMethod(methodName, parameterTypes)
                .isAnnotationPresent(Deprecated.class));
    }
}

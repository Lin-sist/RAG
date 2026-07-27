package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IdempotencyAspectTest {

    private final IdempotencyHandler idempotencyHandler = mock(IdempotencyHandler.class);
    private final IdempotencyScopeResolver scopeResolver = mock(IdempotencyScopeResolver.class);
    private final IdempotencyAspect aspect = new IdempotencyAspect(idempotencyHandler, scopeResolver);

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldPassAuthenticatedScopeEndpointAndRequestKeyToHandler() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Idempotency-Key", "idem-123");
        bindRequest(request);
        IdempotencyScope scope = new IdempotencyScope(11L, 21L);
        when(scopeResolver.resolve(request)).thenReturn(Optional.of(scope));

        Method method = DummyService.class.getDeclaredMethod("create");
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "OK");
        when(idempotencyHandler.execute(
                eq(scope),
                eq("demo:create"),
                eq("idem-123"),
                any(),
                eq(String.class),
                eq(idempotent.ttlSeconds())))
                .thenReturn(IdempotencyResult.newRequest("OK"));

        Object result = aspect.handleIdempotency(joinPoint, idempotent);

        assertThat(result).isEqualTo("OK");

        verify(idempotencyHandler).execute(
                eq(scope),
                eq("demo:create"),
                eq("idem-123"),
                any(),
                eq(String.class),
                eq(idempotent.ttlSeconds()));
    }

    @Test
    void shouldFailClosedWhenIdempotencyKeyHasNoAuthenticatedScope() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Idempotency-Key", "idem-123");
        bindRequest(request);
        when(scopeResolver.resolve(request)).thenReturn(Optional.empty());

        Method method = DummyService.class.getDeclaredMethod("create");
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "SHOULD_NOT_RUN");

        IdempotencyException exception = assertThrows(IdempotencyException.class,
                () -> aspect.handleIdempotency(joinPoint, idempotent));

        assertThat(exception.getErrorCode()).isEqualTo(IdempotencyException.ERROR_CODE_IDENTITY_REQUIRED);
        verify(joinPoint, never()).proceed();
        verifyNoInteractions(idempotencyHandler);
    }

    @Test
    void shouldThrowWhenKeyMissingAndRequired() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        bindRequest(request);

        Method method = DummyService.class.getDeclaredMethod("requiredAction");
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "OK");

        assertThrows(BusinessException.class, () -> aspect.handleIdempotency(joinPoint, idempotent));
        verifyNoInteractions(idempotencyHandler, scopeResolver);
    }

    @Test
    void shouldBypassHandlerWhenKeyMissingAndNotRequired() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        bindRequest(request);

        Method method = DummyService.class.getDeclaredMethod("optionalAction");
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "DIRECT");

        Object result = aspect.handleIdempotency(joinPoint, idempotent);

        assertThat(result).isEqualTo("DIRECT");
        verifyNoInteractions(idempotencyHandler, scopeResolver);
    }

    private void bindRequest(HttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private ProceedingJoinPoint mockJoinPoint(Method method, Object proceedResult) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn(proceedResult);

        when(signature.getReturnType()).thenReturn((Class) method.getReturnType());
        when(signature.getName()).thenReturn(method.getName());
        when(signature.getDeclaringTypeName()).thenReturn(method.getDeclaringClass().getName());
        when(signature.getMethod()).thenReturn(method);

        Signature baseSignature = signature;
        when(joinPoint.getSignature()).thenReturn(baseSignature);

        return joinPoint;
    }

    static class DummyService {

        @Idempotent(keyPrefix = "demo:create", ttlSeconds = 120)
        public String create() {
            return "";
        }

        @Idempotent(required = true)
        public String requiredAction() {
            return "";
        }

        @Idempotent(required = false)
        public String optionalAction() {
            return "";
        }
    }
}

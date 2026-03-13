package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IdempotencyAspectTest {

    private final IdempotencyHandler idempotencyHandler = mock(IdempotencyHandler.class);
    private final IdempotencyAspect aspect = new IdempotencyAspect(idempotencyHandler);

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldIncludePrincipalInIdempotencyKey() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Idempotency-Key", "idem-123");
        request.setUserPrincipal((Principal) () -> "user-a");
        bindRequest(request);

        Method method = DummyService.class.getDeclaredMethod("create");
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "OK");
        when(idempotencyHandler.execute(any(), any(), eq(String.class), eq(idempotent.ttlSeconds())))
                .thenReturn(IdempotencyResult.newRequest("OK"));

        Object result = aspect.handleIdempotency(joinPoint, idempotent);

        assertThat(result).isEqualTo("OK");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(idempotencyHandler).execute(keyCaptor.capture(), any(), eq(String.class), eq(idempotent.ttlSeconds()));
        assertThat(keyCaptor.getValue()).isEqualTo("demo:create:user-a:idem-123");
    }

    @Test
    void shouldThrowWhenKeyMissingAndRequired() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest();
        bindRequest(request);

        Method method = DummyService.class.getDeclaredMethod("requiredAction");
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        ProceedingJoinPoint joinPoint = mockJoinPoint(method, "OK");

        assertThrows(BusinessException.class, () -> aspect.handleIdempotency(joinPoint, idempotent));
        verify(idempotencyHandler, never()).execute(any(), any(), any(), anyLong());
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
        verify(idempotencyHandler, never()).execute(any(), any(), any(), anyLong());
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

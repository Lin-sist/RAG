package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 幂等性切面
 * <p>
 * 拦截带有 @Idempotent 注解的方法，执行幂等性检查。
 * 从请求头中提取幂等性 Key，使用 IdempotencyHandler 进行处理。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyHandler idempotencyHandler;

    /**
     * 环绕通知：处理幂等性逻辑
     */
    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 获取请求
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            log.warn("No HTTP request context available, skipping idempotency check");
            return joinPoint.proceed();
        }

        // 从请求头提取幂等性 Key
        String idempotencyKey = request.getHeader(idempotent.headerName());

        // 检查是否提供了幂等性 Key
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            if (idempotent.required()) {
                throw new BusinessException(
                    "IDEMPOTENCY_003",
                    idempotent.message(),
                    HttpStatus.BAD_REQUEST
                );
            }
            // 不要求必须提供 Key，跳过幂等性检查
            log.debug("Idempotency key not provided, skipping check");
            return joinPoint.proceed();
        }

        // 构建完整的幂等性 Key
        String fullKey = buildFullKey(idempotent, joinPoint, idempotencyKey);

        // 获取返回类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();

        // 执行幂等性处理
        @SuppressWarnings("unchecked")
        IdempotencyResult<Object> result = idempotencyHandler.execute(
            fullKey,
            () -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException(e);
                }
            },
            (Class<Object>) returnType,
            idempotent.ttlSeconds()
        );

        if (!result.isNew()) {
            log.info("Returning cached result for idempotency key: {}", fullKey);
        }

        return result.result();
    }

    /**
     * 获取当前 HTTP 请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 构建完整的幂等性 Key
     * <p>
     * 格式: {prefix}:{idempotencyKey}
     * 如果未指定前缀，使用方法签名作为前缀
     */
    private String buildFullKey(Idempotent idempotent, ProceedingJoinPoint joinPoint, String idempotencyKey) {
        String prefix = idempotent.keyPrefix();
        if (prefix.isEmpty()) {
            // 使用方法签名作为前缀
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            prefix = signature.getDeclaringTypeName() + "." + signature.getName();
        }
        return prefix + ":" + idempotencyKey;
    }
}

package com.enterprise.rag.auth.exception;

import com.enterprise.rag.common.exception.ErrorResponse;
import com.enterprise.rag.common.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证授权异常处理器
 * <p>
 * 处理 Spring Security 相关的认证和授权异常
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        log.warn("Authentication exception: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "AUTH_001",
                "认证失败",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 处理凭证错误异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {
        log.warn("Bad credentials: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "AUTH_001",
                "用户名或密码错误",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        log.warn("Access denied: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "AUTH_004",
                "权限不足",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 构建错误响应
     */
    private ErrorResponse buildErrorResponse(int status, String errorCode, 
            String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .errorCode(errorCode)
                .message(message)
                .traceId(TraceContext.getTraceId())
                .path(path)
                .build();
    }
}

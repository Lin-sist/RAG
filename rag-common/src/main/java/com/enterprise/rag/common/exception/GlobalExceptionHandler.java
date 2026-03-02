package com.enterprise.rag.common.exception;

import com.enterprise.rag.common.trace.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 统一处理各类异常，返回标准化的错误响应格式
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    // ==================== 业务异常处理 ====================

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        log.warn("Business exception: [{}] {}", e.getErrorCode(), e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                e.getHttpStatus().value(),
                e.getErrorCode(),
                e.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }

    // ==================== 参数验证异常处理 ====================

    /**
     * 处理方法参数验证异常（@Valid 注解触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("Validation failed: {}", e.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("VALIDATION_001")
                .message("请求参数验证失败")
                .traceId(TraceContext.getTraceId())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException e, HttpServletRequest request) {
        log.warn("Bind exception: {}", e.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("VALIDATION_002")
                .message("请求参数绑定失败")
                .traceId(TraceContext.getTraceId())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        log.warn("Constraint violation: {}", e.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = e.getConstraintViolations()
                .stream()
                .map(violation -> ErrorResponse.FieldError.builder()
                        .field(getFieldName(violation))
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("VALIDATION_003")
                .message("请求参数约束验证失败")
                .traceId(TraceContext.getTraceId())
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("Missing request parameter: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "PARAM_001",
                String.format("缺少必需的请求参数: %s", e.getParameterName()),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("Argument type mismatch: {}", e.getMessage());
        
        String requiredType = e.getRequiredType() != null 
                ? e.getRequiredType().getSimpleName() 
                : "unknown";
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "PARAM_002",
                String.format("参数 '%s' 类型不匹配，期望类型: %s", e.getName(), requiredType),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理请求体不可读异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("Message not readable: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "PARAM_003",
                "请求体格式错误或不可读",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== HTTP 请求异常处理 ====================

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not supported: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "HTTP_001",
                String.format("不支持的请求方法: %s", e.getMethod()),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 处理媒体类型不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("Media type not supported: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "HTTP_002",
                String.format("不支持的媒体类型: %s", e.getContentType()),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("No handler found: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "HTTP_003",
                String.format("资源不存在: %s %s", e.getHttpMethod(), e.getRequestURL()),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("Max upload size exceeded: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "DOC_002",
                "上传文件大小超过限制",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }


    // ==================== 服务异常处理 ====================

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Illegal argument: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "SYS_001",
                e.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException e, HttpServletRequest request) {
        log.warn("Illegal state: {}", e.getMessage());
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.CONFLICT.value(),
                "SYS_002",
                e.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ==================== 通用异常处理 ====================

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {
        log.error("Unexpected exception: ", e);
        
        ErrorResponse response = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "SYS_500",
                "系统内部错误，请稍后重试",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ==================== 辅助方法 ====================

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

    /**
     * 从约束违反中提取字段名
     */
    private String getFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        int lastDotIndex = propertyPath.lastIndexOf('.');
        return lastDotIndex > 0 ? propertyPath.substring(lastDotIndex + 1) : propertyPath;
    }
}

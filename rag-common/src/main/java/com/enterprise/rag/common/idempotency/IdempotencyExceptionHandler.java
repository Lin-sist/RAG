package com.enterprise.rag.common.idempotency;

import com.enterprise.rag.common.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 幂等性异常处理器
 */
@Slf4j
@RestControllerAdvice
@Order(1)
public class IdempotencyExceptionHandler {

    /**
     * 处理幂等性异常
     */
    @ExceptionHandler(IdempotencyException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyException(IdempotencyException e) {
        log.warn("Idempotency exception: {}", e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.error(e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(response);
    }
}

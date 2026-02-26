package com.enterprise.rag.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 统一错误响应格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * HTTP 状态码
     */
    private int status;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 时间戳
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * 请求路径
     */
    private String path;

    /**
     * 字段验证错误详情
     */
    private List<FieldError> fieldErrors;

    /**
     * 额外信息
     */
    private Map<String, Object> details;

    /**
     * 字段错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}

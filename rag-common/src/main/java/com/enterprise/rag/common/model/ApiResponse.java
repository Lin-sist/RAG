package com.enterprise.rag.common.model;

import com.enterprise.rag.common.trace.TraceContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 统一 API 响应封装
 * <p>
 * 所有 API 响应都使用此类进行封装，确保响应格式一致
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 响应状态码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 链路追踪 ID
     */
    private String traceId;
    
    /**
     * 响应时间戳
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .traceId(TraceContext.getTraceId())
                .build();
    }

    /**
     * 成功响应（带数据和自定义消息）
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .traceId(TraceContext.getTraceId())
                .build();
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 错误响应（带状态码和消息）
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .traceId(TraceContext.getTraceId())
                .build();
    }

    /**
     * 错误响应（带错误码和消息）
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .code(500)
                .message(String.format("[%s] %s", errorCode, message))
                .traceId(TraceContext.getTraceId())
                .build();
    }

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }
}

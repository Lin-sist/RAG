package com.enterprise.rag.common.model;

import com.enterprise.rag.common.exception.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应封装处理器
 * <p>
 * 自动将 Controller 返回值封装为 ApiResponse 格式
 * <p>
 * 排除以下情况：
 * - 已经是 ApiResponse 类型的响应
 * - ErrorResponse 类型的响应（异常处理器返回）
 * - Swagger/OpenAPI 相关的响应
 * - 资源文件响应
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    /**
     * 判断是否需要处理响应
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查方法或类是否标记了 @RawResponse 注解
        if (returnType.hasMethodAnnotation(RawResponse.class) 
                || returnType.getContainingClass().isAnnotationPresent(RawResponse.class)) {
            return false;
        }
        
        // 获取 Controller 类名
        String className = returnType.getContainingClass().getName();
        
        // 排除 Swagger/OpenAPI 相关的 Controller
        if (className.contains("swagger") || className.contains("springdoc") || className.contains("openapi")) {
            return false;
        }
        
        // 排除 Spring 内置的 Controller
        if (className.startsWith("org.springframework")) {
            return false;
        }
        
        return true;
    }

    /**
     * 处理响应体
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        
        // 如果已经是 ApiResponse，直接返回
        if (body instanceof ApiResponse) {
            return body;
        }
        
        // 如果是 ErrorResponse（异常处理器返回），直接返回
        if (body instanceof ErrorResponse) {
            return body;
        }
        
        // 如果是 null，返回成功响应
        if (body == null) {
            return handleNullBody(returnType, selectedConverterType);
        }
        
        // 如果是 String 类型，需要特殊处理（因为 StringHttpMessageConverter 会先处理）
        if (body instanceof String) {
            return handleStringBody((String) body, selectedConverterType);
        }
        
        // 封装为 ApiResponse
        return ApiResponse.success(body);
    }

    /**
     * 处理 null 响应体
     */
    private Object handleNullBody(MethodParameter returnType, 
            Class<? extends HttpMessageConverter<?>> selectedConverterType) {
        // 如果返回类型是 String，需要返回 JSON 字符串
        if (returnType.getParameterType().equals(String.class)) {
            return convertToJsonString(ApiResponse.success());
        }
        return ApiResponse.success();
    }

    /**
     * 处理 String 类型响应体
     * <p>
     * 由于 StringHttpMessageConverter 优先级较高，
     * 当返回类型是 String 时，需要将 ApiResponse 转换为 JSON 字符串
     */
    private Object handleStringBody(String body, 
            Class<? extends HttpMessageConverter<?>> selectedConverterType) {
        // 检查是否已经是 JSON 格式（可能是手动序列化的 ApiResponse）
        if (isJsonString(body)) {
            return body;
        }
        
        // 封装为 ApiResponse 并转换为 JSON 字符串
        return convertToJsonString(ApiResponse.success(body));
    }

    /**
     * 将对象转换为 JSON 字符串
     */
    private String convertToJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON string", e);
            throw new RuntimeException("Failed to serialize response", e);
        }
    }

    /**
     * 判断字符串是否为 JSON 格式
     */
    private boolean isJsonString(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        str = str.trim();
        return (str.startsWith("{") && str.endsWith("}")) 
                || (str.startsWith("[") && str.endsWith("]"));
    }
}

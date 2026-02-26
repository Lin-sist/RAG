package com.enterprise.rag.common.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法或类不进行统一响应封装
 * <p>
 * 使用此注解的 Controller 方法或类将直接返回原始响应，
 * 不会被 ApiResponseAdvice 自动封装为 ApiResponse
 * <p>
 * 适用场景：
 * - 文件下载
 * - 流式响应
 * - 第三方回调接口
 * - 需要自定义响应格式的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RawResponse {
}

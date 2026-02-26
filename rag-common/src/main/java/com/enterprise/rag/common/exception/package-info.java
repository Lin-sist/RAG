/**
 * 异常处理包
 * <p>
 * 包含全局异常处理相关的类：
 * <ul>
 *   <li>{@link com.enterprise.rag.common.exception.GlobalExceptionHandler} - 全局异常处理器</li>
 *   <li>{@link com.enterprise.rag.common.exception.BusinessException} - 业务异常基类</li>
 *   <li>{@link com.enterprise.rag.common.exception.ErrorResponse} - 统一错误响应格式</li>
 * </ul>
 * <p>
 * 错误码规范：
 * <ul>
 *   <li>AUTH_xxx - 认证授权相关错误</li>
 *   <li>VALIDATION_xxx - 参数验证相关错误</li>
 *   <li>PARAM_xxx - 请求参数相关错误</li>
 *   <li>HTTP_xxx - HTTP 请求相关错误</li>
 *   <li>DOC_xxx - 文档处理相关错误</li>
 *   <li>KB_xxx - 知识库相关错误</li>
 *   <li>VEC_xxx - 向量服务相关错误</li>
 *   <li>LLM_xxx - LLM 服务相关错误</li>
 *   <li>RATE_xxx - 限流相关错误</li>
 *   <li>SYS_xxx - 系统相关错误</li>
 * </ul>
 */
package com.enterprise.rag.common.exception;

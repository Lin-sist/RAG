/**
 * 链路追踪模块
 * 
 * <p>提供请求链路追踪能力，包括：
 * <ul>
 *   <li>{@link com.enterprise.rag.common.trace.TraceIdGenerator} - TraceId 生成器</li>
 *   <li>{@link com.enterprise.rag.common.trace.TraceContext} - 追踪上下文管理</li>
 *   <li>{@link com.enterprise.rag.common.trace.TraceFilter} - 请求过滤器，自动设置 MDC</li>
 * </ul>
 * 
 * <p>使用方式：
 * <pre>
 * // 获取当前请求的 TraceId
 * String traceId = TraceContext.getTraceId();
 * 
 * // 在日志中自动包含 TraceId（通过 MDC）
 * log.info("Processing request"); // 日志会自动包含 traceId
 * </pre>
 * 
 * @see com.enterprise.rag.common.trace.TraceIdGenerator
 * @see com.enterprise.rag.common.trace.TraceContext
 * @see com.enterprise.rag.common.trace.TraceFilter
 */
package com.enterprise.rag.common.trace;

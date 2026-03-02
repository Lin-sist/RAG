/**
 * 异步任务模块
 * <p>
 * 提供异步任务管理功能，支持：
 * - 使用 CompletableFuture 实现异步执行
 * - 任务状态持久化到 Redis 和数据库
 * - 任务进度更新和结果查询
 * - 任务重试机制
 * <p>
 * 主要组件：
 * - {@link com.enterprise.rag.common.async.AsyncTaskManager} - 异步任务管理器接口
 * - {@link com.enterprise.rag.common.async.TaskStatus} - 任务状态记录
 * - {@link com.enterprise.rag.common.async.TaskState} - 任务状态枚举
 * - {@link com.enterprise.rag.common.async.TaskHandle} - 任务句柄
 */
package com.enterprise.rag.common.async;

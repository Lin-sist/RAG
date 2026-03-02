/**
 * 消息队列集成模块
 * <p>
 * 提供消息队列抽象，支持：
 * - 文档索引消息生产者
 * - 文档索引消息消费者
 * - 可选的 RabbitMQ 集成
 * <p>
 * 主要组件：
 * - {@link com.enterprise.rag.common.async.mq.DocumentIndexMessage} - 文档索引消息
 * - {@link com.enterprise.rag.common.async.mq.DocumentIndexProducer} - 消息生产者接口
 * - {@link com.enterprise.rag.common.async.mq.DocumentIndexConsumer} - 消息消费者接口
 */
package com.enterprise.rag.common.async.mq;

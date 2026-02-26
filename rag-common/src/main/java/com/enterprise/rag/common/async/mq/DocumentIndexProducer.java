package com.enterprise.rag.common.async.mq;

/**
 * 文档索引消息生产者接口
 * <p>
 * 用于发送文档索引任务到消息队列。
 */
public interface DocumentIndexProducer {

    /**
     * 发送文档索引消息
     *
     * @param message 文档索引消息
     */
    void send(DocumentIndexMessage message);

    /**
     * 发送文档索引消息（带延迟）
     *
     * @param message      文档索引消息
     * @param delaySeconds 延迟秒数
     */
    void sendDelayed(DocumentIndexMessage message, long delaySeconds);

    /**
     * 判断消息队列是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}

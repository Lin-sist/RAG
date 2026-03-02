package com.enterprise.rag.common.async.mq;

/**
 * 文档索引消息消费者接口
 * <p>
 * 用于处理文档索引任务消息。
 */
public interface DocumentIndexConsumer {

    /**
     * 处理文档索引消息
     *
     * @param message 文档索引消息
     */
    void consume(DocumentIndexMessage message);

    /**
     * 处理消息失败时的回调
     *
     * @param message 文档索引消息
     * @param error   错误信息
     */
    default void onError(DocumentIndexMessage message, Throwable error) {
        // 默认实现：记录日志
    }
}

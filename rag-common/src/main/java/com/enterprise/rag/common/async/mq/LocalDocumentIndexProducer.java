package com.enterprise.rag.common.async.mq;

import com.enterprise.rag.common.async.AsyncTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本地文档索引消息生产者
 * <p>
 * 当消息队列不可用时，使用本地异步执行。
 * 这是一个备用实现，生产环境建议使用 RabbitMQ。
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "rabbitDocumentIndexProducer")
@RequiredArgsConstructor
public class LocalDocumentIndexProducer implements DocumentIndexProducer {

    private final AsyncTaskManager asyncTaskManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // 消费者引用，由 Spring 注入
    private DocumentIndexConsumer consumer;

    /**
     * 设置消费者
     */
    public void setConsumer(DocumentIndexConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void send(DocumentIndexMessage message) {
        log.info("Sending document index message locally: messageId={}, documentId={}", 
            message.messageId(), message.documentId());
        
        // 使用异步任务管理器执行
        asyncTaskManager.submit("DocumentIndex", progressCallback -> {
            if (consumer != null) {
                try {
                    consumer.consume(message);
                    return "SUCCESS";
                } catch (Exception e) {
                    consumer.onError(message, e);
                    throw e;
                }
            } else {
                log.warn("No consumer registered for document index messages");
                return "NO_CONSUMER";
            }
        });
    }

    @Override
    public void sendDelayed(DocumentIndexMessage message, long delaySeconds) {
        log.info("Scheduling delayed document index message: messageId={}, delay={}s", 
            message.messageId(), delaySeconds);
        
        scheduler.schedule(() -> send(message), delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean isAvailable() {
        return true; // 本地实现始终可用
    }
}

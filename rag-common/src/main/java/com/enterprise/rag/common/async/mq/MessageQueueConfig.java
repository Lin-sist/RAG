package com.enterprise.rag.common.async.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 消息队列配置
 * <p>
 * 配置消息生产者和消费者的关联。
 */
@Slf4j
@Configuration
public class MessageQueueConfig {

    @Autowired(required = false)
    private LocalDocumentIndexProducer localProducer;

    @Autowired(required = false)
    private DocumentIndexConsumer consumer;

    @PostConstruct
    public void init() {
        // 将消费者注入到本地生产者
        if (localProducer != null && consumer != null) {
            localProducer.setConsumer(consumer);
            log.info("Configured local document index producer with consumer: {}", 
                consumer.getClass().getSimpleName());
        }
    }
}

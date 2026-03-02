package com.enterprise.rag.common.async.mq;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * 文档索引消息
 * <p>
 * 用于异步文档索引任务的消息载体。
 */
public record DocumentIndexMessage(
    String messageId,
    String taskId,
    Long documentId,
    Long knowledgeBaseId,
    String documentPath,
    String documentType,
    Map<String, Object> metadata,
    Instant createdAt
) implements Serializable {

    /**
     * 创建文档索引消息
     */
    public static DocumentIndexMessage create(
        String taskId,
        Long documentId,
        Long knowledgeBaseId,
        String documentPath,
        String documentType,
        Map<String, Object> metadata
    ) {
        String messageId = java.util.UUID.randomUUID().toString().replace("-", "");
        return new DocumentIndexMessage(
            messageId,
            taskId,
            documentId,
            knowledgeBaseId,
            documentPath,
            documentType,
            metadata,
            Instant.now()
        );
    }

    /**
     * 创建简单的文档索引消息
     */
    public static DocumentIndexMessage simple(String taskId, Long documentId, Long knowledgeBaseId) {
        return create(taskId, documentId, knowledgeBaseId, null, null, Map.of());
    }
}

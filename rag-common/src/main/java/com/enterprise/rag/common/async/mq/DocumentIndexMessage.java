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
    Long tenantId,
    String messageId,
    String taskId,
    Long documentId,
    Long knowledgeBaseId,
    String documentPath,
    String documentType,
    Map<String, Object> metadata,
    Instant createdAt
) implements Serializable {

    public DocumentIndexMessage {
        if (tenantId == null || tenantId <= 0L) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
    }

    /**
     * 创建文档索引消息
     */
    public static DocumentIndexMessage create(
        long tenantId,
        String taskId,
        Long documentId,
        Long knowledgeBaseId,
        String documentPath,
        String documentType,
        Map<String, Object> metadata
    ) {
        String messageId = java.util.UUID.randomUUID().toString().replace("-", "");
        return new DocumentIndexMessage(
            tenantId,
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
    public static DocumentIndexMessage simple(long tenantId, String taskId, Long documentId, Long knowledgeBaseId) {
        return create(tenantId, taskId, documentId, knowledgeBaseId, null, null, Map.of());
    }
}

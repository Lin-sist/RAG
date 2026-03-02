package com.enterprise.rag.common.async.mq;

import com.enterprise.rag.common.async.AsyncTaskManager;
import com.enterprise.rag.common.async.TaskState;
import com.enterprise.rag.common.async.TaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 默认文档索引消息消费者
 * <p>
 * 提供基础的消息处理逻辑，实际的文档索引逻辑需要由具体实现类完成。
 * 这是一个模板实现，子类可以覆盖 processDocument 方法。
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "documentIndexConsumerImpl")
@RequiredArgsConstructor
public class DefaultDocumentIndexConsumer implements DocumentIndexConsumer {

    private final AsyncTaskManager asyncTaskManager;

    @Override
    public void consume(DocumentIndexMessage message) {
        log.info("Consuming document index message: messageId={}, documentId={}, taskId={}", 
            message.messageId(), message.documentId(), message.taskId());
        
        String taskId = message.taskId();
        
        try {
            // 更新任务进度
            asyncTaskManager.updateProgress(taskId, 10, "开始处理文档");
            
            // 处理文档（子类可覆盖）
            processDocument(message);
            
            // 更新任务进度
            asyncTaskManager.updateProgress(taskId, 100, "文档处理完成");
            
            log.info("Document index message processed successfully: messageId={}", message.messageId());
            
        } catch (Exception e) {
            log.error("Failed to process document index message: messageId={}", message.messageId(), e);
            onError(message, e);
            throw e;
        }
    }

    @Override
    public void onError(DocumentIndexMessage message, Throwable error) {
        log.error("Document index error: messageId={}, documentId={}, error={}", 
            message.messageId(), message.documentId(), error.getMessage());
        
        // 更新任务状态为失败
        String taskId = message.taskId();
        if (taskId != null) {
            Optional<TaskStatus> statusOpt = asyncTaskManager.getStatus(taskId);
            if (statusOpt.isPresent() && statusOpt.get().state() != TaskState.FAILED) {
                // 任务状态会在 AsyncTaskManager 中自动更新为失败
                log.debug("Task status will be updated to FAILED by AsyncTaskManager");
            }
        }
    }

    /**
     * 处理文档
     * <p>
     * 子类应覆盖此方法实现具体的文档索引逻辑。
     *
     * @param message 文档索引消息
     */
    protected void processDocument(DocumentIndexMessage message) {
        // 默认实现：仅记录日志
        log.info("Processing document: documentId={}, knowledgeBaseId={}", 
            message.documentId(), message.knowledgeBaseId());
        
        // 模拟处理过程
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

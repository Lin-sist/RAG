package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.storage.IndexInputState;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * C5a durable input 的有界清理协调；不触发 parser、embedding 或 vector mutation。
 */
@Slf4j
@Component
public class IndexInputCleanupCoordinator {

    private final DocumentMapper documentMapper;
    private final DocumentService documentService;
    private final IndexInputStore inputStore;
    private final int batchSize;

    public IndexInputCleanupCoordinator(
            DocumentMapper documentMapper,
            DocumentService documentService,
            IndexInputStore inputStore,
            @Value("${document.index-task-reconciliation.batch-size:20}") int batchSize) {
        this.documentMapper = documentMapper;
        this.documentService = documentService;
        this.inputStore = inputStore;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${document.index-task-reconciliation.scan-interval-ms:30000}")
    public void reconcileOnce() {
        Iterable<Document> pendingDocuments;
        try {
            pendingDocuments = documentMapper.findCleanupPending(batchSize);
        } catch (RuntimeException e) {
            log.warn("索引输入清理扫描暂不可用: errorType={}", e.getClass().getSimpleName());
            return;
        }
        for (Document document : pendingDocuments) {
            try {
                IndexInputStore.DeleteResult result = inputStore.delete(document.getFilePath());
                if (result == IndexInputStore.DeleteResult.DELETED
                        || result == IndexInputStore.DeleteResult.ALREADY_MISSING) {
                    documentService.updateInputState(document.getId(), IndexInputState.CLEANED.name());
                }
            } catch (RuntimeException e) {
                log.warn("索引输入清理重试失败: documentId={}, errorType={}",
                        document.getId(), e.getClass().getSimpleName());
            }
        }
    }
}

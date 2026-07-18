package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 隔离 C5 durable ledger 启用前遗留的无 ledger 文档。
 *
 * <p>这里只改变 document 的稳定诊断状态，不合成任务、不读取或删除输入，
 * 也不触发 parser、embedding、vector 等下游副作用。</p>
 */
@Slf4j
@Component
public class LegacyIndexTaskReconciliationCoordinator {

    private final DocumentMapper documentMapper;
    private final int batchSize;

    public LegacyIndexTaskReconciliationCoordinator(
            DocumentMapper documentMapper,
            @Value("${document.index-task-reconciliation.batch-size:20}") int batchSize) {
        this.documentMapper = documentMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${document.index-task-reconciliation.scan-interval-ms:30000}")
    public void reconcileOnce() {
        Iterable<Document> legacyDocuments;
        try {
            legacyDocuments = documentMapper.findLegacyUnledgered(batchSize);
        } catch (RuntimeException e) {
            log.warn("legacy 索引文档扫描暂不可用: errorType={}", e.getClass().getSimpleName());
            return;
        }
        for (Document document : legacyDocuments) {
            try {
                documentMapper.quarantineLegacyUnledgered(document.getId());
            } catch (RuntimeException e) {
                log.warn("legacy 索引文档隔离失败: documentId={}, errorType={}",
                        document.getId(), e.getClass().getSimpleName());
            }
        }
    }
}

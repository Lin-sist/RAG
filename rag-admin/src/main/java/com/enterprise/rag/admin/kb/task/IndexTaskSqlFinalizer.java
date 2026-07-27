package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.DocumentStatus;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.IndexTaskMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * C5 索引任务的单一 SQL 收尾事务。
 */
@Service
@RequiredArgsConstructor
public class IndexTaskSqlFinalizer {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final IndexTaskMapper taskMapper;

    @Transactional
    public void finalizeSql(long tenantId,
            String taskId,
            long kbId,
            long documentId,
            String contentHash,
            List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Index finalization requires at least one chunk");
        }
        if (tenantId <= 0) {
            throw new IllegalArgumentException("tenantId must be positive");
        }
        IndexTaskRecord task = taskMapper.lockByTenantAndTaskIdForUpdate(tenantId, taskId);
        if (task == null) {
            throw new IllegalStateException("Index task does not exist in tenant scope: " + taskId);
        }
        Document document = documentMapper.lockByTenantAndIdForUpdate(tenantId, documentId);
        if (document == null) {
            throw new IllegalStateException("Index document does not exist: " + documentId);
        }
        if (!Long.valueOf(tenantId).equals(task.getTenantId())
                || !Long.valueOf(tenantId).equals(document.getTenantId())
                || !Long.valueOf(documentId).equals(task.getDocumentId())
                || !Long.valueOf(kbId).equals(document.getKbId())
                || !java.util.Objects.equals(task.getOwnerId(), document.getUploaderId())) {
            throw new IllegalStateException("Index task scope does not match document facts: " + taskId);
        }

        if (!DocumentStatus.COMPLETED.name().equals(document.getStatus())) {
            int existingChunkCount = chunkMapper.countActiveByTenantAndDocumentId(tenantId, documentId);
            if (existingChunkCount == 0) {
                for (DocumentChunk chunk : chunks) {
                    if (!Long.valueOf(documentId).equals(chunk.getDocumentId())) {
                        throw new IllegalStateException("Index chunk document scope mismatch: " + taskId);
                    }
                    chunk.setTenantId(tenantId);
                    requireSingleUpdate(chunkMapper.insertFinalizationChunk(chunk),
                            "Index chunk was not persisted: " + documentId + "/" + chunk.getChunkIndex());
                }
            } else if (existingChunkCount != chunks.size()) {
                throw new IllegalStateException("Existing chunk facts do not match finalization: " + documentId);
            }
            requireSingleUpdate(
                    documentMapper.finalizeIndexDocument(tenantId, documentId, contentHash, chunks.size()),
                    "Index document was not finalized: " + documentId);
            requireSingleUpdate(
                    knowledgeBaseMapper.incrementDocumentCount(tenantId, kbId),
                    "Knowledge base document count was not incremented: " + kbId);
        }

        int completed = taskMapper.completeFinalization(tenantId, taskId);
        if (completed != 1 && !taskMapper.isCompleted(tenantId, taskId)) {
            throw new IllegalStateException("Index task was not completed: " + taskId);
        }
    }

    private void requireSingleUpdate(int updated, String message) {
        if (updated != 1) {
            throw new IllegalStateException(message);
        }
    }
}

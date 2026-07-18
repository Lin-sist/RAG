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
    public void finalizeSql(String taskId,
            long kbId,
            long documentId,
            String contentHash,
            List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Index finalization requires at least one chunk");
        }
        Document document = documentMapper.lockByIdForUpdate(documentId);
        if (document == null) {
            throw new IllegalStateException("Index document does not exist: " + documentId);
        }

        if (!DocumentStatus.COMPLETED.name().equals(document.getStatus())) {
            int existingChunkCount = chunkMapper.countActiveByDocumentId(documentId);
            if (existingChunkCount == 0) {
                for (DocumentChunk chunk : chunks) {
                    requireSingleUpdate(chunkMapper.insertFinalizationChunk(chunk),
                            "Index chunk was not persisted: " + documentId + "/" + chunk.getChunkIndex());
                }
            } else if (existingChunkCount != chunks.size()) {
                throw new IllegalStateException("Existing chunk facts do not match finalization: " + documentId);
            }
            requireSingleUpdate(
                    documentMapper.finalizeIndexDocument(documentId, contentHash, chunks.size()),
                    "Index document was not finalized: " + documentId);
            requireSingleUpdate(
                    knowledgeBaseMapper.incrementDocumentCount(kbId),
                    "Knowledge base document count was not incremented: " + kbId);
        }

        int completed = taskMapper.completeFinalization(taskId);
        if (completed != 1 && !taskMapper.isCompleted(taskId)) {
            throw new IllegalStateException("Index task was not completed: " + taskId);
        }
    }

    private void requireSingleUpdate(int updated, String message) {
        if (updated != 1) {
            throw new IllegalStateException(message);
        }
    }
}

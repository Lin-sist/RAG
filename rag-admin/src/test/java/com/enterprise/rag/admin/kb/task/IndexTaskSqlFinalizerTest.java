package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.IndexTaskMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexTaskSqlFinalizerTest {

    private static final long TENANT_ID = 901L;
    private static final long OWNER_ID = 1001L;

    @Test
    void repeatedFinalizeIncrementsDocumentCountAndInsertsChunksOnlyOnce() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentChunkMapper chunkMapper = mock(DocumentChunkMapper.class);
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        IndexTaskMapper taskMapper = mock(IndexTaskMapper.class);
        Document processing = document(180L, "PROCESSING");
        Document completed = document(180L, "COMPLETED");
        IndexTaskRecord task = task("task-180", 180L);
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(180L);
        chunk.setChunkIndex(0);
        chunk.setVectorId("vector-180-0");
        chunk.setContent("content");
        when(taskMapper.lockByTenantAndTaskIdForUpdate(TENANT_ID, "task-180")).thenReturn(task);
        when(documentMapper.lockByTenantAndIdForUpdate(TENANT_ID, 180L)).thenReturn(processing, completed);
        when(chunkMapper.countActiveByTenantAndDocumentId(TENANT_ID, 180L)).thenReturn(0);
        when(chunkMapper.insertFinalizationChunk(chunk)).thenReturn(1);
        when(documentMapper.finalizeIndexDocument(TENANT_ID, 180L, "hash-180", 1)).thenReturn(1);
        when(knowledgeBaseMapper.incrementDocumentCount(TENANT_ID, 10L)).thenReturn(1);
        when(taskMapper.completeFinalization(TENANT_ID, "task-180")).thenReturn(1, 0);
        when(taskMapper.isCompleted(TENANT_ID, "task-180")).thenReturn(true);
        IndexTaskSqlFinalizer finalizer = new IndexTaskSqlFinalizer(
                documentMapper, chunkMapper, knowledgeBaseMapper, taskMapper);

        finalizer.finalizeSql(TENANT_ID, "task-180", 10L, 180L, "hash-180", List.of(chunk));
        finalizer.finalizeSql(TENANT_ID, "task-180", 10L, 180L, "hash-180", List.of(chunk));

        verify(chunkMapper, times(1)).insertFinalizationChunk(chunk);
        verify(documentMapper, times(1)).finalizeIndexDocument(TENANT_ID, 180L, "hash-180", 1);
        verify(knowledgeBaseMapper, times(1)).incrementDocumentCount(TENANT_ID, 10L);
        verify(taskMapper, times(2)).completeFinalization(TENANT_ID, "task-180");
        verify(chunkMapper, never()).deleteById(chunk.getId());
    }

    @Test
    void mapperReturningForeignTenantDocumentFailsBeforeChunkMutation() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentChunkMapper chunkMapper = mock(DocumentChunkMapper.class);
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        IndexTaskMapper taskMapper = mock(IndexTaskMapper.class);
        IndexTaskRecord task = task("task-foreign", 180L);
        Document foreignDocument = document(180L, "PROCESSING");
        foreignDocument.setTenantId(902L);
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(180L);
        chunk.setChunkIndex(0);
        when(taskMapper.lockByTenantAndTaskIdForUpdate(TENANT_ID, "task-foreign")).thenReturn(task);
        when(documentMapper.lockByTenantAndIdForUpdate(TENANT_ID, 180L)).thenReturn(foreignDocument);
        IndexTaskSqlFinalizer finalizer = new IndexTaskSqlFinalizer(
                documentMapper, chunkMapper, knowledgeBaseMapper, taskMapper);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> finalizer.finalizeSql(
                        TENANT_ID, "task-foreign", 10L, 180L, "hash", List.of(chunk)));

        assertTrue(exception.getMessage().contains("scope"));
        verify(chunkMapper, never()).insertFinalizationChunk(chunk);
    }

    private static Document document(long id, String status) {
        Document document = new Document();
        document.setId(id);
        document.setTenantId(TENANT_ID);
        document.setKbId(10L);
        document.setUploaderId(OWNER_ID);
        document.setStatus(status);
        return document;
    }

    private static IndexTaskRecord task(String taskId, long documentId) {
        IndexTaskRecord task = new IndexTaskRecord();
        task.setTenantId(TENANT_ID);
        task.setTaskId(taskId);
        task.setDocumentId(documentId);
        task.setOwnerId(OWNER_ID);
        task.setExecutionPhase(IndexTaskPhase.FINALIZING.name());
        return task;
    }
}

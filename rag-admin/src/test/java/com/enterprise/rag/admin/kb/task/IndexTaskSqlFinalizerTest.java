package com.enterprise.rag.admin.kb.task;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.IndexTaskMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexTaskSqlFinalizerTest {

    @Test
    void repeatedFinalizeIncrementsDocumentCountAndInsertsChunksOnlyOnce() {
        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentChunkMapper chunkMapper = mock(DocumentChunkMapper.class);
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        IndexTaskMapper taskMapper = mock(IndexTaskMapper.class);
        Document processing = document(180L, "PROCESSING");
        Document completed = document(180L, "COMPLETED");
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(180L);
        chunk.setChunkIndex(0);
        chunk.setVectorId("vector-180-0");
        chunk.setContent("content");
        when(documentMapper.lockByIdForUpdate(180L)).thenReturn(processing, completed);
        when(chunkMapper.countActiveByDocumentId(180L)).thenReturn(0);
        when(chunkMapper.insertFinalizationChunk(chunk)).thenReturn(1);
        when(documentMapper.finalizeIndexDocument(180L, "hash-180", 1)).thenReturn(1);
        when(knowledgeBaseMapper.incrementDocumentCount(10L)).thenReturn(1);
        when(taskMapper.completeFinalization("task-180")).thenReturn(1, 0);
        when(taskMapper.isCompleted("task-180")).thenReturn(true);
        IndexTaskSqlFinalizer finalizer = new IndexTaskSqlFinalizer(
                documentMapper, chunkMapper, knowledgeBaseMapper, taskMapper);

        finalizer.finalizeSql("task-180", 10L, 180L, "hash-180", List.of(chunk));
        finalizer.finalizeSql("task-180", 10L, 180L, "hash-180", List.of(chunk));

        verify(chunkMapper, times(1)).insertFinalizationChunk(chunk);
        verify(documentMapper, times(1)).finalizeIndexDocument(180L, "hash-180", 1);
        verify(knowledgeBaseMapper, times(1)).incrementDocumentCount(10L);
        verify(taskMapper, times(2)).completeFinalization("task-180");
        verify(chunkMapper, never()).deleteById(chunk.getId());
    }

    private static Document document(long id, String status) {
        Document document = new Document();
        document.setId(id);
        document.setStatus(status);
        return document;
    }
}

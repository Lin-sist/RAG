package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.VectorModelRebuildProperties;
import com.enterprise.rag.admin.kb.service.VectorModelRebuildService;
import com.enterprise.rag.core.embedding.EmbeddingModelIdentity;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VectorModelRebuildServiceTest {
    private VectorModelRebuildProperties properties;
    private KnowledgeBaseMapper kbMapper;
    private DocumentChunkMapper chunkMapper;
    private EmbeddingService embeddingService;
    private VectorStore vectorStore;
    private VectorModelRebuildService service;

    @BeforeEach
    void setUp() {
        properties = new VectorModelRebuildProperties();
        kbMapper = mock(KnowledgeBaseMapper.class);
        chunkMapper = mock(DocumentChunkMapper.class);
        embeddingService = mock(EmbeddingService.class);
        vectorStore = mock(VectorStore.class);
        service = new VectorModelRebuildService(properties, kbMapper, chunkMapper, embeddingService, vectorStore);
    }

    @Test
    void disabledGateStopsBeforeAnyReadOrExternalOperation() {
        assertThrows(IllegalStateException.class, () -> service.rebuild(901L, 7L, "c17g1"));
        verify(kbMapper, never()).selectByTenantAndId(901L, 7L);
        verify(embeddingService, never()).embedBatchUncached(eq(901L), anyList());
        verify(vectorStore, never()).createCollection(any(TenantVectorScope.class), eq(2048));
    }

    @Test
    void planFreezesFiftyItemsAndElevenHttpRequestsWithoutMutation() {
        arrangeSnapshot();

        VectorModelRebuildService.RebuildPlan plan = service.plan(901L, 7L, "c17g1");

        assertEquals(50, plan.expectedCount());
        assertEquals(11, plan.maxHttpRequests());
        assertEquals("nvidia/nemotron-3-embed-1b", plan.targetIdentity().model());
        verify(embeddingService, never()).embedBatchUncached(eq(901L), anyList());
        verify(vectorStore, never()).createCollection(any(TenantVectorScope.class), eq(2048));
    }

    @Test
    void cleanReadBackIsRequiredBeforeAtomicSwitch() {
        arrangeSnapshot();
        properties.setEnabled(true);
        when(kbMapper.beginVectorModelRebuild(
                eq(901L), eq(7L), eq("legacy_vectors"), any(), any(), any(), any(), any(), any(), any(), any(),
                eq(2048), eq("c17g1"), eq(50L))).thenReturn(1);
        when(kbMapper.completeVectorModelRebuild(
                eq(901L), eq(7L), eq("legacy_vectors"), any(), eq("c17g1"), eq(50L), eq(50L)))
                .thenReturn(1);
        when(embeddingService.embedBatchUncached(eq(901L), anyList()))
                .thenAnswer(invocation -> vectors(invocation.<List<String>>getArgument(1).size()));
        ArgumentCaptor<List<VectorDocument>> documents = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.doNothing().when(vectorStore).upsert(any(TenantVectorScope.class), documents.capture());
        when(vectorStore.getByIds(any(TenantVectorScope.class), anyList()))
                .thenAnswer(ignored -> documents.getValue());
        when(vectorStore.count(any(TenantVectorScope.class))).thenReturn(50L);

        VectorModelRebuildService.RebuildResult result = service.rebuild(901L, 7L, "c17g1");

        assertEquals("MODEL_REBUILD_READY", result.status());
        assertEquals(50, documents.getValue().size());
        assertEquals("c17g1", documents.getValue().get(0).metadata().get("embeddingGeneration"));
        verify(embeddingService, org.mockito.Mockito.times(3)).embedBatchUncached(eq(901L), anyList());
    }

    private void arrangeSnapshot() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(7L);
        kb.setTenantId(901L);
        kb.setVectorCollection("legacy_vectors");
        kb.setVectorReadiness("READY");
        kb.setVectorModel("nvidia/legacy-embed");
        kb.setVectorRequestContract("legacy-v1");
        kb.setVectorGeneration("legacyg1");
        when(kbMapper.selectByTenantAndId(901L, 7L)).thenReturn(kb);
        when(chunkMapper.selectByTenantAndKnowledgeBaseId(901L, 7L)).thenReturn(chunks());
        when(embeddingService.getActiveModelIdentity()).thenReturn(new EmbeddingModelIdentity(
                "openai-compatible", "nvidia/nemotron-3-embed-1b",
                "https://integrate.api.nvidia.com/v1/embeddings",
                "nvidia-openai-embedding-v1", 2048));
        when(embeddingService.getMaxBatchSize()).thenReturn(5);
    }

    private List<DocumentChunk> chunks() {
        List<DocumentChunk> chunks = new ArrayList<>();
        int id = 0;
        for (int document = 0; document < 3; document++) {
            int count = List.of(11, 14, 25).get(document);
            for (int index = 0; index < count; index++) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(100L + document);
                chunk.setChunkIndex(index);
                chunk.setVectorId("vector-" + id);
                chunk.setContent("chunk-" + id++);
                chunks.add(chunk);
            }
        }
        return chunks;
    }

    private List<float[]> vectors(int count) {
        List<float[]> vectors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            vectors.add(new float[2048]);
        }
        return vectors;
    }
}

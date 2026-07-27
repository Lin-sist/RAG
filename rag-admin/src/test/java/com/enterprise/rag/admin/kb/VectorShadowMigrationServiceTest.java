package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.VectorShadowMaintenanceProperties;
import com.enterprise.rag.admin.kb.service.VectorShadowMigrationService;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.maintenance.LegacyVectorSourceReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VectorShadowMigrationServiceTest {

    private final VectorShadowMaintenanceProperties properties = new VectorShadowMaintenanceProperties();
    private final KnowledgeBaseMapper kbMapper = mock(KnowledgeBaseMapper.class);
    private final DocumentChunkMapper chunkMapper = mock(DocumentChunkMapper.class);
    private final LegacyVectorSourceReader sourceReader = mock(LegacyVectorSourceReader.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    private final VectorShadowMigrationService service = new VectorShadowMigrationService(
            properties, kbMapper, chunkMapper, sourceReader, vectorStore);

    @BeforeEach
    void setUp() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(10L);
        kb.setTenantId(77L);
        kb.setVectorCollection("legacy_vectors");
        kb.setVectorReadiness("LEGACY_PENDING");
        when(kbMapper.selectByTenantAndId(77L, 10L)).thenReturn(kb);
    }

    @Test
    void defaultOffGateShouldPreventAnySourceReadOrVectorWrite() {
        assertThrows(IllegalStateException.class, () -> service.migrate(77L, 10L));

        verify(sourceReader, never()).readByIds(any(), anyList());
        verify(vectorStore, never()).createCollection(any(TenantVectorScope.class), anyInt());
    }

    @Test
    void missingLegacyVectorShouldPersistAuditFailureWithoutCreatingShadow() {
        properties.setEnabled(true);
        when(kbMapper.beginVectorShadowCopy(77L, 10L, "legacy_vectors",
                "tenant_77_kb_10_shadow_v1", 1L)).thenReturn(1);
        when(chunkMapper.selectByTenantAndKnowledgeBaseId(77L, 10L)).thenReturn(List.of(chunk()));
        when(sourceReader.readByIds("legacy_vectors", List.of("vector-1"))).thenReturn(List.of());

        VectorShadowMigrationService.MigrationReport report = service.migrate(77L, 10L);

        assertEquals("AUDIT_FAILED", report.status());
        assertEquals(1, report.missing());
        verify(kbMapper).failVectorShadowCopy(
                77L, 10L, 0L, 0L, 1L, 0L, "source_audit_mismatch");
        verify(vectorStore, never()).createCollection(any(TenantVectorScope.class), anyInt());
    }

    @Test
    void fullyAuditedShadowShouldSwitchMappingOnlyAfterReadBackMatches() {
        properties.setEnabled(true);
        DocumentChunk chunk = chunk();
        VectorDocument vector = new VectorDocument(
                "vector-1", new float[] {0.1f, 0.2f}, "content",
                Map.of("kbId", 10.0d, "documentId", 20.0d));
        TenantVectorScope shadow = new TenantVectorScope(77L, 10L, "tenant_77_kb_10_shadow_v1");
        when(kbMapper.beginVectorShadowCopy(77L, 10L, "legacy_vectors",
                shadow.collectionName(), 1L)).thenReturn(1);
        when(chunkMapper.selectByTenantAndKnowledgeBaseId(77L, 10L)).thenReturn(List.of(chunk));
        when(sourceReader.readByIds("legacy_vectors", List.of("vector-1"))).thenReturn(List.of(vector));
        when(sourceReader.readDimension("legacy_vectors")).thenReturn(2);
        when(vectorStore.count(shadow)).thenReturn(1L);
        when(vectorStore.getByIds(shadow, List.of("vector-1"))).thenReturn(List.of(vector));
        when(kbMapper.completeVectorShadowSwitch(
                77L, 10L, "legacy_vectors", shadow.collectionName(), 1L, 1L)).thenReturn(1);

        VectorShadowMigrationService.MigrationReport report = service.migrate(77L, 10L);

        assertEquals("READY", report.status());
        verify(vectorStore).createCollection(shadow, 2);
        verify(vectorStore).upsert(shadow, List.of(vector));
        verify(kbMapper).completeVectorShadowSwitch(
                77L, 10L, "legacy_vectors", shadow.collectionName(), 1L, 1L);
    }

    @Test
    void emptyKnowledgeBaseShouldPreserveSourceVectorDimension() {
        properties.setEnabled(true);
        TenantVectorScope shadow = new TenantVectorScope(77L, 10L, "tenant_77_kb_10_shadow_v1");
        when(kbMapper.beginVectorShadowCopy(77L, 10L, "legacy_vectors",
                shadow.collectionName(), 0L)).thenReturn(1);
        when(chunkMapper.selectByTenantAndKnowledgeBaseId(77L, 10L)).thenReturn(List.of());
        when(sourceReader.readDimension("legacy_vectors")).thenReturn(1536);
        when(vectorStore.count(shadow)).thenReturn(0L);
        when(kbMapper.completeVectorShadowSwitch(
                77L, 10L, "legacy_vectors", shadow.collectionName(), 0L, 0L)).thenReturn(1);

        VectorShadowMigrationService.MigrationReport report = service.migrate(77L, 10L);

        assertEquals("READY", report.status());
        verify(vectorStore).createCollection(shadow, 1536);
        verify(vectorStore, never()).upsert(any(TenantVectorScope.class), anyList());
    }

    private DocumentChunk chunk() {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setTenantId(77L);
        chunk.setDocumentId(20L);
        chunk.setVectorId("vector-1");
        chunk.setContent("content");
        return chunk;
    }
}

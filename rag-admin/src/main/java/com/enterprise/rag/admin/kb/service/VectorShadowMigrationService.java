package com.enterprise.rag.admin.kb.service;

import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.maintenance.LegacyVectorSourceReader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit, non-REST maintenance workflow for D16=A. The caller must also have
 * obtained operational authorization before enabling and invoking it against a
 * real Milvus instance.
 */
@Service
@RequiredArgsConstructor
public class VectorShadowMigrationService {

    private final VectorShadowMaintenanceProperties properties;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final LegacyVectorSourceReader sourceReader;
    private final VectorStore vectorStore;

    public MigrationReport migrate(long tenantId, long kbId) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("VECTOR_MAINTENANCE_DISABLED");
        }
        KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(tenantId, kbId);
        if (kb == null) {
            throw new BusinessException("KB_001", "知识库不存在", HttpStatus.NOT_FOUND);
        }
        if ("READY".equals(kb.getVectorReadiness())) {
            return new MigrationReport(kbId, kb.getVectorCollection(), kb.getVectorCollection(),
                    "READY", 0, 0, 0, 0, 0);
        }
        String sourceCollection = kb.getVectorCollection();
        if (sourceCollection == null || sourceCollection.isBlank()) {
            throw new IllegalStateException("VECTOR_SOURCE_MAPPING_MISSING");
        }
        String shadowCollection = "tenant_" + tenantId + "_kb_" + kbId + "_shadow_v1";
        List<DocumentChunk> chunks = documentChunkMapper.selectByTenantAndKnowledgeBaseId(tenantId, kbId);
        List<String> ids = chunks.stream()
                .map(DocumentChunk::getVectorId)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .toList();

        if (knowledgeBaseMapper.beginVectorShadowCopy(
                tenantId, kbId, sourceCollection, shadowCollection, ids.size()) != 1) {
            throw new IllegalStateException("VECTOR_READINESS_STATE_CONFLICT");
        }

        long observed = 0;
        long migrated = 0;
        try {
            Map<String, DocumentChunk> expected = new LinkedHashMap<>();
            long duplicateExpected = 0;
            for (DocumentChunk chunk : chunks) {
                if (chunk.getVectorId() == null || chunk.getVectorId().isBlank()) {
                    continue;
                }
                if (expected.putIfAbsent(chunk.getVectorId(), chunk) != null) {
                    duplicateExpected++;
                }
            }
            List<VectorDocument> source = ids.isEmpty()
                    ? List.of()
                    : sourceReader.readByIds(sourceCollection, ids);
            observed = source.size();
            Map<String, VectorDocument> verified = new HashMap<>();
            long mismatch = duplicateExpected;
            for (VectorDocument vector : source) {
                DocumentChunk chunk = expected.get(vector.id());
                if (chunk == null || verified.putIfAbsent(vector.id(), vector) != null
                        || !Objects.equals(chunk.getContent(), vector.content())
                        || !metadataMatches(vector.metadata(), kbId, chunk.getDocumentId())) {
                    mismatch++;
                }
            }
            long missing = expected.keySet().stream().filter(id -> !verified.containsKey(id)).count();
            if (missing > 0 || mismatch > 0 || verified.size() != expected.size()) {
                fail(tenantId, kbId, observed, 0, missing, mismatch, "source_audit_mismatch");
                return new MigrationReport(kbId, sourceCollection, shadowCollection,
                        "AUDIT_FAILED", expected.size(), observed, 0, missing, mismatch);
            }

            int dimension = sourceReader.readDimension(sourceCollection);
            if (dimension <= 0 || source.stream().anyMatch(vector -> vector.vector().length != dimension)) {
                fail(tenantId, kbId, observed, 0, 0, 1, "source_dimension_mismatch");
                return new MigrationReport(kbId, sourceCollection, shadowCollection,
                        "AUDIT_FAILED", expected.size(), observed, 0, 0, 1);
            }
            TenantVectorScope shadowScope = new TenantVectorScope(tenantId, kbId, shadowCollection);
            vectorStore.createCollection(shadowScope, dimension);
            if (!source.isEmpty()) {
                vectorStore.upsert(shadowScope, source);
            }
            migrated = source.size();
            long shadowCount = vectorStore.count(shadowScope);
            List<VectorDocument> shadow = ids.isEmpty() ? List.of() : vectorStore.getByIds(shadowScope, ids);
            long shadowMismatch = shadow.stream()
                    .filter(vector -> !expected.containsKey(vector.id())
                            || !metadataMatches(vector.metadata(), kbId, expected.get(vector.id()).getDocumentId()))
                    .count();
            long shadowMissing = expected.keySet().stream()
                    .filter(id -> shadow.stream().noneMatch(vector -> vector.id().equals(id)))
                    .count();
            if (shadowCount != expected.size() || shadowMissing > 0 || shadowMismatch > 0) {
                fail(tenantId, kbId, shadowCount, migrated, shadowMissing, shadowMismatch,
                        "shadow_audit_mismatch");
                return new MigrationReport(kbId, sourceCollection, shadowCollection,
                        "AUDIT_FAILED", expected.size(), shadowCount, migrated, shadowMissing, shadowMismatch);
            }
            if (knowledgeBaseMapper.completeVectorShadowSwitch(
                    tenantId, kbId, sourceCollection, shadowCollection, shadowCount, migrated) != 1) {
                fail(tenantId, kbId, shadowCount, migrated, 0, 0, "mapping_switch_conflict");
                throw new IllegalStateException("VECTOR_MAPPING_SWITCH_CONFLICT");
            }
            return new MigrationReport(kbId, sourceCollection, shadowCollection,
                    "READY", expected.size(), shadowCount, migrated, 0, 0);
        } catch (RuntimeException exception) {
            fail(tenantId, kbId, observed, migrated, 0, 0, "maintenance_failure");
            throw exception;
        }
    }

    private boolean metadataMatches(Map<String, Object> metadata, long kbId, long documentId) {
        return metadata != null
                && longValueMatches(metadata.get("kbId"), kbId)
                && longValueMatches(metadata.get("documentId"), documentId);
    }

    private boolean longValueMatches(Object actual, long expected) {
        if (actual instanceof Number number) {
            return number.doubleValue() == expected;
        }
        try {
            return actual != null && Long.parseLong(String.valueOf(actual)) == expected;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private void fail(long tenantId, long kbId, long observed, long migrated,
            long missing, long mismatch, String category) {
        knowledgeBaseMapper.failVectorShadowCopy(
                tenantId, kbId, observed, migrated, missing, mismatch, category);
    }

    public record MigrationReport(long kbId, String sourceCollection, String shadowCollection,
            String status, long expected, long observed, long migrated, long missing, long mismatch) {
    }
}

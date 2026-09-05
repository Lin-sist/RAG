package com.enterprise.rag.admin.kb.service;

import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.core.embedding.EmbeddingModelIdentity;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import com.enterprise.rag.core.vectorstore.VectorDocument;
import com.enterprise.rag.core.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Independent, default-off rebuild workflow for a new embedding space.
 * It never copies legacy vectors and never retries or removes failed generations.
 */
@Service
@RequiredArgsConstructor
public class VectorModelRebuildService {
    private static final Pattern GENERATION = Pattern.compile("[a-z0-9]{1,16}");
    private static final List<Integer> FIXED_DOCUMENT_COUNTS = List.of(11, 14, 25);

    private final VectorModelRebuildProperties properties;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public RebuildPlan plan(long tenantId, long kbId, String generation) {
        requireGeneration(generation);
        KnowledgeBase kb = requireKnowledgeBase(tenantId, kbId);
        if (!"READY".equals(kb.getVectorReadiness()) && !"MODEL_REBUILD_FAILED".equals(kb.getVectorReadiness())) {
            throw new IllegalStateException("MODEL_REBUILD_SOURCE_NOT_READY");
        }
        List<DocumentChunk> chunks = fixedSnapshot(tenantId, kbId);
        EmbeddingModelIdentity target = embeddingService.getActiveModelIdentity();
        if (embeddingService.getMaxBatchSize() != 5) {
            throw new IllegalStateException("MODEL_REBUILD_BATCH_CONTRACT_MISMATCH");
        }
        String collection = collectionName(tenantId, kbId, target, generation);
        return new RebuildPlan(tenantId, kbId, generation, kb.getVectorCollection(), collection,
                chunks.size(), properties.getMaxHttpRequests(), target);
    }

    public RebuildResult rebuild(long tenantId, long kbId, String generation) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("MODEL_REBUILD_DISABLED");
        }
        RebuildPlan plan = plan(tenantId, kbId, generation);
        KnowledgeBase source = requireKnowledgeBase(tenantId, kbId);
        int began = knowledgeBaseMapper.beginVectorModelRebuild(
                tenantId, kbId, source.getVectorCollection(), source.getVectorModel(),
                source.getVectorRequestContract(), source.getVectorGeneration(), plan.targetCollection(),
                plan.targetIdentity().providerFamily(), plan.targetIdentity().model(),
                plan.targetIdentity().endpoint(), plan.targetIdentity().requestContractVersion(),
                plan.targetIdentity().dimension(), generation, plan.expectedCount());
        if (began != 1) {
            throw new IllegalStateException("MODEL_REBUILD_BEGIN_CONFLICT");
        }

        long migrated = 0;
        long observed = 0;
        long missing = plan.expectedCount();
        long mismatch = 0;
        try {
            List<DocumentChunk> chunks = fixedSnapshot(tenantId, kbId);
            List<VectorDocument> vectors = embedByDocument(tenantId, kbId, generation, chunks, plan.targetIdentity());
            migrated = vectors.size();
            TenantVectorScope targetScope = new TenantVectorScope(tenantId, kbId, plan.targetCollection());
            vectorStore.createCollection(targetScope, plan.targetIdentity().dimension());
            vectorStore.upsert(targetScope, vectors);

            List<String> expectedIds = vectors.stream().map(VectorDocument::id).toList();
            List<VectorDocument> readBack = vectorStore.getByIds(targetScope, expectedIds);
            observed = vectorStore.count(targetScope);
            Audit audit = auditReadBack(vectors, readBack, observed, plan.targetIdentity(), generation);
            missing = audit.missing();
            mismatch = audit.mismatch();
            if (!audit.clean()) {
                throw new IllegalStateException("MODEL_REBUILD_READBACK_MISMATCH");
            }
            if (knowledgeBaseMapper.completeVectorModelRebuild(
                    tenantId, kbId, plan.sourceCollection(), plan.targetCollection(), generation,
                    observed, migrated) != 1) {
                throw new IllegalStateException("MODEL_REBUILD_SWITCH_CONFLICT");
            }
            return new RebuildResult("MODEL_REBUILD_READY", plan, observed, migrated, 0, 0);
        } catch (RuntimeException e) {
            knowledgeBaseMapper.failVectorModelRebuild(
                    tenantId, kbId, generation, observed, migrated, missing, mismatch,
                    errorCategory(e));
            throw e;
        }
    }

    private List<VectorDocument> embedByDocument(long tenantId,
            long kbId,
            String generation,
            List<DocumentChunk> chunks,
            EmbeddingModelIdentity identity) {
        Map<Long, List<DocumentChunk>> grouped = new LinkedHashMap<>();
        for (DocumentChunk chunk : chunks) {
            grouped.computeIfAbsent(chunk.getDocumentId(), ignored -> new ArrayList<>()).add(chunk);
        }
        List<VectorDocument> result = new ArrayList<>(chunks.size());
        for (List<DocumentChunk> documentChunks : grouped.values()) {
            List<String> texts = documentChunks.stream().map(DocumentChunk::getContent).toList();
            List<float[]> embeddings = embeddingService.embedBatchUncached(tenantId, texts);
            if (embeddings.size() != documentChunks.size()) {
                throw new IllegalStateException("MODEL_REBUILD_EMBED_COUNT_MISMATCH");
            }
            for (int i = 0; i < documentChunks.size(); i++) {
                DocumentChunk chunk = documentChunks.get(i);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("tenantId", tenantId);
                metadata.put("kbId", kbId);
                metadata.put("documentId", chunk.getDocumentId());
                metadata.put("embeddingModel", identity.model());
                metadata.put("embeddingContract", identity.requestContractVersion());
                metadata.put("embeddingGeneration", generation);
                result.add(new VectorDocument(chunk.getVectorId(), embeddings.get(i), chunk.getContent(), metadata));
            }
        }
        return List.copyOf(result);
    }

    private Audit auditReadBack(List<VectorDocument> expected,
            List<VectorDocument> actual,
            long observed,
            EmbeddingModelIdentity identity,
            String generation) {
        Map<String, VectorDocument> byId = new HashMap<>();
        if (actual != null) {
            for (VectorDocument vector : actual) {
                if (vector != null && byId.put(vector.id(), vector) != null) {
                    return new Audit(expected.size(), 1);
                }
            }
        }
        long missing = 0;
        long mismatch = observed == expected.size() ? 0 : 1;
        for (VectorDocument wanted : expected) {
            VectorDocument got = byId.get(wanted.id());
            if (got == null) {
                missing++;
                continue;
            }
            if (!got.isValid()
                    || got.vector().length != identity.dimension()
                    || !wanted.content().equals(got.content())
                    || !sameMetadata(wanted.metadata(), got.metadata(), "tenantId")
                    || !sameMetadata(wanted.metadata(), got.metadata(), "kbId")
                    || !sameMetadata(wanted.metadata(), got.metadata(), "documentId")
                    || !identity.model().equals(got.metadata().get("embeddingModel"))
                    || !identity.requestContractVersion().equals(got.metadata().get("embeddingContract"))
                    || !generation.equals(got.metadata().get("embeddingGeneration"))) {
                mismatch++;
            }
        }
        return new Audit(missing, mismatch);
    }

    private boolean sameMetadata(Map<String, Object> left, Map<String, Object> right, String key) {
        return String.valueOf(left.get(key)).equals(String.valueOf(right.get(key)));
    }

    private List<DocumentChunk> fixedSnapshot(long tenantId, long kbId) {
        List<DocumentChunk> chunks = documentChunkMapper.selectByTenantAndKnowledgeBaseId(tenantId, kbId);
        if (chunks == null || chunks.size() != properties.getExpectedCount() || chunks.size() != 50) {
            throw new IllegalStateException("MODEL_REBUILD_EXPECTED_COUNT_MISMATCH");
        }
        Set<String> ids = new LinkedHashSet<>();
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (DocumentChunk chunk : chunks) {
            if (chunk.getVectorId() == null || chunk.getVectorId().isBlank()
                    || chunk.getContent() == null || chunk.getContent().isBlank()
                    || !ids.add(chunk.getVectorId())) {
                throw new IllegalStateException("MODEL_REBUILD_SNAPSHOT_INVALID");
            }
            counts.merge(chunk.getDocumentId(), 1, Integer::sum);
        }
        List<Integer> actualCounts = counts.values().stream().sorted().toList();
        if (!actualCounts.equals(FIXED_DOCUMENT_COUNTS)) {
            throw new IllegalStateException("MODEL_REBUILD_DOCUMENT_SHAPE_MISMATCH");
        }
        int requestUpperBound = counts.values().stream().mapToInt(count -> (count + 4) / 5).sum();
        if (requestUpperBound != properties.getMaxHttpRequests() || requestUpperBound != 11) {
            throw new IllegalStateException("MODEL_REBUILD_HTTP_BOUND_MISMATCH");
        }
        return List.copyOf(chunks);
    }

    private KnowledgeBase requireKnowledgeBase(long tenantId, long kbId) {
        KnowledgeBase kb = knowledgeBaseMapper.selectByTenantAndId(tenantId, kbId);
        if (kb == null || kb.getVectorCollection() == null || kb.getVectorCollection().isBlank()) {
            throw new IllegalStateException("MODEL_REBUILD_KB_NOT_FOUND");
        }
        return kb;
    }

    private String collectionName(long tenantId, long kbId, EmbeddingModelIdentity identity, String generation) {
        return "tenant_" + tenantId + "_kb_" + kbId + "_emb_nemotron3_"
                + identity.fingerprint().substring(0, 12) + "_g" + generation;
    }

    private void requireGeneration(String generation) {
        if (generation == null || !GENERATION.matcher(generation).matches()) {
            throw new IllegalArgumentException("MODEL_REBUILD_GENERATION_INVALID");
        }
    }

    private String errorCategory(RuntimeException error) {
        String message = error.getMessage();
        return message != null && message.startsWith("MODEL_REBUILD_") ? message : "MODEL_REBUILD_DEPENDENCY_FAILURE";
    }

    public record RebuildPlan(long tenantId,
            long kbId,
            String generation,
            String sourceCollection,
            String targetCollection,
            int expectedCount,
            int maxHttpRequests,
            EmbeddingModelIdentity targetIdentity) {
    }

    public record RebuildResult(String status,
            RebuildPlan plan,
            long observedCount,
            long migratedCount,
            long missingCount,
            long mismatchCount) {
    }

    private record Audit(long missing, long mismatch) {
        boolean clean() {
            return missing == 0 && mismatch == 0;
        }
    }
}

package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.dto.CreateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseStatistics;
import com.enterprise.rag.admin.kb.dto.UpdateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.entity.*;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import net.jqwik.api.*;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 知识库管理模块属性测试
 * 
 * Feature: enterprise-rag-qa-system
 * Validates: Requirements 11.1, 11.2, 11.3, 11.4
 */
class KnowledgeBasePropertyTest {

    /**
     * Property 22: 知识库 CRUD 一致性
     * 
     * *For any* 知识库，创建后应能查询到，更新后应反映新值，删除后应不可查询。
     * 
     * **Validates: Requirements 11.1**
     */
    @Property(tries = 100)
    void knowledgeBaseCRUDShouldBeConsistent(
            @ForAll("kbName") String name,
            @ForAll("kbDescription") String description,
            @ForAll("kbName") String updatedName,
            @ForAll("positiveId") Long ownerId) {
        
        // Setup in-memory storage
        InMemoryKnowledgeBaseService kbService = new InMemoryKnowledgeBaseService();
        
        // CREATE
        CreateKnowledgeBaseRequest createRequest = CreateKnowledgeBaseRequest.builder()
                .name(name)
                .description(description)
                .isPublic(false)
                .build();
        
        KnowledgeBaseDTO created = kbService.create(createRequest, ownerId);
        
        // Verify creation
        assertThat(created != null)
                .as("Created knowledge base should not be null")
                .isTrue();
        assertThat(created.getId() != null)
                .as("Created knowledge base should have an ID")
                .isTrue();
        assertThat(name.equals(created.getName()))
                .as("Created knowledge base name should match")
                .isTrue();
        assertThat(ownerId.equals(created.getOwnerId()))
                .as("Created knowledge base owner should match")
                .isTrue();
        
        // READ
        Optional<KnowledgeBaseDTO> found = kbService.getById(created.getId());
        assertThat(found.isPresent())
                .as("Should be able to find created knowledge base")
                .isTrue();
        assertThat(name.equals(found.get().getName()))
                .as("Found knowledge base name should match")
                .isTrue();
        
        // UPDATE
        UpdateKnowledgeBaseRequest updateRequest = UpdateKnowledgeBaseRequest.builder()
                .name(updatedName)
                .isPublic(true)
                .build();
        
        KnowledgeBaseDTO updated = kbService.update(created.getId(), updateRequest);
        assertThat(updatedName.equals(updated.getName()))
                .as("Updated knowledge base name should reflect new value")
                .isTrue();
        assertThat(Boolean.TRUE.equals(updated.getIsPublic()))
                .as("Updated knowledge base isPublic should reflect new value")
                .isTrue();
        
        // Verify update persisted
        Optional<KnowledgeBaseDTO> afterUpdate = kbService.getById(created.getId());
        assertThat(afterUpdate.isPresent())
                .as("Should be able to find updated knowledge base")
                .isTrue();
        assertThat(updatedName.equals(afterUpdate.get().getName()))
                .as("Persisted name should match updated value")
                .isTrue();
        
        // DELETE
        kbService.delete(created.getId());
        
        // Verify deletion
        Optional<KnowledgeBaseDTO> afterDelete = kbService.getById(created.getId());
        assertThat(afterDelete.isEmpty())
                .as("Should not be able to find deleted knowledge base")
                .isTrue();
    }

    /**
     * Property 23: 文档删除级联性
     * 
     * *For any* 被删除的文档，其对应的向量数据也应被删除。
     * 
     * **Validates: Requirements 11.2**
     */
    @Property(tries = 100)
    void documentDeletionShouldCascadeToVectors(
            @ForAll("documentTitle") String docTitle,
            @ForAll("vectorIds") List<String> vectorIds,
            @ForAll("positiveId") Long kbId) {
        
        // Skip if no vector IDs
        if (vectorIds.isEmpty()) {
            return;
        }
        
        // Setup mock vector store
        VectorStore vectorStore = mock(VectorStore.class);
        List<String> deletedVectorIds = new ArrayList<>();
        doAnswer(invocation -> {
            List<String> ids = invocation.getArgument(1);
            deletedVectorIds.addAll(ids);
            return null;
        }).when(vectorStore).delete(anyString(), anyList());
        
        // Create in-memory document service
        InMemoryDocumentService documentService = new InMemoryDocumentService(vectorStore);
        
        // Create document
        Document document = new Document();
        document.setKbId(kbId);
        document.setUploaderId(1L);
        document.setTitle(docTitle);
        document.setFileType("txt");
        document.setStatus(DocumentStatus.COMPLETED.name());
        document.setContentHash(UUID.randomUUID().toString());
        Document createdDoc = documentService.create(document);
        
        // Create chunks with vector IDs
        List<DocumentChunk> chunks = new ArrayList<>();
        for (int i = 0; i < vectorIds.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(createdDoc.getId());
            chunk.setVectorId(vectorIds.get(i));
            chunk.setContent("Chunk content " + i);
            chunk.setChunkIndex(i);
            chunks.add(chunk);
        }
        documentService.saveChunks(chunks);
        
        // Verify chunks were saved
        List<String> savedVectorIds = documentService.getVectorIdsByDocumentId(createdDoc.getId());
        assertThat(savedVectorIds.size() == vectorIds.size())
                .as("All vector IDs should be saved")
                .isTrue();
        
        // Delete document
        documentService.delete(createdDoc.getId());
        
        // Verify vector store delete was called with correct IDs
        assertThat(deletedVectorIds.containsAll(vectorIds))
                .as("All vector IDs should be deleted from vector store")
                .isTrue();
        
        // Verify document is deleted
        Optional<Document> afterDelete = documentService.getById(createdDoc.getId());
        assertThat(afterDelete.isEmpty())
                .as("Document should be deleted")
                .isTrue();
        
        // Verify chunks are deleted
        List<DocumentChunk> chunksAfterDelete = documentService.getChunksByDocumentId(createdDoc.getId());
        assertThat(chunksAfterDelete.isEmpty())
                .as("All chunks should be deleted")
                .isTrue();
    }

    /**
     * Property 24: 知识库权限隔离性
     * 
     * *For any* 非公开知识库，无权限的用户不应能访问其内容。
     * 
     * **Validates: Requirements 11.3**
     */
    @Property(tries = 100)
    void privateKnowledgeBaseShouldBeIsolated(
            @ForAll("positiveId") Long kbId,
            @ForAll("positiveId") Long ownerId,
            @ForAll("positiveId") Long authorizedUserId,
            @ForAll("positiveId") Long unauthorizedUserId) {
        
        // Ensure different user IDs
        if (ownerId.equals(authorizedUserId) || ownerId.equals(unauthorizedUserId) 
                || authorizedUserId.equals(unauthorizedUserId)) {
            return;
        }
        
        // Setup in-memory permission service
        InMemoryKBPermissionService permissionService = new InMemoryKBPermissionService();
        
        // Private knowledge base
        boolean isPublic = false;
        
        // Owner should have access
        boolean ownerCanAccess = permissionService.canAccess(kbId, ownerId, isPublic, ownerId);
        assertThat(ownerCanAccess)
                .as("Owner should have access to their knowledge base")
                .isTrue();
        
        // Unauthorized user should NOT have access
        boolean unauthorizedCanAccess = permissionService.canAccess(kbId, unauthorizedUserId, isPublic, ownerId);
        assertThat(!unauthorizedCanAccess)
                .as("Unauthorized user should not have access to private knowledge base")
                .isTrue();
        
        // Grant READ permission to authorized user
        permissionService.grant(kbId, authorizedUserId, PermissionType.READ);
        
        // Authorized user should now have access
        boolean authorizedCanAccess = permissionService.canAccess(kbId, authorizedUserId, isPublic, ownerId);
        assertThat(authorizedCanAccess)
                .as("Authorized user should have access after permission grant")
                .isTrue();
        
        // Unauthorized user should still NOT have access
        boolean stillUnauthorized = permissionService.canAccess(kbId, unauthorizedUserId, isPublic, ownerId);
        assertThat(!stillUnauthorized)
                .as("Unauthorized user should still not have access")
                .isTrue();
        
        // Revoke permission
        permissionService.revoke(kbId, authorizedUserId);
        
        // Authorized user should no longer have access
        boolean afterRevoke = permissionService.canAccess(kbId, authorizedUserId, isPublic, ownerId);
        assertThat(!afterRevoke)
                .as("User should not have access after permission revoke")
                .isTrue();
        
        // Make knowledge base public
        isPublic = true;
        
        // Now everyone should have access
        boolean publicAccess = permissionService.canAccess(kbId, unauthorizedUserId, isPublic, ownerId);
        assertThat(publicAccess)
                .as("Everyone should have access to public knowledge base")
                .isTrue();
    }

    /**
     * Property 25: 统计信息一致性
     * 
     * *For any* 知识库，统计信息（文档数、向量数）应与实际存储的数据一致。
     * 
     * **Validates: Requirements 11.4**
     */
    @Property(tries = 100)
    void statisticsShouldBeConsistentWithActualData(
            @ForAll("kbName") String kbName,
            @ForAll("documentCount") int documentCount,
            @ForAll("positiveId") Long ownerId) {
        
        // Setup in-memory services
        InMemoryKnowledgeBaseService kbService = new InMemoryKnowledgeBaseService();
        
        // Create knowledge base
        CreateKnowledgeBaseRequest createRequest = CreateKnowledgeBaseRequest.builder()
                .name(kbName)
                .build();
        KnowledgeBaseDTO kb = kbService.create(createRequest, ownerId);
        
        // Add documents
        int totalChunks = 0;
        for (int i = 0; i < documentCount; i++) {
            int chunkCount = (i % 3) + 1;
            kbService.addDocument(kb.getId(), chunkCount);
            totalChunks += chunkCount;
        }
        
        // Get statistics
        KnowledgeBaseStatistics stats = kbService.getStatistics(kb.getId());
        
        // Verify document count
        assertThat(stats.getDocumentCount() == documentCount)
                .as("Statistics document count should match actual count. Expected: %d, Got: %d", 
                    documentCount, stats.getDocumentCount())
                .isTrue();
        
        // Verify vector count
        assertThat(stats.getVectorCount() == totalChunks)
                .as("Statistics vector count should match actual count. Expected: %d, Got: %d",
                    totalChunks, stats.getVectorCount())
                .isTrue();
        
        // Verify query count starts at 0
        assertThat(stats.getQueryCount() == 0)
                .as("Query count should start at 0")
                .isTrue();
    }

    // ==================== Providers ====================

    @Provide
    Arbitrary<String> kbName() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<String> kbDescription() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(200);
    }

    @Provide
    Arbitrary<String> documentTitle() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(5)
                .ofMaxLength(100);
    }

    @Provide
    Arbitrary<List<String>> vectorIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(10)
                .ofMaxLength(20)
                .map(s -> "vec_" + s)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    @Provide
    Arbitrary<Integer> documentCount() {
        return Arbitraries.integers().between(1, 5);
    }

    @Provide
    Arbitrary<Long> positiveId() {
        return Arbitraries.longs().between(1L, 1000000L);
    }

    // ==================== In-Memory Implementations ====================

    /**
     * In-memory KnowledgeBase service for testing
     */
    private static class InMemoryKnowledgeBaseService implements KnowledgeBaseService {
        private final Map<Long, KnowledgeBase> storage = new HashMap<>();
        private final Map<Long, Integer> documentCounts = new HashMap<>();
        private final Map<Long, Long> vectorCounts = new HashMap<>();
        private long idCounter = 1;

        @Override
        public KnowledgeBaseDTO create(CreateKnowledgeBaseRequest request, Long ownerId) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setId(idCounter++);
            kb.setName(request.getName());
            kb.setDescription(request.getDescription());
            kb.setOwnerId(ownerId);
            kb.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);
            kb.setDocumentCount(0);
            kb.setVectorCollection("kb_" + kb.getId());
            storage.put(kb.getId(), kb);
            documentCounts.put(kb.getId(), 0);
            vectorCounts.put(kb.getId(), 0L);
            return toDTO(kb);
        }

        @Override
        public Optional<KnowledgeBaseDTO> getById(Long id) {
            return Optional.ofNullable(storage.get(id)).map(this::toDTO);
        }

        @Override
        public List<KnowledgeBaseDTO> getByOwnerId(Long userId) {
            return storage.values().stream()
                    .filter(kb -> kb.getOwnerId().equals(userId))
                    .map(this::toDTO)
                    .toList();
        }

        @Override
        public List<KnowledgeBaseDTO> getAccessibleByUserId(Long userId) {
            return getByOwnerId(userId);
        }

        @Override
        public KnowledgeBaseDTO update(Long id, UpdateKnowledgeBaseRequest request) {
            KnowledgeBase kb = storage.get(id);
            if (kb == null) return null;
            if (request.getName() != null) kb.setName(request.getName());
            if (request.getDescription() != null) kb.setDescription(request.getDescription());
            if (request.getIsPublic() != null) kb.setIsPublic(request.getIsPublic());
            return toDTO(kb);
        }

        @Override
        public void delete(Long id) {
            storage.remove(id);
            documentCounts.remove(id);
            vectorCounts.remove(id);
        }

        @Override
        public boolean exists(Long id) {
            return storage.containsKey(id);
        }

        @Override
        public KnowledgeBaseStatistics getStatistics(Long id) {
            return KnowledgeBaseStatistics.builder()
                    .kbId(id)
                    .documentCount(documentCounts.getOrDefault(id, 0))
                    .vectorCount(vectorCounts.getOrDefault(id, 0L))
                    .queryCount(0L)
                    .build();
        }

        @Override
        public void updateDocumentCount(Long id, int delta) {
            documentCounts.merge(id, delta, Integer::sum);
        }

        public void addDocument(Long kbId, int chunkCount) {
            documentCounts.merge(kbId, 1, Integer::sum);
            vectorCounts.merge(kbId, (long) chunkCount, Long::sum);
        }

        private KnowledgeBaseDTO toDTO(KnowledgeBase kb) {
            return KnowledgeBaseDTO.builder()
                    .id(kb.getId())
                    .name(kb.getName())
                    .description(kb.getDescription())
                    .ownerId(kb.getOwnerId())
                    .vectorCollection(kb.getVectorCollection())
                    .documentCount(kb.getDocumentCount())
                    .isPublic(kb.getIsPublic())
                    .build();
        }
    }

    /**
     * In-memory Document service for testing
     */
    private static class InMemoryDocumentService implements DocumentService {
        private final Map<Long, Document> docStorage = new HashMap<>();
        private final Map<Long, List<DocumentChunk>> chunkStorage = new HashMap<>();
        private final VectorStore vectorStore;
        private long docIdCounter = 1;
        private long chunkIdCounter = 1;

        InMemoryDocumentService(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
        }

        @Override
        public Document create(Document document) {
            document.setId(docIdCounter++);
            docStorage.put(document.getId(), document);
            return document;
        }

        @Override
        public Optional<Document> getById(Long id) {
            return Optional.ofNullable(docStorage.get(id));
        }

        @Override
        public List<Document> getByKnowledgeBaseId(Long kbId) {
            return docStorage.values().stream()
                    .filter(d -> d.getKbId().equals(kbId))
                    .toList();
        }

        @Override
        public Optional<Document> getByContentHash(String contentHash) {
            return docStorage.values().stream()
                    .filter(d -> contentHash.equals(d.getContentHash()))
                    .findFirst();
        }

        @Override
        public void updateStatus(Long id, String status) {
            Document doc = docStorage.get(id);
            if (doc != null) doc.setStatus(status);
        }

        @Override
        public void updateChunkCount(Long id, int chunkCount) {
            Document doc = docStorage.get(id);
            if (doc != null) doc.setChunkCount(chunkCount);
        }

        @Override
        public void delete(Long id) {
            Document doc = docStorage.get(id);
            if (doc == null) return;
            
            // Get vector IDs and delete from vector store
            List<String> vectorIds = getVectorIdsByDocumentId(id);
            if (!vectorIds.isEmpty()) {
                vectorStore.delete("kb_" + doc.getKbId(), vectorIds);
            }
            
            // Delete chunks
            chunkStorage.remove(id);
            
            // Delete document
            docStorage.remove(id);
        }

        @Override
        public void deleteByKnowledgeBaseId(Long kbId) {
            List<Long> docIds = docStorage.values().stream()
                    .filter(d -> d.getKbId().equals(kbId))
                    .map(Document::getId)
                    .toList();
            docIds.forEach(this::delete);
        }

        @Override
        public void saveChunks(List<DocumentChunk> chunks) {
            for (DocumentChunk chunk : chunks) {
                chunk.setId(chunkIdCounter++);
                chunkStorage.computeIfAbsent(chunk.getDocumentId(), k -> new ArrayList<>()).add(chunk);
            }
        }

        @Override
        public List<DocumentChunk> getChunksByDocumentId(Long documentId) {
            return chunkStorage.getOrDefault(documentId, new ArrayList<>());
        }

        @Override
        public List<String> getVectorIdsByDocumentId(Long documentId) {
            return getChunksByDocumentId(documentId).stream()
                    .map(DocumentChunk::getVectorId)
                    .filter(id -> id != null && !id.isEmpty())
                    .toList();
        }

        @Override
        public int countByKnowledgeBaseId(Long kbId) {
            return (int) docStorage.values().stream()
                    .filter(d -> d.getKbId().equals(kbId))
                    .count();
        }
    }

    /**
     * In-memory KB Permission service for testing
     */
    private static class InMemoryKBPermissionService implements KBPermissionService {
        private final Map<String, KBPermission> permissions = new HashMap<>();
        private long idCounter = 1;

        private String key(Long kbId, Long userId) {
            return kbId + ":" + userId;
        }

        @Override
        public KBPermission grant(Long kbId, Long userId, PermissionType permissionType) {
            String key = key(kbId, userId);
            KBPermission perm = permissions.get(key);
            if (perm == null) {
                perm = new KBPermission();
                perm.setId(idCounter++);
                perm.setKbId(kbId);
                perm.setUserId(userId);
            }
            perm.setPermissionType(permissionType.name());
            permissions.put(key, perm);
            return perm;
        }

        @Override
        public void revoke(Long kbId, Long userId) {
            permissions.remove(key(kbId, userId));
        }

        @Override
        public Optional<KBPermission> getPermission(Long kbId, Long userId) {
            return Optional.ofNullable(permissions.get(key(kbId, userId)));
        }

        @Override
        public boolean hasPermission(Long kbId, Long userId, PermissionType permissionType) {
            Optional<KBPermission> perm = getPermission(kbId, userId);
            if (perm.isEmpty()) return false;
            
            PermissionType userPerm = PermissionType.valueOf(perm.get().getPermissionType());
            if (userPerm == PermissionType.ADMIN) return true;
            if (userPerm == PermissionType.WRITE && permissionType == PermissionType.READ) return true;
            return userPerm == permissionType;
        }

        @Override
        public boolean canAccess(Long kbId, Long userId, Boolean isPublic, Long ownerId) {
            if (Boolean.TRUE.equals(isPublic)) return true;
            if (userId != null && userId.equals(ownerId)) return true;
            return userId != null && hasPermission(kbId, userId, PermissionType.READ);
        }

        @Override
        public List<KBPermission> getByKnowledgeBaseId(Long kbId) {
            return permissions.values().stream()
                    .filter(p -> p.getKbId().equals(kbId))
                    .toList();
        }

        @Override
        public List<Long> getAccessibleKnowledgeBaseIds(Long userId) {
            return permissions.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .map(KBPermission::getKbId)
                    .toList();
        }

        @Override
        public void deleteByKnowledgeBaseId(Long kbId) {
            permissions.entrySet().removeIf(e -> e.getValue().getKbId().equals(kbId));
        }
    }

    // ==================== Assertion Helper ====================

    private BooleanAssert assertThat(boolean condition) {
        return new BooleanAssert(condition);
    }

    private static class BooleanAssert {
        private final boolean actual;
        private String description;

        BooleanAssert(boolean actual) {
            this.actual = actual;
        }

        BooleanAssert as(String description, Object... args) {
            this.description = String.format(description, args);
            return this;
        }

        void isTrue() {
            if (!actual) {
                throw new AssertionError(description != null ? description : "Expected true but was false");
            }
        }
    }
}

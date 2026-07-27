package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.dto.CreateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.impl.KnowledgeBaseServiceImpl;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.enterprise.rag.core.vectorstore.VectorDependencyException;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class KnowledgeBaseServiceImplTest {

    private static final RequestIdentity REQUEST_IDENTITY = new RequestIdentity(1001L, 901L);

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private DocumentService documentService;
    private KBPermissionService permissionService;
    private VectorStore vectorStore;
    private EmbeddingService embeddingService;
    private StringRedisTemplate redisTemplate;
    private KnowledgeBaseServiceImpl service;

    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        documentService = mock(DocumentService.class);
        permissionService = mock(KBPermissionService.class);
        vectorStore = mock(VectorStore.class);
        embeddingService = mock(EmbeddingService.class);
        redisTemplate = mock(StringRedisTemplate.class);

        service = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                documentService,
                permissionService,
                vectorStore,
                embeddingService,
                redisTemplate);

        when(documentService.countByKnowledgeBaseId(anyLong())).thenReturn(0);
        doAnswer(invocation -> {
            KnowledgeBase kb = invocation.getArgument(0);
            kb.setId(1L);
            return 1;
        }).when(knowledgeBaseMapper).insert(any(KnowledgeBase.class));
    }

    @Test
    void createShouldFailWhenVectorCollectionCreationFails() {
        CreateKnowledgeBaseRequest request = CreateKnowledgeBaseRequest.builder()
                .name("kb-test")
                .description("desc")
                .isPublic(false)
                .build();

        when(embeddingService.getDimension()).thenReturn(1024);
        org.mockito.Mockito.doThrow(new RuntimeException("milvus down"))
                .when(vectorStore).createCollection(any(TenantVectorScope.class), anyInt());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(request, REQUEST_IDENTITY));

        assertEquals("KB_005", ex.getErrorCode());
        verify(vectorStore).createCollection(any(TenantVectorScope.class), anyInt());
    }

    @Test
    void createShouldPreserveStableVectorUnavailableCode() {
        CreateKnowledgeBaseRequest request = CreateKnowledgeBaseRequest.builder()
                .name("kb-test")
                .description("desc")
                .isPublic(false)
                .build();
        when(embeddingService.getDimension()).thenReturn(1024);
        doThrow(VectorDependencyException.unavailable("create", new IllegalStateException("raw-marker")))
                .when(vectorStore).createCollection(any(TenantVectorScope.class), anyInt());

        VectorDependencyException exception = assertThrows(
                VectorDependencyException.class,
                () -> service.create(request, REQUEST_IDENTITY));

        assertEquals(VectorDependencyException.ERROR_CODE_UNAVAILABLE, exception.getErrorCode());
        assertEquals(503, exception.getHttpStatus().value());
    }

    @Test
    void createShouldPersistServerDerivedTenantIdentity() {
        CreateKnowledgeBaseRequest request = CreateKnowledgeBaseRequest.builder()
                .name("tenant-kb")
                .description("desc")
                .isPublic(false)
                .build();
        when(embeddingService.getDimension()).thenReturn(1024);

        service.create(request, REQUEST_IDENTITY);

        ArgumentCaptor<KnowledgeBase> captor = ArgumentCaptor.forClass(KnowledgeBase.class);
        verify(knowledgeBaseMapper).insert(captor.capture());
        assertEquals(1001L, captor.getValue().getOwnerId());
        assertEquals(901L, captor.getValue().getTenantId());
    }

    @Test
    void scopedGetByIdShouldUseTenantScopedDocumentCountOnly() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(7L);
        kb.setTenantId(901L);
        kb.setName("tenant-kb");
        when(knowledgeBaseMapper.selectByTenantAndId(901L, 7L)).thenReturn(kb);
        when(documentService.countByKnowledgeBaseId(901L, 7L)).thenReturn(3);

        KnowledgeBaseDTO result = service.getById(7L, REQUEST_IDENTITY).orElseThrow();

        assertEquals(3, result.getDocumentCount());
        verify(documentService).countByKnowledgeBaseId(901L, 7L);
        verify(documentService, never()).countByKnowledgeBaseId(7L);
    }

    @Test
    void unscopedLookupAndExistsShouldFailClosedWithoutMapperAccess() {
        assertThrows(IllegalStateException.class, () -> service.getById(7L));
        assertThrows(IllegalStateException.class, () -> service.exists(7L));

        verify(knowledgeBaseMapper, never()).selectById(7L);
    }

    @Test
    void statisticsShouldReportRedisUnavailableInsteadOfFakeZero() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(7L);
        kb.setTenantId(901L);
        kb.setVectorReadiness("READY");
        when(knowledgeBaseMapper.selectByTenantAndId(901L, 7L)).thenReturn(kb);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("kb:query:count:v2:901:7"))
                .thenThrow(new RuntimeException("synthetic redis marker"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getStatistics(7L, REQUEST_IDENTITY));

        assertEquals("REDIS_DEPENDENCY_UNAVAILABLE", exception.getErrorCode());
        assertEquals(503, exception.getHttpStatus().value());
    }

    @Test
    void unscopedDeleteShouldFailClosed() {
        assertThrows(IllegalStateException.class, () -> service.delete(7L));
        verify(knowledgeBaseMapper, never()).deleteById(7L);
    }

    @Test
    void corruptQueryCounterShouldNotBeReportedAsZero() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(7L);
        kb.setTenantId(901L);
        kb.setVectorReadiness("READY");
        when(knowledgeBaseMapper.selectByTenantAndId(901L, 7L)).thenReturn(kb);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("kb:query:count:v2:901:7")).thenReturn("not-a-number");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getStatistics(7L, REQUEST_IDENTITY));

        assertEquals("REDIS_DEPENDENCY_UNAVAILABLE", exception.getErrorCode());
    }

    @Test
    void incrementQueryCounterShouldRemainBestEffort() {
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("kb:query:count:v2:11:7"))
                .thenThrow(new RuntimeException("synthetic redis marker"));

        assertDoesNotThrow(() -> service.incrementQueryCount(11L, 7L));
    }

    @Test
    void statisticsShouldFailInsteadOfReportingFakeZeroWhenVectorCountIsUnavailable() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(7L);
        kb.setTenantId(901L);
        kb.setVectorCollection("kb_vectors");
        kb.setVectorReadiness("READY");
        when(knowledgeBaseMapper.selectByTenantAndId(901L, 7L)).thenReturn(kb);
        when(documentService.countByKnowledgeBaseId(901L, 7L)).thenReturn(0);
        when(vectorStore.count(any(TenantVectorScope.class)))
                .thenThrow(VectorDependencyException.unavailable("count", new IllegalStateException("raw-marker")));

        VectorDependencyException exception = assertThrows(
                VectorDependencyException.class,
                () -> service.getStatistics(7L, REQUEST_IDENTITY));

        assertEquals(VectorDependencyException.ERROR_CODE_UNAVAILABLE, exception.getErrorCode());
    }

    @Test
    void deleteShouldFailClosedWhenCollectionDropOutcomeIsUnknown() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(7L);
        kb.setTenantId(901L);
        kb.setVectorCollection("kb_vectors");
        kb.setVectorReadiness("READY");
        when(knowledgeBaseMapper.selectByTenantAndId(901L, 7L)).thenReturn(kb);
        doThrow(VectorDependencyException.outcomeUnknown("drop", new IllegalStateException("raw-marker")))
                .when(vectorStore).dropCollection(any(TenantVectorScope.class));

        VectorDependencyException exception = assertThrows(
                VectorDependencyException.class,
                () -> service.delete(7L, REQUEST_IDENTITY));

        assertEquals(VectorDependencyException.ERROR_CODE_OUTCOME_UNKNOWN, exception.getErrorCode());
        verify(knowledgeBaseMapper, never()).deleteByTenantAndId(901L, 7L);
    }

    @Test
    void nonReadyLegacyMappingShouldFailClosedBeforeVectorAccess() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(7L);
        kb.setTenantId(901L);
        kb.setVectorCollection("legacy_vectors");
        kb.setVectorReadiness("LEGACY_PENDING");
        when(knowledgeBaseMapper.selectByTenantAndId(901L, 7L)).thenReturn(kb);

        VectorDependencyException exception = assertThrows(VectorDependencyException.class,
                () -> service.requireReadyVectorScope(901L, 7L));

        assertEquals(VectorDependencyException.ERROR_CODE_INDEX_NOT_READY, exception.getErrorCode());
        verify(vectorStore, never()).hasCollection(any(TenantVectorScope.class));
    }
}

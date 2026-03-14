package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.dto.CreateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.impl.KnowledgeBaseServiceImpl;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseServiceImplTest {

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
                .when(vectorStore).createCollection(anyString(), anyInt());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request, 1001L));

        assertEquals("KB_005", ex.getErrorCode());
        verify(vectorStore).createCollection(anyString(), anyInt());
    }
}

package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.dto.UpdateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.KBPermissionMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentIndexingService;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.kb.service.impl.KBPermissionServiceImpl;
import com.enterprise.rag.admin.kb.service.impl.KnowledgeBaseServiceImpl;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseUpdateTenantEnforcementTest {

    private static final long USER_ID = 1001L;
    private static final long TENANT_A = 901L;
    private static final long TENANT_B = 902L;
    private static final long KB_ID = 100L;

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private DocumentService documentService;
    private KnowledgeBaseController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        documentService = mock(DocumentService.class);
        KBPermissionService permissionService = new KBPermissionServiceImpl(mock(KBPermissionMapper.class));
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                documentService,
                permissionService,
                mock(VectorStore.class),
                mock(EmbeddingService.class),
                mock(StringRedisTemplate.class));
        AuthorizationService authorizationService = new AuthorizationService(
                knowledgeBaseService,
                permissionService,
                mock(QAHistoryService.class));
        controller = new KnowledgeBaseController(
                knowledgeBaseService,
                documentService,
                mock(DocumentIndexingService.class),
                new CurrentUserService(),
                authorizationService);
        principal = UserPrincipal.builder()
                .id(USER_ID)
                .tenantId(TENANT_A)
                .username("tenant-a-owner")
                .password("unused")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
    }

    @Test
    void crossTenantOwnerIdMatchStillReturnsNotFoundAndDoesNotUpdate() {
        KnowledgeBase foreignKnowledgeBase = knowledgeBase(TENANT_B, USER_ID, "tenant-b");
        when(knowledgeBaseMapper.selectById(KB_ID)).thenReturn(foreignKnowledgeBase);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.update(KB_ID, updateRequest("blocked"), principal));

        assertEquals("KB_001", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(knowledgeBaseMapper, never()).updateById(foreignKnowledgeBase);
    }

    @Test
    void sameTenantOwnerUpdateUsesTenantScopedMutation() {
        KnowledgeBase localKnowledgeBase = knowledgeBase(TENANT_A, USER_ID, "before");
        when(knowledgeBaseMapper.selectByTenantAndId(TENANT_A, KB_ID)).thenReturn(localKnowledgeBase);
        when(knowledgeBaseMapper.updateMutableFieldsByTenantAndId(
                TENANT_A, KB_ID, "after", "description", true)).thenReturn(1);
        when(documentService.countByKnowledgeBaseId(TENANT_A, KB_ID)).thenReturn(4);

        var response = controller.update(KB_ID, updateRequest("after"), principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("after", response.getBody().getData().getName());
        assertEquals(4, response.getBody().getData().getDocumentCount());
        verify(knowledgeBaseMapper).updateMutableFieldsByTenantAndId(
                TENANT_A, KB_ID, "after", "description", true);
        verify(knowledgeBaseMapper, never()).updateById(localKnowledgeBase);
    }

    private UpdateKnowledgeBaseRequest updateRequest(String name) {
        return UpdateKnowledgeBaseRequest.builder()
                .name(name)
                .description("description")
                .isPublic(true)
                .build();
    }

    private KnowledgeBase knowledgeBase(long tenantId, long ownerId, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(KB_ID);
        knowledgeBase.setTenantId(tenantId);
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription("before-description");
        knowledgeBase.setIsPublic(false);
        knowledgeBase.setDocumentCount(0);
        return knowledgeBase;
    }
}

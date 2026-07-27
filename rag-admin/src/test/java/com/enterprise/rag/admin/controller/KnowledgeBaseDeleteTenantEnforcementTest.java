package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentIndexingService;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
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

class KnowledgeBaseDeleteTenantEnforcementTest {

    private static final long USER_ID = 1001L;
    private static final long TENANT_A = 901L;
    private static final long TENANT_B = 902L;
    private static final long KB_ID = 100L;

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private DocumentService documentService;
    private KBPermissionService permissionService;
    private KnowledgeBaseController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        documentService = mock(DocumentService.class);
        permissionService = mock(KBPermissionService.class);
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

        when(documentService.countByKnowledgeBaseId(KB_ID)).thenReturn(0);
        when(documentService.countByKnowledgeBaseId(TENANT_A, KB_ID)).thenReturn(0);
    }

    @Test
    void foreignTenantKnowledgeBaseWithSameOwnerIdIsNotFoundAndNeverDeleted() {
        KnowledgeBase foreignKnowledgeBase = new KnowledgeBase();
        foreignKnowledgeBase.setId(KB_ID);
        foreignKnowledgeBase.setTenantId(TENANT_B);
        foreignKnowledgeBase.setOwnerId(USER_ID);
        foreignKnowledgeBase.setName("foreign-kb");
        foreignKnowledgeBase.setIsPublic(false);
        when(knowledgeBaseMapper.selectById(KB_ID)).thenReturn(foreignKnowledgeBase);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.delete(KB_ID, principal));

        assertEquals("KB_001", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(knowledgeBaseMapper, never()).deleteById(KB_ID);
    }

    @Test
    void sameTenantOwnerUsesTenantScopedCascadeAndDelete() {
        KnowledgeBase localKnowledgeBase = new KnowledgeBase();
        localKnowledgeBase.setId(KB_ID);
        localKnowledgeBase.setTenantId(TENANT_A);
        localKnowledgeBase.setOwnerId(USER_ID);
        localKnowledgeBase.setName("local-kb");
        localKnowledgeBase.setIsPublic(false);
        when(knowledgeBaseMapper.selectByTenantAndId(TENANT_A, KB_ID)).thenReturn(localKnowledgeBase);
        when(knowledgeBaseMapper.deleteByTenantAndId(TENANT_A, KB_ID)).thenReturn(1);

        var response = controller.delete(KB_ID, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(documentService).deleteByKnowledgeBaseId(TENANT_A, KB_ID);
        verify(permissionService).deleteByKnowledgeBaseId(TENANT_A, KB_ID);
        verify(knowledgeBaseMapper).deleteByTenantAndId(TENANT_A, KB_ID);
        verify(knowledgeBaseMapper, never()).deleteById(KB_ID);
    }
}

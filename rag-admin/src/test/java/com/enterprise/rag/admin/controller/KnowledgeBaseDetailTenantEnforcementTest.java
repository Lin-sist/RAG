package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.entity.KBPermission;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class KnowledgeBaseDetailTenantEnforcementTest {

    private static final long REQUEST_USER_ID = 1001L;
    private static final long REQUEST_TENANT_ID = 901L;
    private static final long KNOWLEDGE_BASE_ID = 100L;

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private KBPermissionMapper permissionMapper;
    private DocumentService documentService;
    private KnowledgeBaseController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        documentService = mock(DocumentService.class);
        permissionMapper = mock(KBPermissionMapper.class);
        KBPermissionService permissionService = new KBPermissionServiceImpl(permissionMapper);
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                documentService,
                permissionService,
                mock(VectorStore.class),
                mock(EmbeddingService.class),
                mock(StringRedisTemplate.class));
        CurrentUserService currentUserService = new CurrentUserService();
        AuthorizationService authorizationService = new AuthorizationService(
                knowledgeBaseService,
                permissionService,
                mock(QAHistoryService.class));
        controller = new KnowledgeBaseController(
                knowledgeBaseService,
                documentService,
                mock(DocumentIndexingService.class),
                currentUserService,
                authorizationService);
        principal = UserPrincipal.builder()
                .id(REQUEST_USER_ID)
                .tenantId(REQUEST_TENANT_ID)
                .username("tenant-a-user")
                .password("unused")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
    }

    @Test
    void crossTenantPublicKnowledgeBaseIsNotFound() {
        KnowledgeBase foreignPublicKnowledgeBase = knowledgeBase(902L, true, 2002L, "tenant-b-public");
        when(knowledgeBaseMapper.selectById(KNOWLEDGE_BASE_ID)).thenReturn(foreignPublicKnowledgeBase);
        when(documentService.countByKnowledgeBaseId(KNOWLEDGE_BASE_ID)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.getById(KNOWLEDGE_BASE_ID, principal));

        assertEquals("KB_001", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void permissionOutsideCurrentTenantDoesNotGrantReadAccess() {
        KnowledgeBase tenantLocalPrivateKnowledgeBase = knowledgeBase(
                REQUEST_TENANT_ID, false, 2002L, "tenant-a-private");
        when(knowledgeBaseMapper.selectByTenantAndId(REQUEST_TENANT_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(tenantLocalPrivateKnowledgeBase);
        when(documentService.countByKnowledgeBaseId(KNOWLEDGE_BASE_ID)).thenReturn(0);

        KBPermission permissionFromUnscopedLookup = new KBPermission();
        permissionFromUnscopedLookup.setKbId(KNOWLEDGE_BASE_ID);
        permissionFromUnscopedLookup.setUserId(REQUEST_USER_ID);
        permissionFromUnscopedLookup.setPermissionType("READ");
        when(permissionMapper.selectOne(any())).thenReturn(permissionFromUnscopedLookup);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.getById(KNOWLEDGE_BASE_ID, principal));

        assertEquals("AUTH_004", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    }

    @Test
    void sameTenantReadPermissionGrantsAccess() {
        KnowledgeBase tenantLocalPrivateKnowledgeBase = knowledgeBase(
                REQUEST_TENANT_ID, false, 2002L, "tenant-a-private");
        when(knowledgeBaseMapper.selectByTenantAndId(REQUEST_TENANT_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(tenantLocalPrivateKnowledgeBase);
        when(documentService.countByKnowledgeBaseId(KNOWLEDGE_BASE_ID)).thenReturn(0);
        when(permissionMapper.findPermissionTypeByTenantAndResource(
                REQUEST_TENANT_ID, KNOWLEDGE_BASE_ID, REQUEST_USER_ID))
                .thenReturn("READ");

        var response = controller.getById(KNOWLEDGE_BASE_ID, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("tenant-a-private", response.getBody().getData().getName());
    }

    @Test
    void sameTenantPublicKnowledgeBaseUsesScopedLookupResult() {
        KnowledgeBase tenantLocalPublicKnowledgeBase = knowledgeBase(
                REQUEST_TENANT_ID, true, 2002L, "tenant-a-public");
        KnowledgeBase unscopedLookupResult = knowledgeBase(
                902L, true, 2002L, "unscoped-result-must-not-be-used");
        when(knowledgeBaseMapper.selectByTenantAndId(REQUEST_TENANT_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(tenantLocalPublicKnowledgeBase);
        when(knowledgeBaseMapper.selectById(KNOWLEDGE_BASE_ID)).thenReturn(unscopedLookupResult);
        when(documentService.countByKnowledgeBaseId(KNOWLEDGE_BASE_ID)).thenReturn(0);

        var response = controller.getById(KNOWLEDGE_BASE_ID, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("tenant-a-public", response.getBody().getData().getName());
    }

    @Test
    void sameTenantOwnerCanReadPrivateKnowledgeBase() {
        KnowledgeBase tenantLocalPrivateKnowledgeBase = knowledgeBase(
                REQUEST_TENANT_ID, false, REQUEST_USER_ID, "tenant-a-owned");
        when(knowledgeBaseMapper.selectByTenantAndId(REQUEST_TENANT_ID, KNOWLEDGE_BASE_ID))
                .thenReturn(tenantLocalPrivateKnowledgeBase);
        when(documentService.countByKnowledgeBaseId(KNOWLEDGE_BASE_ID)).thenReturn(0);

        var response = controller.getById(KNOWLEDGE_BASE_ID, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("tenant-a-owned", response.getBody().getData().getName());
    }

    private KnowledgeBase knowledgeBase(long tenantId, boolean isPublic, long ownerId, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(KNOWLEDGE_BASE_ID);
        knowledgeBase.setTenantId(tenantId);
        knowledgeBase.setName(name);
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setIsPublic(isPublic);
        knowledgeBase.setDocumentCount(0);
        return knowledgeBase;
    }
}

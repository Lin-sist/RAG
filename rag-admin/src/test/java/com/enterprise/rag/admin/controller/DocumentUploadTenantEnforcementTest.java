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
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentUploadTenantEnforcementTest {

    private static final long USER_ID = 1001L;
    private static final long TENANT_A = 901L;
    private static final long TENANT_B = 902L;
    private static final long KB_ID = 100L;

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private DocumentService documentService;
    private DocumentIndexingService indexingService;
    private KnowledgeBaseController controller;
    private UserPrincipal principal;
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        documentService = mock(DocumentService.class);
        KBPermissionService permissionService = mock(KBPermissionService.class);
        indexingService = mock(DocumentIndexingService.class);
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
                indexingService,
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
        file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("document.md");
        when(documentService.countByKnowledgeBaseId(KB_ID)).thenReturn(0);
    }

    @Test
    void foreignTenantKnowledgeBaseWithSameOwnerIdNeverSubmitsIndexing() {
        KnowledgeBase foreignKnowledgeBase = new KnowledgeBase();
        foreignKnowledgeBase.setId(KB_ID);
        foreignKnowledgeBase.setTenantId(TENANT_B);
        foreignKnowledgeBase.setOwnerId(USER_ID);
        foreignKnowledgeBase.setName("foreign-kb");
        foreignKnowledgeBase.setIsPublic(false);
        when(knowledgeBaseMapper.selectById(KB_ID)).thenReturn(foreignKnowledgeBase);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.uploadDocument(KB_ID, file, null, principal));

        assertEquals("KB_001", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(indexingService, never()).submitIndexing(
                eq(TENANT_A), eq(KB_ID), eq(USER_ID), any(MultipartFile.class), isNull());
    }

    @Test
    void sameTenantOwnerSubmitsIndexingWithServerIdentityTenant() throws Exception {
        KnowledgeBase localKnowledgeBase = new KnowledgeBase();
        localKnowledgeBase.setId(KB_ID);
        localKnowledgeBase.setTenantId(TENANT_A);
        localKnowledgeBase.setOwnerId(USER_ID);
        localKnowledgeBase.setName("local-kb");
        localKnowledgeBase.setIsPublic(false);
        when(knowledgeBaseMapper.selectByTenantAndId(TENANT_A, KB_ID)).thenReturn(localKnowledgeBase);
        when(documentService.countByKnowledgeBaseId(TENANT_A, KB_ID)).thenReturn(0);

        var response = controller.uploadDocument(KB_ID, file, null, principal);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(indexingService).submitIndexing(
                eq(TENANT_A), eq(KB_ID), eq(USER_ID), any(MultipartFile.class), isNull());
    }
}

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
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseStatisticsTenantEnforcementTest {

    private static final long USER_ID = 1001L;
    private static final long TENANT_A = 901L;
    private static final long TENANT_B = 902L;
    private static final long KB_ID = 100L;

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private DocumentService documentService;
    private KBPermissionService permissionService;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private KnowledgeBaseController controller;
    private UserPrincipal principal;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        documentService = mock(DocumentService.class);
        permissionService = mock(KBPermissionService.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                documentService,
                permissionService,
                mock(VectorStore.class),
                mock(EmbeddingService.class),
                redisTemplate);
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
                .username("tenant-a-user")
                .password("unused")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
    }

    @Test
    void foreignTenantPublicKnowledgeBaseStatisticsAreNotFound() {
        KnowledgeBase foreignKnowledgeBase = new KnowledgeBase();
        foreignKnowledgeBase.setId(KB_ID);
        foreignKnowledgeBase.setTenantId(TENANT_B);
        foreignKnowledgeBase.setOwnerId(2002L);
        foreignKnowledgeBase.setName("foreign-public-kb");
        foreignKnowledgeBase.setIsPublic(true);
        when(knowledgeBaseMapper.selectById(KB_ID)).thenReturn(foreignKnowledgeBase);
        when(documentService.countByKnowledgeBaseId(KB_ID)).thenReturn(99);
        when(permissionService.canAccess(KB_ID, USER_ID, true, 2002L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.getStatistics(KB_ID, principal));

        assertEquals("KB_001", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(documentService, never()).countByKnowledgeBaseId(TENANT_A, KB_ID);
    }

    @Test
    void sameTenantStatisticsUseTenantScopedCountAndV2RedisKey() {
        KnowledgeBase localKnowledgeBase = new KnowledgeBase();
        localKnowledgeBase.setId(KB_ID);
        localKnowledgeBase.setTenantId(TENANT_A);
        localKnowledgeBase.setOwnerId(USER_ID);
        localKnowledgeBase.setName("local-kb");
        localKnowledgeBase.setIsPublic(false);
        when(knowledgeBaseMapper.selectByTenantAndId(TENANT_A, KB_ID)).thenReturn(localKnowledgeBase);
        when(documentService.countByKnowledgeBaseId(TENANT_A, KB_ID)).thenReturn(3);
        when(valueOperations.get("kb:query:count:v2:901:100")).thenReturn("7");

        var response = controller.getStatistics(KB_ID, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(documentService, times(2)).countByKnowledgeBaseId(TENANT_A, KB_ID);
        verify(valueOperations).get("kb:query:count:v2:901:100");
        verify(documentService, never()).countByKnowledgeBaseId(KB_ID);
    }
}

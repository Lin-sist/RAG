package com.enterprise.rag.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.enterprise.rag.admin.kb.entity.KBPermission;
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
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeBaseListTenantEnforcementTest {

    private static final long REQUEST_USER_ID = 1001L;
    private static final long TENANT_A = 901L;
    private static final long TENANT_B = 902L;

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private KBPermissionMapper permissionMapper;
    private DocumentService documentService;
    private KnowledgeBaseController controller;
    private UserPrincipal principal;

    @BeforeAll
    static void initializeMyBatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "kb-list-tenant-test");
        TableInfoHelper.initTableInfo(assistant, KnowledgeBase.class);
        TableInfoHelper.initTableInfo(assistant, KBPermission.class);
    }

    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        permissionMapper = mock(KBPermissionMapper.class);
        documentService = mock(DocumentService.class);
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
                .tenantId(TENANT_A)
                .username("tenant-a-user")
                .password("unused")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
    }

    @Test
    void listReturnsOnlyCurrentTenantOwnerPublicAndPermissionKnowledgeBases() {
        KnowledgeBase localOwned = knowledgeBase(101L, TENANT_A, REQUEST_USER_ID, false, "local-owned");
        KnowledgeBase foreignOwned = knowledgeBase(201L, TENANT_B, REQUEST_USER_ID, false, "foreign-owned");
        KnowledgeBase localPublic = knowledgeBase(102L, TENANT_A, 3001L, true, "local-public");
        KnowledgeBase foreignPublic = knowledgeBase(202L, TENANT_B, 3002L, true, "foreign-public");
        KnowledgeBase localPermitted = knowledgeBase(103L, TENANT_A, 3003L, false, "local-permitted");
        KnowledgeBase foreignPermitted = knowledgeBase(203L, TENANT_B, 3004L, false, "foreign-permitted");

        when(knowledgeBaseMapper.selectList(any()))
                .thenReturn(List.of(localOwned, foreignOwned))
                .thenReturn(List.of(localPublic, foreignPublic))
                .thenReturn(List.of(
                        localOwned, localPublic, localPermitted,
                        foreignOwned, foreignPublic, foreignPermitted));
        when(permissionMapper.selectList(any())).thenReturn(List.of(
                permission(1L, TENANT_A, localPermitted.getId()),
                permission(2L, TENANT_B, foreignPermitted.getId())));
        when(documentService.countByKnowledgeBaseId(anyLong())).thenReturn(999);
        when(documentService.countByKnowledgeBaseId(eq(TENANT_A), anyLong()))
                .thenAnswer(invocation -> Math.toIntExact(invocation.getArgument(1, Long.class) - 100L));

        var response = controller.list(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Integer> countsByName = response.getBody().getData().stream()
                .collect(Collectors.toMap(
                        knowledgeBase -> knowledgeBase.getName(),
                        knowledgeBase -> knowledgeBase.getDocumentCount(),
                        (left, right) -> left));
        assertEquals(Map.of(
                "local-owned", 1,
                "local-public", 2,
                "local-permitted", 3), countsByName);
        verify(documentService, never()).countByKnowledgeBaseId(anyLong());
    }

    private KnowledgeBase knowledgeBase(long id, long tenantId, long ownerId, boolean isPublic, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setTenantId(tenantId);
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setIsPublic(isPublic);
        knowledgeBase.setName(name);
        knowledgeBase.setDocumentCount(0);
        return knowledgeBase;
    }

    private KBPermission permission(long id, long tenantId, long kbId) {
        KBPermission permission = new KBPermission();
        permission.setId(id);
        permission.setTenantId(tenantId);
        permission.setKbId(kbId);
        permission.setUserId(REQUEST_USER_ID);
        permission.setPermissionType("READ");
        return permission;
    }
}

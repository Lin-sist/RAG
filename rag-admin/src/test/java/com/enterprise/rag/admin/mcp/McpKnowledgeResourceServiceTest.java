package com.enterprise.rag.admin.mcp;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.enterprise.rag.admin.kb.entity.KBPermission;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.KBPermissionMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.kb.service.impl.KBPermissionServiceImpl;
import com.enterprise.rag.admin.kb.service.impl.KnowledgeBaseServiceImpl;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.vectorstore.VectorStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class McpKnowledgeResourceServiceTest {

    private static final long USER_ID = 1001L;
    private static final long TENANT_A = 901L;
    private static final long TENANT_B = 902L;

    @BeforeAll
    static void initializeMyBatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "mcp-kb-resource-test");
        TableInfoHelper.initTableInfo(assistant, KnowledgeBase.class);
        TableInfoHelper.initTableInfo(assistant, KBPermission.class);
    }

    @Test
    void listReturnsOnlyCurrentIdentityAccessibleKnowledgeBasesInAscendingIdOrder() {
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        KBPermissionMapper permissionMapper = mock(KBPermissionMapper.class);
        DocumentService documentService = mock(DocumentService.class);
        KBPermissionService permissionService = new KBPermissionServiceImpl(permissionMapper);
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                documentService,
                permissionService,
                mock(VectorStore.class),
                mock(EmbeddingService.class),
                mock(StringRedisTemplate.class));

        KnowledgeBase localOwned = knowledgeBase(42L, TENANT_A, USER_ID, false, "owned");
        KnowledgeBase localPublic = knowledgeBase(7L, TENANT_A, 2001L, true, "public");
        KnowledgeBase localPermitted = knowledgeBase(19L, TENANT_A, 2002L, false, "permitted");
        KnowledgeBase foreign = knowledgeBase(8L, TENANT_B, USER_ID, true, "foreign-canary");
        when(knowledgeBaseMapper.selectList(any()))
                .thenReturn(List.of(localOwned, foreign))
                .thenReturn(List.of(localPublic, foreign))
                .thenReturn(List.of(localOwned, localPublic, localPermitted, foreign));
        when(permissionMapper.selectList(any())).thenReturn(List.of(
                permission(1L, TENANT_A, localPermitted.getId()),
                permission(2L, TENANT_B, foreign.getId())));
        when(documentService.countByKnowledgeBaseId(eq(TENANT_A), any(Long.class)))
                .thenReturn(0);

        McpKnowledgeResourceService service = new McpKnowledgeResourceService(
                knowledgeBaseService,
                mock(AuthorizationService.class),
                new ObjectMapper(),
                50);

        McpSchema.ListResourcesResult result = service.list(
                new RequestIdentity(USER_ID, TENANT_A), null);

        assertEquals(List.of(
                        "rag://knowledge-bases/7",
                        "rag://knowledge-bases/19",
                        "rag://knowledge-bases/42"),
                result.resources().stream().map(McpSchema.Resource::uri).toList());
        assertEquals(List.of("public", "permitted", "owned"),
                result.resources().stream().map(McpSchema.Resource::name).toList());
        assertNull(result.resources().get(0).description());
        assertNull(result.resources().get(0).meta());
        assertNull(result.resources().get(0).size());
        assertNull(result.nextCursor());
    }

    @Test
    void listUsesBoundedPagesAndAnOpaqueCursor() {
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        KBPermissionMapper permissionMapper = mock(KBPermissionMapper.class);
        DocumentService documentService = mock(DocumentService.class);
        KBPermissionService permissionService = new KBPermissionServiceImpl(permissionMapper);
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                documentService,
                permissionService,
                mock(VectorStore.class),
                mock(EmbeddingService.class),
                mock(StringRedisTemplate.class));

        List<KnowledgeBase> accessible = LongStream.rangeClosed(1, 51)
                .mapToObj(id -> knowledgeBase(
                        id, TENANT_A, USER_ID, false, "kb-" + id))
                .toList();
        when(knowledgeBaseMapper.selectList(any()))
                .thenReturn(accessible)
                .thenReturn(List.of())
                .thenReturn(accessible)
                .thenReturn(accessible)
                .thenReturn(List.of())
                .thenReturn(accessible);
        when(permissionMapper.selectList(any())).thenReturn(List.of());
        when(documentService.countByKnowledgeBaseId(eq(TENANT_A), any(Long.class)))
                .thenReturn(0);
        McpKnowledgeResourceService service = new McpKnowledgeResourceService(
                knowledgeBaseService,
                mock(AuthorizationService.class),
                new ObjectMapper(),
                50);
        RequestIdentity identity = new RequestIdentity(USER_ID, TENANT_A);

        McpSchema.ListResourcesResult firstPage = service.list(identity, null);

        assertEquals(50, firstPage.resources().size());
        assertEquals("rag://knowledge-bases/1", firstPage.resources().get(0).uri());
        assertEquals("rag://knowledge-bases/50", firstPage.resources().get(49).uri());
        assertNotNull(firstPage.nextCursor());
        assertFalse(firstPage.nextCursor().matches("[0-9]+"));
        assertFalse(firstPage.nextCursor().contains(Long.toString(USER_ID)));
        assertFalse(firstPage.nextCursor().contains(Long.toString(TENANT_A)));

        McpSchema.ListResourcesResult secondPage = service.list(
                identity, firstPage.nextCursor());

        assertEquals(List.of("rag://knowledge-bases/51"),
                secondPage.resources().stream().map(McpSchema.Resource::uri).toList());
        assertNull(secondPage.nextCursor());
    }

    @Test
    void readUsesTenantScopedAuthorizationAndNeverTouchesVectorProviders() throws Exception {
        KnowledgeBaseMapper knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        KBPermissionMapper permissionMapper = mock(KBPermissionMapper.class);
        DocumentService documentService = mock(DocumentService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        KBPermissionService permissionService = new KBPermissionServiceImpl(permissionMapper);
        KnowledgeBaseService knowledgeBaseService = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                documentService,
                permissionService,
                vectorStore,
                embeddingService,
                mock(StringRedisTemplate.class));
        AuthorizationService authorizationService = new AuthorizationService(
                knowledgeBaseService,
                permissionService,
                mock(QAHistoryService.class));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        McpKnowledgeResourceService service = new McpKnowledgeResourceService(
                knowledgeBaseService, authorizationService, objectMapper, 50);
        KnowledgeBase localOwned = knowledgeBase(
                42L, TENANT_A, USER_ID, false, "authorized");
        localOwned.setVectorCollection("private-vector-canary");
        localOwned.setCreatedAt(LocalDateTime.of(2026, 7, 28, 9, 15));
        localOwned.setUpdatedAt(LocalDateTime.of(2026, 7, 29, 10, 30));
        when(knowledgeBaseMapper.selectByTenantAndId(TENANT_A, 42L))
                .thenReturn(localOwned);
        when(documentService.countByKnowledgeBaseId(TENANT_A, 42L)).thenReturn(3);

        McpSchema.ReadResourceResult result = service.read(
                new RequestIdentity(USER_ID, TENANT_A),
                "rag://knowledge-bases/42");

        McpSchema.TextResourceContents contents = assertInstanceOf(
                McpSchema.TextResourceContents.class, result.contents().get(0));
        assertEquals("rag://knowledge-bases/42", contents.uri());
        assertEquals("application/json", contents.mimeType());
        JsonNode json = objectMapper.readTree(contents.text());
        assertEquals(Set.of(
                        "id",
                        "name",
                        "description",
                        "documentCount",
                        "isPublic",
                        "createdAt",
                        "updatedAt"),
                fieldNames(json));
        assertEquals(3, json.path("documentCount").asInt());
        assertFalse(contents.text().contains("private-vector-canary"));
        assertFalse(contents.text().contains("ownerId"));
        verify(knowledgeBaseMapper).selectByTenantAndId(TENANT_A, 42L);
        verify(documentService).countByKnowledgeBaseId(TENANT_A, 42L);
        verifyNoInteractions(vectorStore, embeddingService);
    }

    private Set<String> fieldNames(JsonNode object) {
        Set<String> names = new java.util.HashSet<>();
        object.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private KnowledgeBase knowledgeBase(
            long id, long tenantId, long ownerId, boolean isPublic, String name) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setTenantId(tenantId);
        knowledgeBase.setOwnerId(ownerId);
        knowledgeBase.setIsPublic(isPublic);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(name + " description");
        knowledgeBase.setDocumentCount(0);
        return knowledgeBase;
    }

    private KBPermission permission(long id, long tenantId, long kbId) {
        KBPermission permission = new KBPermission();
        permission.setId(id);
        permission.setTenantId(tenantId);
        permission.setKbId(kbId);
        permission.setUserId(USER_ID);
        permission.setPermissionType("READ");
        return permission;
    }
}

package com.enterprise.rag.admin.controller;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import com.enterprise.rag.admin.kb.mapper.DocumentChunkMapper;
import com.enterprise.rag.admin.kb.mapper.DocumentMapper;
import com.enterprise.rag.admin.kb.mapper.KBPermissionMapper;
import com.enterprise.rag.admin.kb.mapper.KnowledgeBaseMapper;
import com.enterprise.rag.admin.kb.service.DocumentIndexingService;
import com.enterprise.rag.admin.kb.service.DocumentService;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.kb.service.impl.DocumentServiceImpl;
import com.enterprise.rag.admin.kb.service.impl.KBPermissionServiceImpl;
import com.enterprise.rag.admin.kb.service.impl.KnowledgeBaseServiceImpl;
import com.enterprise.rag.admin.kb.storage.IndexInputStore;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.admin.security.AuthorizationService;
import com.enterprise.rag.admin.security.CurrentUserService;
import com.enterprise.rag.auth.model.UserPrincipal;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentListTenantEnforcementTest {

    private static final long USER_ID = 1001L;
    private static final long TENANT_A = 901L;
    private static final long TENANT_B = 902L;
    private static final long KB_ID = 100L;

    private DocumentMapper documentMapper;
    private KnowledgeBaseMapper knowledgeBaseMapper;
    private KnowledgeBaseController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        documentMapper = mock(DocumentMapper.class);
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        DocumentService documentService = new DocumentServiceImpl(
                documentMapper,
                mock(DocumentChunkMapper.class),
                knowledgeBaseMapper,
                mock(VectorStore.class),
                mock(KeywordIndex.class),
                mock(IndexInputStore.class));
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
                .username("tenant-a-user")
                .password("unused")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
    }

    @Test
    void listDocumentsReturnsOnlyCurrentTenantRows() {
        KnowledgeBase localKnowledgeBase = new KnowledgeBase();
        localKnowledgeBase.setId(KB_ID);
        localKnowledgeBase.setTenantId(TENANT_A);
        localKnowledgeBase.setOwnerId(USER_ID);
        localKnowledgeBase.setIsPublic(false);
        localKnowledgeBase.setName("local-kb");
        when(knowledgeBaseMapper.selectByTenantAndId(TENANT_A, KB_ID))
                .thenReturn(localKnowledgeBase);
        when(documentMapper.selectCount(any())).thenReturn(0L);
        when(documentMapper.selectList(any())).thenReturn(List.of(
                document(11L, TENANT_A, "local-document"),
                document(12L, TENANT_B, "foreign-document")));

        var response = controller.listDocuments(KB_ID, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of("local-document"), response.getBody().getData().stream()
                .map(Document::getTitle)
                .toList());
    }

    private Document document(long id, long tenantId, String title) {
        Document document = new Document();
        document.setId(id);
        document.setTenantId(tenantId);
        document.setKbId(KB_ID);
        document.setTitle(title);
        return document;
    }
}

package com.enterprise.rag.admin.controller;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
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
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.core.embedding.EmbeddingService;
import com.enterprise.rag.core.rag.keyword.KeywordIndex;
import com.enterprise.rag.core.vectorstore.VectorStore;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentDeleteTenantEnforcementTest {

    private static final long USER_ID = 1001L;
    private static final long TENANT_A = 901L;
    private static final long TENANT_B = 902L;
    private static final long KB_ID = 100L;
    private static final long DOCUMENT_ID = 200L;

    private DocumentMapper documentMapper;
    private DocumentChunkMapper chunkMapper;
    private KnowledgeBaseMapper knowledgeBaseMapper;
    private KnowledgeBaseController controller;
    private UserPrincipal principal;

    @BeforeAll
    static void initializeMyBatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(
                new MybatisConfiguration(), "document-delete-tenant-test");
        TableInfoHelper.initTableInfo(assistant, Document.class);
        TableInfoHelper.initTableInfo(assistant, DocumentChunk.class);
    }

    @BeforeEach
    void setUp() {
        documentMapper = mock(DocumentMapper.class);
        chunkMapper = mock(DocumentChunkMapper.class);
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        DocumentService documentService = new DocumentServiceImpl(
                documentMapper,
                chunkMapper,
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
                .username("tenant-a-owner")
                .password("unused")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        KnowledgeBase localKnowledgeBase = new KnowledgeBase();
        localKnowledgeBase.setId(KB_ID);
        localKnowledgeBase.setTenantId(TENANT_A);
        localKnowledgeBase.setOwnerId(USER_ID);
        localKnowledgeBase.setName("local-kb");
        localKnowledgeBase.setIsPublic(false);
        when(knowledgeBaseMapper.selectById(KB_ID)).thenReturn(localKnowledgeBase);
        when(knowledgeBaseMapper.selectByTenantAndId(TENANT_A, KB_ID)).thenReturn(localKnowledgeBase);
        when(documentMapper.selectCount(any())).thenReturn(0L);
        when(chunkMapper.selectList(any())).thenReturn(List.of());
        when(chunkMapper.selectByTenantAndDocumentId(TENANT_A, DOCUMENT_ID)).thenReturn(List.of());
        when(documentMapper.deleteById(DOCUMENT_ID)).thenReturn(1);
        when(documentMapper.deleteByTenantAndId(TENANT_A, DOCUMENT_ID)).thenReturn(1);
    }

    @Test
    void foreignTenantDocumentIdIsNotFoundAndNeverDeleted() {
        Document foreignDocument = new Document();
        foreignDocument.setId(DOCUMENT_ID);
        foreignDocument.setTenantId(TENANT_B);
        foreignDocument.setKbId(KB_ID);
        foreignDocument.setTitle("foreign-document");
        when(documentMapper.selectById(DOCUMENT_ID)).thenReturn(foreignDocument);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.deleteDocument(KB_ID, DOCUMENT_ID, principal));

        assertEquals("DOC_004", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        verify(documentMapper, never()).deleteById(DOCUMENT_ID);
    }

    @Test
    void sameTenantDocumentUsesTenantScopedDeleteAndCountMutation() {
        Document localDocument = new Document();
        localDocument.setId(DOCUMENT_ID);
        localDocument.setTenantId(TENANT_A);
        localDocument.setKbId(KB_ID);
        localDocument.setTitle("local-document");
        when(documentMapper.selectByTenantAndId(TENANT_A, DOCUMENT_ID)).thenReturn(localDocument);

        var response = controller.deleteDocument(KB_ID, DOCUMENT_ID, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(chunkMapper).deleteByTenantAndDocumentId(TENANT_A, DOCUMENT_ID);
        verify(documentMapper).deleteByTenantAndId(TENANT_A, DOCUMENT_ID);
        verify(knowledgeBaseMapper).updateDocumentCountByTenantAndId(TENANT_A, KB_ID, -1);
        verify(documentMapper, never()).deleteById(DOCUMENT_ID);
    }
}

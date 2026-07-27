package com.enterprise.rag.admin.security;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.PermissionType;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.dto.QAHistoryDTO;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {

    private static final RequestIdentity IDENTITY = new RequestIdentity(2L, 11L);

    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final KBPermissionService kbPermissionService = mock(KBPermissionService.class);
    private final QAHistoryService qaHistoryService = mock(QAHistoryService.class);

    private final AuthorizationService authorizationService = new AuthorizationService(knowledgeBaseService,
            kbPermissionService, qaHistoryService);

    @Test
    void shouldAllowReadWhenKnowledgeBaseIsPublic() {
        KnowledgeBaseDTO kb = KnowledgeBaseDTO.builder()
                .id(100L)
                .ownerId(1L)
                .isPublic(true)
                .build();
        when(knowledgeBaseService.getById(100L, IDENTITY)).thenReturn(Optional.of(kb));

        KnowledgeBaseDTO result = authorizationService.requireKnowledgeBaseReadAccess(100L, IDENTITY);

        assertEquals(100L, result.getId());
    }

    @Test
    void shouldDenyReadWhenNoAccess() {
        KnowledgeBaseDTO kb = KnowledgeBaseDTO.builder()
                .id(100L)
                .ownerId(1L)
                .isPublic(false)
                .build();
        when(knowledgeBaseService.getById(100L, IDENTITY)).thenReturn(Optional.of(kb));
        when(kbPermissionService.hasPermission(11L, 100L, 2L, PermissionType.READ)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authorizationService.requireKnowledgeBaseReadAccess(100L, IDENTITY));

        assertEquals("AUTH_004", exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getHttpStatus());
    }

    @Test
    void shouldAllowWriteWhenUserHasWritePermission() {
        KnowledgeBaseDTO kb = KnowledgeBaseDTO.builder()
                .id(100L)
                .ownerId(1L)
                .isPublic(false)
                .build();
        when(knowledgeBaseService.getById(100L, IDENTITY)).thenReturn(Optional.of(kb));
        when(kbPermissionService.hasPermission(11L, 100L, 2L, PermissionType.WRITE)).thenReturn(true);

        KnowledgeBaseDTO result = authorizationService.requireKnowledgeBaseWriteAccess(100L, IDENTITY);

        assertEquals(100L, result.getId());
    }

    @Test
    void shouldDenyWriteWhenOnlyReadPermission() {
        KnowledgeBaseDTO kb = KnowledgeBaseDTO.builder()
                .id(100L)
                .ownerId(1L)
                .isPublic(false)
                .build();
        when(knowledgeBaseService.getById(100L, IDENTITY)).thenReturn(Optional.of(kb));
        when(kbPermissionService.hasPermission(11L, 100L, 2L, PermissionType.WRITE)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authorizationService.requireKnowledgeBaseWriteAccess(100L, IDENTITY));

        assertEquals("AUTH_004", exception.getErrorCode());
    }

    @Test
    void shouldAllowAdminWhenUserIsOwner() {
        KnowledgeBaseDTO kb = KnowledgeBaseDTO.builder()
                .id(100L)
                .ownerId(2L)
                .isPublic(false)
                .build();
        when(knowledgeBaseService.getById(100L, IDENTITY)).thenReturn(Optional.of(kb));

        KnowledgeBaseDTO result = authorizationService.requireKnowledgeBaseAdminAccess(100L, IDENTITY);

        assertEquals(100L, result.getId());
    }

    @Test
    void shouldDenyAdminWhenNoAdminPermission() {
        KnowledgeBaseDTO kb = KnowledgeBaseDTO.builder()
                .id(100L)
                .ownerId(1L)
                .isPublic(false)
                .build();
        when(knowledgeBaseService.getById(100L, IDENTITY)).thenReturn(Optional.of(kb));
        when(kbPermissionService.hasPermission(11L, 100L, 2L, PermissionType.ADMIN)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authorizationService.requireKnowledgeBaseAdminAccess(100L, IDENTITY));

        assertEquals("AUTH_004", exception.getErrorCode());
    }

    @Test
    void shouldAllowHistoryWhenCurrentUserIsOwner() {
        QAHistoryDTO history = QAHistoryDTO.builder()
                .id(88L)
                .userId(2L)
                .build();
        when(qaHistoryService.getById(IDENTITY, 88L)).thenReturn(Optional.of(history));

        QAHistoryDTO result = authorizationService.requireHistoryOwner(88L, IDENTITY);

        assertEquals(88L, result.getId());
    }

    @Test
    void shouldReturnNotFoundWhenHistoryIsOutsideIdentityScope() {
        when(qaHistoryService.getById(IDENTITY, 88L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authorizationService.requireHistoryOwner(88L, IDENTITY));

        assertEquals("HISTORY_001", exception.getErrorCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    }

    @Test
    void unscopedAuthorizationEntrypointsFailClosed() {
        assertAll(
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class,
                                () -> authorizationService.requireKnowledgeBaseReadAccess(100L, 2L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class,
                                () -> authorizationService.requireKnowledgeBaseWriteAccess(100L, 2L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class,
                                () -> authorizationService.requireKnowledgeBaseAdminAccess(100L, 2L)).getMessage()),
                () -> assertEquals("TENANT_IDENTITY_REQUIRED",
                        assertThrows(IllegalStateException.class,
                                () -> authorizationService.requireHistoryOwner(88L, 2L)).getMessage()));
    }
}

package com.enterprise.rag.admin.security;

import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.entity.PermissionType;
import com.enterprise.rag.admin.kb.service.KBPermissionService;
import com.enterprise.rag.admin.kb.service.KnowledgeBaseService;
import com.enterprise.rag.admin.qa.dto.QAHistoryDTO;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 统一资源级授权校验入口。
 */
@Component
@RequiredArgsConstructor
public class AuthorizationService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KBPermissionService kbPermissionService;
    private final QAHistoryService qaHistoryService;

    public KnowledgeBaseDTO requireKnowledgeBaseReadAccess(Long kbId, Long userId) {
        throw tenantIdentityRequired();
    }

    /**
     * 在服务端认证 tenant 边界内校验知识库读取权限。
     */
    public KnowledgeBaseDTO requireKnowledgeBaseReadAccess(Long kbId, RequestIdentity identity) {
        KnowledgeBaseDTO kb = knowledgeBaseService.getById(kbId, identity)
                .orElseThrow(() -> new BusinessException(
                        "KB_001", "知识库不存在: " + kbId, HttpStatus.NOT_FOUND));
        boolean canAccess = Boolean.TRUE.equals(kb.getIsPublic())
                || isOwner(kb, identity.userId())
                || kbPermissionService.hasPermission(
                        identity.tenantId(), kbId, identity.userId(), PermissionType.READ);
        if (!canAccess) {
            throw forbidden("无权访问该知识库");
        }
        return kb;
    }

    public KnowledgeBaseDTO requireKnowledgeBaseWriteAccess(Long kbId, Long userId) {
        throw tenantIdentityRequired();
    }

    public KnowledgeBaseDTO requireKnowledgeBaseAdminAccess(Long kbId, Long userId) {
        throw tenantIdentityRequired();
    }

    public KnowledgeBaseDTO requireKnowledgeBaseWriteAccess(Long kbId, RequestIdentity identity) {
        KnowledgeBaseDTO kb = knowledgeBaseService.getById(kbId, identity)
                .orElseThrow(() -> new BusinessException(
                        "KB_001", "知识库不存在: " + kbId, HttpStatus.NOT_FOUND));
        if (isOwner(kb, identity.userId())) {
            return kb;
        }
        if (!kbPermissionService.hasPermission(
                identity.tenantId(), kbId, identity.userId(), PermissionType.WRITE)) {
            throw forbidden("无权修改该知识库");
        }
        return kb;
    }

    public KnowledgeBaseDTO requireKnowledgeBaseAdminAccess(Long kbId, RequestIdentity identity) {
        KnowledgeBaseDTO kb = knowledgeBaseService.getById(kbId, identity)
                .orElseThrow(() -> new BusinessException(
                        "KB_001", "知识库不存在: " + kbId, HttpStatus.NOT_FOUND));
        if (isOwner(kb, identity.userId())) {
            return kb;
        }
        if (!kbPermissionService.hasPermission(
                identity.tenantId(), kbId, identity.userId(), PermissionType.ADMIN)) {
            throw forbidden("无权管理该知识库");
        }
        return kb;
    }

    public QAHistoryDTO requireHistoryOwner(Long historyId, Long userId) {
        throw tenantIdentityRequired();
    }

    public QAHistoryDTO requireHistoryOwner(Long historyId, RequestIdentity identity) {
        return qaHistoryService.getById(identity, historyId)
                .orElseThrow(() -> new BusinessException(
                        "HISTORY_001", "历史记录不存在: " + historyId, HttpStatus.NOT_FOUND));
    }

    private boolean isOwner(KnowledgeBaseDTO kb, Long userId) {
        return kb.getOwnerId() != null && kb.getOwnerId().equals(userId);
    }

    private BusinessException forbidden(String message) {
        return new BusinessException("AUTH_004", message, HttpStatus.FORBIDDEN);
    }

    private IllegalStateException tenantIdentityRequired() {
        return new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }
}

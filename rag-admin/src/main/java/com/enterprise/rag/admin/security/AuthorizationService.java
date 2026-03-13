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
        KnowledgeBaseDTO kb = getKnowledgeBaseOrThrow(kbId);
        boolean canAccess = kbPermissionService.canAccess(kbId, userId, kb.getIsPublic(), kb.getOwnerId());
        if (!canAccess) {
            throw forbidden("无权访问该知识库");
        }
        return kb;
    }

    public KnowledgeBaseDTO requireKnowledgeBaseWriteAccess(Long kbId, Long userId) {
        KnowledgeBaseDTO kb = getKnowledgeBaseOrThrow(kbId);
        if (isOwner(kb, userId)) {
            return kb;
        }
        if (!kbPermissionService.hasPermission(kbId, userId, PermissionType.WRITE)) {
            throw forbidden("无权修改该知识库");
        }
        return kb;
    }

    public KnowledgeBaseDTO requireKnowledgeBaseAdminAccess(Long kbId, Long userId) {
        KnowledgeBaseDTO kb = getKnowledgeBaseOrThrow(kbId);
        if (isOwner(kb, userId)) {
            return kb;
        }
        if (!kbPermissionService.hasPermission(kbId, userId, PermissionType.ADMIN)) {
            throw forbidden("无权管理该知识库");
        }
        return kb;
    }

    public QAHistoryDTO requireHistoryOwner(Long historyId, Long userId) {
        QAHistoryDTO history = qaHistoryService.getById(historyId)
                .orElseThrow(() -> new BusinessException("HISTORY_001", "历史记录不存在: " + historyId));
        if (!userId.equals(history.getUserId())) {
            throw forbidden("无权访问该历史记录");
        }
        return history;
    }

    private KnowledgeBaseDTO getKnowledgeBaseOrThrow(Long kbId) {
        return knowledgeBaseService.getById(kbId)
                .orElseThrow(() -> new BusinessException("KB_001", "知识库不存在: " + kbId));
    }

    private boolean isOwner(KnowledgeBaseDTO kb, Long userId) {
        return kb.getOwnerId() != null && kb.getOwnerId().equals(userId);
    }

    private BusinessException forbidden(String message) {
        return new BusinessException("AUTH_004", message, HttpStatus.FORBIDDEN);
    }
}
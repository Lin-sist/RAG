package com.enterprise.rag.admin.qa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.rag.admin.qa.dto.QAFeedbackDTO;
import com.enterprise.rag.admin.qa.dto.SubmitFeedbackRequest;
import com.enterprise.rag.admin.qa.entity.QAFeedback;
import com.enterprise.rag.admin.qa.mapper.QAFeedbackMapper;
import com.enterprise.rag.admin.qa.service.QAFeedbackService;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.common.exception.BusinessException;
import com.enterprise.rag.common.idempotency.Idempotent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 问答反馈服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QAFeedbackServiceImpl implements QAFeedbackService {

    private final QAFeedbackMapper qaFeedbackMapper;

    @Override
    @Transactional
    @Idempotent(keyPrefix = "qa:feedback:submit", required = false, ttlSeconds = 86400)
    public QAFeedbackDTO submit(SubmitFeedbackRequest request) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    @Idempotent(keyPrefix = "qa:feedback:submit", required = false, ttlSeconds = 86400)
    public QAFeedbackDTO submit(RequestIdentity identity, SubmitFeedbackRequest request) {
        requireAuthenticatedUser(identity, request == null ? null : request.getUserId());
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BusinessException("FEEDBACK_001", "评分必须在1-5之间");
        }
        if (!qaFeedbackMapper.historyOwnedByTenantAndUser(
                identity.tenantId(), request.getQaId(), identity.userId())) {
            throw new BusinessException("HISTORY_001", "历史记录不存在", HttpStatus.NOT_FOUND);
        }
        if (hasUserFeedback(identity, request.getQaId())) {
            throw new BusinessException("FEEDBACK_002", "您已对该问答提交过反馈");
        }

        QAFeedback feedback = new QAFeedback();
        feedback.setTenantId(identity.tenantId());
        feedback.setQaId(request.getQaId());
        feedback.setUserId(identity.userId());
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());

        try {
            qaFeedbackMapper.insert(feedback);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("FEEDBACK_002", "您已对该问答提交过反馈");
        }
        log.info("Submitted tenant-scoped feedback: id={}, qaId={}, userId={}, rating={}",
                feedback.getId(), feedback.getQaId(), feedback.getUserId(), feedback.getRating());
        return toDTO(feedback);
    }

    @Override
    public Optional<QAFeedbackDTO> getById(Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public Optional<QAFeedbackDTO> getById(RequestIdentity identity, Long id) {
        requireIdentity(identity);
        return Optional.ofNullable(qaFeedbackMapper.selectByTenantUserAndId(
                        identity.tenantId(), identity.userId(), id))
                .map(this::toDTO);
    }

    @Override
    public Optional<QAFeedbackDTO> getByQaId(Long qaId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public Optional<QAFeedbackDTO> getByQaId(RequestIdentity identity, Long qaId) {
        requireIdentity(identity);
        return Optional.ofNullable(qaFeedbackMapper.selectLatestByTenantQaAndUser(
                        identity.tenantId(), qaId, identity.userId()))
                .map(this::toDTO);
    }

    @Override
    public List<QAFeedbackDTO> listByQaId(Long qaId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public List<QAFeedbackDTO> listByQaId(RequestIdentity identity, Long qaId) {
        requireIdentity(identity);
        return qaFeedbackMapper.selectByTenantQaAndUser(
                        identity.tenantId(), qaId, identity.userId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<QAFeedbackDTO> listByUserId(Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public List<QAFeedbackDTO> listByUserId(RequestIdentity identity) {
        requireIdentity(identity);
        return qaFeedbackMapper.selectByTenantAndUser(identity.tenantId(), identity.userId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public boolean hasUserFeedback(Long qaId, Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    public boolean hasUserFeedback(RequestIdentity identity, Long qaId) {
        requireIdentity(identity);
        return qaFeedbackMapper.countByTenantQaAndUser(
                identity.tenantId(), qaId, identity.userId()) > 0;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public void delete(RequestIdentity identity, Long id) {
        requireIdentity(identity);
        qaFeedbackMapper.deleteByTenantUserAndId(identity.tenantId(), identity.userId(), id);
        log.info("Deleted tenant-scoped feedback: id={}", id);
    }

    @Override
    @Transactional
    public void deleteByQaId(Long qaId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    @Override
    @Transactional
    public void deleteByQaId(RequestIdentity identity, Long qaId) {
        requireIdentity(identity);
        int deleted = qaFeedbackMapper.deleteByTenantQaAndUser(
                identity.tenantId(), qaId, identity.userId());
        log.info("Deleted {} tenant-scoped feedback records for qaId={}", deleted, qaId);
    }

    /**
     * 将实体转换为DTO
     */
    private QAFeedbackDTO toDTO(QAFeedback feedback) {
        return QAFeedbackDTO.builder()
                .id(feedback.getId())
                .qaId(feedback.getQaId())
                .userId(feedback.getUserId())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    private void requireAuthenticatedUser(RequestIdentity identity, Long requestedUserId) {
        requireIdentity(identity);
        if (requestedUserId == null || requestedUserId.longValue() != identity.userId()) {
            throw new BusinessException("AUTH_004", "无权访问该资源", HttpStatus.FORBIDDEN);
        }
    }

    private void requireIdentity(RequestIdentity identity) {
        if (identity == null) {
            throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
        }
    }
}

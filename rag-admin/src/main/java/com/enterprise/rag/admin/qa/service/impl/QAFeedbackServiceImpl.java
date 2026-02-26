package com.enterprise.rag.admin.qa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterprise.rag.admin.qa.dto.QAFeedbackDTO;
import com.enterprise.rag.admin.qa.dto.SubmitFeedbackRequest;
import com.enterprise.rag.admin.qa.entity.QAFeedback;
import com.enterprise.rag.admin.qa.mapper.QAFeedbackMapper;
import com.enterprise.rag.admin.qa.service.QAFeedbackService;
import com.enterprise.rag.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public QAFeedbackDTO submit(SubmitFeedbackRequest request) {
        // 验证评分范围
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new BusinessException("FEEDBACK_001", "评分必须在1-5之间");
        }

        // 检查是否已提交过反馈
        if (hasUserFeedback(request.getQaId(), request.getUserId())) {
            throw new BusinessException("FEEDBACK_002", "您已对该问答提交过反馈");
        }

        QAFeedback feedback = new QAFeedback();
        feedback.setQaId(request.getQaId());
        feedback.setUserId(request.getUserId());
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());

        qaFeedbackMapper.insert(feedback);
        log.info("Submitted feedback: id={}, qaId={}, userId={}, rating={}", 
                feedback.getId(), feedback.getQaId(), feedback.getUserId(), feedback.getRating());

        return toDTO(feedback);
    }

    @Override
    public Optional<QAFeedbackDTO> getById(Long id) {
        QAFeedback feedback = qaFeedbackMapper.selectById(id);
        return Optional.ofNullable(feedback).map(this::toDTO);
    }

    @Override
    public Optional<QAFeedbackDTO> getByQaId(Long qaId) {
        LambdaQueryWrapper<QAFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QAFeedback::getQaId, qaId)
               .orderByDesc(QAFeedback::getCreatedAt)
               .last("LIMIT 1");
        QAFeedback feedback = qaFeedbackMapper.selectOne(wrapper);
        return Optional.ofNullable(feedback).map(this::toDTO);
    }

    @Override
    public List<QAFeedbackDTO> listByQaId(Long qaId) {
        LambdaQueryWrapper<QAFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QAFeedback::getQaId, qaId)
               .orderByDesc(QAFeedback::getCreatedAt);
        return qaFeedbackMapper.selectList(wrapper)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public List<QAFeedbackDTO> listByUserId(Long userId) {
        LambdaQueryWrapper<QAFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QAFeedback::getUserId, userId)
               .orderByDesc(QAFeedback::getCreatedAt);
        return qaFeedbackMapper.selectList(wrapper)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public boolean hasUserFeedback(Long qaId, Long userId) {
        LambdaQueryWrapper<QAFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QAFeedback::getQaId, qaId)
               .eq(QAFeedback::getUserId, userId);
        return qaFeedbackMapper.selectCount(wrapper) > 0;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        qaFeedbackMapper.deleteById(id);
        log.info("Deleted feedback: id={}", id);
    }

    @Override
    @Transactional
    public void deleteByQaId(Long qaId) {
        LambdaQueryWrapper<QAFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QAFeedback::getQaId, qaId);
        int deleted = qaFeedbackMapper.delete(wrapper);
        log.info("Deleted {} feedback records for qaId={}", deleted, qaId);
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
}

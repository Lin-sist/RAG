package com.enterprise.rag.admin.qa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.rag.admin.qa.dto.PageResult;
import com.enterprise.rag.admin.qa.dto.QAHistoryDTO;
import com.enterprise.rag.admin.qa.dto.QAHistoryPageRequest;
import com.enterprise.rag.admin.qa.dto.SaveQAHistoryRequest;
import com.enterprise.rag.admin.qa.entity.QAHistory;
import com.enterprise.rag.admin.qa.mapper.QAHistoryMapper;
import com.enterprise.rag.admin.qa.service.QAHistoryService;
import com.enterprise.rag.core.rag.model.Citation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 问答历史服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QAHistoryServiceImpl implements QAHistoryService {

    private final QAHistoryMapper qaHistoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QAHistoryDTO save(SaveQAHistoryRequest request) {
        QAHistory history = new QAHistory();
        history.setUserId(request.getUserId());
        history.setKbId(request.getKbId());
        history.setQuestion(request.getQuestion());
        history.setAnswer(request.getAnswer());
        history.setTraceId(request.getTraceId());
        history.setLatencyMs(request.getLatencyMs());

        // 序列化引用来源
        if (request.getCitations() != null && !request.getCitations().isEmpty()) {
            try {
                history.setCitations(objectMapper.writeValueAsString(request.getCitations()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize citations", e);
                history.setCitations("[]");
            }
        } else {
            history.setCitations("[]");
        }

        qaHistoryMapper.insert(history);
        log.info("Saved QA history: id={}, userId={}, kbId={}", 
                history.getId(), history.getUserId(), history.getKbId());

        return toDTO(history);
    }

    @Override
    public Optional<QAHistoryDTO> getById(Long id) {
        QAHistory history = qaHistoryMapper.selectById(id);
        return Optional.ofNullable(history).map(this::toDTO);
    }

    @Override
    public PageResult<QAHistoryDTO> getPage(QAHistoryPageRequest request) {
        LambdaQueryWrapper<QAHistory> wrapper = new LambdaQueryWrapper<>();
        
        // 添加过滤条件
        if (request.getUserId() != null) {
            wrapper.eq(QAHistory::getUserId, request.getUserId());
        }
        if (request.getKbId() != null) {
            wrapper.eq(QAHistory::getKbId, request.getKbId());
        }
        
        // 按创建时间倒序排列
        wrapper.orderByDesc(QAHistory::getCreatedAt);

        // 执行分页查询
        Page<QAHistory> page = new Page<>(request.getPage(), request.getSize());
        Page<QAHistory> result = qaHistoryMapper.selectPage(page, wrapper);

        List<QAHistoryDTO> records = result.getRecords()
                .stream()
                .map(this::toDTO)
                .toList();

        return PageResult.of(records, result.getTotal(), request.getPage(), request.getSize());
    }

    @Override
    public long countByUserId(Long userId) {
        LambdaQueryWrapper<QAHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QAHistory::getUserId, userId);
        return qaHistoryMapper.selectCount(wrapper);
    }

    @Override
    public long countByKbId(Long kbId) {
        LambdaQueryWrapper<QAHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QAHistory::getKbId, kbId);
        return qaHistoryMapper.selectCount(wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        qaHistoryMapper.deleteById(id);
        log.info("Deleted QA history: id={}", id);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        LambdaQueryWrapper<QAHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QAHistory::getUserId, userId);
        int deleted = qaHistoryMapper.delete(wrapper);
        log.info("Deleted {} QA history records for userId={}", deleted, userId);
    }

    /**
     * 将实体转换为DTO
     */
    private QAHistoryDTO toDTO(QAHistory history) {
        List<Citation> citations = parseCitations(history.getCitations());
        
        return QAHistoryDTO.builder()
                .id(history.getId())
                .userId(history.getUserId())
                .kbId(history.getKbId())
                .question(history.getQuestion())
                .answer(history.getAnswer())
                .citations(citations)
                .traceId(history.getTraceId())
                .latencyMs(history.getLatencyMs())
                .createdAt(history.getCreatedAt())
                .build();
    }

    /**
     * 解析引用来源JSON
     */
    private List<Citation> parseCitations(String citationsJson) {
        if (citationsJson == null || citationsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(citationsJson, new TypeReference<List<Citation>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse citations JSON: {}", citationsJson, e);
            return Collections.emptyList();
        }
    }
}

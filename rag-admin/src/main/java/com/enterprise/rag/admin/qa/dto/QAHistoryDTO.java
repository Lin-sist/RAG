package com.enterprise.rag.admin.qa.dto;

import com.enterprise.rag.core.rag.model.Citation;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问答历史DTO
 */
@Data
@Builder
public class QAHistoryDTO {

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 知识库ID
     */
    private Long kbId;

    /**
     * 问题
     */
    private String question;

    /**
     * 答案
     */
    private String answer;

    /**
     * 引用来源列表
     */
    private List<Citation> citations;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 响应延迟（毫秒）
     */
    private Integer latencyMs;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

package com.enterprise.rag.admin.qa.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答反馈DTO
 */
@Data
@Builder
public class QAFeedbackDTO {

    /**
     * 反馈ID
     */
    private Long id;

    /**
     * 问答历史ID
     */
    private Long qaId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 评分
     */
    private Integer rating;

    /**
     * 评论
     */
    private String comment;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

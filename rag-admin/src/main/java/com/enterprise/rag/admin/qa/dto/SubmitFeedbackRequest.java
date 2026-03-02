package com.enterprise.rag.admin.qa.dto;

import lombok.Data;

/**
 * 提交反馈请求
 */
@Data
public class SubmitFeedbackRequest {

    /**
     * 问答历史ID
     */
    private Long qaId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 评分（1=无用，5=有用，或1-5的评分）
     */
    private Integer rating;

    /**
     * 评论（可选）
     */
    private String comment;
}

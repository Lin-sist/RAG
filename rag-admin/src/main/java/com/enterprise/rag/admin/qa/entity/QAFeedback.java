package com.enterprise.rag.admin.qa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.rag.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 问答反馈实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qa_feedback")
public class QAFeedback extends BaseEntity {

    /**
     * 问答历史ID
     */
    private Long qaId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 评分（1-5，或者简单的有用/无用：1=无用，5=有用）
     */
    private Integer rating;

    /**
     * 评论
     */
    private String comment;
}

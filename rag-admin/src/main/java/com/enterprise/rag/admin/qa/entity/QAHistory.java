package com.enterprise.rag.admin.qa.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.rag.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 问答历史实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qa_history")
public class QAHistory extends BaseEntity {

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
     * 引用来源（JSON格式）
     */
    private String citations;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 响应延迟（毫秒）
     */
    private Integer latencyMs;
}

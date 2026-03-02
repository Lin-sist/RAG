package com.enterprise.rag.admin.qa.dto;

import com.enterprise.rag.core.rag.model.Citation;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 保存问答历史请求
 */
@Data
@Builder
public class SaveQAHistoryRequest {

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
}

package com.enterprise.rag.admin.qa.dto;

import lombok.Data;

/**
 * 问答历史分页查询请求
 */
@Data
public class QAHistoryPageRequest {

    /**
     * 用户ID（可选，管理员可查询所有用户）
     */
    private Long userId;

    /**
     * 知识库ID（可选）
     */
    private Long kbId;

    /**
     * 页码（从1开始）
     */
    private int page = 1;

    /**
     * 每页大小
     */
    private int size = 20;

    /**
     * 获取偏移量
     */
    public long getOffset() {
        return (long) (page - 1) * size;
    }
}

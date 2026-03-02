package com.enterprise.rag.admin.kb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseStatistics {
    
    /**
     * 知识库ID
     */
    private Long kbId;
    
    /**
     * 文档数量
     */
    private Integer documentCount;
    
    /**
     * 向量数量
     */
    private Long vectorCount;
    
    /**
     * 查询次数
     */
    private Long queryCount;
}

package com.enterprise.rag.admin.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.rag.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档分块实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_chunk")
public class DocumentChunk extends BaseEntity {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 向量ID（在向量数据库中的ID）
     */
    private String vectorId;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 分块索引
     */
    private Integer chunkIndex;

    /**
     * 起始位置
     */
    private Integer startPos;

    /**
     * 结束位置
     */
    private Integer endPos;

    /**
     * 元数据（JSON格式）
     */
    private String metadata;
}

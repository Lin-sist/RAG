package com.enterprise.rag.admin.kb.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.rag.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_base")
public class KnowledgeBase extends BaseEntity {

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 所有者ID
     */
    private Long ownerId;

    /**
     * 向量集合名称
     */
    private String vectorCollection;

    /**
     * 文档数量
     */
    private Integer documentCount;

    /**
     * 是否公开
     */
    @TableField("is_public")
    private Boolean isPublic;
}

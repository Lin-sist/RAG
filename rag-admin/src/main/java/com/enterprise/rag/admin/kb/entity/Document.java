package com.enterprise.rag.admin.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.rag.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document")
public class Document extends BaseEntity {

    /**
     * 知识库ID
     */
    private Long kbId;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 内容哈希（用于幂等性检查）
     */
    private String contentHash;

    /**
     * 文档状态
     */
    private String status;

    /**
     * 分块数量
     */
    private Integer chunkCount;
}

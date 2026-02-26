package com.enterprise.rag.admin.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.rag.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("kb_permission")
public class KBPermission extends BaseEntity {

    /**
     * 知识库ID
     */
    private Long kbId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 权限类型：READ, WRITE, ADMIN
     */
    private String permissionType;
}

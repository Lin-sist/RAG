package com.enterprise.rag.admin.kb.entity;

/**
 * 知识库权限类型枚举
 */
public enum PermissionType {
    /**
     * 只读权限 - 可以查看知识库和执行问答
     */
    READ,
    
    /**
     * 写入权限 - 可以上传和删除文档
     */
    WRITE,
    
    /**
     * 管理员权限 - 可以管理权限和删除知识库
     */
    ADMIN
}

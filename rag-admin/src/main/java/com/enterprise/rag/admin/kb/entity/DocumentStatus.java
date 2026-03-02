package com.enterprise.rag.admin.kb.entity;

/**
 * 文档状态枚举
 */
public enum DocumentStatus {
    /**
     * 待处理
     */
    PENDING,
    
    /**
     * 处理中
     */
    PROCESSING,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 处理失败
     */
    FAILED
}

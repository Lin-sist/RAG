package com.enterprise.rag.common.async;

/**
 * 任务状态枚举
 */
public enum TaskState {
    /**
     * 等待执行
     */
    PENDING,
    
    /**
     * 正在执行
     */
    RUNNING,
    
    /**
     * 执行完成
     */
    COMPLETED,
    
    /**
     * 执行失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELLED
}

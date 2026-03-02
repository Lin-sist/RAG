package com.enterprise.rag.document.chunker;

/**
 * 文档分块策略
 */
public enum ChunkStrategy {
    /**
     * 固定大小分块
     */
    FIXED_SIZE,
    
    /**
     * 语义分块（按段落、句子等自然边界）
     */
    SEMANTIC,
    
    /**
     * 代码分块（按函数、类等代码结构）
     */
    CODE
}

package com.enterprise.rag.document.processor;

import com.enterprise.rag.document.chunker.ChunkConfig;
import com.enterprise.rag.document.chunker.DocumentChunk;

import java.util.List;

/**
 * 文档处理器接口
 * 协调文档解析、分块流程
 */
public interface DocumentProcessor {
    
    /**
     * 处理文档
     *
     * @param input 文档输入
     * @return 处理结果
     */
    ProcessResult process(DocumentInput input);
    
    /**
     * 使用指定配置处理文档
     *
     * @param input  文档输入
     * @param config 分块配置
     * @return 处理结果
     */
    ProcessResult process(DocumentInput input, ChunkConfig config);
    
    /**
     * 将文本内容分块
     *
     * @param content 文本内容
     * @param config  分块配置
     * @return 文档块列表
     */
    List<DocumentChunk> chunk(String content, ChunkConfig config);
    
    /**
     * 检查内容是否已存在（幂等性检查）
     *
     * @param contentHash 内容哈希
     * @return 是否已存在
     */
    boolean exists(String contentHash);
    
    /**
     * 计算内容哈希
     *
     * @param content 内容
     * @return 哈希值
     */
    String computeHash(String content);
}

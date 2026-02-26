package com.enterprise.rag.document.chunker;

import java.util.Map;

/**
 * 文档块
 *
 * @param id         块唯一标识
 * @param content    块内容
 * @param startIndex 在原文档中的起始位置
 * @param endIndex   在原文档中的结束位置
 * @param metadata   元数据
 */
public record DocumentChunk(
        String id,
        String content,
        int startIndex,
        int endIndex,
        Map<String, Object> metadata
) {
    public DocumentChunk {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (startIndex < 0) {
            throw new IllegalArgumentException("Start index must be non-negative");
        }
        if (endIndex < startIndex) {
            throw new IllegalArgumentException("End index must be >= start index");
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }
    
    /**
     * 获取块长度
     */
    public int length() {
        return content.length();
    }
    
    /**
     * 创建带索引的块
     */
    public static DocumentChunk of(String id, String content, int startIndex, int endIndex) {
        return new DocumentChunk(id, content, startIndex, endIndex, Map.of());
    }
    
    /**
     * 创建带元数据的块
     */
    public static DocumentChunk of(String id, String content, int startIndex, int endIndex, Map<String, Object> metadata) {
        return new DocumentChunk(id, content, startIndex, endIndex, metadata);
    }
}

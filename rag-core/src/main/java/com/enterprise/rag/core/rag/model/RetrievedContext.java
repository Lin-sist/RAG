package com.enterprise.rag.core.rag.model;

import java.util.Map;

/**
 * 检索上下文记录类
 * 表示从向量数据库检索到的相关文档上下文
 *
 * @param content        文档内容
 * @param source         来源标识（文档ID或路径）
 * @param relevanceScore 相关性分数（0-1之间，越高越相关）
 * @param metadata       元数据（如文档标题、创建时间等）
 */
public record RetrievedContext(
        String content,
        String source,
        float relevanceScore,
        Map<String, Object> metadata
) {
    /**
     * 创建不带元数据的检索上下文
     */
    public RetrievedContext(String content, String source, float relevanceScore) {
        this(content, source, relevanceScore, Map.of());
    }
}

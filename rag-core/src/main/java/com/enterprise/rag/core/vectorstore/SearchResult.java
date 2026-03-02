package com.enterprise.rag.core.vectorstore;

import java.util.Map;

/**
 * 向量搜索结果记录类
 * 表示向量相似度搜索返回的单个结果
 *
 * @param id       文档唯一标识
 * @param content  文档文本内容
 * @param score    相似度分数（越高越相似）
 * @param metadata 文档元数据
 */
public record SearchResult(
        String id,
        String content,
        float score,
        Map<String, Object> metadata
) {
    /**
     * 创建不带元数据的搜索结果
     */
    public SearchResult(String id, String content, float score) {
        this(id, content, score, Map.of());
    }
}

package com.enterprise.rag.core.rag.model;

/**
 * 引用来源记录类
 * 表示答案中引用的文档来源
 *
 * @param source     来源标识（文档ID或路径）
 * @param snippet    引用的文本片段
 * @param startIndex 片段在原文中的起始位置
 * @param endIndex   片段在原文中的结束位置
 */
public record Citation(
        String source,
        String snippet,
        int startIndex,
        int endIndex
) {
    /**
     * 创建简单引用（不带位置信息）
     */
    public static Citation of(String source, String snippet) {
        return new Citation(source, snippet, -1, -1);
    }

    /**
     * 创建带位置信息的引用
     */
    public static Citation of(String source, String snippet, int startIndex, int endIndex) {
        return new Citation(source, snippet, startIndex, endIndex);
    }
}

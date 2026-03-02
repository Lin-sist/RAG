package com.enterprise.rag.core.vectorstore;

import java.util.Map;

/**
 * 向量搜索选项记录类
 * 配置向量搜索的参数
 *
 * @param topK     返回的最大结果数量
 * @param minScore 最小相似度分数阈值（0-1之间）
 * @param filter   元数据过滤条件
 */
public record SearchOptions(
        int topK,
        float minScore,
        Map<String, Object> filter
) {
    /**
     * 默认搜索选项：返回10条结果，无最小分数限制，无过滤
     */
    public static final SearchOptions DEFAULT = new SearchOptions(10, 0.0f, Map.of());

    /**
     * 创建只指定topK的搜索选项
     */
    public static SearchOptions withTopK(int topK) {
        return new SearchOptions(topK, 0.0f, Map.of());
    }

    /**
     * 创建带过滤条件的搜索选项
     */
    public static SearchOptions withFilter(int topK, Map<String, Object> filter) {
        return new SearchOptions(topK, 0.0f, filter);
    }

    /**
     * 验证搜索选项的有效性
     */
    public boolean isValid() {
        return topK > 0 && minScore >= 0.0f && minScore <= 1.0f;
    }
}

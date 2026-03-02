package com.enterprise.rag.core.rag.model;

import java.util.Map;

/**
 * 检索选项记录类
 * 配置查询引擎的检索参数
 *
 * @param collectionName 向量集合名称
 * @param topK           返回的最大结果数量
 * @param minScore       最小相关性分数阈值
 * @param filter         元数据过滤条件
 * @param enableRerank   是否启用重排序
 */
public record RetrieveOptions(
        String collectionName,
        int topK,
        float minScore,
        Map<String, Object> filter,
        boolean enableRerank
) {
    /**
     * 默认检索选项
     */
    public static final int DEFAULT_TOP_K = 5;
    public static final float DEFAULT_MIN_SCORE = 0.5f;

    /**
     * 创建基本检索选项
     */
    public static RetrieveOptions of(String collectionName) {
        return new RetrieveOptions(collectionName, DEFAULT_TOP_K, DEFAULT_MIN_SCORE, Map.of(), true);
    }

    /**
     * 创建指定topK的检索选项
     */
    public static RetrieveOptions of(String collectionName, int topK) {
        return new RetrieveOptions(collectionName, topK, DEFAULT_MIN_SCORE, Map.of(), true);
    }

    /**
     * 创建带过滤条件的检索选项
     */
    public static RetrieveOptions withFilter(String collectionName, int topK, Map<String, Object> filter) {
        return new RetrieveOptions(collectionName, topK, DEFAULT_MIN_SCORE, filter, true);
    }
}

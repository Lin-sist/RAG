package com.enterprise.rag.core.rag.model;

import java.util.Map;

/**
 * 问答请求记录类
 *
 * @param question       用户问题
 * @param collectionName 知识库集合名称
 * @param topK           检索结果数量
 * @param minScore       最小相似度阈值
 * @param filter         元数据过滤条件
 * @param enableCache    是否启用缓存
 * @param stream         是否使用流式响应
 */
public record QARequest(
        String question,
        String collectionName,
        int topK,
     float minScore,
        Map<String, Object> filter,
        boolean enableCache,
        boolean stream) {
    /**
     * 默认检索数量
     */
    public static final int DEFAULT_TOP_K = 5;

    /**
     * 默认最小相似度阈值
     */
    public static final float DEFAULT_MIN_SCORE = RetrieveOptions.DEFAULT_MIN_SCORE;

    /**
     * 向后兼容构造函数（不显式传入 minScore 时使用默认值）
     */
    public QARequest(String question,
            String collectionName,
            int topK,
            Map<String, Object> filter,
            boolean enableCache,
            boolean stream) {
        this(question, collectionName, topK, DEFAULT_MIN_SCORE, filter, enableCache, stream);
    }

    /**
     * 创建基本请求
     */
    public static QARequest of(String question, String collectionName) {
        return new QARequest(question, collectionName, DEFAULT_TOP_K, DEFAULT_MIN_SCORE, Map.of(), true, false);
    }

    /**
     * 创建指定topK的请求
     */
    public static QARequest of(String question, String collectionName, int topK) {
        return new QARequest(question, collectionName, topK, DEFAULT_MIN_SCORE, Map.of(), true, false);
    }

    /**
     * 创建流式请求
     */
    public static QARequest stream(String question, String collectionName) {
        return new QARequest(question, collectionName, DEFAULT_TOP_K, DEFAULT_MIN_SCORE, Map.of(), false, true);
    }

    /**
     * 创建带参数的流式请求
     */
    public static QARequest stream(String question, String collectionName, int topK,
            Map<String, Object> filter, boolean enableCache) {
        return new QARequest(question, collectionName, topK, DEFAULT_MIN_SCORE, filter, enableCache, true);
    }

    /**
     * 创建带最小相似度阈值的流式请求
     */
    public static QARequest stream(String question,
            String collectionName,
            int topK,
            float minScore,
            Map<String, Object> filter,
            boolean enableCache) {
        return new QARequest(question, collectionName, topK, minScore, filter, enableCache, true);
    }

    /**
     * 创建带过滤条件的请求
     */
    public static QARequest withFilter(String question, String collectionName, Map<String, Object> filter) {
        return new QARequest(question, collectionName, DEFAULT_TOP_K, DEFAULT_MIN_SCORE, filter, true, false);
    }
}

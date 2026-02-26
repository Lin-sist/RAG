package com.enterprise.rag.core.embedding;

import java.util.List;

/**
 * 嵌入服务接口
 * 提供文本向量化能力，支持缓存和多提供者降级
 */
public interface EmbeddingService {

    /**
     * 获取单个文本的向量嵌入
     *
     * @param text 输入文本
     * @return 向量表示
     */
    float[] embed(String text);

    /**
     * 批量获取文本的向量嵌入
     *
     * @param texts 输入文本列表
     * @return 向量表示列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取当前使用的向量维度
     *
     * @return 向量维度
     */
    int getDimension();

    /**
     * 获取当前活跃的提供者名称
     *
     * @return 提供者名称
     */
    String getActiveProviderName();

    /**
     * 清除指定文本的缓存
     *
     * @param text 文本内容
     */
    void evictCache(String text);

    /**
     * 清除所有嵌入缓存
     */
    void clearAllCache();
}

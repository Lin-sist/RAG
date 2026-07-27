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
    float[] embed(long tenantId, String text);

    /** @deprecated Tenant scope is required for embedding cache isolation. */
    @Deprecated(since = "C13b", forRemoval = false)
    default float[] embed(String text) {
        throw new EmbeddingException("Tenant scope is required");
    }

    /**
     * 批量获取文本的向量嵌入
     *
     * @param texts 输入文本列表
     * @return 向量表示列表
     */
    List<float[]> embedBatch(long tenantId, List<String> texts);

    /** @deprecated Tenant scope is required for embedding cache isolation. */
    @Deprecated(since = "C13b", forRemoval = false)
    default List<float[]> embedBatch(List<String> texts) {
        throw new EmbeddingException("Tenant scope is required");
    }

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
    void evictCache(long tenantId, String text);

    /** @deprecated Unscoped cache eviction is forbidden. */
    @Deprecated(since = "C13b", forRemoval = false)
    default void evictCache(String text) {
        throw new EmbeddingException("Tenant scope is required");
    }

    /**
     * 清除所有嵌入缓存
     */
    void clearCache(long tenantId);

    /** @deprecated Global business cache clearing is forbidden. */
    @Deprecated(since = "C13b", forRemoval = false)
    default void clearAllCache() {
        throw new IllegalStateException("Global embedding cache clear is disabled");
    }
}

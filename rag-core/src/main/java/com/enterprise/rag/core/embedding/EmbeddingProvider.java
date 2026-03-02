package com.enterprise.rag.core.embedding;

import java.util.List;

/**
 * 嵌入提供者接口
 * 定义向量嵌入模型的标准接口，支持多种实现（OpenAI、通义、BGE本地模型）
 * 
 * @see OpenAIEmbeddingProvider
 * @see QwenEmbeddingProvider
 * @see BGEEmbeddingProvider
 */
public interface EmbeddingProvider {

    /**
     * 获取单个文本的向量嵌入
     *
     * @param text 输入文本
     * @return 向量表示（float数组）
     * @throws EmbeddingException 当嵌入生成失败时抛出
     */
    float[] getEmbedding(String text);

    /**
     * 批量获取文本的向量嵌入
     *
     * @param texts 输入文本列表
     * @return 向量表示列表
     * @throws EmbeddingException 当嵌入生成失败时抛出
     */
    default List<float[]> getEmbeddings(List<String> texts) {
        return texts.stream()
                .map(this::getEmbedding)
                .toList();
    }

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    int getDimension();

    /**
     * 获取模型名称
     *
     * @return 模型名称标识
     */
    String getModelName();

    /**
     * 检查提供者是否可用
     *
     * @return true 如果提供者可用
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 获取提供者优先级（用于降级选择）
     * 数值越小优先级越高
     *
     * @return 优先级值
     */
    default int getPriority() {
        return 100;
    }
}

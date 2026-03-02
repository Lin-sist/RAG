package com.enterprise.rag.core.rag.model;

import java.util.List;
import java.util.Map;

/**
 * 生成的答案记录类
 * 表示 LLM 生成的答案及其元数据
 *
 * @param answer    生成的答案文本
 * @param citations 引用来源列表
 * @param metadata  元数据（如模型名称、token 使用量等）
 */
public record GeneratedAnswer(
        String answer,
        List<Citation> citations,
        Map<String, Object> metadata
) {
    /**
     * 创建简单答案（不带元数据）
     */
    public static GeneratedAnswer of(String answer, List<Citation> citations) {
        return new GeneratedAnswer(answer, citations, Map.of());
    }

    /**
     * 创建带元数据的答案
     */
    public static GeneratedAnswer of(String answer, List<Citation> citations, Map<String, Object> metadata) {
        return new GeneratedAnswer(answer, citations, metadata);
    }

    /**
     * 检查答案是否为空
     */
    public boolean isEmpty() {
        return answer == null || answer.isBlank();
    }

    /**
     * 检查是否有引用
     */
    public boolean hasCitations() {
        return citations != null && !citations.isEmpty();
    }
}

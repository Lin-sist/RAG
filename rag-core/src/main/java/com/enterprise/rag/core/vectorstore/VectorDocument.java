package com.enterprise.rag.core.vectorstore;

import java.util.Map;

/**
 * 向量文档记录类
 * 表示存储到向量数据库的文档，包含向量、内容和元数据
 *
 * @param id       文档唯一标识
 * @param vector   向量表示（float数组）
 * @param content  文档文本内容
 * @param metadata 文档元数据（来源、标题、创建时间等）
 */
public record VectorDocument(
        String id,
        float[] vector,
        String content,
        Map<String, Object> metadata
) {
    /**
     * 创建不带元数据的向量文档
     */
    public VectorDocument(String id, float[] vector, String content) {
        this(id, vector, content, Map.of());
    }

    /**
     * 验证向量文档的有效性
     */
    public boolean isValid() {
        return id != null && !id.isBlank()
                && vector != null && vector.length > 0
                && content != null;
    }
}

package com.enterprise.rag.document.processor;

import com.enterprise.rag.document.chunker.DocumentChunk;

import java.util.List;

/**
 * 文档处理结果
 *
 * @param documentId  文档 ID
 * @param contentHash 内容哈希（用于幂等性检查）
 * @param rawContent  原始文本内容
 * @param chunks      分块列表
 * @param isNew       是否为新文档（非重复上传）
 */
public record ProcessResult(
        String documentId,
        String contentHash,
        String rawContent,
        List<DocumentChunk> chunks,
        boolean isNew) {
    public ProcessResult {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("Document ID cannot be null or blank");
        }
        if (contentHash == null || contentHash.isBlank()) {
            throw new IllegalArgumentException("Content hash cannot be null or blank");
        }
        if (chunks == null) {
            chunks = List.of();
        }
    }

    /**
     * 获取块数量
     */
    public int chunkCount() {
        return chunks.size();
    }

    /**
     * 创建新文档结果
     */
    public static ProcessResult newDocument(String documentId, String contentHash, String rawContent,
            List<DocumentChunk> chunks) {
        return new ProcessResult(documentId, contentHash, rawContent, chunks, true);
    }

    /**
     * 创建重复文档结果
     */
    public static ProcessResult duplicate(String documentId, String contentHash) {
        return duplicate(documentId, contentHash, "");
    }

    /**
     * 创建重复文档结果（保留解析内容，但不重复生成分块）
     */
    public static ProcessResult duplicate(String documentId, String contentHash, String rawContent) {
        return new ProcessResult(documentId, contentHash, rawContent, List.of(), false);
    }
}

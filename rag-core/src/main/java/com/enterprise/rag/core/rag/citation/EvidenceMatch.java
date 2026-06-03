package com.enterprise.rag.core.rag.citation;

/**
 * 描述一个 citation 与检索上下文之间的证据匹配情况。
 */
public record EvidenceMatch(
        String source,
        String sourceFileName,
        String documentTitle,
        Long documentId,
        String chunkId,
        Double score,
        String snippet,
        String matchType,
        double overlapRatio,
        int contextIndex) {
}

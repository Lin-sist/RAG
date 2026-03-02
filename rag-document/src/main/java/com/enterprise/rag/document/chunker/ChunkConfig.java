package com.enterprise.rag.document.chunker;

/**
 * 文档分块配置
 *
 * @param chunkSize    块大小（字符数）
 * @param chunkOverlap 块重叠大小（字符数）
 * @param strategy     分块策略
 */
public record ChunkConfig(
        int chunkSize,
        int chunkOverlap,
        ChunkStrategy strategy
) {
    /**
     * 默认配置
     */
    public static final ChunkConfig DEFAULT = new ChunkConfig(500, 50, ChunkStrategy.SEMANTIC);
    
    public ChunkConfig {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (chunkOverlap < 0) {
            throw new IllegalArgumentException("Chunk overlap must be non-negative");
        }
        if (chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Chunk overlap must be less than chunk size");
        }
    }
    
    /**
     * 创建固定大小分块配置
     */
    public static ChunkConfig fixedSize(int chunkSize, int overlap) {
        return new ChunkConfig(chunkSize, overlap, ChunkStrategy.FIXED_SIZE);
    }
    
    /**
     * 创建语义分块配置
     */
    public static ChunkConfig semantic(int chunkSize, int overlap) {
        return new ChunkConfig(chunkSize, overlap, ChunkStrategy.SEMANTIC);
    }
    
    /**
     * 创建代码分块配置
     */
    public static ChunkConfig code(int chunkSize, int overlap) {
        return new ChunkConfig(chunkSize, overlap, ChunkStrategy.CODE);
    }
}

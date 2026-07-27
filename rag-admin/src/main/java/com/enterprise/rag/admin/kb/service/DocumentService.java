package com.enterprise.rag.admin.kb.service;

import com.enterprise.rag.admin.kb.entity.Document;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;

import java.util.List;
import java.util.Optional;

/**
 * 文档服务接口
 */
public interface DocumentService {

    /**
     * 创建文档记录
     *
     * @param document 文档实体
     * @return 创建的文档
     */
    Document create(Document document);

    /**
     * 根据ID获取文档
     *
     * @param id 文档ID
     * @return 文档（如果存在）
     */
    Optional<Document> getById(Long id);

    /**
     * 在指定 tenant 边界内根据 ID 获取文档。
     */
    default Optional<Document> getById(long tenantId, Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 获取知识库的所有文档
     *
     * @param kbId 知识库ID
     * @return 文档列表
     */
    List<Document> getByKnowledgeBaseId(Long kbId);

    /**
     * 获取明确 tenant 边界内知识库的文档。
     */
    default List<Document> getByKnowledgeBaseId(long tenantId, Long kbId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 根据内容哈希查找文档
     *
     * @param contentHash 内容哈希
     * @return 文档（如果存在）
     */
    Optional<Document> getByContentHash(String contentHash);

    /**
     * 根据知识库 ID 和内容哈希查找文档
     *
     * @param kbId        知识库ID
     * @param contentHash 内容哈希
     * @return 文档（如果存在）
     */
    Optional<Document> getByKnowledgeBaseAndContentHash(Long kbId, String contentHash);

    default Optional<Document> getByKnowledgeBaseAndContentHash(
            long tenantId, Long kbId, String contentHash) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 更新文档状态
     *
     * @param id     文档ID
     * @param status 新状态
     */
    void updateStatus(Long id, String status);

    default void updateStatus(long tenantId, Long id, String status) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 更新文档分块数量
     *
     * @param id         文档ID
     * @param chunkCount 分块数量
     */
    void updateChunkCount(Long id, int chunkCount);

    default void updateChunkCount(long tenantId, Long id, int chunkCount) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 更新文档内容哈希（用于去重和幂等校验）
     *
     * @param id          文档ID
     * @param contentHash SHA-256 内容哈希
     */
    void updateContentHash(Long id, String contentHash);

    default void updateContentHash(long tenantId, Long id, String contentHash) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 更新持久化索引输入状态。
     *
     * @param id         文档ID
     * @param inputState 输入状态
     */
    void updateInputState(Long id, String inputState);

    default void updateInputState(long tenantId, Long id, String inputState) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 删除文档（级联删除分块和向量数据）
     *
     * @param id 文档ID
     * @return true 表示文档记录实际删除，false 表示文档不存在
     */
    boolean delete(Long id);

    /**
     * 在指定 tenant 边界内删除文档及其分块、索引输入和索引数据。
     */
    default boolean delete(long tenantId, Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 删除知识库的所有文档
     *
     * @param kbId 知识库ID
     */
    void deleteByKnowledgeBaseId(Long kbId);

    /**
     * 删除指定 tenant 内知识库的所有文档。
     */
    default void deleteByKnowledgeBaseId(long tenantId, Long kbId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 保存文档分块
     *
     * @param chunks 分块列表
     */
    void saveChunks(List<DocumentChunk> chunks);

    /**
     * 获取文档的所有分块
     *
     * @param documentId 文档ID
     * @return 分块列表
     */
    List<DocumentChunk> getChunksByDocumentId(Long documentId);

    default List<DocumentChunk> getChunksByDocumentId(long tenantId, Long documentId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 获取文档分块的向量ID列表
     *
     * @param documentId 文档ID
     * @return 向量ID列表
     */
    List<String> getVectorIdsByDocumentId(Long documentId);

    /**
     * 统计知识库的文档数量
     *
     * @param kbId 知识库ID
     * @return 文档数量
     */
    int countByKnowledgeBaseId(Long kbId);

    /**
     * 在指定租户内统计知识库的文档数量。
     *
     * @param tenantId 租户ID
     * @param kbId     知识库ID
     * @return 文档数量
     */
    int countByKnowledgeBaseId(long tenantId, Long kbId);
}

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
     * 获取知识库的所有文档
     *
     * @param kbId 知识库ID
     * @return 文档列表
     */
    List<Document> getByKnowledgeBaseId(Long kbId);

    /**
     * 根据内容哈希查找文档
     *
     * @param contentHash 内容哈希
     * @return 文档（如果存在）
     */
    Optional<Document> getByContentHash(String contentHash);

    /**
     * 更新文档状态
     *
     * @param id     文档ID
     * @param status 新状态
     */
    void updateStatus(Long id, String status);

    /**
     * 更新文档分块数量
     *
     * @param id         文档ID
     * @param chunkCount 分块数量
     */
    void updateChunkCount(Long id, int chunkCount);

    /**
     * 删除文档（级联删除分块和向量数据）
     *
     * @param id 文档ID
     */
    void delete(Long id);

    /**
     * 删除知识库的所有文档
     *
     * @param kbId 知识库ID
     */
    void deleteByKnowledgeBaseId(Long kbId);

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
}

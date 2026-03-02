package com.enterprise.rag.admin.kb.service;

import com.enterprise.rag.admin.kb.dto.CreateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseStatistics;
import com.enterprise.rag.admin.kb.dto.UpdateKnowledgeBaseRequest;

import java.util.List;
import java.util.Optional;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     *
     * @param request 创建请求
     * @param ownerId 所有者ID
     * @return 创建的知识库
     */
    KnowledgeBaseDTO create(CreateKnowledgeBaseRequest request, Long ownerId);

    /**
     * 根据ID获取知识库
     *
     * @param id 知识库ID
     * @return 知识库（如果存在）
     */
    Optional<KnowledgeBaseDTO> getById(Long id);

    /**
     * 获取用户的所有知识库
     *
     * @param userId 用户ID
     * @return 知识库列表
     */
    List<KnowledgeBaseDTO> getByOwnerId(Long userId);

    /**
     * 获取用户可访问的所有知识库（包括公开的和有权限的）
     *
     * @param userId 用户ID
     * @return 知识库列表
     */
    List<KnowledgeBaseDTO> getAccessibleByUserId(Long userId);

    /**
     * 更新知识库
     *
     * @param id      知识库ID
     * @param request 更新请求
     * @return 更新后的知识库
     */
    KnowledgeBaseDTO update(Long id, UpdateKnowledgeBaseRequest request);

    /**
     * 删除知识库（级联删除文档和向量数据）
     *
     * @param id 知识库ID
     */
    void delete(Long id);

    /**
     * 检查知识库是否存在
     *
     * @param id 知识库ID
     * @return true 如果存在
     */
    boolean exists(Long id);

    /**
     * 获取知识库统计信息
     *
     * @param id 知识库ID
     * @return 统计信息
     */
    KnowledgeBaseStatistics getStatistics(Long id);

    /**
     * 增加文档计数
     *
     * @param id    知识库ID
     * @param delta 增量（可为负数）
     */
    void updateDocumentCount(Long id, int delta);
}

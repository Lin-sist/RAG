package com.enterprise.rag.admin.kb.service;

import com.enterprise.rag.admin.kb.dto.CreateKnowledgeBaseRequest;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseDTO;
import com.enterprise.rag.admin.kb.dto.KnowledgeBaseStatistics;
import com.enterprise.rag.admin.kb.dto.UpdateKnowledgeBaseRequest;
import com.enterprise.rag.admin.security.RequestIdentity;
import com.enterprise.rag.core.vectorstore.TenantVectorScope;

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
     * @param identity 服务端认证请求身份
     * @return 创建的知识库
     */
    KnowledgeBaseDTO create(CreateKnowledgeBaseRequest request, RequestIdentity identity);

    /**
     * 根据ID获取知识库
     *
     * @param id 知识库ID
     * @return 知识库（如果存在）
     */
    Optional<KnowledgeBaseDTO> getById(Long id);

    /**
     * 供 durable/system 执行链路在明确 tenant 边界内读取知识库。
     */
    default Optional<KnowledgeBaseDTO> getById(long tenantId, Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 根据服务端请求身份在当前 tenant 内获取知识库。
     *
     * @param id       知识库ID
     * @param identity 服务端认证请求身份
     * @return 当前 tenant 内的知识库（如果存在）
     */
    Optional<KnowledgeBaseDTO> getById(Long id, RequestIdentity identity);

    /** Resolve the SQL-owned active mapping and reject non-READY vector state. */
    default TenantVectorScope requireReadyVectorScope(long tenantId, Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    default TenantVectorScope requireReadyVectorScope(Long id, RequestIdentity identity) {
        return requireReadyVectorScope(identity.tenantId(), id);
    }

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
     * 获取当前认证 tenant 内用户可访问的知识库。
     */
    default List<KnowledgeBaseDTO> getAccessibleByIdentity(RequestIdentity identity) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 更新知识库
     *
     * @param id      知识库ID
     * @param request 更新请求
     * @return 更新后的知识库
     */
    KnowledgeBaseDTO update(Long id, UpdateKnowledgeBaseRequest request);

    default KnowledgeBaseDTO update(Long id, UpdateKnowledgeBaseRequest request, RequestIdentity identity) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 删除知识库（级联删除文档和向量数据）
     *
     * @param id 知识库ID
     */
    void delete(Long id);

    /**
     * 在服务端认证 tenant 边界内删除知识库。
     */
    default void delete(Long id, RequestIdentity identity) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

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
     * 在服务端认证 tenant 边界内获取统计信息。
     */
    default KnowledgeBaseStatistics getStatistics(Long id, RequestIdentity identity) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 增加文档计数
     *
     * @param id    知识库ID
     * @param delta 增量（可为负数）
     */
    void updateDocumentCount(Long id, int delta);

    /**
     * 在指定 tenant 边界内更新知识库文档计数。
     */
    default void updateDocumentCount(long tenantId, Long id, int delta) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 增加知识库查询次数
     *
     * @param id 知识库ID
     */
    void incrementQueryCount(Long id);

    default void incrementQueryCount(long tenantId, Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }
}

package com.enterprise.rag.admin.qa.service;

import com.enterprise.rag.admin.qa.dto.PageResult;
import com.enterprise.rag.admin.qa.dto.QAHistoryDTO;
import com.enterprise.rag.admin.qa.dto.QAHistoryPageRequest;
import com.enterprise.rag.admin.qa.dto.SaveQAHistoryRequest;
import com.enterprise.rag.admin.security.RequestIdentity;

import java.util.Optional;

/**
 * 问答历史服务接口
 */
public interface QAHistoryService {

    /**
     * 保存问答历史
     *
     * @param request 保存请求
     * @return 保存后的历史记录
     */
    QAHistoryDTO save(SaveQAHistoryRequest request);

    default QAHistoryDTO save(RequestIdentity identity, SaveQAHistoryRequest request) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 根据ID获取历史记录
     *
     * @param id 历史记录ID
     * @return 历史记录（可选）
     */
    Optional<QAHistoryDTO> getById(Long id);

    default Optional<QAHistoryDTO> getById(RequestIdentity identity, Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 分页查询历史记录
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    PageResult<QAHistoryDTO> getPage(QAHistoryPageRequest request);

    default PageResult<QAHistoryDTO> getPage(RequestIdentity identity, QAHistoryPageRequest request) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 根据用户ID统计历史记录数量
     *
     * @param userId 用户ID
     * @return 记录数量
     */
    long countByUserId(Long userId);

    default long countByUserId(long tenantId, Long userId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 根据知识库ID统计历史记录数量
     *
     * @param kbId 知识库ID
     * @return 记录数量
     */
    long countByKbId(Long kbId);

    default long countByKbId(long tenantId, Long kbId) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 删除历史记录
     *
     * @param id 历史记录ID
     */
    void delete(Long id);

    default void delete(RequestIdentity identity, Long id) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }

    /**
     * 根据用户ID删除所有历史记录
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);

    default void deleteByUserId(RequestIdentity identity) {
        throw new IllegalStateException("TENANT_IDENTITY_REQUIRED");
    }
}

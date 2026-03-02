package com.enterprise.rag.admin.qa.service;

import com.enterprise.rag.admin.qa.dto.PageResult;
import com.enterprise.rag.admin.qa.dto.QAHistoryDTO;
import com.enterprise.rag.admin.qa.dto.QAHistoryPageRequest;
import com.enterprise.rag.admin.qa.dto.SaveQAHistoryRequest;

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

    /**
     * 根据ID获取历史记录
     *
     * @param id 历史记录ID
     * @return 历史记录（可选）
     */
    Optional<QAHistoryDTO> getById(Long id);

    /**
     * 分页查询历史记录
     *
     * @param request 分页查询请求
     * @return 分页结果
     */
    PageResult<QAHistoryDTO> getPage(QAHistoryPageRequest request);

    /**
     * 根据用户ID统计历史记录数量
     *
     * @param userId 用户ID
     * @return 记录数量
     */
    long countByUserId(Long userId);

    /**
     * 根据知识库ID统计历史记录数量
     *
     * @param kbId 知识库ID
     * @return 记录数量
     */
    long countByKbId(Long kbId);

    /**
     * 删除历史记录
     *
     * @param id 历史记录ID
     */
    void delete(Long id);

    /**
     * 根据用户ID删除所有历史记录
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}

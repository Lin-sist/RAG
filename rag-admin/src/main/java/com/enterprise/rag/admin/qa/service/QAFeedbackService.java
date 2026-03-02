package com.enterprise.rag.admin.qa.service;

import com.enterprise.rag.admin.qa.dto.QAFeedbackDTO;
import com.enterprise.rag.admin.qa.dto.SubmitFeedbackRequest;

import java.util.List;
import java.util.Optional;

/**
 * 问答反馈服务接口
 */
public interface QAFeedbackService {

    /**
     * 提交反馈
     *
     * @param request 反馈请求
     * @return 保存后的反馈记录
     */
    QAFeedbackDTO submit(SubmitFeedbackRequest request);

    /**
     * 根据ID获取反馈
     *
     * @param id 反馈ID
     * @return 反馈记录（可选）
     */
    Optional<QAFeedbackDTO> getById(Long id);

    /**
     * 根据问答历史ID获取反馈
     *
     * @param qaId 问答历史ID
     * @return 反馈记录（可选）
     */
    Optional<QAFeedbackDTO> getByQaId(Long qaId);

    /**
     * 根据问答历史ID获取所有反馈
     *
     * @param qaId 问答历史ID
     * @return 反馈列表
     */
    List<QAFeedbackDTO> listByQaId(Long qaId);

    /**
     * 根据用户ID获取所有反馈
     *
     * @param userId 用户ID
     * @return 反馈列表
     */
    List<QAFeedbackDTO> listByUserId(Long userId);

    /**
     * 检查用户是否已对某问答提交反馈
     *
     * @param qaId   问答历史ID
     * @param userId 用户ID
     * @return 是否已提交
     */
    boolean hasUserFeedback(Long qaId, Long userId);

    /**
     * 删除反馈
     *
     * @param id 反馈ID
     */
    void delete(Long id);

    /**
     * 根据问答历史ID删除所有反馈
     *
     * @param qaId 问答历史ID
     */
    void deleteByQaId(Long qaId);
}

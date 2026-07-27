package com.enterprise.rag.admin.qa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.qa.entity.QAFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 问答反馈 Mapper 接口
 */
@Mapper
public interface QAFeedbackMapper extends BaseMapper<QAFeedback> {

    @Select("""
            SELECT COUNT(*) > 0
              FROM qa_history
             WHERE tenant_id = #{tenantId}
               AND id = #{qaId}
               AND user_id = #{userId}
               AND deleted = 0
            """)
    boolean historyOwnedByTenantAndUser(@Param("tenantId") long tenantId,
            @Param("qaId") long qaId,
            @Param("userId") long userId);

    @Select("""
            SELECT COUNT(*)
              FROM qa_feedback
             WHERE tenant_id = #{tenantId}
               AND qa_id = #{qaId}
               AND user_id = #{userId}
               AND deleted = 0
            """)
    long countByTenantQaAndUser(@Param("tenantId") long tenantId,
            @Param("qaId") long qaId,
            @Param("userId") long userId);

    @Select("""
            SELECT *
              FROM qa_feedback
             WHERE tenant_id = #{tenantId}
               AND id = #{feedbackId}
               AND user_id = #{userId}
               AND deleted = 0
             LIMIT 1
            """)
    QAFeedback selectByTenantUserAndId(@Param("tenantId") long tenantId,
            @Param("userId") long userId,
            @Param("feedbackId") long feedbackId);

    @Select("""
            SELECT *
              FROM qa_feedback
             WHERE tenant_id = #{tenantId}
               AND qa_id = #{qaId}
               AND user_id = #{userId}
               AND deleted = 0
             ORDER BY created_at DESC, id DESC
             LIMIT 1
            """)
    QAFeedback selectLatestByTenantQaAndUser(@Param("tenantId") long tenantId,
            @Param("qaId") long qaId,
            @Param("userId") long userId);

    @Select("""
            SELECT *
              FROM qa_feedback
             WHERE tenant_id = #{tenantId}
               AND qa_id = #{qaId}
               AND user_id = #{userId}
               AND deleted = 0
             ORDER BY created_at DESC, id DESC
            """)
    List<QAFeedback> selectByTenantQaAndUser(@Param("tenantId") long tenantId,
            @Param("qaId") long qaId,
            @Param("userId") long userId);

    @Select("""
            SELECT *
              FROM qa_feedback
             WHERE tenant_id = #{tenantId}
               AND user_id = #{userId}
               AND deleted = 0
             ORDER BY created_at DESC, id DESC
            """)
    List<QAFeedback> selectByTenantAndUser(@Param("tenantId") long tenantId,
            @Param("userId") long userId);

    @Update("""
            UPDATE qa_feedback
               SET deleted = 1,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND id = #{feedbackId}
               AND user_id = #{userId}
               AND deleted = 0
            """)
    int deleteByTenantUserAndId(@Param("tenantId") long tenantId,
            @Param("userId") long userId,
            @Param("feedbackId") long feedbackId);

    @Update("""
            UPDATE qa_feedback
               SET deleted = 1,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND qa_id = #{qaId}
               AND user_id = #{userId}
               AND deleted = 0
            """)
    int deleteByTenantQaAndUser(@Param("tenantId") long tenantId,
            @Param("qaId") long qaId,
            @Param("userId") long userId);
}

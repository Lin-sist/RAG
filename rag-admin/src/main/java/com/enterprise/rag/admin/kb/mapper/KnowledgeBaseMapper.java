package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 知识库 Mapper 接口
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    @Select("""
            SELECT *
              FROM knowledge_base
             WHERE tenant_id = #{tenantId}
               AND id = #{kbId}
               AND deleted = 0
             LIMIT 1
            """)
    KnowledgeBase selectByTenantAndId(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId);

    @Update("""
            UPDATE knowledge_base
               SET name = #{name},
                   description = #{description},
                   is_public = #{isPublic},
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND id = #{kbId}
               AND deleted = 0
            """)
    int updateMutableFieldsByTenantAndId(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("name") String name,
            @Param("description") String description,
            @Param("isPublic") Boolean isPublic);

    @Update("""
            UPDATE knowledge_base
               SET document_count = document_count + #{delta},
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND id = #{kbId}
               AND deleted = 0
            """)
    int updateDocumentCountByTenantAndId(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("delta") int delta);

    @Update("""
            UPDATE knowledge_base
               SET deleted = 1,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND id = #{kbId}
               AND deleted = 0
            """)
    int deleteByTenantAndId(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId);

    @Update("""
            UPDATE knowledge_base
               SET document_count = document_count + 1,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE id = #{kbId}
               AND tenant_id = #{tenantId}
               AND deleted = 0
            """)
    int incrementDocumentCount(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId);
}

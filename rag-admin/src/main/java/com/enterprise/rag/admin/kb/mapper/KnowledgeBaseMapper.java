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

    @Update("""
            UPDATE knowledge_base
               SET vector_source_collection = vector_collection,
                   vector_shadow_collection = #{shadowCollection},
                   vector_readiness = 'SHADOW_COPYING',
                   vector_expected_count = #{expectedCount},
                   vector_observed_count = 0,
                   vector_migrated_count = 0,
                   vector_missing_count = 0,
                   vector_mismatch_count = 0,
                   vector_last_error_category = NULL,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND id = #{kbId}
               AND deleted = 0
               AND vector_collection = #{sourceCollection}
               AND vector_readiness IN ('LEGACY_PENDING', 'AUDIT_FAILED')
            """)
    int beginVectorShadowCopy(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("sourceCollection") String sourceCollection,
            @Param("shadowCollection") String shadowCollection,
            @Param("expectedCount") long expectedCount);

    @Update("""
            UPDATE knowledge_base
               SET vector_collection = #{shadowCollection},
                   vector_readiness = 'READY',
                   vector_observed_count = #{observedCount},
                   vector_migrated_count = #{migratedCount},
                   vector_missing_count = 0,
                   vector_mismatch_count = 0,
                   vector_last_error_category = NULL,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND id = #{kbId}
               AND deleted = 0
               AND vector_collection = #{sourceCollection}
               AND vector_shadow_collection = #{shadowCollection}
               AND vector_readiness = 'SHADOW_COPYING'
            """)
    int completeVectorShadowSwitch(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("sourceCollection") String sourceCollection,
            @Param("shadowCollection") String shadowCollection,
            @Param("observedCount") long observedCount,
            @Param("migratedCount") long migratedCount);

    @Update("""
            UPDATE knowledge_base
               SET vector_readiness = 'AUDIT_FAILED',
                   vector_observed_count = #{observedCount},
                   vector_migrated_count = #{migratedCount},
                   vector_missing_count = #{missingCount},
                   vector_mismatch_count = #{mismatchCount},
                   vector_last_error_category = #{errorCategory},
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND id = #{kbId}
               AND deleted = 0
               AND vector_readiness = 'SHADOW_COPYING'
            """)
    int failVectorShadowCopy(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("observedCount") long observedCount,
            @Param("migratedCount") long migratedCount,
            @Param("missingCount") long missingCount,
            @Param("mismatchCount") long mismatchCount,
            @Param("errorCategory") String errorCategory);
}

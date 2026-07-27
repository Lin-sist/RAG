package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.KBPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 知识库权限 Mapper 接口
 */
@Mapper
public interface KBPermissionMapper extends BaseMapper<KBPermission> {

    @Select("""
            SELECT COUNT(*) > 0
              FROM knowledge_base kb
              JOIN `user` u
                ON u.tenant_id = kb.tenant_id
               AND u.id = #{userId}
               AND u.deleted = 0
             WHERE kb.tenant_id = #{tenantId}
               AND kb.id = #{kbId}
               AND kb.deleted = 0
            """)
    boolean tenantResourceAndUserExist(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("userId") long userId);

    @Select("""
            SELECT *
              FROM kb_permission
             WHERE tenant_id = #{tenantId}
               AND kb_id = #{kbId}
               AND user_id = #{userId}
               AND deleted = 0
             LIMIT 1
            """)
    KBPermission selectByTenantAndResource(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("userId") long userId);

    @Update("""
            UPDATE kb_permission
               SET permission_type = #{permissionType},
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND kb_id = #{kbId}
               AND user_id = #{userId}
               AND deleted = 0
            """)
    int updateTypeByTenantAndResource(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("userId") long userId,
            @Param("permissionType") String permissionType);

    @Select("""
            SELECT *
              FROM kb_permission
             WHERE tenant_id = #{tenantId}
               AND kb_id = #{kbId}
               AND deleted = 0
             ORDER BY id
            """)
    java.util.List<KBPermission> selectByTenantAndKnowledgeBase(
            @Param("tenantId") long tenantId,
            @Param("kbId") long kbId);

    @Update("""
            UPDATE kb_permission
               SET deleted = 1,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND kb_id = #{kbId}
               AND user_id = #{userId}
               AND deleted = 0
            """)
    int deleteByTenantAndResource(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("userId") long userId);

    @Update("""
            UPDATE kb_permission
               SET deleted = 1,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND kb_id = #{kbId}
               AND deleted = 0
            """)
    int deleteByTenantAndKnowledgeBase(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId);

    @Select("""
            SELECT permission_type
              FROM kb_permission
             WHERE tenant_id = #{tenantId}
               AND kb_id = #{kbId}
               AND user_id = #{userId}
               AND deleted = 0
             LIMIT 1
            """)
    String findPermissionTypeByTenantAndResource(
            @Param("tenantId") long tenantId,
            @Param("kbId") long kbId,
            @Param("userId") long userId);
}

package com.enterprise.rag.admin.session.mapper;

import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SessionContextMapper {
    @Select("""
            SELECT u.id AS user_id, u.username, u.email,
                   t.id AS workspace_id, t.name AS display_name
            FROM `user` u
            INNER JOIN tenant t ON t.id = u.tenant_id
            WHERE u.id = #{userId} AND u.tenant_id = #{tenantId}
              AND t.id = #{tenantId}
              AND u.enabled = 1 AND u.deleted = 0
              AND t.enabled = 1 AND t.deleted = 0
            """)
    ContextRow findCurrent(@Param("userId") long userId, @Param("tenantId") long tenantId);

    @Data
    class ContextRow {
        private Long userId;
        private String username;
        private String email;
        private Long workspaceId;
        private String displayName;
    }
}

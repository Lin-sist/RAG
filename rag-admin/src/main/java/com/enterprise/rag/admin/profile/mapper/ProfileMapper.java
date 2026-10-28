package com.enterprise.rag.admin.profile.mapper;

import com.enterprise.rag.admin.profile.ProfileInput;
import lombok.Data;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProfileMapper {
    String SCOPE = " FROM `user` u INNER JOIN tenant t ON t.id=u.tenant_id "
            + "WHERE u.id=#{userId} AND u.tenant_id=#{tenantId} AND t.id=#{tenantId} "
            + "AND u.enabled=1 AND u.deleted=0 AND t.enabled=1 AND t.deleted=0";

    @Select("SELECT u.id,u.username,u.display_name,u.email,(u.avatar_data IS NOT NULL) AS has_avatar,u.version AS revision" + SCOPE)
    Row current(@Param("userId") long userId, @Param("tenantId") long tenantId);

    @Select("SELECT u.avatar_data AS data" + SCOPE)
    AvatarRow avatar(@Param("userId") long userId, @Param("tenantId") long tenantId);

    @Update("""
        UPDATE `user` SET display_name=#{input.displayName},email=#{input.email},
          avatar_data=CASE #{input.avatarAction} WHEN 'REMOVE' THEN NULL WHEN 'REPLACE' THEN #{avatar.data,jdbcType=BLOB} ELSE avatar_data END,
          version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id=#{userId} AND tenant_id=#{tenantId} AND version=#{input.expectedRevision}
          AND enabled=1 AND deleted=0
          AND EXISTS(SELECT 1 FROM tenant t WHERE t.id=#{tenantId} AND t.enabled=1 AND t.deleted=0)
        """)
    int save(@Param("userId") long userId, @Param("tenantId") long tenantId,
             @Param("input") ProfileInput input, @Param("avatar") AvatarRow avatar);

    @Data
    class AvatarRow { private byte[] data; }

    @Data
    class Row {
        private Long id;
        private String username;
        private String displayName;
        private String email;
        private boolean hasAvatar;
        private int revision;
    }
}

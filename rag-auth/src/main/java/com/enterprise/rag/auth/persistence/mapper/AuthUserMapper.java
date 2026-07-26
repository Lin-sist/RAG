package com.enterprise.rag.auth.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.auth.persistence.entity.AuthUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {

    @Select("""
            SELECT u.id, u.tenant_id, u.username, u.password_hash, u.email, u.enabled,
                   u.created_at, u.updated_at, u.deleted, u.version
            FROM `user` u
            INNER JOIN tenant t
                    ON t.id = u.tenant_id
                   AND t.enabled = 1
                   AND t.deleted = 0
            WHERE u.username = #{username} AND u.deleted = 0
            LIMIT 1
            """)
    AuthUser findByUsername(@Param("username") String username);

    @Select("""
            SELECT id, tenant_id, username, password_hash, email, enabled, created_at, updated_at, deleted, version
            FROM `user`
            WHERE username = #{username}
            LIMIT 1
            """)
    AuthUser findAnyByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM `user` WHERE deleted = 0")
    long countNonDeletedUsers();

    @Update("""
            UPDATE `user`
            SET password_hash = #{passwordHash},
                email = COALESCE(#{email}, email),
                enabled = 1,
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND password_hash = '{c2-known-seed-quarantined}'
              AND deleted = 0
            """)
    int claimQuarantinedSeed(
            @Param("id") Long id,
            @Param("passwordHash") String passwordHash,
            @Param("email") String email);
}

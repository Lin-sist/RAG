package com.enterprise.rag.auth.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuthRoleMapper {

    @Select("""
            SELECT r.name
            FROM role r
            JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            ORDER BY r.name
            """)
    List<String> findRoleNamesByUserId(@Param("userId") Long userId);

    @Select("SELECT id FROM role WHERE name = #{name} AND deleted = 0 LIMIT 1")
    Long findRoleIdByName(@Param("name") String name);
}

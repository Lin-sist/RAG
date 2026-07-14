package com.enterprise.rag.auth.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthUserRoleMapper {

    @Select("SELECT COUNT(*) FROM user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    long countAssignment(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Insert("INSERT INTO user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertAssignment(@Param("userId") Long userId, @Param("roleId") Long roleId);
}

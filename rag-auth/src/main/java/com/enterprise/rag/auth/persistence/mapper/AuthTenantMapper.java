package com.enterprise.rag.auth.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuthTenantMapper {

    @Select("""
            SELECT id
            FROM tenant
            WHERE code = 'legacy-default'
              AND enabled = 1
              AND deleted = 0
            LIMIT 1
            """)
    Long findLegacyTenantId();
}

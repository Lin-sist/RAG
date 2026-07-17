package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档 Mapper 接口
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    @Select("""
            SELECT * FROM document
             WHERE deleted = 0
               AND input_state = 'CLEANUP_PENDING'
             ORDER BY updated_at, id
             LIMIT #{limit}
            """)
    List<Document> findCleanupPending(@Param("limit") int limit);
}

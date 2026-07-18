package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 知识库 Mapper 接口
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    @Update("""
            UPDATE knowledge_base
               SET document_count = document_count + 1,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE id = #{kbId}
               AND deleted = 0
            """)
    int incrementDocumentCount(@Param("kbId") long kbId);
}

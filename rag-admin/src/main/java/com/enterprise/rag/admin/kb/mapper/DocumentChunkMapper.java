package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文档分块 Mapper 接口
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    @Select("""
            SELECT COUNT(*)
              FROM document_chunk
             WHERE document_id = #{documentId}
               AND deleted = 0
            """)
    int countActiveByDocumentId(@Param("documentId") long documentId);

    @Insert("""
            INSERT INTO document_chunk
                (document_id, vector_id, content, chunk_index, start_pos, end_pos, metadata)
            VALUES
                (#{chunk.documentId}, #{chunk.vectorId}, #{chunk.content}, #{chunk.chunkIndex},
                 #{chunk.startPos}, #{chunk.endPos}, #{chunk.metadata})
            """)
    int insertFinalizationChunk(@Param("chunk") DocumentChunk chunk);
}

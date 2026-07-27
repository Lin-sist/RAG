package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文档分块 Mapper 接口
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    @Select("""
            SELECT *
              FROM document_chunk
             WHERE tenant_id = #{tenantId}
               AND document_id = #{documentId}
               AND deleted = 0
             ORDER BY chunk_index
            """)
    List<DocumentChunk> selectByTenantAndDocumentId(@Param("tenantId") long tenantId,
            @Param("documentId") long documentId);

    @Update("""
            UPDATE document_chunk
               SET deleted = 1,
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND document_id = #{documentId}
               AND deleted = 0
            """)
    int deleteByTenantAndDocumentId(@Param("tenantId") long tenantId,
            @Param("documentId") long documentId);

    @Select("""
            SELECT COUNT(*)
              FROM document_chunk
             WHERE tenant_id = #{tenantId}
               AND document_id = #{documentId}
               AND deleted = 0
            """)
    int countActiveByTenantAndDocumentId(@Param("tenantId") long tenantId,
            @Param("documentId") long documentId);

    @Select("""
            SELECT dc.*
              FROM document_chunk dc
              JOIN document d
                ON d.id = dc.document_id
               AND d.tenant_id = dc.tenant_id
               AND d.deleted = 0
             WHERE dc.tenant_id = #{tenantId}
               AND d.kb_id = #{kbId}
               AND dc.deleted = 0
             ORDER BY dc.document_id, dc.chunk_index
            """)
    List<DocumentChunk> selectByTenantAndKnowledgeBaseId(@Param("tenantId") long tenantId,
            @Param("kbId") long kbId);

    @Insert("""
            INSERT INTO document_chunk
                (tenant_id, document_id, vector_id, content, chunk_index, start_pos, end_pos, metadata)
            VALUES
                (#{chunk.tenantId}, #{chunk.documentId}, #{chunk.vectorId}, #{chunk.content}, #{chunk.chunkIndex},
                 #{chunk.startPos}, #{chunk.endPos}, #{chunk.metadata})
            """)
    int insertFinalizationChunk(@Param("chunk") DocumentChunk chunk);
}

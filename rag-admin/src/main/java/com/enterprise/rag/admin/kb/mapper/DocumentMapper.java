package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Select("""
            SELECT d.*
              FROM document d
             WHERE d.deleted = 0
               AND d.status IN ('PENDING', 'FAILED')
               AND d.input_state = 'AVAILABLE'
               AND NOT EXISTS (
                   SELECT 1
                     FROM async_task t
                    WHERE t.deleted = 0
                      AND t.task_type = 'DOCUMENT_INDEX'
                      AND t.document_id = d.id
               )
             ORDER BY d.updated_at, d.id
             LIMIT #{limit}
            """)
    List<Document> findLegacyUnledgered(@Param("limit") int limit);

    @Update("""
            UPDATE document d
               SET d.status = 'RECONCILIATION_REQUIRED',
                   d.updated_at = CURRENT_TIMESTAMP
             WHERE d.id = #{documentId}
               AND d.deleted = 0
               AND d.status IN ('PENDING', 'FAILED')
               AND d.input_state = 'AVAILABLE'
               AND NOT EXISTS (
                   SELECT 1
                     FROM async_task t
                    WHERE t.deleted = 0
                      AND t.task_type = 'DOCUMENT_INDEX'
                      AND t.document_id = d.id
               )
            """)
    int quarantineLegacyUnledgered(@Param("documentId") long documentId);

    @Select("""
            SELECT *
              FROM document
             WHERE id = #{documentId}
               AND deleted = 0
             FOR UPDATE
            """)
    Document lockByIdForUpdate(@Param("documentId") long documentId);

    @Update("""
            UPDATE document
               SET content_hash = #{contentHash},
                   chunk_count = #{chunkCount},
                   status = 'COMPLETED',
                   updated_at = CURRENT_TIMESTAMP,
                   version = version + 1
             WHERE id = #{documentId}
               AND deleted = 0
               AND status <> 'COMPLETED'
            """)
    int finalizeIndexDocument(@Param("documentId") long documentId,
            @Param("contentHash") String contentHash,
            @Param("chunkCount") int chunkCount);
}

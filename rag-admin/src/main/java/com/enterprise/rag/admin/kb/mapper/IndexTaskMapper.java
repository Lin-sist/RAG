package com.enterprise.rag.admin.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.rag.admin.kb.task.IndexTaskRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface IndexTaskMapper extends BaseMapper<IndexTaskRecord> {

    @Select("""
            SELECT *
              FROM async_task
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND task_type = 'DOCUMENT_INDEX'
               AND deleted = 0
             FOR UPDATE
            """)
    IndexTaskRecord lockByTenantAndTaskIdForUpdate(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId);

    @Update("""
            UPDATE async_task
               SET lease_owner = #{workerId},
                   lease_until = DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL #{leaseSeconds} SECOND),
                   heartbeat_at = CURRENT_TIMESTAMP(6),
                   attempt_count = attempt_count + 1,
                   version = version + 1,
                   updated_at = CURRENT_TIMESTAMP(6)
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND task_type = 'DOCUMENT_INDEX'
               AND deleted = 0
               AND status IN ('ACCEPTED', 'RUNNING')
               AND execution_phase IN ('ACCEPTED', 'SAFE_PRE_VECTOR', 'VECTOR_IN_FLIGHT',
                                       'VECTOR_CONFIRMED', 'FINALIZING')
               AND attempt_count < #{maxAttempts}
               AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP(6))
               AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP(6))
            """)
    int claim(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId,
            @Param("workerId") String workerId,
            @Param("leaseSeconds") int leaseSeconds,
            @Param("maxAttempts") int maxAttempts);

    @Select("""
            SELECT *
              FROM async_task
             WHERE task_type = 'DOCUMENT_INDEX'
               AND deleted = 0
               AND status IN ('ACCEPTED', 'RUNNING')
               AND execution_phase IN ('ACCEPTED', 'SAFE_PRE_VECTOR', 'VECTOR_IN_FLIGHT',
                                       'VECTOR_CONFIRMED', 'FINALIZING')
               AND attempt_count < #{maxAttempts}
               AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP(6))
               AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP(6))
             ORDER BY created_at, id
             LIMIT #{limit}
            """)
    List<IndexTaskRecord> scanClaimable(@Param("limit") int limit,
            @Param("maxAttempts") int maxAttempts);

    @Update("""
            UPDATE async_task
               SET lease_owner = NULL,
                   lease_until = NULL,
                   updated_at = CURRENT_TIMESTAMP(6),
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND lease_owner = #{workerId}
               AND deleted = 0
            """)
    int release(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId,
            @Param("workerId") String workerId);

    @Update("""
            UPDATE async_task
               SET lease_until = DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL #{leaseSeconds} SECOND),
                   heartbeat_at = CURRENT_TIMESTAMP(6),
                   updated_at = CURRENT_TIMESTAMP(6),
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND lease_owner = #{workerId}
               AND lease_until >= CURRENT_TIMESTAMP(6)
               AND deleted = 0
            """)
    int heartbeat(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId,
            @Param("workerId") String workerId,
            @Param("leaseSeconds") int leaseSeconds);

    @Update("""
            UPDATE async_task
               SET status = 'RECONCILIATION_REQUIRED',
                   failure_code = #{failureCode},
                   lease_owner = NULL,
                   lease_until = NULL,
                   next_attempt_at = NULL,
                   updated_at = CURRENT_TIMESTAMP(6),
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND task_type = 'DOCUMENT_INDEX'
               AND deleted = 0
               AND status IN ('ACCEPTED', 'RUNNING')
               AND execution_phase IN ('ACCEPTED', 'SAFE_PRE_VECTOR', 'VECTOR_IN_FLIGHT',
                                       'VECTOR_CONFIRMED', 'FINALIZING')
            """)
    int markReconciliationRequired(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId,
            @Param("failureCode") String failureCode);

    @Update("""
            UPDATE async_task
               SET status = 'FAILED',
                   execution_phase = 'TERMINAL',
                   failure_code = #{failureCode},
                   error_message = NULL,
                   lease_owner = NULL,
                   lease_until = NULL,
                   next_attempt_at = NULL,
                   updated_at = CURRENT_TIMESTAMP(6),
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND task_type = 'DOCUMENT_INDEX'
               AND deleted = 0
               AND status IN ('ACCEPTED', 'RUNNING')
               AND lease_owner = #{workerId}
            """)
    int markAttemptsExhausted(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId,
            @Param("workerId") String workerId,
            @Param("failureCode") String failureCode);

    @Update("""
            UPDATE async_task
               SET failure_code = #{failureCode},
                   error_message = NULL,
                   next_attempt_at = DATE_ADD(
                       CURRENT_TIMESTAMP(6), INTERVAL #{backoffSeconds} SECOND),
                   lease_owner = NULL,
                   lease_until = NULL,
                   updated_at = CURRENT_TIMESTAMP(6),
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND task_type = 'DOCUMENT_INDEX'
               AND deleted = 0
               AND status IN ('ACCEPTED', 'RUNNING')
               AND lease_owner = #{workerId}
            """)
    int scheduleRetry(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId,
            @Param("workerId") String workerId,
            @Param("failureCode") String failureCode,
            @Param("backoffSeconds") int backoffSeconds);

    @Update("""
            UPDATE async_task
               SET status = 'COMPLETED',
                   execution_phase = 'TERMINAL',
                   progress = 100,
                   failure_code = NULL,
                   error_message = NULL,
                   lease_owner = NULL,
                   lease_until = NULL,
                   next_attempt_at = NULL,
                   updated_at = CURRENT_TIMESTAMP(6),
                   version = version + 1
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND task_type = 'DOCUMENT_INDEX'
               AND deleted = 0
               AND status IN ('ACCEPTED', 'RUNNING')
               AND execution_phase = 'FINALIZING'
            """)
    int completeFinalization(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId);

    @Select("""
            SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END
              FROM async_task
             WHERE tenant_id = #{tenantId}
               AND task_id = #{taskId}
               AND task_type = 'DOCUMENT_INDEX'
               AND deleted = 0
               AND status = 'COMPLETED'
               AND execution_phase = 'TERMINAL'
            """)
    boolean isCompleted(@Param("tenantId") long tenantId,
            @Param("taskId") String taskId);
}

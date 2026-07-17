package com.enterprise.rag.admin.kb.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.rag.admin.kb.mapper.IndexTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.enterprise.rag.document.chunker.DocumentChunkingProperties;

import java.util.UUID;
import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MySqlIndexTaskLedger implements IndexTaskLedger {

    static final String DOCUMENT_INDEX_TASK_TYPE = "DOCUMENT_INDEX";

    private final IndexTaskMapper mapper;
    private final DocumentChunkingProperties chunkingProperties;

    @Override
    public String createAccepted(Long documentId, Long ownerId) {
        IndexTaskRecord record = new IndexTaskRecord();
        record.setTaskId(UUID.randomUUID().toString());
        record.setTaskType(DOCUMENT_INDEX_TASK_TYPE);
        record.setStatus(IndexTaskStatus.ACCEPTED.name());
        record.setProgress(0);
        record.setDocumentId(documentId);
        record.setOwnerId(ownerId);
        record.setExecutionPhase(IndexTaskPhase.ACCEPTED.name());
        record.setAttemptCount(0);
        record.setIndexContractVersion(DeterministicChunkIdentity.CONTRACT_VERSION);
        record.setChunkSize(chunkingProperties.getChunkSize());
        record.setChunkOverlap(chunkingProperties.getChunkOverlap());
        mapper.insert(record);
        return record.getTaskId();
    }

    @Override
    public void markAcceptanceFailed(String taskId, String failureCode) {
        int updated = mapper.update(null, Wrappers.<IndexTaskRecord>lambdaUpdate()
                .eq(IndexTaskRecord::getTaskId, taskId)
                .eq(IndexTaskRecord::getStatus, IndexTaskStatus.ACCEPTED.name())
                .set(IndexTaskRecord::getStatus, IndexTaskStatus.FAILED.name())
                .set(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.TERMINAL.name())
                .set(IndexTaskRecord::getFailureCode, failureCode));
        if (updated != 1) {
            throw new IllegalStateException("Accepted index task was not transitioned: " + taskId);
        }
    }

    @Override
    public void markSafePreVector(String taskId) {
        int updated = mapper.update(null, Wrappers.<IndexTaskRecord>lambdaUpdate()
                .eq(IndexTaskRecord::getTaskId, taskId)
                .in(IndexTaskRecord::getExecutionPhase,
                        IndexTaskPhase.ACCEPTED.name(), IndexTaskPhase.SAFE_PRE_VECTOR.name())
                .set(IndexTaskRecord::getStatus, IndexTaskStatus.RUNNING.name())
                .set(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.SAFE_PRE_VECTOR.name()));
        requireSingleUpdate(updated, "Index task cannot enter safe pre-vector phase: ", taskId);
    }

    @Override
    public void markVectorInFlight(String taskId, String contentHash, int chunkCount) {
        int updated = mapper.update(null, Wrappers.<IndexTaskRecord>lambdaUpdate()
                .eq(IndexTaskRecord::getTaskId, taskId)
                .in(IndexTaskRecord::getStatus,
                        IndexTaskStatus.ACCEPTED.name(), IndexTaskStatus.RUNNING.name())
                .set(IndexTaskRecord::getStatus, IndexTaskStatus.RUNNING.name())
                .set(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.VECTOR_IN_FLIGHT.name())
                .set(IndexTaskRecord::getPreparedContentHash, contentHash)
                .set(IndexTaskRecord::getPreparedChunkCount, chunkCount)
                .setSql("vector_started_at = CURRENT_TIMESTAMP(6)"));
        if (updated != 1) {
            throw new IllegalStateException("Index task cannot enter vector in-flight: " + taskId);
        }
    }

    @Override
    public void markVectorConfirmed(String taskId) {
        int updated = mapper.update(null, Wrappers.<IndexTaskRecord>lambdaUpdate()
                .eq(IndexTaskRecord::getTaskId, taskId)
                .eq(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.VECTOR_IN_FLIGHT.name())
                .set(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.VECTOR_CONFIRMED.name())
                .setSql("vector_confirmed_at = CURRENT_TIMESTAMP(6)"));
        requireSingleUpdate(updated, "Index task vector confirmation was not persisted: ", taskId);
    }

    @Override
    public void markFinalizing(String taskId) {
        int updated = mapper.update(null, Wrappers.<IndexTaskRecord>lambdaUpdate()
                .eq(IndexTaskRecord::getTaskId, taskId)
                .in(IndexTaskRecord::getExecutionPhase,
                        IndexTaskPhase.VECTOR_CONFIRMED.name(), IndexTaskPhase.FINALIZING.name())
                .set(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.FINALIZING.name()));
        requireSingleUpdate(updated, "Index task cannot enter finalizing: ", taskId);
    }

    @Override
    public void markCompleted(String taskId) {
        int updated = mapper.update(null, Wrappers.<IndexTaskRecord>lambdaUpdate()
                .eq(IndexTaskRecord::getTaskId, taskId)
                .eq(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.FINALIZING.name())
                .set(IndexTaskRecord::getStatus, IndexTaskStatus.COMPLETED.name())
                .set(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.TERMINAL.name())
                .set(IndexTaskRecord::getProgress, 100)
                .set(IndexTaskRecord::getLeaseOwner, null)
                .set(IndexTaskRecord::getLeaseUntil, null));
        requireSingleUpdate(updated, "Index task completion was not persisted: ", taskId);
    }

    @Override
    public void markReconciliationRequired(String taskId, String failureCode) {
        int updated = mapper.update(null, Wrappers.<IndexTaskRecord>lambdaUpdate()
                .eq(IndexTaskRecord::getTaskId, taskId)
                .eq(IndexTaskRecord::getExecutionPhase, IndexTaskPhase.VECTOR_IN_FLIGHT.name())
                .set(IndexTaskRecord::getStatus, IndexTaskStatus.RECONCILIATION_REQUIRED.name())
                .set(IndexTaskRecord::getFailureCode, failureCode)
                .set(IndexTaskRecord::getLeaseOwner, null)
                .set(IndexTaskRecord::getLeaseUntil, null));
        if (updated != 1) {
            throw new IllegalStateException("Index task was not quarantined: " + taskId);
        }
    }

    @Override
    public boolean claim(String taskId, String workerId, int leaseSeconds, int maxAttempts) {
        return mapper.claim(taskId, workerId, leaseSeconds, maxAttempts) == 1;
    }

    @Override
    public Optional<IndexTaskRecord> find(String taskId) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<IndexTaskRecord>lambdaQuery()
                .eq(IndexTaskRecord::getTaskId, taskId)));
    }

    @Override
    public List<IndexTaskRecord> scanClaimable(int limit) {
        return mapper.scanClaimable(limit);
    }

    @Override
    public boolean release(String taskId, String workerId) {
        return mapper.release(taskId, workerId) == 1;
    }

    @Override
    public boolean heartbeat(String taskId, String workerId, int leaseSeconds) {
        return mapper.heartbeat(taskId, workerId, leaseSeconds) == 1;
    }

    private void requireSingleUpdate(int updated, String message, String taskId) {
        if (updated != 1) {
            throw new IllegalStateException(message + taskId);
        }
    }
}

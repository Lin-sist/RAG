package com.enterprise.rag.admin.kb.task;

import com.baomidou.mybatisplus.annotation.TableName;
import com.enterprise.rag.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * MySQL 中的 durable index-task ledger；不保存文档正文、分块正文或向量。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("async_task")
public class IndexTaskRecord extends BaseEntity {

    private String taskId;
    private String taskType;
    private String status;
    private Integer progress;
    private Long documentId;
    private Long ownerId;
    private String executionPhase;
    private Integer attemptCount;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private LocalDateTime heartbeatAt;
    private LocalDateTime nextAttemptAt;
    private String failureCode;
    private String indexContractVersion;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private String preparedContentHash;
    private Integer preparedChunkCount;
    private LocalDateTime vectorStartedAt;
    private LocalDateTime vectorConfirmedAt;
    private String errorMessage;
}

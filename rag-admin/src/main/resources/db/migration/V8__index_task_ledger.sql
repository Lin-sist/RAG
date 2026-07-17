-- C5b: extend the existing async_task table into a durable index-task ledger.
-- Input bytes remain in IndexInputStore; this table stores only orchestration facts.
ALTER TABLE `async_task`
    MODIFY COLUMN `status` VARCHAR(32) DEFAULT 'PENDING',
    ADD COLUMN `document_id` BIGINT NULL AFTER `task_type`,
    ADD COLUMN `owner_id` BIGINT NULL AFTER `document_id`,
    ADD COLUMN `execution_phase` VARCHAR(32) NULL AFTER `status`,
    ADD COLUMN `attempt_count` INT NOT NULL DEFAULT 0 AFTER `progress`,
    ADD COLUMN `lease_owner` VARCHAR(128) NULL AFTER `attempt_count`,
    ADD COLUMN `lease_until` DATETIME(6) NULL AFTER `lease_owner`,
    ADD COLUMN `heartbeat_at` DATETIME(6) NULL AFTER `lease_until`,
    ADD COLUMN `next_attempt_at` DATETIME(6) NULL AFTER `heartbeat_at`,
    ADD COLUMN `failure_code` VARCHAR(64) NULL AFTER `next_attempt_at`,
    ADD COLUMN `index_contract_version` VARCHAR(32) NULL AFTER `failure_code`,
    ADD COLUMN `chunk_size` INT NULL AFTER `index_contract_version`,
    ADD COLUMN `chunk_overlap` INT NULL AFTER `chunk_size`,
    ADD COLUMN `prepared_content_hash` CHAR(64) NULL AFTER `chunk_overlap`,
    ADD COLUMN `prepared_chunk_count` INT NULL AFTER `prepared_content_hash`,
    ADD COLUMN `vector_started_at` DATETIME(6) NULL AFTER `prepared_chunk_count`,
    ADD COLUMN `vector_confirmed_at` DATETIME(6) NULL AFTER `vector_started_at`,
    ADD KEY `idx_async_task_document` (`document_id`),
    ADD KEY `idx_async_task_recovery` (`task_type`, `status`, `execution_phase`, `next_attempt_at`),
    ADD KEY `idx_async_task_lease` (`lease_until`);

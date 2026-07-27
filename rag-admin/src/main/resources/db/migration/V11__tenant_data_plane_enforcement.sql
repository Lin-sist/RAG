-- C13b: persist tenant identity on every business data-plane row.
-- Columns are nullable only during deterministic parent-based backfill.
-- MySQL commits DDL implicitly, so reject known orphan/mismatch rows before
-- adding permanent columns. The named CHECK is the stable Flyway error gate.
CREATE TEMPORARY TABLE `c13b_tenant_integrity_guard` (
    `root_tenant_missing` TINYINT NOT NULL DEFAULT 0,
    `kb_owner_mismatch` TINYINT NOT NULL DEFAULT 0,
    `document_parent_mismatch` TINYINT NOT NULL DEFAULT 0,
    `chunk_parent_missing` TINYINT NOT NULL DEFAULT 0,
    `permission_parent_mismatch` TINYINT NOT NULL DEFAULT 0,
    `history_parent_mismatch` TINYINT NOT NULL DEFAULT 0,
    `feedback_parent_mismatch` TINYINT NOT NULL DEFAULT 0,
    `task_parent_mismatch` TINYINT NOT NULL DEFAULT 0,
    `backfill_incomplete` TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT `chk_c13b_root_tenant` CHECK (`root_tenant_missing` = 0),
    CONSTRAINT `chk_c13b_kb_owner` CHECK (`kb_owner_mismatch` = 0),
    CONSTRAINT `chk_c13b_document_parent` CHECK (`document_parent_mismatch` = 0),
    CONSTRAINT `chk_c13b_chunk_parent` CHECK (`chunk_parent_missing` = 0),
    CONSTRAINT `chk_c13b_permission_parent` CHECK (`permission_parent_mismatch` = 0),
    CONSTRAINT `chk_c13b_history_parent` CHECK (`history_parent_mismatch` = 0),
    CONSTRAINT `chk_c13b_feedback_parent` CHECK (`feedback_parent_mismatch` = 0),
    CONSTRAINT `chk_c13b_task_parent` CHECK (`task_parent_mismatch` = 0),
    CONSTRAINT `chk_c13b_backfill_complete` CHECK (`backfill_incomplete` = 0)
);

INSERT INTO `c13b_tenant_integrity_guard` (`root_tenant_missing`)
SELECT 1
FROM (
    SELECT user_row.tenant_id
    FROM `user` user_row
    LEFT JOIN `tenant` tenant_row ON tenant_row.id = user_row.tenant_id
    WHERE tenant_row.id IS NULL
    UNION ALL
    SELECT knowledge_base_row.tenant_id
    FROM `knowledge_base` knowledge_base_row
    LEFT JOIN `tenant` tenant_row ON tenant_row.id = knowledge_base_row.tenant_id
    WHERE tenant_row.id IS NULL
) missing_root
LIMIT 1;

INSERT INTO `c13b_tenant_integrity_guard` (`kb_owner_mismatch`)
SELECT 1
FROM `knowledge_base` knowledge_base_row
LEFT JOIN `user` owner_row ON owner_row.id = knowledge_base_row.owner_id
WHERE owner_row.id IS NULL
   OR knowledge_base_row.tenant_id <> owner_row.tenant_id
LIMIT 1;

INSERT INTO `c13b_tenant_integrity_guard` (`document_parent_mismatch`)
SELECT 1
FROM `document` document_row
LEFT JOIN `knowledge_base` knowledge_base_row ON knowledge_base_row.id = document_row.kb_id
LEFT JOIN `user` uploader_row ON uploader_row.id = document_row.uploader_id
WHERE knowledge_base_row.id IS NULL
   OR uploader_row.id IS NULL
   OR knowledge_base_row.tenant_id <> uploader_row.tenant_id
LIMIT 1;

INSERT INTO `c13b_tenant_integrity_guard` (`chunk_parent_missing`)
SELECT 1
FROM `document_chunk` chunk_row
LEFT JOIN `document` document_row ON document_row.id = chunk_row.document_id
WHERE document_row.id IS NULL
LIMIT 1;

INSERT INTO `c13b_tenant_integrity_guard` (`permission_parent_mismatch`)
SELECT 1
FROM `kb_permission` permission_row
LEFT JOIN `knowledge_base` knowledge_base_row ON knowledge_base_row.id = permission_row.kb_id
LEFT JOIN `user` user_row ON user_row.id = permission_row.user_id
WHERE knowledge_base_row.id IS NULL
   OR user_row.id IS NULL
   OR knowledge_base_row.tenant_id <> user_row.tenant_id
LIMIT 1;

INSERT INTO `c13b_tenant_integrity_guard` (`history_parent_mismatch`)
SELECT 1
FROM `qa_history` history_row
LEFT JOIN `user` user_row ON user_row.id = history_row.user_id
LEFT JOIN `knowledge_base` knowledge_base_row ON knowledge_base_row.id = history_row.kb_id
WHERE user_row.id IS NULL
   OR (history_row.kb_id IS NOT NULL AND (
        knowledge_base_row.id IS NULL
        OR knowledge_base_row.tenant_id <> user_row.tenant_id
   ))
LIMIT 1;

INSERT INTO `c13b_tenant_integrity_guard` (`feedback_parent_mismatch`)
SELECT 1
FROM `qa_feedback` feedback_row
LEFT JOIN `qa_history` history_row ON history_row.id = feedback_row.qa_id
LEFT JOIN `user` history_user_row ON history_user_row.id = history_row.user_id
LEFT JOIN `knowledge_base` knowledge_base_row ON knowledge_base_row.id = history_row.kb_id
LEFT JOIN `user` feedback_user_row ON feedback_user_row.id = feedback_row.user_id
WHERE history_row.id IS NULL
   OR history_user_row.id IS NULL
   OR feedback_user_row.id IS NULL
   OR (history_row.kb_id IS NULL
       AND history_user_row.tenant_id <> feedback_user_row.tenant_id)
   OR (history_row.kb_id IS NOT NULL AND (
        knowledge_base_row.id IS NULL
        OR knowledge_base_row.tenant_id <> history_user_row.tenant_id
        OR knowledge_base_row.tenant_id <> feedback_user_row.tenant_id
   ))
LIMIT 1;

INSERT INTO `c13b_tenant_integrity_guard` (`task_parent_mismatch`)
SELECT 1
FROM `async_task` task_row
LEFT JOIN `document` document_row ON document_row.id = task_row.document_id
LEFT JOIN `knowledge_base` knowledge_base_row ON knowledge_base_row.id = document_row.kb_id
LEFT JOIN `user` owner_row ON owner_row.id = task_row.owner_id
WHERE (task_row.document_id IS NULL AND (
          task_row.owner_id IS NULL
          OR owner_row.id IS NULL
      ))
   OR (task_row.document_id IS NOT NULL AND (
          document_row.id IS NULL
          OR knowledge_base_row.id IS NULL
          OR (task_row.owner_id IS NOT NULL AND (
                 owner_row.id IS NULL
                 OR owner_row.tenant_id <> knowledge_base_row.tenant_id
             ))
      ))
LIMIT 1;

ALTER TABLE `document`
    ADD COLUMN `tenant_id` BIGINT NULL AFTER `id`;

ALTER TABLE `document_chunk`
    ADD COLUMN `tenant_id` BIGINT NULL AFTER `id`;

ALTER TABLE `kb_permission`
    ADD COLUMN `tenant_id` BIGINT NULL AFTER `id`;

ALTER TABLE `qa_history`
    ADD COLUMN `tenant_id` BIGINT NULL AFTER `id`;

ALTER TABLE `qa_feedback`
    ADD COLUMN `tenant_id` BIGINT NULL AFTER `id`;

ALTER TABLE `async_task`
    ADD COLUMN `tenant_id` BIGINT NULL AFTER `id`;

UPDATE `document` document_row
JOIN `knowledge_base` knowledge_base_row
  ON knowledge_base_row.id = document_row.kb_id
SET document_row.tenant_id = knowledge_base_row.tenant_id;

UPDATE `document_chunk` chunk_row
JOIN `document` document_row
  ON document_row.id = chunk_row.document_id
SET chunk_row.tenant_id = document_row.tenant_id;

UPDATE `kb_permission` permission_row
JOIN `knowledge_base` knowledge_base_row
  ON knowledge_base_row.id = permission_row.kb_id
JOIN `user` user_row
  ON user_row.id = permission_row.user_id
SET permission_row.tenant_id = CASE
    WHEN knowledge_base_row.tenant_id = user_row.tenant_id THEN knowledge_base_row.tenant_id
    ELSE NULL
END;

UPDATE `qa_history` history_row
JOIN `user` user_row
  ON user_row.id = history_row.user_id
LEFT JOIN `knowledge_base` knowledge_base_row
  ON knowledge_base_row.id = history_row.kb_id
SET history_row.tenant_id = CASE
    WHEN history_row.kb_id IS NULL THEN user_row.tenant_id
    WHEN knowledge_base_row.tenant_id = user_row.tenant_id THEN knowledge_base_row.tenant_id
    ELSE NULL
END;

UPDATE `qa_feedback` feedback_row
JOIN `qa_history` history_row
  ON history_row.id = feedback_row.qa_id
JOIN `user` user_row
  ON user_row.id = feedback_row.user_id
SET feedback_row.tenant_id = CASE
    WHEN history_row.tenant_id = user_row.tenant_id THEN history_row.tenant_id
    ELSE NULL
END;

UPDATE `async_task` task_row
LEFT JOIN `document` document_row
  ON document_row.id = task_row.document_id
LEFT JOIN `user` user_row
  ON user_row.id = task_row.owner_id
SET task_row.tenant_id = CASE
    WHEN task_row.document_id IS NOT NULL
         AND task_row.owner_id IS NOT NULL
         AND document_row.tenant_id = user_row.tenant_id THEN document_row.tenant_id
    WHEN task_row.document_id IS NOT NULL
         AND task_row.owner_id IS NULL THEN document_row.tenant_id
    WHEN task_row.document_id IS NULL
         AND task_row.owner_id IS NOT NULL THEN user_row.tenant_id
    ELSE NULL
END;

INSERT INTO `c13b_tenant_integrity_guard` (`backfill_incomplete`)
SELECT 1
WHERE EXISTS (SELECT 1 FROM `document` WHERE tenant_id IS NULL)
   OR EXISTS (SELECT 1 FROM `document_chunk` WHERE tenant_id IS NULL)
   OR EXISTS (SELECT 1 FROM `kb_permission` WHERE tenant_id IS NULL)
   OR EXISTS (SELECT 1 FROM `qa_history` WHERE tenant_id IS NULL)
   OR EXISTS (SELECT 1 FROM `qa_feedback` WHERE tenant_id IS NULL)
   OR EXISTS (SELECT 1 FROM `async_task` WHERE tenant_id IS NULL);

DROP TEMPORARY TABLE `c13b_tenant_integrity_guard`;

-- NOT NULL conversion is the fail-closed migration gate for orphaned or mismatched rows.
ALTER TABLE `document`
    MODIFY COLUMN `tenant_id` BIGINT NOT NULL,
    DROP INDEX `uk_kb_content_hash`,
    ADD UNIQUE KEY `uk_kb_content_hash` (`tenant_id`, `kb_id`, `content_hash`),
    ADD KEY `idx_document_tenant_id` (`tenant_id`, `id`),
    ADD KEY `idx_document_tenant_kb` (`tenant_id`, `kb_id`);

ALTER TABLE `document_chunk`
    MODIFY COLUMN `tenant_id` BIGINT NOT NULL,
    DROP INDEX `uk_document_chunk_position`,
    ADD UNIQUE KEY `uk_document_chunk_position` (`tenant_id`, `document_id`, `chunk_index`),
    ADD KEY `idx_chunk_tenant_document` (`tenant_id`, `document_id`),
    ADD KEY `idx_chunk_tenant_vector` (`tenant_id`, `vector_id`);

ALTER TABLE `kb_permission`
    MODIFY COLUMN `tenant_id` BIGINT NOT NULL,
    DROP INDEX `uk_kb_user`,
    ADD UNIQUE KEY `uk_kb_user` (`tenant_id`, `kb_id`, `user_id`),
    ADD KEY `idx_kb_permission_tenant_user` (`tenant_id`, `user_id`);

ALTER TABLE `qa_history`
    MODIFY COLUMN `tenant_id` BIGINT NOT NULL,
    ADD KEY `idx_history_tenant_id` (`tenant_id`, `id`),
    ADD KEY `idx_history_tenant_user_created` (`tenant_id`, `user_id`, `created_at`),
    ADD KEY `idx_history_tenant_kb` (`tenant_id`, `kb_id`);

ALTER TABLE `qa_feedback`
    MODIFY COLUMN `tenant_id` BIGINT NOT NULL,
    DROP INDEX `uk_qa_feedback_qa_user`,
    ADD UNIQUE KEY `uk_qa_feedback_qa_user` (`tenant_id`, `qa_id`, `user_id`),
    ADD KEY `idx_feedback_tenant_qa` (`tenant_id`, `qa_id`);

ALTER TABLE `async_task`
    MODIFY COLUMN `tenant_id` BIGINT NOT NULL,
    DROP INDEX `uk_task_id`,
    DROP INDEX `idx_async_task_recovery`,
    ADD UNIQUE KEY `uk_task_id` (`tenant_id`, `task_id`),
    ADD KEY `idx_async_task_tenant_document` (`tenant_id`, `document_id`),
    ADD KEY `idx_async_task_tenant_lease` (`tenant_id`, `lease_until`),
    ADD KEY `idx_async_task_recovery`
        (`task_type`, `status`, `execution_phase`, `next_attempt_at`),
    ADD KEY `idx_async_task_tenant_recovery`
        (`tenant_id`, `task_type`, `status`, `execution_phase`, `next_attempt_at`);

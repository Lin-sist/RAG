-- C13b: persist the active vector mapping and shadow-copy audit state.
-- Existing collections predate trusted tenant/kb scalar markers and therefore
-- remain unavailable to runtime traffic until maintenance completes.
ALTER TABLE `knowledge_base`
    ADD COLUMN `vector_readiness` VARCHAR(32) NOT NULL DEFAULT 'LEGACY_PENDING'
        COMMENT 'LEGACY_PENDING/INITIALIZING/SHADOW_COPYING/AUDIT_FAILED/READY'
        AFTER `vector_collection`,
    ADD COLUMN `vector_source_collection` VARCHAR(255) NULL
        COMMENT 'immutable source during shadow migration'
        AFTER `vector_readiness`,
    ADD COLUMN `vector_shadow_collection` VARCHAR(255) NULL
        COMMENT 'candidate tenant-aware collection'
        AFTER `vector_source_collection`,
    ADD COLUMN `vector_expected_count` BIGINT NOT NULL DEFAULT 0
        AFTER `vector_shadow_collection`,
    ADD COLUMN `vector_observed_count` BIGINT NOT NULL DEFAULT 0
        AFTER `vector_expected_count`,
    ADD COLUMN `vector_migrated_count` BIGINT NOT NULL DEFAULT 0
        AFTER `vector_observed_count`,
    ADD COLUMN `vector_missing_count` BIGINT NOT NULL DEFAULT 0
        AFTER `vector_migrated_count`,
    ADD COLUMN `vector_mismatch_count` BIGINT NOT NULL DEFAULT 0
        AFTER `vector_missing_count`,
    ADD COLUMN `vector_last_error_category` VARCHAR(64) NULL
        AFTER `vector_mismatch_count`,
    ADD KEY `idx_kb_tenant_vector_readiness` (`tenant_id`, `vector_readiness`);

UPDATE `knowledge_base`
   SET `vector_source_collection` = `vector_collection`,
       `vector_readiness` = 'LEGACY_PENDING'
 WHERE `vector_collection` IS NOT NULL;

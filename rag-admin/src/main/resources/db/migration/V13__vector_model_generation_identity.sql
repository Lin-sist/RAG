-- C17: bind every active/source/shadow vector mapping to one embedding space.
-- Existing READY rows intentionally remain identity-null and therefore fail closed
-- until an explicitly authorized model rebuild completes.
ALTER TABLE `knowledge_base`
    ADD COLUMN `vector_provider_family` VARCHAR(64) NULL AFTER `vector_last_error_category`,
    ADD COLUMN `vector_model` VARCHAR(128) NULL AFTER `vector_provider_family`,
    ADD COLUMN `vector_endpoint_identity` VARCHAR(255) NULL AFTER `vector_model`,
    ADD COLUMN `vector_request_contract` VARCHAR(64) NULL AFTER `vector_endpoint_identity`,
    ADD COLUMN `vector_dimension` INT NULL AFTER `vector_request_contract`,
    ADD COLUMN `vector_generation` VARCHAR(32) NULL AFTER `vector_dimension`,
    ADD COLUMN `vector_source_provider_family` VARCHAR(64) NULL AFTER `vector_generation`,
    ADD COLUMN `vector_source_model` VARCHAR(128) NULL AFTER `vector_source_provider_family`,
    ADD COLUMN `vector_source_endpoint_identity` VARCHAR(255) NULL AFTER `vector_source_model`,
    ADD COLUMN `vector_source_request_contract` VARCHAR(64) NULL AFTER `vector_source_endpoint_identity`,
    ADD COLUMN `vector_source_dimension` INT NULL AFTER `vector_source_request_contract`,
    ADD COLUMN `vector_source_generation` VARCHAR(32) NULL AFTER `vector_source_dimension`,
    ADD COLUMN `vector_shadow_provider_family` VARCHAR(64) NULL AFTER `vector_source_generation`,
    ADD COLUMN `vector_shadow_model` VARCHAR(128) NULL AFTER `vector_shadow_provider_family`,
    ADD COLUMN `vector_shadow_endpoint_identity` VARCHAR(255) NULL AFTER `vector_shadow_model`,
    ADD COLUMN `vector_shadow_request_contract` VARCHAR(64) NULL AFTER `vector_shadow_endpoint_identity`,
    ADD COLUMN `vector_shadow_dimension` INT NULL AFTER `vector_shadow_request_contract`,
    ADD COLUMN `vector_shadow_generation` VARCHAR(32) NULL AFTER `vector_shadow_dimension`,
    ADD UNIQUE KEY `uk_kb_vector_generation` (`tenant_id`, `id`, `vector_generation`);

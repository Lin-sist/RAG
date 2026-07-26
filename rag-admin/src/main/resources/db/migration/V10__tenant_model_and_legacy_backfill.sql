-- C13a: establish a single legacy tenant without changing existing business identities.
CREATE TABLE IF NOT EXISTS `tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `code` VARCHAR(64) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) DEFAULT 0,
    `version` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `tenant` (`code`, `name`, `enabled`, `deleted`, `version`)
VALUES ('legacy-default', 'Legacy Default Tenant', 1, 0, 0);

ALTER TABLE `user`
    ADD COLUMN `tenant_id` BIGINT NULL AFTER `id`;

ALTER TABLE `knowledge_base`
    ADD COLUMN `tenant_id` BIGINT NULL AFTER `id`;

UPDATE `user`
SET `tenant_id` = (SELECT `id` FROM `tenant` WHERE `code` = 'legacy-default')
WHERE `tenant_id` IS NULL;

UPDATE `knowledge_base`
SET `tenant_id` = (SELECT `id` FROM `tenant` WHERE `code` = 'legacy-default')
WHERE `tenant_id` IS NULL;

ALTER TABLE `user`
    MODIFY COLUMN `tenant_id` BIGINT NOT NULL,
    ADD KEY `idx_user_tenant_id` (`tenant_id`);

ALTER TABLE `knowledge_base`
    MODIFY COLUMN `tenant_id` BIGINT NOT NULL,
    ADD KEY `idx_kb_tenant_id` (`tenant_id`);

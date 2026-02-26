-- Document and related tables
-- Version: 1.0.0

-- Document table
CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `kb_id` BIGINT NOT NULL,
    `uploader_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(500),
    `file_type` VARCHAR(50),
    `content_hash` VARCHAR(64),
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `chunk_count` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) DEFAULT 0,
    `version` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_content_hash` (`content_hash`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_uploader_id` (`uploader_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Document Chunk table
CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `document_id` BIGINT NOT NULL,
    `vector_id` VARCHAR(100),
    `content` TEXT NOT NULL,
    `chunk_index` INT NOT NULL,
    `start_pos` INT,
    `end_pos` INT,
    `metadata` JSON,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) DEFAULT 0,
    `version` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_vector_id` (`vector_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- QA History table
CREATE TABLE IF NOT EXISTS `qa_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `kb_id` BIGINT,
    `question` TEXT NOT NULL,
    `answer` TEXT,
    `citations` JSON,
    `trace_id` VARCHAR(64),
    `latency_ms` INT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) DEFAULT 0,
    `version` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_trace_id` (`trace_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- QA Feedback table
CREATE TABLE IF NOT EXISTS `qa_feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `qa_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `rating` INT,
    `comment` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) DEFAULT 0,
    `version` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_qa_id` (`qa_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Async Task table
CREATE TABLE IF NOT EXISTS `async_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` VARCHAR(64) NOT NULL,
    `task_type` VARCHAR(50) NOT NULL,
    `status` VARCHAR(20) DEFAULT 'PENDING',
    `progress` INT DEFAULT 0,
    `payload` JSON,
    `result` JSON,
    `error_message` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) DEFAULT 0,
    `version` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_task_type` (`task_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Knowledge Base Permission table
CREATE TABLE IF NOT EXISTS `kb_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `kb_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `permission_type` VARCHAR(20) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT(1) DEFAULT 0,
    `version` INT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_user` (`kb_id`, `user_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

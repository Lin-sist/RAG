-- C2: quarantine the exact historical admin seed without deleting its identity.
-- Do not broaden this predicate to username-only matching.
UPDATE `user`
SET `password_hash` = '{c2-known-seed-quarantined}',
    `enabled` = 0,
    `version` = `version` + 1,
    `updated_at` = CURRENT_TIMESTAMP
WHERE `username` = 'admin'
  AND `password_hash` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH'
  AND `deleted` = 0;

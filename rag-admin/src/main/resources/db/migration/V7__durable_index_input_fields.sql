ALTER TABLE document
    ADD COLUMN input_size_bytes BIGINT NULL AFTER file_path,
    ADD COLUMN input_sha256 CHAR(64) NULL AFTER input_size_bytes,
    ADD COLUMN input_state VARCHAR(32) NULL AFTER input_sha256;

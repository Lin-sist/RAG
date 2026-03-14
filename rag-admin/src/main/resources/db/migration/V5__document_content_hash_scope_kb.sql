-- DOC-08: content_hash 唯一性改为知识库内唯一，支持跨知识库复用同内容

ALTER TABLE document
    DROP INDEX uk_content_hash;

ALTER TABLE document
    ADD CONSTRAINT uk_kb_content_hash UNIQUE (kb_id, content_hash);

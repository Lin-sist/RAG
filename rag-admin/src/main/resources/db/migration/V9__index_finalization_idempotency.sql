-- C5 debt closeout: keep one durable row per document chunk position before
-- enforcing the invariant used by transactional index finalization.
ALTER TABLE document
    MODIFY COLUMN status VARCHAR(32) DEFAULT 'PENDING';

DELETE duplicate_chunk
  FROM document_chunk duplicate_chunk
  JOIN document_chunk retained_chunk
    ON retained_chunk.document_id = duplicate_chunk.document_id
   AND retained_chunk.chunk_index = duplicate_chunk.chunk_index
   AND retained_chunk.id < duplicate_chunk.id;

ALTER TABLE document_chunk
    ADD UNIQUE KEY uk_document_chunk_position (document_id, chunk_index);

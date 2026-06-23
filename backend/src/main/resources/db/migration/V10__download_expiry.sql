ALTER TABLE generation_idempotency
    ADD COLUMN download_expires_at TIMESTAMPTZ;

CREATE INDEX idx_generation_idempotency_document_id
    ON generation_idempotency (document_id)
    WHERE document_id IS NOT NULL;

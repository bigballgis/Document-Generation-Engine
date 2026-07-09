ALTER TABLE generation_idempotency
    ADD COLUMN IF NOT EXISTS resolved_release_version VARCHAR(32);

-- P7: generation idempotency store (DB slice; in-memory overlay for tests).

CREATE TABLE generation_idempotency (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(256) NOT NULL,
    template_id UUID NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_storage_key VARCHAR(512),
    document_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_key_template UNIQUE (idempotency_key, template_id)
);

CREATE INDEX idx_generation_idempotency_expires ON generation_idempotency (expires_at);

CREATE TABLE generation_async_task (
    id UUID PRIMARY KEY,
    task_external_id VARCHAR(64) UNIQUE,
    batch_external_id VARCHAR(64) NOT NULL,
    template_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    route_type VARCHAR(32) NOT NULL,
    release_version VARCHAR(32),
    request_id VARCHAR(256) NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    request_payload_json TEXT NOT NULL,
    batch_result_json TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_generation_async_task_idempotency UNIQUE (idempotency_key, template_id)
);

CREATE INDEX idx_generation_async_task_template ON generation_async_task (template_id);
CREATE INDEX idx_generation_async_task_task_external_id ON generation_async_task (task_external_id);

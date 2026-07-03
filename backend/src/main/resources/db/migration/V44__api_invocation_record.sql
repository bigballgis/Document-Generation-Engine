-- P12-API-PKG-T03: Caller-scoped invocation records (single table, all kinds).

CREATE TABLE api_invocation_record (
    id UUID PRIMARY KEY,
    invocation_external_id VARCHAR(64) NOT NULL UNIQUE,
    invocation_kind VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    environment VARCHAR(32) NOT NULL,
    template_id UUID NOT NULL,
    template_external_id VARCHAR(64) NOT NULL,
    credential_id UUID NOT NULL,
    access_account VARCHAR(64) NOT NULL,
    request_id VARCHAR(256) NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL,
    route_type VARCHAR(32),
    requested_release_version VARCHAR(32),
    resolved_release_version VARCHAR(32),
    output_format VARCHAR(16),
    output_mode VARCHAR(32),
    outcome VARCHAR(32),
    duration_ms BIGINT,
    parameters_storage TEXT NOT NULL,
    document_id VARCHAR(128),
    artifact_storage_key VARCHAR(512),
    artifact_saved BOOLEAN NOT NULL DEFAULT false,
    record_expires_at TIMESTAMPTZ NOT NULL,
    document_expires_at TIMESTAMPTZ,
    batch_external_id VARCHAR(64),
    parent_invocation_external_id VARCHAR(64),
    item_id VARCHAR(128),
    task_external_id VARCHAR(64),
    idempotency_record_id UUID,
    audit_id VARCHAR(64),
    is_batch BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_api_invocation_template_credential_created
    ON api_invocation_record (template_id, credential_id, created_at DESC);

CREATE INDEX idx_api_invocation_idempotency_lookup
    ON api_invocation_record (template_id, credential_id, idempotency_key, created_at DESC);

CREATE INDEX idx_api_invocation_request_id
    ON api_invocation_record (template_id, credential_id, request_id, created_at DESC);

CREATE INDEX idx_api_invocation_record_expires
    ON api_invocation_record (record_expires_at);

CREATE INDEX idx_api_invocation_batch
    ON api_invocation_record (batch_external_id);

CREATE INDEX idx_api_invocation_task
    ON api_invocation_record (task_external_id);

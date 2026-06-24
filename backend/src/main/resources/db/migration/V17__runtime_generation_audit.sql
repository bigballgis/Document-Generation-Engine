CREATE TABLE runtime_generation_audit_event (
    id UUID PRIMARY KEY,
    event_at TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    environment VARCHAR(32) NOT NULL,
    template_id UUID NOT NULL,
    group_code VARCHAR(64),
    credential_id UUID,
    credential_fingerprint VARCHAR(64),
    access_account VARCHAR(64),
    release_version VARCHAR(32),
    resolved_release_version VARCHAR(32),
    route_type VARCHAR(32),
    output_format VARCHAR(16),
    output_mode VARCHAR(32),
    request_id VARCHAR(256),
    idempotency_key_hash VARCHAR(128),
    idempotency_status VARCHAR(32),
    task_external_id VARCHAR(64),
    batch_external_id VARCHAR(64),
    document_id VARCHAR(128),
    outcome VARCHAR(32) NOT NULL,
    result_summary VARCHAR(512),
    error_summary VARCHAR(512),
    duration_ms BIGINT,
    audit_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64) NOT NULL
);

CREATE INDEX idx_runtime_gen_audit_event_at ON runtime_generation_audit_event (event_at DESC);
CREATE INDEX idx_runtime_gen_audit_template ON runtime_generation_audit_event (template_id, event_at DESC);

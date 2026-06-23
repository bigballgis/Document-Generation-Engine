-- P4/P5: preview records and lifecycle audit trail.

CREATE TABLE preview_record (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES template (id),
    template_version_id UUID NOT NULL REFERENCES template_version (id),
    status VARCHAR(32) NOT NULL,
    output_format VARCHAR(16) NOT NULL DEFAULT 'DOCX',
    variables_hash VARCHAR(128),
    artifact_storage_key VARCHAR(512),
    fidelity_warnings_json TEXT,
    created_by VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_preview_record_template ON preview_record (template_id, created_at DESC);

CREATE TABLE template_lifecycle_record (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES template (id),
    action VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    decision VARCHAR(32),
    comment_summary VARCHAR(2048),
    release_version VARCHAR(32),
    actor_username VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_template_lifecycle_template ON template_lifecycle_record (template_id, created_at DESC);

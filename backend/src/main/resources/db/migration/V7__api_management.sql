-- P6: API policy and credential lifecycle.

CREATE TABLE api_policy (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES template (id),
    policy_version INT NOT NULL DEFAULT 1,
    allowed_ad_groups TEXT NOT NULL,
    default_route_release_version VARCHAR(32),
    output_formats TEXT NOT NULL DEFAULT '["DOCX"]',
    output_modes TEXT NOT NULL DEFAULT '["SYNC_STREAM"]',
    batch_enabled BOOLEAN NOT NULL DEFAULT false,
    max_batch_size INT NOT NULL DEFAULT 10,
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_api_policy_template UNIQUE (template_id)
);

CREATE TABLE api_credential (
    id UUID PRIMARY KEY,
    external_id VARCHAR(128) NOT NULL,
    template_id UUID NOT NULL REFERENCES template (id),
    secret_hash VARCHAR(256) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(8) NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_api_credential_external_id UNIQUE (external_id)
);

CREATE INDEX idx_api_credential_template ON api_credential (template_id, status);

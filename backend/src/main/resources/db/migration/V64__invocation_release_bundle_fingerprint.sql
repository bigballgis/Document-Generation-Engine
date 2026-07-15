-- CE-G06: release-bundle fingerprint on invocation + regeneration metadata.

ALTER TABLE api_invocation_record
    ADD COLUMN release_bundle_snapshot_id UUID NULL,
    ADD COLUMN release_bundle_hash VARCHAR(64) NULL;

CREATE INDEX idx_api_invocation_release_bundle_snapshot
    ON api_invocation_record (release_bundle_snapshot_id)
    WHERE release_bundle_snapshot_id IS NOT NULL;

-- Independent regeneration rows (G06-C14 / Q3): never masquerade as caller runtime SUCCESS.
CREATE TABLE invocation_regeneration (
    id UUID PRIMARY KEY,
    regeneration_external_id VARCHAR(64) NOT NULL UNIQUE,
    source_invocation_external_id VARCHAR(64) NOT NULL,
    template_id UUID NOT NULL,
    release_bundle_snapshot_id UUID NOT NULL,
    release_bundle_hash VARCHAR(64) NOT NULL,
    output_format VARCHAR(16) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    error_code VARCHAR(128),
    artifact_storage_key VARCHAR(512),
    specimen BOOLEAN NOT NULL DEFAULT true,
    encryption_reapplied BOOLEAN NOT NULL DEFAULT false,
    actor_username VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_invocation_regeneration_source
    ON invocation_regeneration (source_invocation_external_id, created_at DESC);

CREATE INDEX idx_invocation_regeneration_template
    ON invocation_regeneration (template_id, created_at DESC);

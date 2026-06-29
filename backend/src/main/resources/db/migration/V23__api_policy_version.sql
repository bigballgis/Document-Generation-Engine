-- P17-T01: Immutable api_policy_version history for rollback lineage.

CREATE TABLE api_policy_version (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES template (id),
    policy_version INT NOT NULL,
    changed_areas TEXT NOT NULL,
    config_snapshot TEXT NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_api_policy_version_template_version UNIQUE (template_id, policy_version)
);

CREATE INDEX idx_api_policy_version_template ON api_policy_version (template_id, policy_version DESC);

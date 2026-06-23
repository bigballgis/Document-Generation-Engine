CREATE TABLE management_audit_event (
    id UUID PRIMARY KEY,
    event_at TIMESTAMPTZ NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    template_id UUID,
    group_code VARCHAR(64),
    credential_id UUID,
    previous_policy_version INT,
    policy_version INT,
    changed_areas JSONB NOT NULL DEFAULT '[]',
    rollback BOOLEAN NOT NULL DEFAULT FALSE,
    rollback_source_policy_version INT,
    actor_username VARCHAR(8) NOT NULL,
    actor_summary VARCHAR(256),
    credential_fingerprint VARCHAR(64),
    status_summary VARCHAR(512),
    warning_codes JSONB NOT NULL DEFAULT '[]'
);

CREATE INDEX idx_management_audit_event_template_id ON management_audit_event (template_id);
CREATE INDEX idx_management_audit_event_group_code ON management_audit_event (group_code);
CREATE INDEX idx_management_audit_event_event_at ON management_audit_event (event_at DESC);
CREATE INDEX idx_management_audit_event_event_type ON management_audit_event (event_type);

CREATE TABLE collaboration_timeout_config (
    id                              UUID PRIMARY KEY,
    scope_type                      VARCHAR(16)  NOT NULL,
    group_code                      VARCHAR(64),
    test_threshold_hours            INTEGER      NOT NULL,
    approval_threshold_hours        INTEGER      NOT NULL,
    pending_release_threshold_hours INTEGER      NOT NULL,
    remediation_threshold_hours     INTEGER      NOT NULL,
    created_at                      TIMESTAMP    NOT NULL,
    updated_at                      TIMESTAMP    NOT NULL,
    CONSTRAINT uq_collaboration_timeout_scope UNIQUE (scope_type, group_code)
);

INSERT INTO collaboration_timeout_config (
    id,
    scope_type,
    group_code,
    test_threshold_hours,
    approval_threshold_hours,
    pending_release_threshold_hours,
    remediation_threshold_hours,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000029',
    'GLOBAL',
    NULL,
    72,
    72,
    48,
    168,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

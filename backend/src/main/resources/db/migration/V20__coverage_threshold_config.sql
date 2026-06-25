CREATE TABLE coverage_threshold_config (
    id                      UUID PRIMARY KEY,
    scope_type              VARCHAR(16)  NOT NULL,
    group_code              VARCHAR(64),
    min_required_variable_pct INTEGER      NOT NULL,
    min_required_sample_pct INTEGER      NOT NULL,
    min_anchor_binding_pct  INTEGER      NOT NULL,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    CONSTRAINT uq_coverage_threshold_scope UNIQUE (scope_type, group_code)
);

INSERT INTO coverage_threshold_config (
    id,
    scope_type,
    group_code,
    min_required_variable_pct,
    min_required_sample_pct,
    min_anchor_binding_pct,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000020',
    'GLOBAL',
    NULL,
    80,
    100,
    80,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

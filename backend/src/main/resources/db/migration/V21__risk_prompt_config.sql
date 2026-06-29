CREATE TABLE risk_prompt_config (
    id                      UUID PRIMARY KEY,
    scope_type              VARCHAR(16)  NOT NULL,
    group_code              VARCHAR(64),
    reason_categories_json  TEXT         NOT NULL,
    risk_prompt_copy_json   TEXT         NOT NULL,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL,
    CONSTRAINT uq_risk_prompt_scope UNIQUE (scope_type, group_code)
);

INSERT INTO risk_prompt_config (
    id,
    scope_type,
    group_code,
    reason_categories_json,
    risk_prompt_copy_json,
    created_at,
    updated_at
) VALUES (
    '00000000-0000-4000-8000-000000000021',
    'GLOBAL',
    NULL,
    '["BINDING_ISSUE","COVERAGE_GAP","FIDELITY_WARNING","CONTRACT_CHANGE","EXCEPTION_INTERVENTION"]',
    '{"UNRESOLVED_BLOCKERS":"Review unresolved blockers before approving.","BELOW_THRESHOLD_COVERAGE":"Coverage is below the configured threshold.","PREVIEW_COMPARISON_DIFF":"Preview comparison shows differences from the final template."}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

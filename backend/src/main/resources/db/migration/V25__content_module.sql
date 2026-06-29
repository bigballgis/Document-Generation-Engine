-- P14-T01a: clause / content module header and versioned content.

CREATE TABLE content_module (
    id UUID PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    shared_group_codes_json TEXT NOT NULL DEFAULT '[]',
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_content_module_group_active ON content_module (group_code)
    WHERE deleted_at IS NULL;

CREATE TABLE content_module_version (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES content_module (id),
    semantic_version VARCHAR(32) NOT NULL,
    review_state VARCHAR(32) NOT NULL,
    lifecycle_state VARCHAR(32),
    content_structure_json TEXT NOT NULL,
    change_description VARCHAR(2048),
    rejection_reason VARCHAR(2048),
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_content_module_version UNIQUE (module_id, semantic_version)
);

CREATE INDEX idx_content_module_version_module ON content_module_version (module_id, review_state);

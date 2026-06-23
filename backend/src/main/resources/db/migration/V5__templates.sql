-- P3: template authoring — template, version, variable schema, anchor bindings.

CREATE TABLE template (
    id UUID PRIMARY KEY,
    external_id VARCHAR(128) NOT NULL,
    group_code VARCHAR(64) NOT NULL,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    master_id UUID NOT NULL REFERENCES master_document (id),
    lifecycle_status VARCHAR(32) NOT NULL,
    release_version VARCHAR(32),
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_template_external_id UNIQUE (external_id)
);

CREATE INDEX idx_template_group_status ON template (group_code, lifecycle_status)
    WHERE deleted_at IS NULL;

CREATE TABLE template_version (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES template (id),
    dev_version_number INT NOT NULL DEFAULT 1,
    release_version VARCHAR(32),
    lifecycle_status VARCHAR(32) NOT NULL,
    master_catalog_version VARCHAR(32) NOT NULL DEFAULT 'v1',
    created_by VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_template_version_dev UNIQUE (template_id, dev_version_number)
);

CREATE TABLE variable_schema (
    id UUID PRIMARY KEY,
    template_version_id UUID NOT NULL REFERENCES template_version (id) ON DELETE CASCADE,
    variable_key VARCHAR(128) NOT NULL,
    variable_type VARCHAR(32) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT false,
    default_value TEXT,
    enum_values TEXT,
    description VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_variable_schema_key UNIQUE (template_version_id, variable_key)
);

CREATE TABLE anchor_binding (
    id UUID PRIMARY KEY,
    template_version_id UUID NOT NULL REFERENCES template_version (id) ON DELETE CASCADE,
    anchor_id VARCHAR(128) NOT NULL,
    declared_content_type VARCHAR(64) NOT NULL DEFAULT 'TEXT',
    structured_content_json TEXT NOT NULL,
    validation_status VARCHAR(64) NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_anchor_binding_anchor UNIQUE (template_version_id, anchor_id)
);

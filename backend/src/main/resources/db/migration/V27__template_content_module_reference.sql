-- P14-T01c: template references to locked content module versions.

CREATE TABLE template_content_module_reference (
    id UUID PRIMARY KEY,
    template_version_id UUID NOT NULL REFERENCES template_version (id) ON DELETE CASCADE,
    reference_key VARCHAR(128) NOT NULL,
    content_module_version_id UUID NOT NULL REFERENCES content_module_version (id),
    locked_flag BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_template_content_module_ref_key UNIQUE (template_version_id, reference_key)
);

CREATE INDEX idx_template_cm_ref_module_version ON template_content_module_reference (content_module_version_id);

CREATE INDEX idx_template_cm_ref_template_version ON template_content_module_reference (template_version_id);

CREATE TABLE template_test_data_set (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES template (id),
    external_id VARCHAR(64) NOT NULL,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(512),
    variables_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_template_test_data_set_external UNIQUE (template_id, external_id)
);

CREATE INDEX idx_template_test_data_set_template ON template_test_data_set (template_id, updated_at DESC);

ALTER TABLE preview_record
    ADD COLUMN test_data_set_external_id VARCHAR(64);

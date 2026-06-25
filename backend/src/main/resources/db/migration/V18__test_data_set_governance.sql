ALTER TABLE template_test_data_set
    ADD COLUMN required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN scenario_name VARCHAR(256),
    ADD COLUMN coverage_tags_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN dataset_version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN derived_from_id UUID REFERENCES template_test_data_set (id);

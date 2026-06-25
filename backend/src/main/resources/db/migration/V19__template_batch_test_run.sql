CREATE TABLE template_batch_test_run (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES template (id),
    created_by VARCHAR(8) NOT NULL,
    total_samples INTEGER NOT NULL,
    succeeded_count INTEGER NOT NULL,
    failed_count INTEGER NOT NULL,
    warning_count INTEGER NOT NULL,
    blocker_count INTEGER NOT NULL,
    summary_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_template_batch_test_run_template ON template_batch_test_run (template_id, created_at DESC);

ALTER TABLE preview_record
    ADD COLUMN batch_test_run_id UUID REFERENCES template_batch_test_run (id);

CREATE INDEX idx_preview_record_batch_test_run ON preview_record (batch_test_run_id);

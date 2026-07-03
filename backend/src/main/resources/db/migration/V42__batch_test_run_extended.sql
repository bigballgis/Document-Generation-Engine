-- V42: Extend template_batch_test_run with SSE lifecycle, coverage, and invalidation support
ALTER TABLE template_batch_test_run
    ADD COLUMN status                  VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN completed_at            TIMESTAMP WITH TIME ZONE,
    ADD COLUMN invalidated_at          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN template_version_id     UUID,
    ADD COLUMN sample_results_json     TEXT,
    ADD COLUMN persistent_artifacts_json TEXT,
    ADD COLUMN anchor_coverage_pct     NUMERIC(5, 2),
    ADD COLUMN variable_coverage_pct   NUMERIC(5, 2),
    ADD COLUMN sample_coverage_pct     NUMERIC(5, 2),
    ADD COLUMN all_samples_succeeded   BOOLEAN,
    ADD COLUMN gate_passed             BOOLEAN,
    ADD COLUMN hidden                  BOOLEAN NOT NULL DEFAULT FALSE;

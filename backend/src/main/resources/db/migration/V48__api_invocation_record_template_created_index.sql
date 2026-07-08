-- P13-ESO-C05: Management invocation history list index (template + recency).

CREATE INDEX IF NOT EXISTS idx_api_invocation_template_created
    ON api_invocation_record (template_id, created_at DESC);

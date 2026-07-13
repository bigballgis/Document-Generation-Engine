-- LR-D1 corrective: drop mistaken per-row retention_days columns added by V47.
-- Retention windows are application config only (ADR-0048 D1-C19/C20).

ALTER TABLE management_audit_event DROP COLUMN IF EXISTS retention_days;
ALTER TABLE runtime_generation_audit_event DROP COLUMN IF EXISTS retention_days;

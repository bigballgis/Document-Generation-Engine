-- LR-D1: Audit data retention configuration.
-- Mirrors the pattern from V43 (api_policy invocation retention).

ALTER TABLE management_audit_event
    ADD COLUMN retention_days INT NOT NULL DEFAULT 90;

ALTER TABLE runtime_generation_audit_event
    ADD COLUMN retention_days INT NOT NULL DEFAULT 365;

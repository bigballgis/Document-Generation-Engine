-- P12-API-PKG-T01: Package-level invocation and document retention on api_policy.

ALTER TABLE api_policy
    ADD COLUMN save_generated_documents BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE api_policy
    ADD COLUMN invocation_record_retention_days INT NOT NULL DEFAULT 90;

ALTER TABLE api_policy
    ADD COLUMN document_retention_days INT NOT NULL DEFAULT 30;

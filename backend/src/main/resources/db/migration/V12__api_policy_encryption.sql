ALTER TABLE api_policy
    ADD COLUMN docx_encryption_enabled BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE api_policy
    ADD COLUMN pdf_encryption_enabled BOOLEAN NOT NULL DEFAULT false;

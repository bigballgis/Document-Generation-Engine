-- P14-T01b: business module code for API path resolution (OpenAPI moduleId string).

ALTER TABLE content_module
    ADD COLUMN module_code VARCHAR(128);

UPDATE content_module
SET module_code = id::text
WHERE module_code IS NULL;

ALTER TABLE content_module
    ALTER COLUMN module_code SET NOT NULL;

CREATE UNIQUE INDEX uq_content_module_code_active ON content_module (module_code)
    WHERE deleted_at IS NULL;

-- Preview runs store both DOCX and PDF artifacts for download in test workflow.

ALTER TABLE preview_record
    ADD COLUMN pdf_artifact_storage_key VARCHAR(512);

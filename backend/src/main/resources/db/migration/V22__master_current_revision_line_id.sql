-- Phase A: stable revision line identity synthesized from master_document.

ALTER TABLE master_document
    ADD COLUMN current_revision_line_id UUID;

UPDATE master_document
SET current_revision_line_id = gen_random_uuid()
WHERE current_revision_line_id IS NULL;

ALTER TABLE master_document
    ALTER COLUMN current_revision_line_id SET NOT NULL;

CREATE INDEX idx_master_document_current_revision_line
    ON master_document (current_revision_line_id)
    WHERE deleted_at IS NULL;

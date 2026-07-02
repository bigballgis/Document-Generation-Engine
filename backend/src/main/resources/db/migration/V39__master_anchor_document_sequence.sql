-- Persist document traversal order for master anchor catalogs and revision snapshots.

ALTER TABLE master_anchor
    ADD COLUMN document_sequence INTEGER NOT NULL DEFAULT 0;

ALTER TABLE master_revision_line_anchor
    ADD COLUMN document_sequence INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_master_anchor_master_document_sequence
    ON master_anchor (master_id, document_sequence);

CREATE INDEX idx_master_revision_line_anchor_line_document_sequence
    ON master_revision_line_anchor (revision_line_id, document_sequence);

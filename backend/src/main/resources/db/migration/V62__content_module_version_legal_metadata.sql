-- CE-K08: optional legal metadata on content_module_version.

ALTER TABLE content_module_version
    ADD COLUMN jurisdiction VARCHAR(128),
    ADD COLUMN effective_from TIMESTAMPTZ,
    ADD COLUMN effective_to TIMESTAMPTZ,
    ADD COLUMN legal_review_ref VARCHAR(128);

CREATE INDEX idx_content_module_version_jurisdiction
    ON content_module_version (jurisdiction);

CREATE INDEX idx_content_module_version_legal_review_ref
    ON content_module_version (legal_review_ref);

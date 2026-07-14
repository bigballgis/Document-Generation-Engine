-- CE-K01: release-bundle immutable pinning columns on template_version.
-- Pinning fields are populated by the publish flow (pinOrigin = PUBLISHED) or by the
-- ReleaseBundleBackfillService (pinOrigin = PINNED_RETROACTIVELY) post-migration. The
-- hash requires object-storage access and is therefore computed by the backfill service,
-- not in SQL.

ALTER TABLE template_version
    ADD COLUMN master_revision_id UUID NULL,
    ADD COLUMN master_file_hash VARCHAR(64) NULL,
    ADD COLUMN pin_metadata_json TEXT NULL;

-- Index delete-protected lookups: "is this revision referenced by any non-deleted
-- published-lifecycle template_version?" (CE-K01 delete-protection guard).
CREATE INDEX idx_template_version_master_revision
    ON template_version (master_revision_id)
    WHERE master_revision_id IS NOT NULL AND deleted_at IS NULL;

-- Index the backfill scan: PUBLISHED rows missing a pin (run once by the backfill service).
CREATE INDEX idx_template_version_published_unpinned
    ON template_version (lifecycle_status)
    WHERE lifecycle_status = 'PUBLISHED' AND master_revision_id IS NULL AND deleted_at IS NULL;

-- FOS-W8: optimistic lock column + unique active release stamp (PUBLISHED only so
-- supersede-keeps-release_version on STOPPED rows remains valid — FOS-W6-5).

ALTER TABLE template_version
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_template_version_active_release
    ON template_version (template_id, release_version)
    WHERE deleted_at IS NULL
      AND release_version IS NOT NULL
      AND lifecycle_status = 'PUBLISHED';

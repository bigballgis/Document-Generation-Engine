-- P2-T06 Phase B: persisted master revision lines with anchor snapshots.

CREATE TABLE master_revision_line (
    id UUID PRIMARY KEY,
    master_id UUID NOT NULL REFERENCES master_document (id) ON DELETE CASCADE,
    storage_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(256) NOT NULL,
    anchor_count INTEGER NOT NULL,
    status_snapshot VARCHAR(32) NOT NULL,
    revision_sequence INTEGER NOT NULL,
    is_current BOOLEAN NOT NULL,
    change_summary VARCHAR(2048),
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_master_revision_line_master_sequence UNIQUE (master_id, revision_sequence)
);

CREATE INDEX idx_master_revision_line_master_recency
    ON master_revision_line (master_id, is_current DESC, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_master_revision_line_one_current
    ON master_revision_line (master_id)
    WHERE is_current = TRUE AND deleted_at IS NULL;

CREATE TABLE master_revision_line_anchor (
    revision_line_id UUID NOT NULL REFERENCES master_revision_line (id) ON DELETE CASCADE,
    anchor_id VARCHAR(128) NOT NULL,
    display_label VARCHAR(256),
    PRIMARY KEY (revision_line_id, anchor_id)
);

INSERT INTO master_revision_line (
    id,
    master_id,
    storage_key,
    original_filename,
    anchor_count,
    status_snapshot,
    revision_sequence,
    is_current,
    change_summary,
    created_by,
    updated_by,
    created_at,
    updated_at
)
SELECT
    md.current_revision_line_id,
    md.id,
    md.storage_key,
    md.original_filename,
    COALESCE(anchor_counts.cnt, 0),
    md.status,
    1,
    TRUE,
    md.change_summary,
    md.created_by,
    md.updated_by,
    md.created_at,
    md.updated_at
FROM master_document md
LEFT JOIN (
    SELECT master_id, COUNT(*) AS cnt
    FROM master_anchor
    GROUP BY master_id
) anchor_counts ON anchor_counts.master_id = md.id
WHERE md.deleted_at IS NULL;

INSERT INTO master_revision_line_anchor (revision_line_id, anchor_id, display_label)
SELECT md.current_revision_line_id, ma.anchor_id, ma.display_label
FROM master_anchor ma
JOIN master_document md ON ma.master_id = md.id
WHERE md.deleted_at IS NULL;

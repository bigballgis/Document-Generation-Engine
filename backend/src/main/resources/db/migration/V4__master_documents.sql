-- P2: master documents, anchors, and review history.

CREATE TABLE master_document (
    id UUID PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    status VARCHAR(32) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(256) NOT NULL,
    change_summary VARCHAR(2048),
    created_by VARCHAR(8) NOT NULL,
    updated_by VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_master_document_group_status ON master_document (group_code, status)
    WHERE deleted_at IS NULL;

CREATE TABLE master_anchor (
    master_id UUID NOT NULL REFERENCES master_document (id),
    anchor_id VARCHAR(128) NOT NULL,
    display_label VARCHAR(256),
    PRIMARY KEY (master_id, anchor_id)
);

CREATE TABLE master_review_record (
    id UUID PRIMARY KEY,
    master_id UUID NOT NULL REFERENCES master_document (id),
    action VARCHAR(32) NOT NULL,
    decision VARCHAR(32),
    change_summary VARCHAR(2048),
    comment_summary VARCHAR(2048),
    actor_username VARCHAR(8) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_master_review_record_master ON master_review_record (master_id, created_at DESC);

-- LR-C7: per-user read markers for collaboration notification projections.
-- Does not introduce a standalone notification aggregate.

CREATE TABLE collaboration_work_item_read_marker (
    id UUID PRIMARY KEY,
    user_id VARCHAR(8) NOT NULL,
    work_item_id UUID NOT NULL,
    read_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_collaboration_work_item_read_marker_user_item UNIQUE (user_id, work_item_id),
    CONSTRAINT fk_collaboration_work_item_read_marker_item
        FOREIGN KEY (work_item_id) REFERENCES collaboration_work_item (id)
);

CREATE INDEX idx_collaboration_work_item_read_marker_user
    ON collaboration_work_item_read_marker (user_id);

CREATE INDEX idx_collaboration_work_item_read_marker_item
    ON collaboration_work_item_read_marker (work_item_id);

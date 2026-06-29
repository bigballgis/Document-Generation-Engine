-- P14-T02c: link escalation work items to their source to-do for deduplication.

ALTER TABLE collaboration_work_item
    ADD COLUMN source_work_item_id UUID NULL;

CREATE INDEX idx_collaboration_work_item_escalation_source
    ON collaboration_work_item (source_work_item_id, status)
    WHERE deleted_at IS NULL
      AND queue_type = 'ESCALATION'
      AND status = 'OPEN';

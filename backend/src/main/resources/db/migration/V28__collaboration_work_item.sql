-- P14-T02a: template collaboration work items for role-scoped in-app to-do queues.

CREATE TABLE collaboration_work_item (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    template_external_id VARCHAR(128) NOT NULL,
    template_name VARCHAR(256) NOT NULL,
    group_code VARCHAR(64) NOT NULL,
    queue_type VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitter_user_id VARCHAR(8) NOT NULL,
    summary_text VARCHAR(512) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    resolved_at TIMESTAMPTZ NULL,
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_collaboration_work_item_open_queue ON collaboration_work_item (group_code, queue_type, status)
    WHERE deleted_at IS NULL AND status = 'OPEN';

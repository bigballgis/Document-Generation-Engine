-- CE-U08: durable content-module review history (master-aligned timeline source).

CREATE TABLE content_module_review_record (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL REFERENCES content_module (id),
    version_id UUID REFERENCES content_module_version (id),
    semantic_version VARCHAR(32),
    action VARCHAR(32) NOT NULL,
    decision VARCHAR(32),
    change_summary VARCHAR(2048),
    comment_summary VARCHAR(2048),
    actor_username VARCHAR(8) NOT NULL,
    self_approval_exception BOOLEAN NOT NULL DEFAULT FALSE,
    exception_reason VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_content_module_review_record_module
    ON content_module_review_record (module_id, created_at DESC);

-- IBL-E6 / ADR-0067: CM↔CM nesting edge projection from contentModuleRef nodes.

CREATE TABLE content_module_nesting_edge (
    id UUID PRIMARY KEY,
    parent_version_id UUID NOT NULL REFERENCES content_module_version (id) ON DELETE CASCADE,
    target_module_id UUID NOT NULL REFERENCES content_module (id),
    reference_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_cm_nesting_edge_parent_target UNIQUE (parent_version_id, target_module_id)
);

CREATE INDEX idx_cm_nesting_edge_target ON content_module_nesting_edge (target_module_id);

CREATE INDEX idx_cm_nesting_edge_parent_version ON content_module_nesting_edge (parent_version_id);

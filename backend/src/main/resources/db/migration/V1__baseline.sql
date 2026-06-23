-- P0 platform baseline: UUID + UTC conventions placeholder.
-- Domain tables are introduced in later phases.

CREATE TABLE IF NOT EXISTS platform_revision (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    revision_tag VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

INSERT INTO platform_revision (revision_tag)
SELECT 'P0_BASELINE'
WHERE NOT EXISTS (SELECT 1 FROM platform_revision WHERE revision_tag = 'P0_BASELINE');

-- P13-T01: promote business_group to a first-class, locally governed object.
-- Adds dimension (BUSINESS_LINE | DEPARTMENT), enabled flag, updated_at and a
-- logical-delete marker (deleted_at). Existing seed groups are backfilled to the
-- BUSINESS_LINE dimension and left enabled. group_code + dimension are immutable
-- once created; only the display name may change (enforced in the service layer).

ALTER TABLE business_group ADD COLUMN dimension VARCHAR(32);
ALTER TABLE business_group ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE business_group ADD COLUMN updated_at TIMESTAMPTZ;
ALTER TABLE business_group ADD COLUMN deleted_at TIMESTAMPTZ NULL;

UPDATE business_group
SET dimension = 'BUSINESS_LINE',
    enabled = TRUE,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE dimension IS NULL;

ALTER TABLE business_group ALTER COLUMN dimension SET NOT NULL;
ALTER TABLE business_group ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX uq_business_group_code_dimension
    ON business_group (group_code, dimension);

-- ALGI-M1: group-scoped asset library (hard isolation v1).
-- Legacy CE-E02 rows: assign quarantine group_code (CORP if present else lexicographic first
-- business group), force DISABLED, stamp migrated_quarantine_at. Object purge + audit are
-- completed by AlgiM1QuarantineCleanupRunner (bare MinIO keys + ASSET_LIBRARY_MIGRATE_QUARANTINE).
-- Fail-closed: if library_asset rows exist but no non-deleted business_group, migration aborts
-- (cannot invent PLATFORM). Greenfield empty catalog is a no-op beyond schema.

ALTER TABLE library_asset ADD COLUMN group_code VARCHAR(64);
ALTER TABLE library_asset ADD COLUMN migrated_quarantine_at TIMESTAMPTZ;
ALTER TABLE library_asset ADD COLUMN object_purge_completed_at TIMESTAMPTZ;

-- Abort when catalog rows exist without any business group to own quarantine.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM library_asset)
       AND NOT EXISTS (SELECT 1 FROM business_group WHERE deleted_at IS NULL) THEN
        RAISE EXCEPTION
            'ALGI-M1: cannot quarantine library_asset without business groups (no PLATFORM invent)';
    END IF;
END $$;

UPDATE library_asset la
SET group_code = COALESCE(
        (SELECT bg.group_code
         FROM business_group bg
         WHERE bg.group_code = 'CORP' AND bg.deleted_at IS NULL
         ORDER BY bg.group_code
         LIMIT 1),
        (SELECT bg.group_code
         FROM business_group bg
         WHERE bg.deleted_at IS NULL
         ORDER BY bg.group_code ASC
         LIMIT 1)
    ),
    status = 'DISABLED',
    migrated_quarantine_at = (NOW() AT TIME ZONE 'UTC'),
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE la.group_code IS NULL;

ALTER TABLE library_asset ALTER COLUMN group_code SET NOT NULL;

ALTER TABLE library_asset DROP CONSTRAINT pk_library_asset;
ALTER TABLE library_asset ADD CONSTRAINT pk_library_asset PRIMARY KEY (group_code, asset_key);

CREATE INDEX idx_library_asset_group_code ON library_asset (group_code);
CREATE INDEX idx_library_asset_group_status ON library_asset (group_code, status);

-- ADR-0070 / SYS-NORM Wave 5: compress assignable management roles to six codes.
-- Remap durable rows; idempotent on re-run (ON CONFLICT + DELETE of retired codes).

-- TEMPLATE_APPROVER → GROUP_ADMIN (skip insert when already GROUP_ADMIN)
INSERT INTO management_user_role (user_id, role)
SELECT user_id, 'GROUP_ADMIN'
FROM management_user_role
WHERE role = 'TEMPLATE_APPROVER'
ON CONFLICT (user_id, role) DO NOTHING;

DELETE FROM management_user_role
WHERE role = 'TEMPLATE_APPROVER';

-- MASTER_DESIGNER and/or TEMPLATE_AUTHOR → DOCUMENT_AUTHOR (once)
INSERT INTO management_user_role (user_id, role)
SELECT DISTINCT user_id, 'DOCUMENT_AUTHOR'
FROM management_user_role
WHERE role IN ('MASTER_DESIGNER', 'TEMPLATE_AUTHOR')
ON CONFLICT (user_id, role) DO NOTHING;

DELETE FROM management_user_role
WHERE role IN ('MASTER_DESIGNER', 'TEMPLATE_AUTHOR');

-- Seed display names for remapped bootstrap accounts (ids from V2 / V14 / V34)
UPDATE management_user
SET display_name = 'Document Author',
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE id IN (
    '11111111-1111-1111-1111-111111111103',
    '11111111-1111-1111-1111-111111111105',
    '11111111-1111-1111-1111-111111111108'
)
  AND deleted_at IS NULL;

UPDATE management_user
SET display_name = 'Group Admin',
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE id = '11111111-1111-1111-1111-111111111107'
  AND deleted_at IS NULL;

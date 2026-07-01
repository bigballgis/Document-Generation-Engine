-- Extend demo authoring roles with CORP group scope for corporate-banking E2E fixtures (FOL).

INSERT INTO management_user_group_scope (user_id, group_code) VALUES
    ('11111111-1111-1111-1111-111111111103', 'CORP'),
    ('11111111-1111-1111-1111-111111111105', 'CORP'),
    ('11111111-1111-1111-1111-111111111106', 'CORP'),
    ('11111111-1111-1111-1111-111111111107', 'CORP')
ON CONFLICT DO NOTHING;

-- Wealth demo content modules (mock data only)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'g1000001-0001-4000-8000-000000000001', 'WEALTH-STATEMENT-STD', 'WEALTH', 'Wealth Statement Intro', 'Private wealth statement introductory clause', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'WEALTH-STATEMENT-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'g2000001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"This statement summarises the holdings held in custody on your behalf as at the statement date. It is provided for information purposes and does not constitute investment advice or an offer to transact."},{"type":"paragraph","text":"Foreign currency holdings are converted at the prevailing exchange rate on the statement date for display purposes only. Actual settlement amounts may differ."},{"type":"paragraph","text":"Corporate actions, pending trades, and accrued income items may not be fully reflected until settlement completes."}]}',
       'Wealth demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'WEALTH-STATEMENT-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

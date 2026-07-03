-- Credit limit demo content modules (mock data only)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'c1000001-0001-4000-8000-000000000001', 'CREDIT-LIMIT-STD', 'CORP', 'Credit Limit Standard Clause', 'Standard credit limit confirmation text', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'CREDIT-LIMIT-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c2000001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"This letter confirms the aggregate credit limits made available to you. Drawings under the facility remain subject to compliance with the financial covenants set out in the facility agreement dated 15 March 2024."},{"type":"paragraph","text":"Availability: Limits are reviewed annually and may be reduced or withdrawn if there is a material adverse change in your financial condition or if you breach any covenant."}]}',
       'Credit limit demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'CREDIT-LIMIT-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'c1000001-0001-4000-8000-000000000002', 'CREDIT-LIMIT-COV', 'CORP', 'Credit Limit Covenants', 'Financial covenant summary', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'CREDIT-LIMIT-COV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c2000001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"Financial covenants: (a) Minimum tangible net worth GBP 8,000,000 tested quarterly. (b) Maximum leverage ratio 3.0:1. (c) Minimum interest cover 2.5:1."},{"type":"paragraph","text":"Reporting: Audited annual accounts within 120 days of year end; management accounts within 45 days of each quarter end."},{"type":"paragraph","text":"Material events: You must notify us promptly of any litigation, change of control, or breach of covenant."}]}',
       'Credit limit demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'CREDIT-LIMIT-COV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

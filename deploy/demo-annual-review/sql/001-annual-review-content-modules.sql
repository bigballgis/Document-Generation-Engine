-- Annual review demo content modules (mock data only)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'f1000001-0001-4000-8000-000000000001', 'ANNUAL-REVIEW-STD', 'CORP', 'Annual Review Standard Clause', 'Annual credit review standard text', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'ANNUAL-REVIEW-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'f2000001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"Based on our review of your latest financial statements, management accounts, and industry outlook, we confirm that the facility remains available subject to continued compliance with financial covenants."},{"type":"paragraph","text":"Our relationship manager will contact you to discuss any pricing or structural adjustments ahead of the next review cycle."}]}',
       'Annual review demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'ANNUAL-REVIEW-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'f1000001-0001-4000-8000-000000000002', 'FACILITY-RENEWAL-STD', 'CORP', 'Facility Renewal Standard Clause', 'Facility renewal standard text', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'FACILITY-RENEWAL-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'f2000001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"This renewal is subject to no material adverse change in your financial condition and continued satisfaction of know-your-customer requirements."},{"type":"paragraph","text":"Please execute and return the enclosed renewal confirmation by the date stated above to avoid interruption of availability."}]}',
       'Annual review demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'FACILITY-RENEWAL-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

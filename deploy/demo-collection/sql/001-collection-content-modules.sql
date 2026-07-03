-- Collection demo content modules (mock data only)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'e1000001-0001-4000-8000-000000000001', 'COLLECTION-RATE-STD', 'RETAIL', 'Rate Change Standard Clause', 'Rate change notice regulatory text', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'COLLECTION-RATE-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'e2000001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"If you have a fixed-rate product, this notice does not apply. For variable-rate products, we will give you at least 30 days notice before any increase takes effect, in accordance with FCA CONC rules."},{"type":"paragraph","text":"You may switch to an alternative product or repay without penalty where applicable. Contact us on 0800 123 4567 if you wish to discuss your options."}]}',
       'Collection demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'COLLECTION-RATE-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'e1000001-0001-4000-8000-000000000002', 'COLLECTION-OVERDUE-STD', 'RETAIL', 'Overdue Collection Standard Clause', 'Overdue collection regulatory text', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'COLLECTION-OVERDUE-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'e2000001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"Please contact us immediately to discuss repayment arrangements. Failure to respond may result in further action including referral to a collections agency, in accordance with our collections policy and FCA guidance."},{"type":"paragraph","text":"If you are experiencing financial difficulty, free impartial advice is available from StepChange or Citizens Advice. We are committed to treating customers fairly throughout the collections process."}]}',
       'Collection demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'COLLECTION-OVERDUE-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

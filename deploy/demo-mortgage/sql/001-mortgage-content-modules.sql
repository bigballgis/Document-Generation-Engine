-- Mortgage demo content modules (mock data only)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'b1000001-0001-4000-8000-000000000001', 'MORTGAGE-STD-TERMS', 'RETAIL', 'Standard Mortgage Terms', 'Mortgage approval standard terms clause', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'MORTGAGE-STD-TERMS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'b2000001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"Standard Conditions: (1) You must maintain buildings insurance for the full reinstatement value. (2) The property must remain your main residence unless we agree otherwise in writing. (3) You must not let the property without our prior written consent."},{"type":"paragraph","text":"Early Repayment: If you repay all or part of the loan before the end of any fixed-rate period, an early repayment charge may apply as set out in your mortgage offer illustration."},{"type":"paragraph","text":"Arrears: If you fail to make a payment when due, we will contact you promptly. Persistent arrears may affect your credit rating and could lead to possession proceedings in accordance with FCA rules."},{"type":"paragraph","text":"Complaints: If you are dissatisfied with our service, please contact Meridian Home Finance Customer Relations, PO Box 4400, Manchester M1 4HQ, or call 0800 123 4567."}]}',
       'Mortgage demo import', '10000003', '10000007'
FROM content_module cm
WHERE cm.module_code = 'MORTGAGE-STD-TERMS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

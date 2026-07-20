-- Wave B demo content modules — bank-grade operative clauses
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'fd100001-0001-4000-8000-000000000001', 'FORMAL-DEMAND-OPS', 'CORP', 'Formal Demand Operative Terms', 'Payment demand, deadline and cure mechanics', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'FORMAL-DEMAND-OPS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'fd200001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Formal demand: Pursuant to the Facility Agreement, the Bank hereby demands immediate payment of the Sums Demanded set out in Schedule 1, being amounts due and payable arising from the Event(s) of Default described in this letter."}, {"type": "paragraph", "text": "Payment deadline: Payment must be received in cleared funds in the currency of the Sums Demanded no later than the Payment Deadline, by transfer to the account nominated by the Agent in writing (or as previously notified under the Facility Agreement)."}, {"type": "paragraph", "text": "Partial payments: Any partial payment will be applied in accordance with the Facility Agreement. Acceptance of a partial payment does not constitute a waiver of the remainder of the Sums Demanded or of any Event of Default."}, {"type": "paragraph", "text": "Without prejudice: This demand is made without prejudice to any other right or remedy of the Bank or any Lender under the Finance Documents or at law, including the right to accelerate, enforce Security and claim damages."}]}',
       'Wave B formal demand operative import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'FORMAL-DEMAND-OPS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Formal demand: Pursuant to the Facility Agreement, the Bank hereby demands immediate payment of the Sums Demanded set out in Schedule 1, being amounts due and payable arising from the Event(s) of Default described in this letter."}, {"type": "paragraph", "text": "Payment deadline: Payment must be received in cleared funds in the currency of the Sums Demanded no later than the Payment Deadline, by transfer to the account nominated by the Agent in writing (or as previously notified under the Facility Agreement)."}, {"type": "paragraph", "text": "Partial payments: Any partial payment will be applied in accordance with the Facility Agreement. Acceptance of a partial payment does not constitute a waiver of the remainder of the Sums Demanded or of any Event of Default."}, {"type": "paragraph", "text": "Without prejudice: This demand is made without prejudice to any other right or remedy of the Bank or any Lender under the Finance Documents or at law, including the right to accelerate, enforce Security and claim damages."}]}',
    change_description = 'Wave B formal demand operative import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'FORMAL-DEMAND-OPS' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'fd100001-0001-4000-8000-000000000002', 'FORMAL-DEMAND-RSV', 'CORP', 'Formal Demand Reservation Clause', 'Acceleration and enforcement reservation', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'FORMAL-DEMAND-RSV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'fd200001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Acceleration: If the Sums Demanded are not paid in full by the Payment Deadline, the Bank may, by further notice, cancel any undrawn Commitment and declare all or any part of the Loans, together with accrued interest and other amounts, immediately due and payable."}, {"type": "paragraph", "text": "Enforcement: The Bank reserves the right to enforce any Security and to exercise any guarantee, set-off or other remedy available under the Finance Documents or at law, without further demand except where required by law."}, {"type": "paragraph", "text": "No waiver: Failure to exercise, or any delay in exercising, any right under this letter or the Finance Documents does not operate as a waiver. Any waiver must be in writing and signed by the Bank."}, {"type": "paragraph", "text": "Distinction: This letter is a formal demand under a corporate facility. It is not a retail overdue collection notice and does not replace or amend DEMO-OVERDUE-COLLECTION."}]}',
       'Wave B formal demand reservation import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'FORMAL-DEMAND-RSV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Acceleration: If the Sums Demanded are not paid in full by the Payment Deadline, the Bank may, by further notice, cancel any undrawn Commitment and declare all or any part of the Loans, together with accrued interest and other amounts, immediately due and payable."}, {"type": "paragraph", "text": "Enforcement: The Bank reserves the right to enforce any Security and to exercise any guarantee, set-off or other remedy available under the Finance Documents or at law, without further demand except where required by law."}, {"type": "paragraph", "text": "No waiver: Failure to exercise, or any delay in exercising, any right under this letter or the Finance Documents does not operate as a waiver. Any waiver must be in writing and signed by the Bank."}, {"type": "paragraph", "text": "Distinction: This letter is a formal demand under a corporate facility. It is not a retail overdue collection notice and does not replace or amend DEMO-OVERDUE-COLLECTION."}]}',
    change_description = 'Wave B formal demand reservation import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'FORMAL-DEMAND-RSV' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

COMMIT;

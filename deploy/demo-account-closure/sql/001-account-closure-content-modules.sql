-- Wave B demo content modules — bank-grade operative clauses
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'ac100001-0001-4000-8000-000000000001', 'ACCT-CLOSE-OPS', 'RETAIL', 'Account Closure Operative Terms', 'Closure effectiveness, payment instructions and interest', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'ACCT-CLOSE-OPS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'ac200001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Closure confirmation: Meridian Retail Banking confirms that the Account identified in this notice will be closed with effect from the Closure Effective Date, subject to clearance of any pending transactions initiated before that date."}, {"type": "paragraph", "text": "Payment instructions and standing orders: All standing orders, direct debits and recurring payment mandates linked to the Account will cease on the Closure Effective Date. You remain responsible for arranging alternative payment methods with payees before closure."}, {"type": "paragraph", "text": "Cards and access: Debit cards, online banking access credentials and cheque books relating solely to the Account will be cancelled on the Closure Effective Date. Please destroy physical cards and unused cheques securely."}, {"type": "paragraph", "text": "Interest and charges: Accrued interest (if any) will be calculated to the Closure Effective Date and credited or debited in accordance with the account terms. Outstanding fees lawfully due may be deducted from any closing balance before remittance."}]}',
       'Wave B account closure operative import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'ACCT-CLOSE-OPS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Closure confirmation: Meridian Retail Banking confirms that the Account identified in this notice will be closed with effect from the Closure Effective Date, subject to clearance of any pending transactions initiated before that date."}, {"type": "paragraph", "text": "Payment instructions and standing orders: All standing orders, direct debits and recurring payment mandates linked to the Account will cease on the Closure Effective Date. You remain responsible for arranging alternative payment methods with payees before closure."}, {"type": "paragraph", "text": "Cards and access: Debit cards, online banking access credentials and cheque books relating solely to the Account will be cancelled on the Closure Effective Date. Please destroy physical cards and unused cheques securely."}, {"type": "paragraph", "text": "Interest and charges: Accrued interest (if any) will be calculated to the Closure Effective Date and credited or debited in accordance with the account terms. Outstanding fees lawfully due may be deducted from any closing balance before remittance."}]}',
    change_description = 'Wave B account closure operative import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'ACCT-CLOSE-OPS' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'ac100001-0001-4000-8000-000000000002', 'ACCT-CLOSE-NEXT', 'RETAIL', 'Account Closure Next Steps', 'Final statement, balance remittance and residual items', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'ACCT-CLOSE-NEXT' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'ac200001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Final statement: A final statement covering the period to the Closure Effective Date will be issued to your correspondence address (or secure inbox) within ten Business Days after closure."}, {"type": "paragraph", "text": "Closing balance remittance: Any cleared credit balance remaining after deduction of lawful charges will be remitted to the nominated destination account stated in this notice, ordinarily within five Business Days after the Closure Effective Date."}, {"type": "paragraph", "text": "Residual items: If any uncleared item, chargeback or reclaim arises after closure, Meridian Retail Banking may reopen a suspense ledger solely to process that item and will notify you in writing."}, {"type": "paragraph", "text": "Records retention: Account records will be retained for the period required by applicable law and Meridian Retail Banking''s retention policy."}]}',
       'Wave B account closure next-steps import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'ACCT-CLOSE-NEXT' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Final statement: A final statement covering the period to the Closure Effective Date will be issued to your correspondence address (or secure inbox) within ten Business Days after closure."}, {"type": "paragraph", "text": "Closing balance remittance: Any cleared credit balance remaining after deduction of lawful charges will be remitted to the nominated destination account stated in this notice, ordinarily within five Business Days after the Closure Effective Date."}, {"type": "paragraph", "text": "Residual items: If any uncleared item, chargeback or reclaim arises after closure, Meridian Retail Banking may reopen a suspense ledger solely to process that item and will notify you in writing."}, {"type": "paragraph", "text": "Records retention: Account records will be retained for the period required by applicable law and Meridian Retail Banking''s retention policy."}]}',
    change_description = 'Wave B account closure next-steps import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'ACCT-CLOSE-NEXT' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

COMMIT;

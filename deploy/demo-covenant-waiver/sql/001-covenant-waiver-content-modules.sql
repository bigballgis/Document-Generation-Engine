-- Wave B demo content modules — bank-grade operative clauses
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'c0d10001-0001-4000-8000-000000000001', 'COV-WAIVER-OPS', 'CORP', 'Covenant Waiver Operative Terms', 'Limited waiver grant and conditions', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'COV-WAIVER-OPS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c0d20001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Limited waiver: Subject to the Consent Conditions, the Bank hereby waives the Specified Covenant Breach solely to the extent described in this letter and solely for the Waiver Period. This waiver is one-off and does not amend the Facility Agreement except as expressly stated."}, {"type": "paragraph", "text": "No amendment of covenant levels: Except as expressly set out in this letter, the financial covenant levels, testing dates and definitions in the Facility Agreement remain unchanged."}, {"type": "paragraph", "text": "Consent Conditions: The waiver is conditional upon satisfaction of each Consent Condition by the stated deadline. If any Consent Condition is not satisfied, this waiver is void ab initio unless the Bank confirms otherwise in writing."}, {"type": "paragraph", "text": "Fee: The Borrower shall pay the waiver fee stated in this letter on or before the Waiver Effective Date."}]}',
       'Wave B covenant waiver operative import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'COV-WAIVER-OPS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Limited waiver: Subject to the Consent Conditions, the Bank hereby waives the Specified Covenant Breach solely to the extent described in this letter and solely for the Waiver Period. This waiver is one-off and does not amend the Facility Agreement except as expressly stated."}, {"type": "paragraph", "text": "No amendment of covenant levels: Except as expressly set out in this letter, the financial covenant levels, testing dates and definitions in the Facility Agreement remain unchanged."}, {"type": "paragraph", "text": "Consent Conditions: The waiver is conditional upon satisfaction of each Consent Condition by the stated deadline. If any Consent Condition is not satisfied, this waiver is void ab initio unless the Bank confirms otherwise in writing."}, {"type": "paragraph", "text": "Fee: The Borrower shall pay the waiver fee stated in this letter on or before the Waiver Effective Date."}]}',
    change_description = 'Wave B covenant waiver operative import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'COV-WAIVER-OPS' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'c0d10001-0001-4000-8000-000000000002', 'COV-WAIVER-RSV', 'CORP', 'Covenant Waiver Reservation Clause', 'Non-waiver of other defaults and reservation', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'COV-WAIVER-RSV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c0d20001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Reservation of rights: Except for the Specified Covenant Breach expressly waived herein, the Bank reserves all rights and remedies in respect of any other Default or Event of Default, whether known or unknown, continuing or prospective."}, {"type": "paragraph", "text": "No course of dealing: This waiver does not establish a course of dealing or entitle the Borrower to expect any future waiver, consent or amendment."}, {"type": "paragraph", "text": "Reaffirmation: The Borrower and each Guarantor reaffirm their obligations under the Finance Documents and confirm that Security remains in full force and effect."}, {"type": "paragraph", "text": "Chinese (摘要): 本豁免仅针对本函载明的特定契约违约及豁免期限；不构成对其余违约的弃权，亦不构成对授信协议的持续修订。"}]}',
       'Wave B covenant waiver reservation import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'COV-WAIVER-RSV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Reservation of rights: Except for the Specified Covenant Breach expressly waived herein, the Bank reserves all rights and remedies in respect of any other Default or Event of Default, whether known or unknown, continuing or prospective."}, {"type": "paragraph", "text": "No course of dealing: This waiver does not establish a course of dealing or entitle the Borrower to expect any future waiver, consent or amendment."}, {"type": "paragraph", "text": "Reaffirmation: The Borrower and each Guarantor reaffirm their obligations under the Finance Documents and confirm that Security remains in full force and effect."}, {"type": "paragraph", "text": "Chinese (摘要): 本豁免仅针对本函载明的特定契约违约及豁免期限；不构成对其余违约的弃权，亦不构成对授信协议的持续修订。"}]}',
    change_description = 'Wave B covenant waiver reservation import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'COV-WAIVER-RSV' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

COMMIT;

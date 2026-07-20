-- Wave B demo content modules — bank-grade operative clauses
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT '1e100001-0001-4000-8000-000000000001', 'INS-ENDORS-OPS', 'RETAIL', 'Insurance Endorsement Operative Terms', 'Bank interest as loss payee / interested party', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'INS-ENDORS-OPS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT '1e200001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Bank''s interest: Meridian Home Finance / Meridian Retail Banking (the Lender) has a security interest in the Secured Property. The Lender must be noted on the buildings insurance policy as first loss payee and interested party for the Mortgage Reference stated in this notice."}, {"type": "paragraph", "text": "Endorsement: The Borrower shall procure that the insurer issues an endorsement (or equivalent confirmation) noting the Lender''s interest, and shall deliver a copy of the endorsement and schedule to the Lender within the Response Deadline."}, {"type": "paragraph", "text": "Claims: In the event of an insured loss affecting the Secured Property, insurance proceeds shall be applied in accordance with the mortgage terms, including reinstate-or-apply provisions, and the Lender may require proceeds to be paid to the Lender as loss payee."}, {"type": "paragraph", "text": "This notice relates to security insurance for a residential mortgage and is not a general marketing communication."}]}',
       'Wave B insurance endorsement operative import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'INS-ENDORS-OPS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Bank''s interest: Meridian Home Finance / Meridian Retail Banking (the Lender) has a security interest in the Secured Property. The Lender must be noted on the buildings insurance policy as first loss payee and interested party for the Mortgage Reference stated in this notice."}, {"type": "paragraph", "text": "Endorsement: The Borrower shall procure that the insurer issues an endorsement (or equivalent confirmation) noting the Lender''s interest, and shall deliver a copy of the endorsement and schedule to the Lender within the Response Deadline."}, {"type": "paragraph", "text": "Claims: In the event of an insured loss affecting the Secured Property, insurance proceeds shall be applied in accordance with the mortgage terms, including reinstate-or-apply provisions, and the Lender may require proceeds to be paid to the Lender as loss payee."}, {"type": "paragraph", "text": "This notice relates to security insurance for a residential mortgage and is not a general marketing communication."}]}',
    change_description = 'Wave B insurance endorsement operative import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'INS-ENDORS-OPS' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT '1e100001-0001-4000-8000-000000000002', 'INS-ENDORS-REQ', 'RETAIL', 'Insurance Endorsement Requirements', 'Coverage, sum insured and continuity requirements', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'INS-ENDORS-REQ' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT '1e200001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Minimum cover: Buildings insurance must be maintained for not less than the Required Sum Insured, on a reinstatement basis, with a reputable insurer authorised to write UK household risks, and must include cover for standard perils customary for residential mortgages."}, {"type": "paragraph", "text": "Continuity: The policy must not be cancelled, allowed to lapse, or materially reduced without prior written notice to the Lender. The Borrower shall pay premiums when due and provide evidence of renewal annually."}, {"type": "paragraph", "text": "Failure to insure: If satisfactory evidence of cover and endorsement is not provided by the Response Deadline, the Lender may (without obligation) arrange insurance and recover the cost under the mortgage terms, and/or treat the failure as a breach of the mortgage conditions."}, {"type": "paragraph", "text": "Chinese (摘要): 出借人须被批注为第一损失受偿人及利害关系人；保额不得低于要求保额；未按期提供批单的，出借人可按按揭条款安排保险并追索费用。"}]}',
       'Wave B insurance endorsement requirements import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'INS-ENDORS-REQ' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Minimum cover: Buildings insurance must be maintained for not less than the Required Sum Insured, on a reinstatement basis, with a reputable insurer authorised to write UK household risks, and must include cover for standard perils customary for residential mortgages."}, {"type": "paragraph", "text": "Continuity: The policy must not be cancelled, allowed to lapse, or materially reduced without prior written notice to the Lender. The Borrower shall pay premiums when due and provide evidence of renewal annually."}, {"type": "paragraph", "text": "Failure to insure: If satisfactory evidence of cover and endorsement is not provided by the Response Deadline, the Lender may (without obligation) arrange insurance and recover the cost under the mortgage terms, and/or treat the failure as a breach of the mortgage conditions."}, {"type": "paragraph", "text": "Chinese (摘要): 出借人须被批注为第一损失受偿人及利害关系人；保额不得低于要求保额；未按期提供批单的，出借人可按按揭条款安排保险并追索费用。"}]}',
    change_description = 'Wave B insurance endorsement requirements import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'INS-ENDORS-REQ' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

COMMIT;

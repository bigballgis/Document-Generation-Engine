-- Wave B demo content modules — bank-grade operative clauses
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'a0c10001-0001-4000-8000-000000000001', 'KYC-CDD-PUR', 'RETAIL', 'KYC CDD Purpose Clause', 'Regulatory purpose of customer due diligence refresh', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'KYC-CDD-PUR' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'a0c20001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Purpose: Meridian Retail Banking is required under the Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 and related FCA requirements to keep customer due diligence information up to date. This notice requests updated identification and verification materials for your accounts."}, {"type": "paragraph", "text": "Scope: The request covers identity verification, residential address confirmation, source of funds / source of wealth information where applicable, and beneficial ownership details for any joint or business-linked relationship held with Meridian Retail Banking."}, {"type": "paragraph", "text": "Data protection: Personal data supplied in response to this notice will be processed in accordance with Meridian Retail Banking''s privacy notice and applicable data protection law, solely for customer due diligence, fraud prevention and regulatory compliance purposes."}, {"type": "paragraph", "text": "This is a regulatory correspondence notice and is not a marketing communication."}]}',
       'Wave B KYC/CDD purpose import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'KYC-CDD-PUR' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Purpose: Meridian Retail Banking is required under the Money Laundering, Terrorist Financing and Transfer of Funds (Information on the Payer) Regulations 2017 and related FCA requirements to keep customer due diligence information up to date. This notice requests updated identification and verification materials for your accounts."}, {"type": "paragraph", "text": "Scope: The request covers identity verification, residential address confirmation, source of funds / source of wealth information where applicable, and beneficial ownership details for any joint or business-linked relationship held with Meridian Retail Banking."}, {"type": "paragraph", "text": "Data protection: Personal data supplied in response to this notice will be processed in accordance with Meridian Retail Banking''s privacy notice and applicable data protection law, solely for customer due diligence, fraud prevention and regulatory compliance purposes."}, {"type": "paragraph", "text": "This is a regulatory correspondence notice and is not a marketing communication."}]}',
    change_description = 'Wave B KYC/CDD purpose import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'KYC-CDD-PUR' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'a0c10001-0001-4000-8000-000000000002', 'KYC-CDD-CON', 'RETAIL', 'KYC CDD Consequences Clause', 'Timeline and consequences of non-response', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'KYC-CDD-CON' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'a0c20001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Response timeline: Please provide the requested information and documents no later than the Response Deadline stated in this notice. If you have already supplied equivalent materials within the preceding three months, please advise us in writing and we will confirm whether a further submission is required."}, {"type": "paragraph", "text": "Consequences of non-response: If satisfactory due diligence materials are not received by the Response Deadline, Meridian Retail Banking may be required to restrict account functionality, decline certain payment instructions, or in appropriate cases terminate the banking relationship, in each case in accordance with applicable law and the account terms and conditions."}, {"type": "paragraph", "text": "Assistance: If you are unable to provide any item on the checklist, contact the Financial Crime Compliance team using the contact details in this letter before the Response Deadline so that alternative verification routes can be considered where permitted."}, {"type": "paragraph", "text": "Complaints: If you are dissatisfied with how this request has been handled, you may raise a complaint under Meridian Retail Banking''s complaints procedure. You may also refer the matter to the Financial Ombudsman Service where eligible."}]}',
       'Wave B KYC/CDD consequences import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'KYC-CDD-CON' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Response timeline: Please provide the requested information and documents no later than the Response Deadline stated in this notice. If you have already supplied equivalent materials within the preceding three months, please advise us in writing and we will confirm whether a further submission is required."}, {"type": "paragraph", "text": "Consequences of non-response: If satisfactory due diligence materials are not received by the Response Deadline, Meridian Retail Banking may be required to restrict account functionality, decline certain payment instructions, or in appropriate cases terminate the banking relationship, in each case in accordance with applicable law and the account terms and conditions."}, {"type": "paragraph", "text": "Assistance: If you are unable to provide any item on the checklist, contact the Financial Crime Compliance team using the contact details in this letter before the Response Deadline so that alternative verification routes can be considered where permitted."}, {"type": "paragraph", "text": "Complaints: If you are dissatisfied with how this request has been handled, you may raise a complaint under Meridian Retail Banking''s complaints procedure. You may also refer the matter to the Financial Ombudsman Service where eligible."}]}',
    change_description = 'Wave B KYC/CDD consequences import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'KYC-CDD-CON' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

COMMIT;

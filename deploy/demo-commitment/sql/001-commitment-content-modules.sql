-- Wave B demo content modules — bank-grade operative clauses
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'c0b10001-0001-4000-8000-000000000001', 'COMMIT-OPS', 'CORP', 'Commitment Letter Operative Terms', 'Offer to commit, CPs and documentation path', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'COMMIT-OPS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c0b20001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"nodes": [{"type": "paragraph", "children": [{"type": "textRun", "value": "Offer to commit: Subject to the Conditions Precedent and the other terms of this Commitment Letter, Meridian Global Banking Corporation is prepared to commit to make available the Commitment Amount in the stated currency for the stated purpose."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Documentation: This Commitment Letter is an offer to commit and is not itself a full form facility agreement, wholesale facility offer documentation, or a substitute for a Facility Offer Letter. Definitive Finance Documents (including a facility agreement) must be negotiated and executed before any utilisation."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Conditions Precedent: The Bank is under no obligation to fund until each Condition Precedent listed in this letter (and any further CPs customary for facilities of this type) has been satisfied or waived in writing by the Bank."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Fees: An arrangement fee and commitment fee as stated in this letter are payable in accordance with the fee schedule. Legal costs of the Bank''s counsel are for the Borrower''s account whether or not the Facility closes."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Chinese (摘要): 本函为承诺放款要约，并非完整授信协议或批发授信要约文件（FOL）。在条件先例获满足并签署最终融资文件前，银行无放款义务。"}]}]}',
       'Wave B commitment operative import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'COMMIT-OPS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"nodes": [{"type": "paragraph", "children": [{"type": "textRun", "value": "Offer to commit: Subject to the Conditions Precedent and the other terms of this Commitment Letter, Meridian Global Banking Corporation is prepared to commit to make available the Commitment Amount in the stated currency for the stated purpose."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Documentation: This Commitment Letter is an offer to commit and is not itself a full form facility agreement, wholesale facility offer documentation, or a substitute for a Facility Offer Letter. Definitive Finance Documents (including a facility agreement) must be negotiated and executed before any utilisation."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Conditions Precedent: The Bank is under no obligation to fund until each Condition Precedent listed in this letter (and any further CPs customary for facilities of this type) has been satisfied or waived in writing by the Bank."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Fees: An arrangement fee and commitment fee as stated in this letter are payable in accordance with the fee schedule. Legal costs of the Bank''s counsel are for the Borrower''s account whether or not the Facility closes."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Chinese (摘要): 本函为承诺放款要约，并非完整授信协议或批发授信要约文件（FOL）。在条件先例获满足并签署最终融资文件前，银行无放款义务。"}]}]}',
    change_description = 'Wave B commitment operative import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'COMMIT-OPS' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'c0b10001-0001-4000-8000-000000000002', 'COMMIT-RSV', 'CORP', 'Commitment Reservation Clause', 'Expiry, MAC and explicit non-FOL reservation', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'COMMIT-RSV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c0b20001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"nodes": [{"type": "paragraph", "children": [{"type": "textRun", "value": "Expiry: This Commitment Letter expires automatically at 5:00 p.m. (London time) on the Commitment Expiry Date if definitive Finance Documents have not been executed, unless the Bank extends the expiry in writing."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Material adverse change: The Bank may withdraw or modify this commitment if a Material Adverse Change occurs in respect of the Borrower, the Guarantor, the Group or the proposed transaction before first utilisation."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "No partnership / no FOL: Nothing in this letter creates a partnership or joint venture. For the avoidance of doubt, this Commitment Letter is not a Facility Offer Letter, is not wholesale facility offer documentation, and does not replace or amend any CORP-FOL-OFFER template or product family."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Confidentiality: The Borrower shall keep the terms of this Commitment Letter confidential except for disclosure to professional advisers and as required by law or regulation."}]}]}',
       'Wave B commitment reservation import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'COMMIT-RSV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"nodes": [{"type": "paragraph", "children": [{"type": "textRun", "value": "Expiry: This Commitment Letter expires automatically at 5:00 p.m. (London time) on the Commitment Expiry Date if definitive Finance Documents have not been executed, unless the Bank extends the expiry in writing."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Material adverse change: The Bank may withdraw or modify this commitment if a Material Adverse Change occurs in respect of the Borrower, the Guarantor, the Group or the proposed transaction before first utilisation."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "No partnership / no FOL: Nothing in this letter creates a partnership or joint venture. For the avoidance of doubt, this Commitment Letter is not a Facility Offer Letter, is not wholesale facility offer documentation, and does not replace or amend any CORP-FOL-OFFER template or product family."}]}, {"type": "paragraph", "children": [{"type": "textRun", "value": "Confidentiality: The Borrower shall keep the terms of this Commitment Letter confidential except for disclosure to professional advisers and as required by law or regulation."}]}]}',
    change_description = 'Wave B commitment reservation import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'COMMIT-RSV' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

COMMIT;

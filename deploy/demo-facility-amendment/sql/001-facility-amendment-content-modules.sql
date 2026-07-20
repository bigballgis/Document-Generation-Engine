-- Wave B demo content modules — bank-grade operative clauses
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'fa100001-0001-4000-8000-000000000001', 'FAC-AMEND-OPS', 'CORP', 'Facility Amendment Operative Terms', 'Operative amendment mechanics and effectiveness', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'FAC-AMEND-OPS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'fa200001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Effectiveness: This Amendment Letter amends the Original Facility Agreement with effect from the Amendment Effective Date. Except as expressly varied herein, the Original Facility Agreement and the other Finance Documents remain in full force and effect and are ratified and confirmed."}, {"type": "paragraph", "text": "Finance Document status: This Amendment Letter constitutes a Finance Document. Capitalised terms used but not defined herein have the meanings given in the Original Facility Agreement."}, {"type": "paragraph", "text": "Conditions to effectiveness: The variations become effective only when the Agent has confirmed in writing that it has received the executed Amendment Letter, any required guarantor consent, updated know-your-customer materials and payment of the amendment fee."}, {"type": "paragraph", "text": "No novation: Nothing in this Amendment Letter constitutes a novation of the Original Facility Agreement or a release of any Security. Each Obligor reaffirms its obligations and each Guarantor confirms that its guarantee extends to the Facility as amended."}, {"type": "paragraph", "text": "Further assurance: Each Obligor shall, at its own expense, execute all documents and do all acts reasonably required by the Agent to give full effect to the variations contemplated by this letter."}, {"type": "paragraph", "text": "Chinese (摘要): 本修订函自修订生效日起修订原授信协议；除明示变更外，原协议及其他融资文件继续有效。本函构成融资文件，不构成债务更新，亦不解除任何担保。"}]}',
       'Wave B facility amendment operative import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'FAC-AMEND-OPS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Effectiveness: This Amendment Letter amends the Original Facility Agreement with effect from the Amendment Effective Date. Except as expressly varied herein, the Original Facility Agreement and the other Finance Documents remain in full force and effect and are ratified and confirmed."}, {"type": "paragraph", "text": "Finance Document status: This Amendment Letter constitutes a Finance Document. Capitalised terms used but not defined herein have the meanings given in the Original Facility Agreement."}, {"type": "paragraph", "text": "Conditions to effectiveness: The variations become effective only when the Agent has confirmed in writing that it has received the executed Amendment Letter, any required guarantor consent, updated know-your-customer materials and payment of the amendment fee."}, {"type": "paragraph", "text": "No novation: Nothing in this Amendment Letter constitutes a novation of the Original Facility Agreement or a release of any Security. Each Obligor reaffirms its obligations and each Guarantor confirms that its guarantee extends to the Facility as amended."}, {"type": "paragraph", "text": "Further assurance: Each Obligor shall, at its own expense, execute all documents and do all acts reasonably required by the Agent to give full effect to the variations contemplated by this letter."}, {"type": "paragraph", "text": "Chinese (摘要): 本修订函自修订生效日起修订原授信协议；除明示变更外，原协议及其他融资文件继续有效。本函构成融资文件，不构成债务更新，亦不解除任何担保。"}]}',
    change_description = 'Wave B facility amendment operative import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'FAC-AMEND-OPS' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by, locale)
SELECT 'fa100001-0001-4000-8000-000000000002', 'FAC-AMEND-COV', 'CORP', 'Facility Amendment Continuing Covenants', 'Continuing covenants and reservation of rights', '[]', '10000003', '10000003', 'zh-CN'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'FAC-AMEND-COV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'fa200001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks": [{"type": "paragraph", "text": "Continuing covenants: The Borrower shall continue to comply with all financial and general covenants under the Original Facility Agreement as amended, and shall deliver Compliance Certificates within thirty days of each Financial Quarter end."}, {"type": "paragraph", "text": "Information undertakings: The Borrower shall notify the Agent promptly of any Event of Default, any material litigation and any proposed change of control, and shall deliver audited annual financial statements within 120 days of each financial year end."}, {"type": "paragraph", "text": "Reservation of rights: No failure or delay by the Agent or any Lender in exercising any right under the Finance Documents shall operate as a waiver. Any waiver granted in connection with this Amendment Letter is limited to its express terms."}, {"type": "paragraph", "text": "Fees and costs: The Borrower shall pay the amendment fee specified in the Variation Schedule and shall reimburse the Agent for all reasonable legal and documentation costs incurred in connection with this Amendment Letter."}]}',
       'Wave B facility amendment covenants import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'FAC-AMEND-COV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks": [{"type": "paragraph", "text": "Continuing covenants: The Borrower shall continue to comply with all financial and general covenants under the Original Facility Agreement as amended, and shall deliver Compliance Certificates within thirty days of each Financial Quarter end."}, {"type": "paragraph", "text": "Information undertakings: The Borrower shall notify the Agent promptly of any Event of Default, any material litigation and any proposed change of control, and shall deliver audited annual financial statements within 120 days of each financial year end."}, {"type": "paragraph", "text": "Reservation of rights: No failure or delay by the Agent or any Lender in exercising any right under the Finance Documents shall operate as a waiver. Any waiver granted in connection with this Amendment Letter is limited to its express terms."}, {"type": "paragraph", "text": "Fees and costs: The Borrower shall pay the amendment fee specified in the Variation Schedule and shall reimburse the Agent for all reasonable legal and documentation costs incurred in connection with this Amendment Letter."}]}',
    change_description = 'Wave B facility amendment covenants import',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'FAC-AMEND-COV' AND cm.deleted_at IS NULL AND v.semantic_version = '1.0.0';

COMMIT;

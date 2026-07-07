-- Annual review demo content modules — P23 bank-grade foreign-bank letter (v3)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'f1000001-0001-4000-8000-000000000001', 'ANNUAL-REVIEW-STD', 'CORP', 'Annual Review Standard Clause', 'Annual credit review standard terms (LMA-aligned)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'ANNUAL-REVIEW-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'f2000001-0001-4000-8000-000000000003', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Based on our review of your latest audited financial statements, management accounts, compliance certificates and industry outlook, we confirm that the Facility remains available subject to continued compliance with the financial covenants, representations and undertakings set out in the facility agreement."},
         {"type":"paragraph","text":"Our review considered the financial performance and position of the Group, the quality and sufficiency of security, the conduct of the account, compliance with all covenants (financial and general), and any material events notified to us during the review period. Save as disclosed in the Material Findings section, no breaches, defaults or material adverse changes were identified."},
         {"type":"paragraph","text":"Pricing adjustment: The margin has been set in accordance with the pricing grid set out in Schedule 1 of the facility agreement, based on the leverage ratio as at the review date. The revised margin takes effect from the renewal date and applies to all outstanding and future utilisations until the next review date."},
         {"type":"paragraph","text":"Continued availability: Availability under the Facility is confirmed for the renewed term, subject to the conditions precedent having been satisfied and the representations being true and correct on each utilisation date. The Bank reserves the right to review the Facility at any time upon the occurrence of a material adverse change or any event of default."},
         {"type":"paragraph","text":"Your relationship manager will contact you to discuss any pricing or structural adjustments, and to arrange delivery of the renewed facility documentation, ahead of the next review cycle."}
       ]}',
       'Annual review demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'ANNUAL-REVIEW-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'f1000001-0001-4000-8000-000000000003', 'ANNUAL-REVIEW-COV', 'CORP', 'Annual Review Covenants', 'Financial covenant summary and reporting requirements', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'ANNUAL-REVIEW-COV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'f2000001-0001-4000-8000-000000000003', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Reporting: The Borrower shall deliver to the Bank (i) audited annual consolidated financial statements within 120 days of each financial year end; (ii) unaudited quarterly management accounts within 45 days of each Financial Quarter end; and (iii) a Compliance Certificate signed by two directors or authorised signatories confirming compliance with the financial covenants, within 30 days of each Financial Quarter end."},
         {"type":"paragraph","text":"Equity Cure: Where a breach of any financial covenant occurs, the Borrower may, with the consent of the Bank, cure such breach by an equity injection, provided that no more than two equity cures may be applied in any rolling four-quarter period and the cured compliance is recalculated in accordance with the facility agreement."},
         {"type":"paragraph","text":"General undertakings: The Borrower shall (a) maintain its corporate existence and obtain all authorisations necessary for its business; (b) comply with all applicable laws including anti-bribery, sanctions and anti-money-laundering regulations; (c) not dispose of any material assets outside the ordinary course of business without prior Bank consent; and (d) maintain insurance on its assets with reputable insurers."},
         {"type":"paragraph","text":"Material events: The Borrower shall notify the Bank promptly (and in any event within five Business Days) of any litigation, claim or proceedings exceeding the materiality threshold, any change of control, any breach of covenant or representation, any Event of Default or any event which would reasonably be expected to have a Material Adverse Effect."}
       ]}',
       'Annual review demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'ANNUAL-REVIEW-COV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'f1000001-0001-4000-8000-000000000002', 'FACILITY-RENEWAL-STD', 'CORP', 'Facility Renewal Standard Clause', 'Facility renewal standard terms (KYC/AML)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'FACILITY-RENEWAL-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'f2000001-0001-4000-8000-000000000004', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This renewal confirmation is issued pursuant to, and forms part of, the facility agreement. The renewal is subject to there being no material adverse change in your financial condition or that of the Group between the review date and the renewal date, and to the continued satisfaction of all know-your-customer, anti-money-laundering and sanctions requirements."},
         {"type":"paragraph","text":"Please execute and return the enclosed renewal confirmation by the renewal date stated above to avoid interruption of availability. Failure to return the executed confirmation by the renewal date may, at the Bank''''s discretion, result in suspension of further utilisations pending execution."},
         {"type":"paragraph","text":"All representations, undertakings, financial covenants, events of default and other terms of the facility agreement remain in full force and effect and are deemed to be repeated on the renewal date, save as expressly varied by the renewed terms set out herein. No variation of the facility agreement is effective unless recorded in writing and signed by an authorised signatory of the Bank."},
         {"type":"paragraph","text":"This renewal does not constitute a fresh commitment or a new facility; it extends the term of the existing Facility on the terms set out herein. The Facility shall continue to be governed by, and construed in accordance with, the governing law specified in the facility agreement."}
       ]}',
       'Annual review demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'FACILITY-RENEWAL-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

COMMIT;

-- Trade LC demo content modules — real foreign-bank-letter grade (v2)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'd1000001-0001-4000-8000-000000000001', 'TRADE-LC-STD', 'TRADE', 'LC Standard Advice Clause', 'Documentary credit advice standard terms (UCP 600)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'TRADE-LC-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'd2000001-0001-4000-8000-000000000001', cm.id, '2.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This documentary credit is subject to the Uniform Customs and Practice for Documentary Credits (UCP 600), as published by the International Chamber of Commerce, and to the International Standard Banking Practice for the Examination of Documents under Documentary Credits (ISBP 745)."},
         {"type":"paragraph","text":"Documents required: All documents must be presented in the form and number of copies specified above. Documents must be presented at the place of expiry on or before the expiry date. Each document must appear on its face to have been issued by the stated party, dated on or before the latest shipment date, and reference this credit number."},
         {"type":"paragraph","text":"Presentation: Documents must be presented at our counters or via an approved electronic channel not later than 21 days after the date of shipment but within the validity of the credit. We shall examine the presentation within a maximum of five banking days following the day of presentation to determine whether it is a complying presentation."},
         {"type":"paragraph","text":"Discrepancies: If a presentation does not comply, we shall give a single notice of refusal to the presenter within five banking days, stating each discrepancy. We may, at our option and upon the applicant''s waiver, waive discrepancies, but are not obliged to do so. Refused documents are held at the presenter''s disposal pending instructions."},
         {"type":"paragraph","text":"Payment: Upon determination of a complying presentation, we shall honour the drawing within seven banking days by telegraphic transfer to the nominated bank, value three working days, less any charges and fees as disclosed."},
         {"type":"paragraph","text":"Charges: All banking charges outside the country of issuance are for the account of the Beneficiary, unless otherwise stated. An issuance fee, amendment fee and discrepancy fee may apply at our standard tariff."},
         {"type":"paragraph","text":"This advice is issued without engagement on our part. We assume no responsibility for the authenticity, form, validity, sufficiency, accuracy or legal effect of any document presented under the credit, nor for the genuineness of any signature or the insolvency of any party."}
       ]}',
       'Trade LC demo v2 real-bank import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'TRADE-LC-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '2.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'd1000001-0001-4000-8000-000000000002', 'TRADE-GUARANTEE-STD', 'TRADE', 'Guarantee Standard Clause', 'Bank guarantee notice standard terms (URDG 758)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'TRADE-GUARANTEE-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'd2000001-0001-4000-8000-000000000002', cm.id, '2.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This guarantee is issued subject to the Uniform Rules for Demand Guarantees (URDG 758), as published by the International Chamber of Commerce. It is an irrevocable, independent and unconditional undertaking of the Guarantor."},
         {"type":"paragraph","text":"Claim procedure: A demand under this guarantee must be in writing, supported by a statement indicating in what respect the Applicant has failed to perform its obligations under the underlying contract, and must be received at our London trade operations centre during banking hours not later than the expiry date. Any demand must be the sole document required, and no other document need accompany it."},
         {"type":"paragraph","text":"Expiry: This guarantee expires on the expiry date stated above unless extended in writing by us. Any demand must be received before expiry; demands received thereafter will be refused. Expiry does not release the Applicant from its obligations under the underlying contract."},
         {"type":"paragraph","text":"Reduction and release: The guaranteed amount shall be automatically reduced by the amount of any payment made by us under this guarantee. The guarantee shall be released and of no further effect upon our receipt of a written release from the Beneficiary, or upon expiry, whichever is earlier."},
         {"type":"paragraph","text":"Governing law: This guarantee is governed by English law. Any dispute arising out of it shall be referred to the exclusive jurisdiction of the courts of England and Wales."}
       ]}',
       'Trade LC demo v2 real-bank import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'TRADE-GUARANTEE-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '2.0.0');

COMMIT;

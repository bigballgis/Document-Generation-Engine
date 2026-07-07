-- Trade LC demo content modules — P23 bank-grade foreign-bank letter (v3)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'd1000001-0001-4000-8000-000000000001', 'TRADE-LC-UCP', 'TRADE', 'UCP Presentation Terms', 'Documentary credit presentation and examination terms (UCP 600 / ISBP 745)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'TRADE-LC-UCP' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'd2000001-0001-4000-8000-000000000003', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This documentary credit is subject to the Uniform Customs and Practice for Documentary Credits (UCP 600), as published by the International Chamber of Commerce, and to the International Standard Banking Practice for the Examination of Documents under Documentary Credits (ISBP 745)."},
         {"type":"paragraph","text":"Documents required: All documents must be presented in the form and number of originals and copies specified in the document checklist. Documents must be presented at the place of expiry on or before the expiry date. Each document must appear on its face to have been issued by the stated party, dated on or before the latest shipment date, and reference this credit number."},
         {"type":"paragraph","text":"Presentation: Documents must be presented at our counters or via an approved electronic channel not later than the presentation period stated above but within the validity of the credit. We shall examine the presentation within a maximum of five banking days following the day of presentation to determine whether it is a complying presentation."},
         {"type":"paragraph","text":"Discrepancies: If a presentation does not comply, we shall give a single notice of refusal to the presenter within five banking days, stating each discrepancy. We may, at our option and upon the applicant''s waiver, waive discrepancies, but are not obliged to do so. Refused documents are held at the presenter''s disposal pending instructions."},
         {"type":"paragraph","text":"Payment: Upon determination of a complying presentation, we shall honour the drawing within seven banking days by telegraphic transfer to the nominated bank, value three working days, less any charges and fees as disclosed."},
         {"type":"paragraph","text":"Charges: All banking charges outside the country of issuance are for the account of the Beneficiary, unless otherwise stated. An issuance fee, amendment fee and discrepancy fee may apply at our standard tariff."},
         {"type":"paragraph","text":"This advice is issued without engagement on our part. We assume no responsibility for the authenticity, form, validity, sufficiency, accuracy or legal effect of any document presented under the credit, nor for the genuineness of any signature or the insolvency of any party."}
       ]}',
       'Trade LC demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'TRADE-LC-UCP' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'd1000001-0001-4000-8000-000000000002', 'TRADE-GUARANTEE-URDG', 'TRADE', 'URDG Standard Guarantee Terms', 'Demand guarantee standard terms (URDG 758)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'TRADE-GUARANTEE-URDG' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'd2000001-0001-4000-8000-000000000004', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This guarantee is issued subject to the Uniform Rules for Demand Guarantees (URDG 758), as published by the International Chamber of Commerce. It is an irrevocable, independent and unconditional undertaking of the Guarantor."},
         {"type":"paragraph","text":"Independence: Our obligation under this Guarantee is independent of the underlying contract and of any other undertaking. We are not bound by, and shall not be required to examine or enforce, any dispute between the Applicant and the Beneficiary under the underlying contract."},
         {"type":"paragraph","text":"Demand examination: We shall examine a demand within five banking days following the day of receipt to determine whether it is a complying demand. A complying demand must be received at our London trade operations centre during banking hours not later than the expiry date."},
         {"type":"paragraph","text":"Non-complying demand: If a demand does not comply, we shall give notice to the presenter within five banking days, stating in what respect the demand does not comply. A non-complying demand is deemed to have been withdrawn unless corrected within the time stated in our notice."},
         {"type":"paragraph","text":"Transfer and assignment: This Guarantee is not transferable or assignable without our prior written consent. Any purported transfer or assignment without consent is void."},
         {"type":"paragraph","text":"Governing law: This Guarantee is governed by English law unless otherwise stated in the body of the guarantee notice. Any dispute arising out of it shall be referred to the exclusive jurisdiction of the courts of England and Wales, without prejudice to our right to commence proceedings in any other court of competent jurisdiction."}
       ]}',
       'Trade LC demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'TRADE-GUARANTEE-URDG' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

COMMIT;

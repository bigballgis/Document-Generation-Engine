-- Trade LC / guarantee demo content modules — Wave A bank-grade (v4)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'd1000001-0001-4000-8000-000000000001', 'TRADE-LC-STD', 'TRADE', 'Documentary Credit Standard Terms', 'UCP 600 documentary credit operative terms', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'TRADE-LC-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'd2000001-0001-4000-8000-000000000004', cm.id, '4.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This irrevocable documentary credit is issued subject to the Uniform Customs and Practice for Documentary Credits, 2007 Revision, ICC Publication No. 600 (UCP 600). Except as expressly modified herein, the credit is available with the nominated bank by negotiation of drafts at sight drawn on the issuing bank."},
         {"type":"paragraph","text":"Documents required: (i) signed commercial invoice in three originals; (ii) full set of clean on-board ocean bills of lading consigned to order of Meridian Global Banking Corporation, notify applicant; (iii) packing list in two originals; (iv) certificate of origin; and (v) insurance certificate covering at least 110 per cent. of CIF value, blank endorsed, covering Institute Cargo Clauses (A)."},
         {"type":"paragraph","text":"Presentation period: Documents must be presented within 21 days after the date of shipment but within the validity of the credit. Partial shipments are allowed. Transhipment is allowed. Latest shipment date and expiry date are as stated on the face of this advice."},
         {"type":"paragraph","text":"Charges: All banking charges outside the issuing bank''s counters are for the account of the beneficiary, unless otherwise stated. Confirmation charges, if any, are for the account of the party requesting confirmation."},
         {"type":"paragraph","text":"Discrepant documents: If documents appear on their face not to constitute a complying presentation, the issuing bank may refuse to honour or negotiate and shall give a single notice to that effect in accordance with UCP 600 Article 16."},
         {"type":"paragraph","text":"Governing law and jurisdiction: Without prejudice to UCP 600, this credit and any dispute arising out of or in connection with it shall be governed by English law, and the courts of England shall have non-exclusive jurisdiction."},
         {"type":"paragraph","text":"This advice is sent without engagement on the part of the advising bank unless and until it adds its confirmation. The issuing bank undertakes to honour a complying presentation."}
       ]}',
       'Trade LC demo Wave A bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'TRADE-LC-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '4.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks":[
         {"type":"paragraph","text":"This irrevocable documentary credit is issued subject to the Uniform Customs and Practice for Documentary Credits, 2007 Revision, ICC Publication No. 600 (UCP 600). Except as expressly modified herein, the credit is available with the nominated bank by negotiation of drafts at sight drawn on the issuing bank."},
         {"type":"paragraph","text":"Documents required: (i) signed commercial invoice in three originals; (ii) full set of clean on-board ocean bills of lading consigned to order of Meridian Global Banking Corporation, notify applicant; (iii) packing list in two originals; (iv) certificate of origin; and (v) insurance certificate covering at least 110 per cent. of CIF value, blank endorsed, covering Institute Cargo Clauses (A)."},
         {"type":"paragraph","text":"Presentation period: Documents must be presented within 21 days after the date of shipment but within the validity of the credit. Partial shipments are allowed. Transhipment is allowed. Latest shipment date and expiry date are as stated on the face of this advice."},
         {"type":"paragraph","text":"Charges: All banking charges outside the issuing bank''s counters are for the account of the beneficiary, unless otherwise stated. Confirmation charges, if any, are for the account of the party requesting confirmation."},
         {"type":"paragraph","text":"Discrepant documents: If documents appear on their face not to constitute a complying presentation, the issuing bank may refuse to honour or negotiate and shall give a single notice to that effect in accordance with UCP 600 Article 16."},
         {"type":"paragraph","text":"Governing law and jurisdiction: Without prejudice to UCP 600, this credit and any dispute arising out of or in connection with it shall be governed by English law, and the courts of England shall have non-exclusive jurisdiction."},
         {"type":"paragraph","text":"This advice is sent without engagement on the part of the advising bank unless and until it adds its confirmation. The issuing bank undertakes to honour a complying presentation."}
       ]}',
    change_description = 'Wave A refresh - UCP 600 operative terms',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id
  AND cm.module_code = 'TRADE-LC-STD'
  AND cm.deleted_at IS NULL
  AND v.semantic_version = '4.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'd1000001-0001-4000-8000-000000000002', 'TRADE-GUARANTEE-STD', 'TRADE', 'Demand Guarantee Standard Terms', 'URDG 758 demand guarantee operative terms', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'TRADE-GUARANTEE-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'd2000001-0001-4000-8000-000000000005', cm.id, '4.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This demand guarantee is subject to the Uniform Rules for Demand Guarantees, ICC Publication No. 758 (URDG 758). The guarantor undertakes to pay the beneficiary any amount up to the maximum guarantee amount upon presentation of a complying demand."},
         {"type":"paragraph","text":"Demand requirements: Any demand shall be in writing, identify the guarantee by number, state the amount demanded, and be accompanied by a statement indicating in what respect the applicant is in breach of its obligations under the underlying contract, in accordance with URDG 758 Article 15."},
         {"type":"paragraph","text":"Expiry: The guarantee expires on the expiry date stated on the face of this notice at the counters of the guarantor, or upon payment of the maximum amount, or upon receipt by the guarantor of the beneficiary''s written release, whichever occurs first."},
         {"type":"paragraph","text":"Reduction and amendment: The guarantee amount may be reduced by the amount of any complying payment and by any reduction authorised in a complying amendment accepted by the beneficiary. Amendments are effective only when accepted by the beneficiary under URDG 758."},
         {"type":"paragraph","text":"Transfer: This guarantee is not transferable except with the prior written consent of the guarantor. Assignment of proceeds may be permitted subject to the guarantor''s standard assignment formalities."},
         {"type":"paragraph","text":"Governing law: This guarantee shall be governed by and construed in accordance with English law. The courts of England shall have exclusive jurisdiction to settle any dispute arising out of or in connection with this guarantee."},
         {"type":"paragraph","text":"The guarantor''s liability is independent of the underlying contract. The guarantor is not concerned with the performance or non-performance of that contract except as evidenced by a complying demand."}
       ]}',
       'Trade guarantee demo Wave A bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'TRADE-GUARANTEE-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '4.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks":[
         {"type":"paragraph","text":"This demand guarantee is subject to the Uniform Rules for Demand Guarantees, ICC Publication No. 758 (URDG 758). The guarantor undertakes to pay the beneficiary any amount up to the maximum guarantee amount upon presentation of a complying demand."},
         {"type":"paragraph","text":"Demand requirements: Any demand shall be in writing, identify the guarantee by number, state the amount demanded, and be accompanied by a statement indicating in what respect the applicant is in breach of its obligations under the underlying contract, in accordance with URDG 758 Article 15."},
         {"type":"paragraph","text":"Expiry: The guarantee expires on the expiry date stated on the face of this notice at the counters of the guarantor, or upon payment of the maximum amount, or upon receipt by the guarantor of the beneficiary''s written release, whichever occurs first."},
         {"type":"paragraph","text":"Reduction and amendment: The guarantee amount may be reduced by the amount of any complying payment and by any reduction authorised in a complying amendment accepted by the beneficiary. Amendments are effective only when accepted by the beneficiary under URDG 758."},
         {"type":"paragraph","text":"Transfer: This guarantee is not transferable except with the prior written consent of the guarantor. Assignment of proceeds may be permitted subject to the guarantor''s standard assignment formalities."},
         {"type":"paragraph","text":"Governing law: This guarantee shall be governed by and construed in accordance with English law. The courts of England shall have exclusive jurisdiction to settle any dispute arising out of or in connection with this guarantee."},
         {"type":"paragraph","text":"The guarantor''s liability is independent of the underlying contract. The guarantor is not concerned with the performance or non-performance of that contract except as evidenced by a complying demand."}
       ]}',
    change_description = 'Wave A refresh - URDG 758 operative terms',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id
  AND cm.module_code = 'TRADE-GUARANTEE-STD'
  AND cm.deleted_at IS NULL
  AND v.semantic_version = '4.0.0';

COMMIT;

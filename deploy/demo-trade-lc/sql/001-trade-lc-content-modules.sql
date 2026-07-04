-- Trade LC demo content modules (mock data only)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'd1000001-0001-4000-8000-000000000001', 'TRADE-LC-STD', 'TRADE', 'LC Standard Advice Clause', 'Documentary credit advice standard text', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'TRADE-LC-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'd2000001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"Documents required: Signed commercial invoice in triplicate; full set of clean on board bills of lading; certificate of origin; packing list; insurance certificate for 110% of invoice value."},{"type":"paragraph","text":"Presentation must be made at our counters or via approved electronic channel not later than 21 days after shipment date but within the expiry date stated above."},{"type":"paragraph","text":"This advice is issued without engagement on our part. We assume no responsibility for the authenticity or compliance of documents presented under the credit."}]}',
       'Trade LC demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'TRADE-LC-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'd1000001-0001-4000-8000-000000000002', 'TRADE-GUARANTEE-STD', 'TRADE', 'Guarantee Standard Clause', 'Bank guarantee notice standard text', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'TRADE-GUARANTEE-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'd2000001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[{"type":"paragraph","text":"Claim procedure: Beneficiary may present a demand in writing stating that the applicant has failed to perform under the underlying contract. Demand must be received at our London trade operations centre during banking hours."},{"type":"paragraph","text":"Expiry: This guarantee expires on the date stated in the underlying contract unless extended by written amendment signed by us."}]}',
       'Trade LC demo import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'TRADE-GUARANTEE-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

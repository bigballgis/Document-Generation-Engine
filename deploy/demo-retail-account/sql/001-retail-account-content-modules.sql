-- Retail account demo content modules — P23 bank-grade retail letters (v3)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'f1000001-0001-4000-8000-000000000001', 'RETAIL-ACCOUNT-OPEN-STD', 'RETAIL', 'Retail Account Opening Standard Clause', 'Standard retail account opening terms and conditions summary', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'RETAIL-ACCOUNT-OPEN-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'f2000001-0001-4000-8000-000000000003', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Your Account is governed by the Account terms and conditions enclosed with this letter, the Personal Banking tariff and the General Terms for Personal Customers, each as amended from time to time. By using the Account, you agree to be bound by these terms."},
         {"type":"paragraph","text":"You must keep your Account in credit or within any agreed overdraft limit. We may charge interest and fees if your Account becomes overdrawn without an arranged overdraft facility, or if you exceed your arranged overdraft limit."},
         {"type":"paragraph","text":"We may close your Account if you do not use it for a continuous period of twelve months, if you breach the Account terms and conditions, or if we are required to do so by law or regulation. We will give you at least 60 days'' notice before closing an Account for inactivity, unless we are prohibited from doing so."},
         {"type":"paragraph","text":"You may close your Account at any time by giving us written notice. Any balance remaining on the Account will be paid to you by cheque or transfer to a nominated account, less any amounts owing to us."},
         {"type":"paragraph","text":"We are committed to treating customers fairly and to providing accessible banking services. If you have a complaint, please contact our Customer Relations team. If we cannot resolve your complaint, you may refer it to the Financial Ombudsman Service."}
       ]}',
       'Retail account demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'RETAIL-ACCOUNT-OPEN-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'f1000001-0001-4000-8000-000000000002', 'RETAIL-ACCOUNT-BALANCE-STD', 'RETAIL', 'Retail Balance Confirmation Standard Clause', 'Standard retail balance confirmation regulatory notice', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'RETAIL-ACCOUNT-BALANCE-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'f2000001-0001-4000-8000-000000000002', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This balance confirmation reflects the position of your Account as at the date stated above. It does not include transactions that were pending or in process at close of business on that date. The available balance shown may differ from the cleared balance if pending transactions have since been processed."},
         {"type":"paragraph","text":"Your eligible deposits with us are protected up to a total of £85,000 by the Financial Services Compensation Scheme (FSCS). Any deposits you hold above this limit are unlikely to be covered. For further information, please visit www.fscs.org.uk."},
         {"type":"paragraph","text":"We are authorised and regulated by the Financial Conduct Authority and the Prudential Regulation Authority. Our Firm Reference Number is available on request or at register.fca.org.uk."},
         {"type":"paragraph","text":"If you believe any entry on this confirmation is incorrect, please contact us within 14 days of the date of this letter. After this period, the balance confirmation will be deemed accepted unless you have notified us of a discrepancy."}
       ]}',
       'Retail account demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'RETAIL-ACCOUNT-BALANCE-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

COMMIT;

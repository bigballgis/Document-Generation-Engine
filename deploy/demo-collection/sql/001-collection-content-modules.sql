-- Collection demo content modules — P23 bank-grade foreign-bank letter (v3)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'e1000001-0001-4000-8000-000000000001', 'COLLECTION-RATE-STD', 'RETAIL', 'Rate Change Standard Clause', 'Rate change notice regulatory text (FCA CONC)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'COLLECTION-RATE-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'e2000001-0001-4000-8000-000000000004', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This notice is given in accordance with the terms and conditions governing your Account and the Financial Conduct Authority''s Consumer Credit sourcebook (CONC). For variable-rate products, we will give you at least 14 days'' personal notice before any decrease to the interest rate on your Account takes effect, except where it is not reasonably practicable to do so, in which case we will notify you as soon as possible thereafter."},
         {"type":"paragraph","text":"Right to switch: If you are dissatisfied with the change, you may, at any time before the effective date, switch your Account to an alternative product we offer, or close your Account without charge and without penalty, by contacting us using the details below. We will not apply early closure fees in these circumstances."},
         {"type":"paragraph","text":"Fixed-rate products: If you hold a fixed-rate product, this notice does not apply to the fixed-rate period; the rate for the fixed-rate period remains unchanged. The change will take effect only on reversion to a variable rate at the end of the fixed-rate period, and we will write to you separately at that time."},
         {"type":"paragraph","text":"Interest calculation: Interest on the Account is calculated daily on the cleared credit balance and applied monthly. The Annual Equivalent Rate (AER) illustrates what the interest rate would be if interest was paid and compounded once each year. The gross rate is the contractual rate of interest payable before the deduction of income tax at the rate specified by law."},
         {"type":"paragraph","text":"Tax: Interest is paid gross. You may be liable to pay tax on the interest you receive in accordance with the laws of the United Kingdom. It is your responsibility to disclose any interest to the relevant tax authority."},
         {"type":"paragraph","text":"Contact us: If you have any questions about this notice or wish to discuss your options, please contact your Relationship Manager using the details set out at the foot of this letter, or call us on 0800 224 482 (lines open 8am to 8pm, Monday to Saturday)."}
       ]}',
       'Collection demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'COLLECTION-RATE-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'e1000001-0001-4000-8000-000000000002', 'COLLECTION-OVERDUE-STD', 'RETAIL', 'Overdue Collection Standard Clause', 'Overdue collection regulatory text (FCA CONC / treating customers fairly)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'COLLECTION-OVERDUE-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'e2000001-0001-4000-8000-000000000005', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Please contact us immediately using the details below to discuss repayment arrangements or to make a payment towards the overdue balance. We are committed to treating customers fairly throughout the collections process, in accordance with the Financial Conduct Authority''s Consumer Credit sourcebook (CONC) and our published collections policy."},
         {"type":"paragraph","text":"Consequences of non-payment: Failure to bring your Account up to date may result in additional charges, the suspension of further credit, default interest being applied at the rate specified in your agreement, and a record of the arrears being placed on your credit file held by credit reference agencies, which may affect your ability to obtain credit in future. Persistent default may ultimately lead to referral to an external collections agency or, in the case of secured lending, to recovery proceedings."},
         {"type":"paragraph","text":"Financial difficulty: If you are experiencing financial difficulty, please contact us as soon as possible so that we may work with you to find a suitable solution. Free, impartial debt advice is available from StepChange Debt Charity (0800 138 1111, stepchange.org), Citizens Advice (citizensadvice.org.uk), and the Money Helper (0800 138 7777, moneyhelper.org.uk)."},
         {"type":"paragraph","text":"Complaints: If you are dissatisfied with the way we have handled your Account, please contact our Customer Relations team. If we cannot resolve your complaint, you may refer it to the Financial Ombudsman Service, Exchange Tower, London E14 9SR (0800 023 4567, financial-ombudsman.org.uk)."},
         {"type":"paragraph","text":"Vulnerability: If there are any circumstances we should be aware of that may affect your ability to deal with this matter (including health, bereavement or other vulnerability), please tell us so that we can offer appropriate support."}
       ]}',
       'Collection demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'COLLECTION-OVERDUE-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

COMMIT;

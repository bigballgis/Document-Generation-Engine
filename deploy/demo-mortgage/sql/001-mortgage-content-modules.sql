-- Mortgage demo content modules — real foreign-bank-letter grade (v2)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'b1000001-0001-4000-8000-000000000001', 'MORTGAGE-STD-TERMS', 'RETAIL', 'Standard Mortgage Terms', 'Residential mortgage standard terms (FCA CONC-aligned)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'MORTGAGE-STD-TERMS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'b2000001-0001-4000-8000-000000000001', cm.id, '2.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Standard Conditions: (1) You must maintain buildings insurance for the full reinstatement value of the Property, with the lender''s interest noted on the policy, for the full term of the mortgage. (2) The Property must remain your main residence unless we agree otherwise in writing. (3) You must not let, sub-let or part with possession of the Property without our prior written consent. (4) You must keep the Property in good repair and condition and comply with all statutory notices affecting it."},
         {"type":"paragraph","text":"Interest: Interest is calculated daily on the outstanding balance of the Loan and applied monthly. The interest rate applicable to the Loan is set out in the Mortgage Particulars. After the fixed-rate period, the rate will revert to the reversion rate stated in this offer."},
         {"type":"paragraph","text":"Repayments: You must pay the monthly instalment on the first day of each calendar month by direct debit. Each instalment comprises principal and interest calculated so that the Loan is repaid in full by the end of the Term, assuming all payments are made on time."},
         {"type":"paragraph","text":"Early Repayment: If you repay all or part of the Loan during the fixed-rate period, an early repayment charge may apply as set out in this offer. Overpayments of up to 10% of the outstanding balance per year may be made without charge. After the fixed-rate period, no early repayment charge applies."},
         {"type":"paragraph","text":"Arrears and Default: If you fail to make a payment when due, we will contact you promptly in accordance with FCA CONC rules. Persistent arrears may affect your credit rating and, as a last resort, could lead to possession proceedings. If you experience financial difficulty, contact us immediately; free impartial advice is available from StepChange Debt Charity or Citizens Advice."},
         {"type":"paragraph","text":"Fees: An arrangement fee and valuation fee may apply as disclosed in the European Standardised Information Sheet (ESIS) provided to you. No undisclosed fees will be charged under this mortgage."},
         {"type":"paragraph","text":"Complaints: If you are dissatisfied with our service, please contact Meridian Home Finance Customer Relations, PO Box 4400, Manchester M1 4HQ, or call 0800 123 4567. If your complaint is not resolved, you may refer it to the Financial Ombudsman Service, Exchange Tower, London E14 9SR."},
         {"type":"paragraph","text":"Regulation: This mortgage is regulated by the Financial Conduct Authority under the Mortgage Conduct of Business rules. Your home may be repossessed if you do not keep up repayments on your mortgage."}
       ]}',
       'Mortgage demo v2 real-bank import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'MORTGAGE-STD-TERMS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '2.0.0');

COMMIT;

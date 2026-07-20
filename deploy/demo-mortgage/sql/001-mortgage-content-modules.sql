-- Mortgage demo content modules — P23 bank-grade foreign-bank letter (v3)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'b1000001-0001-4000-8000-000000000001', 'MORTGAGE-STD-TERMS', 'RETAIL', 'Standard Mortgage Terms', 'Residential mortgage standard operative terms (FCA CONC-aligned)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'MORTGAGE-STD-TERMS' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'b2000001-0001-4000-8000-000000000003', cm.id, '3.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Interest: Interest is calculated daily on the outstanding balance of the Loan and applied monthly. The interest rate applicable to the Loan is set out in the Mortgage Particulars. After the fixed-rate period, the rate will revert to the reversion rate stated in this offer, which may vary from time to time."},
         {"type":"paragraph","text":"Repayments: You must pay the monthly instalment on the first day of each calendar month by direct debit. Each instalment comprises principal and interest calculated so that the Loan is repaid in full by the end of the Term, assuming all payments are made on time and no overpayments are made."},
         {"type":"paragraph","text":"Early Repayment: If you repay all or part of the Loan during the fixed-rate period, an early repayment charge may apply as set out in this offer. Overpayments of up to the maximum annual overpayment percentage may be made without charge. After the fixed-rate period, no early repayment charge applies unless otherwise stated."},
         {"type":"paragraph","text":"Fees: An arrangement fee and valuation fee may apply as disclosed in the European Standardised Information Sheet (ESIS) provided to you. No undisclosed fees will be charged under this mortgage."},
         {"type":"paragraph","text":"Complaints: If you are dissatisfied with our service, please contact Meridian Home Finance Customer Relations, PO Box 4400, Manchester M1 4HQ, or call 0800 123 4567. If your complaint is not resolved, you may refer it to the Financial Ombudsman Service, Exchange Tower, London E14 9SR."},
         {"type":"paragraph","text":"Regulation: This mortgage is regulated by the Financial Conduct Authority under the Mortgage Conduct of Business rules. Your home may be repossessed if you do not keep up repayments on your mortgage."},
         {"type":"paragraph","text":"Governing law: This offer and the mortgage deed are governed by the laws of England and Wales. The courts of England and Wales have exclusive jurisdiction over disputes arising from the mortgage."},
         {"type":"paragraph","text":"Acceptance: To accept this offer, sign and return the enclosed acceptance form by the offer expiry date. No binding mortgage contract arises until we receive your acceptance and all conditions precedent are satisfied."}
       ]}',
       'Mortgage demo Wave A bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'MORTGAGE-STD-TERMS' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '3.0.0');

UPDATE content_module_version v
SET content_structure_json = '{"blocks":[
         {"type":"paragraph","text":"Interest: Interest is calculated daily on the outstanding balance of the Loan and applied monthly. The interest rate applicable to the Loan is set out in the Mortgage Particulars. After the fixed-rate period, the rate will revert to the reversion rate stated in this offer, which may vary from time to time."},
         {"type":"paragraph","text":"Repayments: You must pay the monthly instalment on the first day of each calendar month by direct debit. Each instalment comprises principal and interest calculated so that the Loan is repaid in full by the end of the Term, assuming all payments are made on time and no overpayments are made."},
         {"type":"paragraph","text":"Early Repayment: If you repay all or part of the Loan during the fixed-rate period, an early repayment charge may apply as set out in this offer. Overpayments of up to the maximum annual overpayment percentage may be made without charge. After the fixed-rate period, no early repayment charge applies unless otherwise stated."},
         {"type":"paragraph","text":"Fees: An arrangement fee and valuation fee may apply as disclosed in the European Standardised Information Sheet (ESIS) provided to you. No undisclosed fees will be charged under this mortgage."},
         {"type":"paragraph","text":"Complaints: If you are dissatisfied with our service, please contact Meridian Home Finance Customer Relations, PO Box 4400, Manchester M1 4HQ, or call 0800 123 4567. If your complaint is not resolved, you may refer it to the Financial Ombudsman Service, Exchange Tower, London E14 9SR."},
         {"type":"paragraph","text":"Regulation: This mortgage is regulated by the Financial Conduct Authority under the Mortgage Conduct of Business rules. Your home may be repossessed if you do not keep up repayments on your mortgage."},
         {"type":"paragraph","text":"Governing law: This offer and the mortgage deed are governed by the laws of England and Wales. The courts of England and Wales have exclusive jurisdiction over disputes arising from the mortgage."},
         {"type":"paragraph","text":"Acceptance: To accept this offer, sign and return the enclosed acceptance form by the offer expiry date. No binding mortgage contract arises until we receive your acceptance and all conditions precedent are satisfied."}
       ]}',
    change_description = 'Wave A refresh - mortgage standard terms',
    updated_at = (NOW() AT TIME ZONE 'UTC'),
    updated_by = '10000007'
FROM content_module cm
WHERE v.module_id = cm.id AND cm.module_code = 'MORTGAGE-STD-TERMS' AND cm.deleted_at IS NULL AND v.semantic_version = '3.0.0';

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'b1000001-0001-4000-8000-000000000002', 'MORTGAGE-PROP-COV', 'RETAIL', 'Mortgage Property Covenants', 'Property maintenance, insurance and occupancy covenants', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'MORTGAGE-PROP-COV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'b2000001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Insurance: You must maintain buildings insurance for the full reinstatement value of the Property, with the Lender''s interest noted on the policy, for the full term of the mortgage. You must provide evidence of renewal annually and notify the Lender immediately of any cancellation, reduction or material change to the policy."},
         {"type":"paragraph","text":"Occupancy: The Property must remain your main residence unless we agree otherwise in writing. You must not let, sub-let or part with possession of the Property without our prior written consent, which will not be unreasonably withheld for a single assured shorthold tenancy subject to our standard letting conditions."},
         {"type":"paragraph","text":"Maintenance: You must keep the Property in good repair and condition, comply with all statutory notices affecting it, and not carry out any structural alterations or extensions without our prior written consent. You must notify us promptly of any material damage, subsidence, or environmental hazard affecting the Property."},
         {"type":"paragraph","text":"Title: You must not create any further charge, mortgage or encumbrance over the Property without our prior written consent. You must maintain good and marketable title and comply with all obligations under the title documents."}
       ]}',
       'Mortgage demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'MORTGAGE-PROP-COV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'b1000001-0001-4000-8000-000000000003', 'MORTGAGE-DEFAULT', 'RETAIL', 'Mortgage Arrears and Default', 'Arrears handling and default provisions (FCA CONC-aligned)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'MORTGAGE-DEFAULT' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'b2000001-0001-4000-8000-000000000004', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Arrears: If you fail to make a payment when due, we will contact you promptly in accordance with FCA CONC rules. We will treat you fairly and consider any reasonable proposal to reschedule payments. Persistent arrears may affect your credit rating and, as a last resort, could lead to possession proceedings."},
         {"type":"paragraph","text":"Financial difficulty: If you experience financial difficulty, contact us immediately. Free impartial advice is available from StepChange Debt Charity (0800 138 1111), Citizens Advice (citizensadvice.org.uk), or National Debtline (0808 808 4000). We will work with you to find a sustainable solution before considering enforcement action."},
         {"type":"paragraph","text":"Events of Default: Each of the following is an Event of Default: (a) non-payment of any amount due under the Mortgage when due, unless caused by administrative error and paid within three Business Days; (b) breach of any covenant, representation or undertaking; (c) material misrepresentation in the mortgage application; (d) insolvency or bankruptcy of a Borrower; (e) compulsory purchase or demolition of the Property without satisfactory reinstatement."},
         {"type":"paragraph","text":"Remedies: On the occurrence of an Event of Default which is continuing, the Lender may (i) charge default interest at the rate specified in the mortgage deed; (ii) require immediate repayment of all amounts outstanding; and (iii) exercise any power of sale or possession available under the mortgage deed and applicable law, subject to FCA and court requirements."}
       ]}',
       'Mortgage demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'MORTGAGE-DEFAULT' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

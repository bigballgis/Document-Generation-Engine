-- Credit limit demo content modules — P23 bank-grade foreign-bank letter (v3)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'c1000001-0001-4000-8000-000000000001', 'CREDIT-LIMIT-STD', 'CORP', 'Credit Limit Standard Clause', 'Standard credit limit confirmation operative terms (LMA-aligned)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'CREDIT-LIMIT-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c2000001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Utilisation: Each utilisation must be made by notice to the Agent no later than 10:00 a.m. (London time) on the proposed utilisation date, in the form of a Utilisation Request, specifying the amount, currency and proposed utilisation date. Each utilisation is subject to the satisfaction of the conditions precedent and the representations being true and correct on the utilisation date."},
         {"type":"paragraph","text":"Availability: Limits are reviewed annually and may be reduced, cancelled or withdrawn by the Bank on reasonable written notice if there is a material adverse change in the financial condition of the Borrower or the Group, or if the Borrower breaches any covenant, representation or undertaking under the Finance Documents."},
         {"type":"paragraph","text":"Repayment and Prepayment: The Borrower shall repay the Facility in the instalments and on the dates set out in the Repayment Schedule, subject to mandatory prepayment on the occurrence of any mandatory prepayment event (including change of control, illegality and asset disposal proceeds). Voluntary prepayment is permitted on three Business Days notice, together with any Break Costs."},
         {"type":"paragraph","text":"Representations: The Borrower represents that it is duly incorporated, has power to enter into and perform the Finance Documents, and that the Finance Documents constitute legal, valid and binding obligations. No Event of Default is continuing or would result from the making of any utilisation."},
         {"type":"paragraph","text":"Assignments and Transfers: The Bank may assign, transfer or sub-participate all or any part of its rights and obligations under the Finance Documents to any Eligible Institution, subject to compliance with know-your-customer requirements. The Borrower may not assign or transfer any of its rights without the prior written consent of the Bank."},
         {"type":"paragraph","text":"Confidentiality: Each party shall keep confidential all Confidential Information disclosed under the Finance Documents, save for permitted disclosures to professional advisers, regulators, assignees and as required by law, court order or applicable regulation."}
       ]}',
       'Credit limit demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'CREDIT-LIMIT-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'c1000001-0001-4000-8000-000000000002', 'CREDIT-LIMIT-COV', 'CORP', 'Credit Limit Covenants', 'Financial covenant summary and reporting requirements', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'CREDIT-LIMIT-COV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c2000001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Reporting: The Borrower shall deliver to the Bank (i) audited annual consolidated financial statements within 120 days of each financial year end; (ii) unaudited quarterly management accounts within 45 days of each Financial Quarter end; and (iii) a Compliance Certificate signed by two directors or authorised signatories confirming compliance with the financial covenants, within 30 days of each Financial Quarter end."},
         {"type":"paragraph","text":"Equity Cure: Where a breach of any financial covenant occurs, the Borrower may, with the consent of the Bank, cure such breach by an equity injection, provided that no more than two equity cures may be applied in any rolling four-quarter period and the cured compliance is recalculated in accordance with the facility agreement."},
         {"type":"paragraph","text":"General undertakings: The Borrower shall (a) maintain its corporate existence and obtain all authorisations necessary for its business; (b) comply with all applicable laws including anti-bribery, sanctions and anti-money-laundering regulations; (c) not dispose of any material assets outside the ordinary course of business without prior Bank consent; and (d) maintain insurance on its assets with reputable insurers."},
         {"type":"paragraph","text":"Material events: The Borrower shall notify the Bank promptly (and in any event within five Business Days) of any litigation, claim or proceedings exceeding the materiality threshold, any change of control, any breach of covenant or representation, any Event of Default or any event which would reasonably be expected to have a Material Adverse Effect."}
       ]}',
       'Credit limit demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'CREDIT-LIMIT-COV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'c1000001-0001-4000-8000-000000000003', 'CREDIT-LIMIT-EOD', 'CORP', 'Credit Limit Events of Default', 'Standard events of default clause (LMA-aligned)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'CREDIT-LIMIT-EOD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c2000001-0001-4000-8000-000000000003', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Each of the following is an Event of Default:"},
         {"type":"paragraph","text":"(a) Non-payment: The Borrower does not pay on the due date any amount payable pursuant to a Finance Document at the place and in the currency in which it is expressed to be payable, unless failure to pay is caused by administrative or technical error and payment is made within three Business Days of its due date."},
         {"type":"paragraph","text":"(b) Financial covenants: Any financial covenant is not satisfied, or any requirement of the financial covenants is not complied with."},
         {"type":"paragraph","text":"(c) Other obligations: The Borrower does not comply with any provision of the Finance Documents (other than those referred to in paragraphs (a) and (b) above), and such default is not remedied within 15 Business Days of the earlier of the Bank giving notice and the Borrower becoming aware."},
         {"type":"paragraph","text":"(d) Misrepresentation: Any representation or statement made or deemed to be made by an Obligor in the Finance Documents or any other document delivered by or on behalf of any Obligor is or proves to have been incorrect or misleading in any material respect when made or deemed to be made."},
         {"type":"paragraph","text":"(e) Insolvency: An Obligor is unable or admits inability to pay its debts as they fall due, suspends making payments on any of its debts, or by reason of actual or anticipated financial difficulties commences negotiations with creditors with a view to rescheduling any of its indebtedness."},
         {"type":"paragraph","text":"(f) Insolvency proceedings: Any corporate action, legal proceedings or other procedure or step is taken in relation to the suspension of payments, a moratorium of any indebtedness, winding-up, dissolution, administration or reorganisation of an Obligor."},
         {"type":"paragraph","text":"(g) Creditors process: Any expropriation, attachment, sequestration, distress or execution affects any asset of an Obligor and is not discharged within 15 Business Days."},
         {"type":"paragraph","text":"(h) Unlawfulness: It is or becomes unlawful for an Obligor to perform any of its obligations under the Finance Documents."},
         {"type":"paragraph","text":"(i) Material adverse change: Any event or circumstance occurs which the Bank reasonably believes has or is reasonably likely to have a Material Adverse Effect."},
         {"type":"paragraph","text":"On and at any time after the occurrence of an Event of Default which is continuing, the Bank may by notice to the Borrower cancel the Commitment and/or declare that all or part of the Loans, together with accrued interest and all other amounts accrued under the Finance Documents, be immediately due and payable."}
       ]}',
       'Credit limit demo v3 bank-grade import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'CREDIT-LIMIT-EOD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

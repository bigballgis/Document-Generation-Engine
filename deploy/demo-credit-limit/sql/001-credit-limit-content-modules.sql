-- Credit limit demo content modules — real foreign-bank-letter grade (v2)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'c1000001-0001-4000-8000-000000000001', 'CREDIT-LIMIT-STD', 'CORP', 'Credit Limit Standard Clause', 'Standard credit limit confirmation operative terms (LMA-aligned)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'CREDIT-LIMIT-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c2000001-0001-4000-8000-000000000001', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This letter confirms the aggregate credit limits made available to the Borrower and constitutes a Finance Document. Drawings under the Facility remain subject to compliance with the financial covenants, representations, undertakings and events of default set out in the facility agreement and the Finance Documents."},
         {"type":"paragraph","text":"Availability: Limits are reviewed annually and may be reduced, cancelled or withdrawn by the Bank on reasonable written notice if there is a material adverse change in the financial condition of the Borrower or the Group, or if the Borrower breaches any covenant, representation or undertaking under the Finance Documents."},
         {"type":"paragraph","text":"Utilisation: Each utilisation must be made by notice to the Agent no later than 10:00 a.m. (London time) on the proposed utilisation date, in the form of a Utilisation Request, specifying the amount, currency and proposed utilisation date. Each utilisation is subject to the satisfaction of the conditions precedent set out above and the representations being true and correct on the utilisation date."},
         {"type":"paragraph","text":"Interest: Interest accrues on each drawn amount at the Reference Rate plus the Margin, compounded in arrears in accordance with market convention. Interest periods default to one, three or six months as selected by the Borrower in the Utilisation Request. Default interest applies at the rate specified in the facility agreement on any overdue amount."},
         {"type":"paragraph","text":"Repayment and Prepayment: The Borrower shall repay the Facility in the instalments and on the dates set out in the Repayment Schedule, subject to mandatory prepayment on the occurrence of any mandatory prepayment event (including change of control, illegality and asset disposal proceeds). Voluntary prepayment is permitted on three Business Days notice, together with any Break Costs."},
         {"type":"paragraph","text":"Assignments and Transfers: The Bank may assign, transfer or sub-participate all or any part of its rights and obligations under the Finance Documents to any Eligible Institution, subject to compliance with know-your-customer requirements. The Borrower may not assign or transfer any of its rights under the Finance Documents without the prior written consent of the Bank."},
         {"type":"paragraph","text":"Confidentiality: Each party shall keep confidential all Confidential Information disclosed under the Finance Documents, save for permitted disclosures to professional advisers, regulators, assignees and as required by law, court order or applicable regulation."}
       ]}',
       'Credit limit demo v2 real-bank import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'CREDIT-LIMIT-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT 'c1000001-0001-4000-8000-000000000002', 'CREDIT-LIMIT-COV', 'CORP', 'Credit Limit Covenants', 'Financial covenant summary and reporting requirements', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'CREDIT-LIMIT-COV' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT 'c2000001-0001-4000-8000-000000000002', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"Financial covenants: The Borrower shall procure that the Group complies with the following financial covenants, to be tested quarterly as at the last day of each Financial Quarter based on the consolidated financial statements of the Group:"},
         {"type":"paragraph","text":"(a) Leverage Ratio: Total Financial Indebtedness to EBITDA not to exceed the maximum leverage ratio set out in the Facility Particulars, calculated on a trailing twelve-month basis;"},
         {"type":"paragraph","text":"(b) Interest Cover Ratio: EBITDA to net finance costs to be not less than the minimum interest cover ratio set out in the Facility Particulars, calculated on a trailing twelve-month basis;"},
         {"type":"paragraph","text":"(c) Tangible Net Worth: Tangible net worth of the Group to be not less than the minimum tangible net worth set out in the Facility Particulars, calculated as at each Financial Quarter end."},
         {"type":"paragraph","text":"Reporting: The Borrower shall deliver to the Bank (i) audited annual consolidated financial statements within 120 days of each financial year end; (ii) unaudited quarterly management accounts within 45 days of each Financial Quarter end; and (iii) a Compliance Certificate signed by two directors or authorised signatories of the Borrower confirming compliance with the financial covenants, within 30 days of each Financial Quarter end."},
         {"type":"paragraph","text":"Equity Cure: Where a breach of any financial covenant occurs, the Borrower may, with the consent of the Bank, cure such breach by an equity injection, provided that no more than two equity cures may be applied in any rolling four-quarter period and the cured compliance is recalculated in accordance with the facility agreement."},
         {"type":"paragraph","text":"Material events: The Borrower shall notify the Bank promptly (and in any event within five Business Days) of any litigation, claim or proceedings exceeding the materiality threshold, any change of control, any breach of covenant or representation, any event of default or any event which would reasonably be expected to have a Material Adverse Effect."}
       ]}',
       'Credit limit demo v2 real-bank import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'CREDIT-LIMIT-COV' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0');

COMMIT;

-- Wealth demo content modules — real foreign-bank-letter grade (v2)
BEGIN;

INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT '81000001-0001-4000-8000-000000000001', 'WEALTH-STATEMENT-STD', 'WEALTH', 'Wealth Statement Intro', 'Private wealth statement introductory clause and disclaimers', '[]', '10000003', '10000003'
WHERE NOT EXISTS (SELECT 1 FROM content_module WHERE module_code = 'WEALTH-STATEMENT-STD' AND deleted_at IS NULL);

INSERT INTO content_module_version (id, module_id, semantic_version, review_state, lifecycle_state, content_structure_json, change_description, created_by, updated_by)
SELECT '82000001-0001-4000-8000-000000000001', cm.id, '2.0.0', 'APPROVED', 'ACTIVE',
       '{"blocks":[
         {"type":"paragraph","text":"This statement summarises the holdings held in custody on your behalf as at the statement date. It is provided for information purposes and does not constitute investment advice, an offer to sell or a solicitation of an offer to buy any securities or investment products. All values are expressed in the base currency unless otherwise indicated."},
         {"type":"paragraph","text":"Foreign currency holdings are converted at the prevailing exchange rate on the statement date for display purposes only. Actual settlement amounts may differ from the values shown. Corporate actions, pending trades and accrued income items may not be fully reflected until settlement completes."},
         {"type":"paragraph","text":"Market prices are sourced from recognised exchanges and pricing services as at close of business on the statement date. For unlisted or illiquid instruments, valuations are based on the most recent available independent appraisal or, in the absence of such appraisal, the last known transaction price."},
         {"type":"paragraph","text":"Income received during the statement period includes dividends, coupon payments, interest distributions and any other income credited to your account, net of any withholding tax or charges applied at source."},
         {"type":"paragraph","text":"The unrealised gain or loss shown represents the difference between the cost basis and the current market value of your holdings. Past performance is not indicative of future results. The value of investments and income from them may go down as well as up, and you may not get back the amount originally invested."},
         {"type":"paragraph","text":"Custody: The assets shown in this statement are held by the custodian named above in accordance with the custody agreement governing your account. The custodian acts as agent and is not responsible for any loss arising from the default of any third party."},
         {"type":"paragraph","text":"Tax: You are responsible for any tax arising from your investments. It is your responsibility to disclose any income, gains or losses to the relevant tax authority in accordance with the laws applicable to you. If you are in any doubt about your tax position, you should consult a professional tax adviser."},
         {"type":"paragraph","text":"Regulation: Meridian Private Wealth is authorised and regulated by the Financial Conduct Authority. This statement is issued in accordance with the FCA Client Assets Sourcebook (CASS) and the Conduct of Business Sourcebook (COBS)."}
       ]}',
       'Wealth demo v2 real-bank import', '10000003', '10000007'
FROM content_module cm WHERE cm.module_code = 'WEALTH-STATEMENT-STD' AND cm.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM content_module_version v WHERE v.module_id = cm.id AND v.semantic_version = '2.0.0');

COMMIT;

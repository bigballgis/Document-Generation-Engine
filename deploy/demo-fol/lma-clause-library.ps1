# LMA-style wholesale FOL clause paragraph library for executive demo (100+ page target).
# Dot-sourced by generate-clauses-sql.ps1 - aligned to LMA Investment Grade / mandate letter structure.

function Build-DefinitionsParagraphs {
    $paragraphs = [System.Collections.Generic.List[string]]::new()
    $paragraphs.Add(
        'Clause 1 (Definitions and Interpretation). In this Facility Offer Letter and the Finance Documents, ' +
        'capitalised terms have the meanings given in this Clause 1 or in Schedule 1 (Facility Particulars) unless ' +
        'the context otherwise requires. This document follows the structure and drafting conventions of the LMA ' +
        'recommended form for a multicurrency term and revolving facilities agreement (investment grade), as ' +
        'published by the Loan Market Association and commonly used by international wholesale banks.'
    )
    $paragraphs.Add(
        'Clause 1.2 (Construction). Clause, schedule and paragraph headings are for ease of reference only and do ' +
        'not affect interpretation. Unless a contrary indication appears, references to Clauses and Schedules are ' +
        'to clauses and schedules of this letter; references to a "Finance Document" include this letter, any Fee ' +
        'Letter, the Utilisation Request, Security Documents and any other document designated as such by the Agent.'
    )

    $definedTerms = @(
        @('Acceptable Bank', 'a bank or financial institution which has a rating for its long-term unsecured and non-credit-enhanced debt obligations of at least A- or equivalent from Standard & Poor''s or Fitch or A3 from Moody''s, or is otherwise approved by the Agent (acting on the instructions of the Majority Lenders).')
        @('Accession Letter', 'a document substantially in the form set out in Schedule 6 (Security Principles) or otherwise agreed by the Agent under which an Additional Guarantor accedes to the Finance Documents.')
        @('Additional Guarantor', 'any person which becomes a Guarantor after the Signing Date in accordance with Clause 17 (Guarantee and Indemnity).')
        @('Affiliate', 'in relation to any person, a Subsidiary of that person or a Holding Company of that person or any other Subsidiary of that Holding Company.')
        @('Agent', 'Meridian Global Banking Corporation acting in its capacity as facility agent for the Finance Parties under the Finance Documents, and includes any successor or replacement appointed in accordance with Clause 24 (The Agent and the Arrangers).')
        @('Agent''s Spot Rate of Exchange', 'the Agent''s spot rate of exchange for the purchase of the relevant currency with the Reference Currency in the London foreign exchange market at or about 11:00 a.m. on a particular day.')
        @('Arranger', 'Meridian Global Banking Corporation in its capacity as mandated lead arranger and bookrunner, together with any other arranger appointed under a mandate letter.')
        @('Availability Period', 'the period from and including the Signing Date to and including the date falling six months thereafter (or such later date as the Majority Lenders may agree).')
        @('Borrower', 'Pacific Rim Holdings Ltd., a company incorporated in the Cayman Islands with company registration number CR-284719, whose registered office is at PO Box 309, Ugland House, Grand Cayman KY1-1104.')
        @('Break Costs', 'the amount (if any) by which the interest amount which a Lender would have received in respect of a Loan or Unpaid Sum for the period from the date of receipt of prepayment to the last day of the current Interest Period exceeds the amount which that Lender would be able to obtain by placing an amount equal to the amount prepaid on deposit with a leading bank in the relevant interbank market.')
        @('Business Day', 'a day (other than a Saturday or Sunday) on which banks are open for general business in London and, if a payment is to be made in USD, New York, and if a payment is to be made in EUR, the Trans-European Automated Real-time Gross settlement Express Transfer system is open.')
        @('Commitment', 'in relation to a Lender, the amount set opposite its name in Schedule 1 (Facility Particulars) or in a Transfer Certificate or Assignment Agreement, as that amount may be reduced, cancelled or increased in accordance with this letter.')
        @('Compliance Certificate', 'a certificate substantially in the form set out in Schedule 8 (Form of Compliance Certificate) signed by two directors or authorised signatories of the Borrower confirming compliance with the Financial Covenants.')
        @('Confidential Information', 'all information relating to the business, assets, affairs, customers, suppliers, plans, intentions or market opportunities of any member of the Group which is disclosed by any member of the Group to any Finance Party pursuant to or in connection with any Finance Document.')
        @('Default Rate', 'the rate specified in Clause 8.3 (Default interest) per annum over the rate otherwise applicable to the overdue amount.')
        @('Disruption Event', 'either: (a) a material disruption to those payment or communications systems or to those financial markets which are, in each case, required to operate in order for payments to be made in connection with the Facility; or (b) the occurrence of any other event which results in a disruption to the wholesale funding markets generally.')
        @('Eligible Institution', 'any Lender or other bank, financial institution, trust, fund or other entity selected by the Borrower and which, in each case, is not a Sanctioned Person.')
        @('Event of Default', 'any event or circumstance specified as such in Clause 22 (Events of Default).')
        @('Facility', 'the term loan facility made available under this letter as described in Clause 2 (The Facility) and Schedule 1 (Facility Particulars).')
        @('Fee Letter', 'any letter or letters dated on or about the date of this letter between the Agent and/or the Arranger and the Borrower setting out any fees referred to in Clause 11 (Fees) or Schedule 5 (Fees).')
        @('Finance Document', 'this letter, any Fee Letter, the Utilisation Request, any Security Document, any Accession Letter and any other document designated as such by the Agent and the Borrower.')
        @('Finance Party', 'the Agent, the Arrangers or a Lender.')
        @('Financial Covenant', 'any covenant set out in Clause 20 (Financial Covenants) or Schedule 1 (Facility Particulars).')
        @('Financial Indebtedness', 'any indebtedness for or in respect of moneys borrowed or raised, any redeemable preference shares, any acceptance under acceptance credit facilities, any note purchase facility, any finance lease, any debt instrument issued by way of capital market issuance and any counter-indemnity obligation in respect of a guarantee, indemnity, bond, standby or documentary letter of credit or any other instrument issued by a bank or financial institution.')
        @('Financial Quarter', 'the period of three months ending on 31 March, 30 June, 30 September and 31 December in each year.')
        @('Group', 'the Borrower and each of its Subsidiaries for the time being.')
        @('Guarantor', 'each person listed as a guarantor in Schedule 1 (Facility Particulars) and each Additional Guarantor.')
        @('Holding Company', 'in relation to a person, any other person in respect of which it is a Subsidiary.')
        @('Interest Period', 'each period determined in accordance with Clause 9 (Interest Periods).')
        @('Lender', 'a bank or financial institution which has become a Party as a lender in accordance with Clause 23 (Changes to the Lenders), and includes its successors in title.')
        @('Loan', 'a loan made or to be made under the Facility or the principal amount outstanding for the time being of that loan.')
        @('Majority Lenders', 'a Lender or Lenders whose Commitments aggregate more than 66 and two-thirds per cent. of the Total Commitments (or, if the Total Commitments have been reduced to zero, aggregated more than 66 and two-thirds per cent. of the Total Commitments immediately prior to the reduction).')
        @('Margin', 'the percentage rate per annum specified as such in Schedule 1 (Facility Particulars) or determined in accordance with the pricing grid set out therein.')
        @('Material Adverse Effect', 'a material adverse effect on the business, operations, property, condition (financial or otherwise) or prospects of the Group taken as a whole or the ability of any Obligor to perform its payment obligations under the Finance Documents.')
        @('Obligor', 'the Borrower or a Guarantor.')
        @('Party', 'a party to this letter from time to time.')
        @('Permitted Security', 'any Security permitted under Clause 21 (General Undertakings) and disclosed to the Agent prior to the Signing Date.')
        @('Purpose', 'the purposes set out in Clause 3 (Purpose).')
        @('Quarterly Financial Statement Date', 'each date on which quarterly financial statements are required to be delivered under Clause 19 (Information Undertakings).')
        @('Reference Rate', 'in relation to a Loan denominated in USD, Term SOFR (or Compounded SOFR where applicable) as specified in Schedule 13 (Reference Rate Terms) and as further defined in the LMA recommended form risk-free rate provisions.')
        @('Repayment Date', 'each date specified as such in Schedule 1 (Facility Particulars) or Clause 6 (Repayment).')
        @('Sanctions', 'any economic or financial sanctions or trade embargoes imposed, administered or enforced from time to time by the United States (including OFAC), the United Nations, the European Union, the United Kingdom or any other relevant sanctions authority.')
        @('Sanctioned Person', 'any person listed in any Sanctions-related list of designated persons maintained by OFAC, the US Department of State, the United Nations, the European Union, His Majesty''s Treasury or any other relevant sanctions authority.')
        @('Security', 'a mortgage, charge, pledge, lien or other security interest securing any obligation of any person or any other agreement or arrangement having a similar effect.')
        @('Security Document', 'each document listed in Schedule 6 (Security Principles) and any document creating or evidencing Security over the assets of an Obligor.')
        @('Signing Date', 'the date on which this letter is signed by all parties or such later date as the Agent may notify to the Lenders and the Borrower.')
        @('Subsidiary', 'an entity of which a person has direct or indirect control or owns directly or indirectly more than 50 per cent. of the voting share capital or equivalent.')
        @('Tax', 'any tax, levy, impost, duty or other charge or withholding of a similar nature (including any penalty or interest payable in connection with any failure to pay or any delay in paying any of the same).')
        @('Total Commitments', 'the aggregate of the Commitments, being USD 250,000,000 (two hundred and fifty million United States dollars) or such lower amount as may be cancelled in accordance with this letter.')
        @('Transfer Certificate', 'a certificate substantially in the form set out in Schedule 4 (Form of Transfer Certificate) or any other form agreed by the Agent.')
        @('Unpaid Sum', 'any sum due and payable but unpaid by an Obligor under the Finance Documents.')
        @('Utilisation', 'a borrowing of a Loan.')
        @('Utilisation Date', 'the date on which a Utilisation is to be made, being a Business Day within the Availability Period.')
        @('Utilisation Request', 'a notice substantially in the form set out in Schedule 4 (Form of Utilisation Request).')
        @('VAT', 'value added tax as provided for in the Value Added Tax Act 1994 and any tax of a similar nature.')
    )

    $subIndex = 1
    foreach ($term in $definedTerms) {
        $paragraphs.Add("1.$subIndex `"$($term[0])`" means $($term[1])")
        $paragraphs.Add(
            "For the avoidance of doubt, references to `"$($term[0])`" throughout the Finance Documents shall be construed in accordance with market practice under the LMA recommended form, " +
            "including any supplemental definitions set out in Schedule 1 (Facility Particulars) and the term sheet dated on or about the Signing Date."
        )
        $subIndex++
    }

    $supplementalTerms = @(
        @('Benchmark Transition Event', 'any event giving rise to a transition from Term SOFR to a successor rate in accordance with the LMA recommended form risk-free rate provisions and Schedule 13 (Reference Rate Terms).')
        @('Compounded Rate', 'a rate determined by reference to the aggregation of a series of daily non-cumulative compounded risk-free rates over an Interest Period, calculated in accordance with Schedule 14 (Daily Non-Cumulative Compounded RFR Rate).')
        @('Hedge Counterparty', 'an Eligible Institution with which the Borrower enters into hedging arrangements in respect of interest rate exposure under the Facility.')
        @('Intercreditor Agreement', 'any agreement between the Finance Parties and other creditors regulating relative priorities, enforcement proceeds and standstill periods.')
        @('Qualifying Lender', 'a Lender which is entitled to receive payments under the Finance Documents without a Tax Deduction or with a reduced Tax Deduction.')
        @('Screen Rate', 'the Term SOFR reference rate for the relevant Interest Period displayed on the applicable Reuters or other agreed screen page.')
        @('Tax Deduction', 'a deduction or withholding for or on account of Tax from a payment under a Finance Document.')
        @('Transaction Security', 'the Security created or expressed to be created in favour of the Security Agent pursuant to the Security Documents.')
    )
    foreach ($term in $supplementalTerms) {
        $paragraphs.Add("1.$subIndex `"$($term[0])`" means $($term[1])")
        $paragraphs.Add(
            "The definition of `"$($term[0])`" is included for completeness and aligns with the LMA Investment Grade Facilities Agreement (Term SOFR version) " +
            "as commonly negotiated between international banks and corporate borrowers in the wholesale lending market."
        )
        $subIndex++
    }

    $paragraphs.Add(
        'Clause 1.3 (Third party rights). A person who is not a Party has no right under the Contracts (Rights of Third Parties) Act 1999 to enforce or enjoy the benefit of any term of this letter, except that each Finance Party may enforce its rights under the Finance Documents in accordance with their terms.'
    )
    return ,@($paragraphs)
}

function Build-LmaSectionParagraphs {
    param(
        [string]$ClauseNumber,
        [string]$Title,
        [string[]]$SubClauseTopics
    )

    $paragraphs = [System.Collections.Generic.List[string]]::new()
    $paragraphs.Add(
        "Clause $ClauseNumber ($Title). This Clause sets out the terms relating to $Title in accordance with the LMA recommended form for investment grade syndicated facilities and market practice for wholesale international bank financings documented under English law."
    )

    $subIndex = 1
    foreach ($topic in $SubClauseTopics) {
        $paragraphs.Add(
            "$ClauseNumber.$subIndex $topic. The Borrower and each Finance Party acknowledge that the provisions of this sub-clause reflect standard LMA drafting for investment grade borrowers and are subject to facility-specific negotiation recorded in the term sheet and Schedule 1 (Facility Particulars)."
        )
        $paragraphs.Add(
            "Without limiting the generality of sub-clause $ClauseNumber.$subIndex, each Obligor shall procure that all actions required to give full effect to $topic are completed within the timeframes and according to the procedures customary for a syndicated term facility of this nature, including delivery of certificates, opinions and evidence reasonably requested by the Agent acting on the instructions of the Majority Lenders."
        )
        $paragraphs.Add(
            "The Agent may, after consultation with the Borrower where practicable, issue guidance or process notes to the Lenders regarding operational matters arising under sub-clause $ClauseNumber.$subIndex, provided that such guidance does not amend the legal obligations of the Obligors without a formal amendment to the Finance Documents."
        )
        $paragraphs.Add(
            "In negotiating sub-clause $ClauseNumber.$subIndex, the Borrower and the Lenders typically consider counsel comments against the LMA recommended form, internal credit policy requirements, " +
            "and any side letter or term sheet provisions agreed at mandate stage. Any deviation from the LMA baseline is recorded in the execution version of the Finance Documents."
        )
        $paragraphs.Add(
            "Operational application: where $topic applies to the Facility offered to Pacific Rim Holdings Ltd., the Agent will coordinate with the Borrower and the Security Agent to ensure " +
            "timelines, document versions and evidence standards match those set out in Schedule 2 (Conditions Precedent) and Meridian Global Banking Corporation wholesale credit operations standards."
        )
        $subIndex++
    }

    $genericTopics = @(
        'Relationship with Finance Documents and side letters'
        'Amendments, waivers and consents under agency mechanics'
        'Conflicts with term sheet and order of precedence'
        'Regulatory change and compliance with PRA/FCA requirements'
    )
    foreach ($topic in $genericTopics) {
        $paragraphs.Add(
            "$ClauseNumber.$subIndex $topic. This supplemental sub-clause mirrors additional paragraphs commonly inserted in signed LMA-based facility offer letters and facility agreements " +
            "to address operational and regulatory matters not captured in the short-form term sheet."
        )
        $paragraphs.Add(
            "Each Finance Party acknowledges that $topic may require further detail in the long-form facility agreement and security documents to be entered into on or before the first Utilisation Date."
        )
        $subIndex++
    }

    $paragraphs.Add(
        "Clause ${ClauseNumber}A (Miscellaneous). The provisions of this Clause $ClauseNumber shall survive any utilisation, prepayment or cancellation of the Facility and remain binding on the Obligors until all amounts outstanding under the Finance Documents have been irrevocably paid and discharged in full and the Commitments have been cancelled."
    )
    return ,@($paragraphs)
}

function Build-ScheduleParagraphs {
    param(
        [string]$ScheduleNumber,
        [string]$Title,
        [string[]]$ExtraParagraphs = @()
    )

    $paragraphs = [System.Collections.Generic.List[string]]::new()
    $paragraphs.Add(
        "$Title forms an integral part of this Facility Offer Letter and shall be read together with the operative clauses. " +
        "Capitalised terms used in this Schedule have the meanings given in Clause 1 (Definitions and Interpretation) unless the context otherwise requires."
    )
    foreach ($extra in $ExtraParagraphs) {
        $paragraphs.Add($extra)
    }

    switch ($ScheduleNumber) {
        '1' { Add-Schedule1FacilityParticulars $paragraphs }
        '2' { Add-Schedule2ConditionsPrecedent $paragraphs }
        '3' { Add-Schedule3Representations $paragraphs }
        '4' { Add-Schedule4UtilisationRequest $paragraphs }
        '5' { Add-Schedule5Fees $paragraphs }
        '6' { Add-Schedule6SecurityPrinciples $paragraphs }
        Default {
            throw "Unsupported schedule number for LMA clause library: $ScheduleNumber"
        }
    }

    return ,@($paragraphs)
}

function Add-Schedule1FacilityParticulars([System.Collections.Generic.List[string]]$paragraphs) {
    $paragraphs.Add(
        'Part 1 - Parties. Borrower: Pacific Rim Holdings Ltd. (Cayman Islands company number CR-284719). Original Guarantor: Pacific Rim Group Holdings Ltd. ' +
        'Agent and Security Agent: Meridian Global Banking Corporation. Mandated Lead Arranger: Meridian Global Banking Corporation. Original Lenders: Meridian Global Banking Corporation, ' +
        'Continental Capital Bank AG and Pacific Trade Finance Ltd., with Commitments as set out in the lender matrix forming part of this Schedule.'
    )
    $paragraphs.Add(
        'Part 2 - Facility overview. Facility type: multicurrency term loan facility. Total Commitments: USD 250,000,000. Reference Currency: USD. ' +
        'Availability Period: from the Signing Date to the date falling six months thereafter. Final Maturity Date: 1 July 2031. Purpose: refinancing of existing Financial Indebtedness ' +
        'and general corporate purposes of the Group, excluding any Sanctioned activity.'
    )
    $paragraphs.Add(
        'Part 3 - Pricing. Margin: 1.85 per cent. per annum over Term SOFR (or the applicable Compounded Rate fallback under Schedule 13 (Reference Rate Terms)). ' +
        'Commitment fee: 35 per cent. of the applicable Margin on the undrawn and uncancelled amount of each Lender''s Commitment. Default interest: Margin plus 2.00 per cent. per annum. ' +
        'A pricing grid linked to Net Leverage Ratio shall apply after the first Compliance Certificate Date, with step-ups and step-downs as agreed in the Fee Letter.'
    )
    $paragraphs.Add(
        'Part 4 - Amortisation. Subject to Clause 6 (Repayment), the Borrower shall repay the Loans in semi-annual instalments of USD 12,500,000 on each Repayment Date commencing ' +
        'on 1 January 2028, with the balance of the Loans (together with accrued interest and all other amounts then due) payable in full on the Final Maturity Date. ' +
        'Voluntary prepayments under Clause 7 (Prepayment and Cancellation) shall be applied against remaining amortisation instalments in inverse order of maturity unless the Agent and the Borrower otherwise agree.'
    )
    $paragraphs.Add(
        'Part 5 - Financial covenants (summary). The Borrower shall ensure that: (a) Net Leverage Ratio does not exceed 3.50:1.00; (b) Interest Cover Ratio is not less than 3.00:1.00; and ' +
        '(c) Minimum Liquidity is not less than USD 25,000,000, each tested quarterly by reference to the Compliance Certificate. Equity cure rights are limited to two cures in any rolling four-quarter period.'
    )
    $paragraphs.Add(
        'Part 6 - Lender matrix. Commitments are several. Meridian Global Banking Corporation: USD 100,000,000. Continental Capital Bank AG: USD 85,000,000. Pacific Trade Finance Ltd.: USD 65,000,000. ' +
        'Any Transfer Certificate or Assignment Agreement shall update this Part without requiring a formal amendment of this letter, provided the Total Commitments remain unchanged except as cancelled or increased in accordance with Clause 2 (The Facility).'
    )
    $paragraphs.Add(
        'Part 7 - Business Day and payment conventions. Payments in USD shall be made for value on a day on which banks are open in London and New York. Interest Periods of one, three or six months ' +
        'may be selected by the Borrower, subject to market disruption and Agent consent for non-standard periods. Broken funding compensation and Break Costs apply as set out in Clause 8 (Interest) and Clause 7 (Prepayment and Cancellation).'
    )
    $paragraphs.Add(
        'Part 8 - Notices for Facility Particulars. Any update to contact details for Utilisation Requests, Compliance Certificates or mandatory prepayment notices shall be given under Clause 29 (Notices) ' +
        'and shall not take effect until acknowledged by the Agent. The Agent may rely on the most recent notice without further enquiry as to authority of the sender.'
    )
    foreach ($supplement in @(
            'The Facility Particulars prevail over any inconsistent description in a term sheet dated on or about the Signing Date, except where the Fee Letter expressly states otherwise.'
            'Currency equivalents for non-USD utilisations shall be determined using the Agent''s Spot Rate of Exchange two Business Days before the Utilisation Date.'
            'Accordion increases up to an additional USD 50,000,000 may be offered on terms substantially identical to the original Facility, subject to Majority Lender consent and updated Commitments in this Schedule.'
            'ESG KPI margin ratchet mechanics, if activated by written notice of the Agent, shall adjust Margin by not more than 5 basis points in either direction based on independently verified KPIs.'
            'Any stamp duty, registration or similar Tax arising on the Finance Documents in England and Wales or the Cayman Islands shall be for the account of the Borrower.'
        )) {
        $paragraphs.Add($supplement)
    }
}

function Add-Schedule2ConditionsPrecedent([System.Collections.Generic.List[string]]$paragraphs) {
    $paragraphs.Add(
        'Part 1 - Corporate documents. A certified copy of the constitutional documents of each Obligor; a certified copy of board resolutions approving the Finance Documents and authorising specified signatories; ' +
        'a specimen signature list; and a certificate of an authorised signatory confirming that borrowing or guaranteeing the Total Commitments would not cause any borrowing, guaranteeing or similar limit binding on that Obligor to be exceeded.'
    )
    $paragraphs.Add(
        'Part 2 - Finance Documents. Executed counterparts of this letter, each Fee Letter, each Security Document required on or before first Utilisation, any Accession Letter for an Additional Guarantor, ' +
        'and evidence that all conditions to the effectiveness of each such document (other than conditions expressed to be satisfied on Utilisation) have been satisfied or waived by the Agent.'
    )
    $paragraphs.Add(
        'Part 3 - Legal opinions. A legal opinion of English counsel to the Agent as to English law enforceability; a legal opinion of Cayman Islands counsel as to capacity and due execution of the Borrower and Guarantor; ' +
        'and, if any Security Document is governed by another law, a capacity and enforceability opinion from counsel acceptable to the Agent in that jurisdiction.'
    )
    $paragraphs.Add(
        'Part 4 - KYC, sanctions and AML. Completion of all know-your-customer, anti-money-laundering and sanctions screening procedures reasonably required by each Finance Party in relation to each Obligor and each person ' +
        'purporting to act on behalf of an Obligor, including delivery of beneficial ownership information and evidence that no Obligor is a Sanctioned Person.'
    )
    $paragraphs.Add(
        'Part 5 - Financial and commercial evidence. The Group''s latest audited consolidated financial statements; the most recent quarterly management accounts; a structure chart; ' +
        'evidence of insurance covering material assets with the Security Agent''s interest noted where required; and a funds flow statement for the first Utilisation.'
    )
    $paragraphs.Add(
        'Part 6 - Conditions subsequent. Within 30 days of the Signing Date (or such later date as the Agent may agree), the Borrower shall deliver evidence of registration or perfection of each Security Document ' +
        'required to be registered, and any other condition subsequent listed in the closing checklist agreed with the Agent.'
    )
    $paragraphs.Add(
        'Part 7 - Repeating conditions for each Utilisation. On the date of each Utilisation Request and each Utilisation Date: (a) no Default is continuing or would result from the proposed Utilisation; ' +
        '(b) the Repeating Representations are true in all material respects; (c) the Utilisation is within the Availability Period and does not exceed available Commitments; and (d) the Agent has received a duly completed Utilisation Request.'
    )
    foreach ($item in @(
            'The Agent shall notify the Borrower and the Lenders promptly upon satisfaction or waiver of the conditions in Parts 1 to 5.'
            'Any waiver of a condition precedent may be given by the Agent acting on the instructions of the Majority Lenders, or by all Lenders where the Finance Documents so require.'
            'Documents delivered under this Schedule shall be in English or accompanied by a certified English translation.'
            'Electronic copies are acceptable if originally signed wet-ink or e-signed counterparts follow within five Business Days where a Finance Party so requires.'
            'Failure to satisfy a condition subsequent by its long-stop date shall constitute an Event of Default unless remedied or waived in accordance with Clause 22 (Events of Default).'
            'The Borrower shall pay all reasonable legal fees of the Agent''s counsel incurred in reviewing Conditions Precedent deliveries, as set out in the Fee Letter.'
        )) {
        $paragraphs.Add($item)
    }
}

function Add-Schedule3Representations([System.Collections.Generic.List[string]]$paragraphs) {
    $paragraphs.Add(
        'Part 1 - Status. Each Obligor is a limited liability company, duly incorporated and validly existing under the law of its jurisdiction of incorporation, and has the power to own its assets and carry on its business as it is being conducted.'
    )
    $paragraphs.Add(
        'Part 2 - Binding obligations. The obligations expressed to be assumed by each Obligor in each Finance Document to which it is a party are legal, valid, binding and enforceable obligations, subject to general principles of equity and insolvency laws affecting creditors'' rights generally.'
    )
    $paragraphs.Add(
        'Part 3 - Non-conflict. The entry into and performance by each Obligor of the Finance Documents do not and will not conflict with any law or regulation applicable to it, its constitutional documents, or any agreement or instrument binding upon it or any of its assets, in a manner that would reasonably be expected to have a Material Adverse Effect.'
    )
    $paragraphs.Add(
        'Part 4 - Power and authority. Each Obligor has the power to enter into, perform and deliver, and has taken all necessary action to authorise its entry into, performance and delivery of, the Finance Documents to which it is a party and the transactions contemplated by those Finance Documents.'
    )
    $paragraphs.Add(
        'Part 5 - No default. No Event of Default is continuing or might reasonably be expected to result from the making of any Utilisation or the entry into, performance of, or any transaction contemplated by, any Finance Document.'
    )
    $paragraphs.Add(
        'Part 6 - Financial statements. The most recent audited financial statements of the Group were prepared in accordance with IFRS consistently applied and give a true and fair view of the financial condition of the Group as at the date to which they were prepared.'
    )
    $paragraphs.Add(
        'Part 7 - Sanctions and anti-corruption. No Obligor, nor any of its directors, officers or, to its knowledge, employees or agents, is a Sanctioned Person. Each Obligor has conducted its businesses in compliance with applicable anti-bribery and anti-corruption laws and has instituted and maintained policies and procedures designed to promote such compliance.'
    )
    $paragraphs.Add(
        'Part 8 - Pari passu ranking. The payment obligations of each Obligor under the Finance Documents rank at least pari passu with the claims of all its other unsecured and unsubordinated creditors, except for obligations mandatorily preferred by law applying to companies generally.'
    )
    foreach ($item in @(
            'Repeating Representations are made on the Signing Date, on each Utilisation Date and on the first day of each Interest Period.'
            'Any representation that is qualified by reference to Material Adverse Effect or materiality shall be construed accordingly for enforcement purposes.'
            'The Borrower shall promptly notify the Agent if any Representation ceases to be true in any material respect.'
            'Litigation disclosures above a USD 5,000,000 threshold shall be updated in each Compliance Certificate.'
            'Tax representations include that each Obligor is resident for Tax purposes only in the jurisdiction of its incorporation, unless otherwise disclosed to the Agent in writing before the Signing Date.'
            'Environmental representations confirm that each Obligor has complied with applicable environmental laws where failure to do so would reasonably be expected to have a Material Adverse Effect.'
        )) {
        $paragraphs.Add($item)
    }
}

function Add-Schedule4UtilisationRequest([System.Collections.Generic.List[string]]$paragraphs) {
    $paragraphs.Add(
        'Part 1 - Form. Each Utilisation Request shall be substantially in the form of this Schedule, addressed to the Agent, signed by an authorised signatory of the Borrower, and delivered not later than 10:00 a.m. (London time) ' +
        'three Business Days before the proposed Utilisation Date (or such shorter period as the Agent may agree).'
    )
    $paragraphs.Add(
        'Part 2 - Mandatory content. The Utilisation Request shall specify: (a) the proposed Utilisation Date; (b) the currency and amount of the proposed Loan; (c) the Interest Period; (d) the account to which funds are to be credited; ' +
        'and (e) confirmation that each condition in Clause 4 (Conditions of Utilisation) is satisfied on the date of the Utilisation Request.'
    )
    $paragraphs.Add(
        'Part 3 - Minimum amounts. Unless the Agent otherwise agrees, each Loan shall be in a minimum amount of USD 5,000,000 (or its equivalent) and an integral multiple of USD 1,000,000 (or its equivalent).'
    )
    $paragraphs.Add(
        'Part 4 - Irrevocability. A Utilisation Request is irrevocable once delivered, except with the prior written consent of the Agent (acting on the instructions of the Majority Lenders where a change would alter Commitments or pricing).'
    )
    $paragraphs.Add(
        'Part 5 - Agent notification. Promptly upon receipt of a duly completed Utilisation Request, the Agent shall notify each Lender of the amount of its participation and the account details for funding.'
    )
    $paragraphs.Add(
        'Part 6 - Transfer Certificate cross-reference. Where a Lender has transferred part of its Commitment, Utilisation Requests shall be processed by reference to Commitments recorded in the Agent''s register, which prevails over outdated matrices in Schedule 1 for operational purposes.'
    )
    foreach ($item in @(
            'If a Utilisation Request is defective, the Agent shall notify the Borrower of the defect as soon as practicable and the Borrower may submit a corrected request.'
            'Currency conversions for multicurrency utilisations shall use the Agent''s Spot Rate of Exchange as defined in Clause 1.'
            'The Borrower represents, on delivery of each Utilisation Request, that the proceeds will be applied solely for a Purpose permitted under Clause 3 (Purpose).'
            'Same-day utilisations are not available unless all Lenders consent in writing.'
            'A Utilisation Request may not be delivered during a market disruption period notified under Clause 10 (Changes to the Calculation of Interest) without Agent guidance on rate setting.'
            'Electronic delivery by encrypted email to the Agent''s published utilisation desk address constitutes delivery for the purposes of this Schedule.'
        )) {
        $paragraphs.Add($item)
    }
}

function Add-Schedule5Fees([System.Collections.Generic.List[string]]$paragraphs) {
    $paragraphs.Add(
        'Part 1 - Arrangement fee. The Borrower shall pay to the Arranger an arrangement fee in the amount and on the dates set out in the Fee Letter between the Arranger and the Borrower dated on or about the Signing Date.'
    )
    $paragraphs.Add(
        'Part 2 - Commitment fee. The Borrower shall pay to the Agent (for the account of each Lender) a commitment fee computed at the rate specified in Schedule 1 (Facility Particulars) on that Lender''s Available Commitment, ' +
        'accruing from the Signing Date and payable quarterly in arrear and on the date the relevant Commitment is cancelled in full.'
    )
    $paragraphs.Add(
        'Part 3 - Agency fee. The Borrower shall pay to the Agent an agency fee in the amount and at the times agreed in the Agency Fee Letter. The fee is payable for the Agent''s own account and is non-refundable.'
    )
    $paragraphs.Add(
        'Part 4 - Security agency fee. Where a Security Agent is appointed, the Borrower shall pay the security agency fee set out in the relevant Fee Letter, together with reasonable out-of-pocket expenses properly incurred in connection with the Security Documents.'
    )
    $paragraphs.Add(
        'Part 5 - Utilisation and front-end fees. Any utilisation fee or front-end fee agreed in a Fee Letter shall be payable on the first Utilisation Date (or such other date specified in that Fee Letter) and shall not be refundable whether or not the Facility is utilised in full.'
    )
    $paragraphs.Add(
        'Part 6 - Fee Letter prevalence. If there is any conflict between this Schedule and a Fee Letter, the Fee Letter prevails in respect of the quantum and timing of the relevant fee, without prejudice to the Borrower''s payment obligations under Clause 11 (Fees).'
    )
    foreach ($item in @(
            'Fees are exclusive of any VAT, which shall be paid in addition if chargeable.'
            'The Agent may deduct unpaid fees from amounts otherwise payable to the Borrower under the Finance Documents.'
            'No fee sharing with the Borrower or any Obligor is permitted except as disclosed in a Fee Letter.'
            'Amendment and waiver fees, if any, shall be agreed at the time of the relevant consent request.'
            'Breakage and funding indemnities under Clause 7 and Clause 14 are in addition to the fees in this Schedule.'
            'Evidence of fee payment may be required as a condition subsequent under Schedule 2 (Conditions Precedent).'
        )) {
        $paragraphs.Add($item)
    }
}

function Add-Schedule6SecurityPrinciples([System.Collections.Generic.List[string]]$paragraphs) {
    $paragraphs.Add(
        'Part 1 - Transaction Security. The Obligors shall grant Transaction Security in favour of the Security Agent (as trustee or agent for the Finance Parties) comprising: (a) a charge over shares in the Borrower; ' +
        '(b) fixed and floating charges or equivalent over material bank accounts; (c) assignments of material receivables; and (d) such other Security as the Majority Lenders reasonably require to implement the agreed security package.'
    )
    $paragraphs.Add(
        'Part 2 - Perfection. Each Obligor shall execute all documents and do all acts reasonably required to perfect and protect the Transaction Security, including registrations, notices to account banks and delivery of share certificates and blank stock transfer forms.'
    )
    $paragraphs.Add(
        'Part 3 - Accession. An Additional Guarantor shall accede by delivering an Accession Letter substantially in the form agreed with the Agent, together with the corporate documents and legal opinions listed in Schedule 2 (Conditions Precedent) as applicable to that Additional Guarantor.'
    )
    $paragraphs.Add(
        'Part 4 - Intercreditor. If any Intercreditor Agreement is entered into, the ranking, enforcement standstill and turnover provisions of that agreement shall regulate the relative rights of the Finance Parties and other creditors party to it.'
    )
    $paragraphs.Add(
        'Part 5 - Release. Subject to the Finance Documents, Transaction Security shall be released when all amounts outstanding under the Finance Documents have been irrevocably paid and discharged in full and the Commitments have been cancelled.'
    )
    $paragraphs.Add(
        'Part 6 - Further assurance. Each Obligor shall, at its own expense, promptly execute any further document and take any further action that the Security Agent may reasonably require to give effect to the security principles in this Schedule.'
    )
    foreach ($item in @(
            'Security Documents shall be governed by the law most appropriate for the asset location, as advised by counsel to the Agent.'
            'Account charges shall require blocked or controlled account mechanics only to the extent specified in the agreed term sheet.'
            'Insurance proceeds in respect of secured assets shall be applied in accordance with Clause 7 (Prepayment and Cancellation) unless reinvested as Permitted Security exceptions allow.'
            'The Security Agent may appoint delegates and co-agents in accordance with Clause 24 (The Agent and the Arrangers).'
            'Whitewash, financial assistance and corporate benefit analysis shall be completed before any English or Cayman share security is taken.'
            'No Obligor shall create Permitted Security except as expressly allowed under Clause 21 (General Undertakings).'
        )) {
        $paragraphs.Add($item)
    }
}

function Get-LmaSectionCatalog {
    # Anchor ids are derived at generation time via Resolve-FolHybridAnchorId (fol-catalog-shared.ps1).
    return @(
        @{ Code = 'MOD-FOL-SEC-01'; Name = '1. Definitions and Interpretation'; Builder = { Build-DefinitionsParagraphs } }
        @{ Code = 'MOD-FOL-SEC-02'; Name = '2. The Facility'; Topics = @(
            'Commitments and several obligations of Lenders'
            'Availability and cancellation of undrawn Commitments'
            'Increase confirmations and accordion mechanics'
            'Swingline and ancillary facilities where applicable'
        ) }
        @{ Code = 'MOD-FOL-SEC-03'; Name = '3. Purpose'; Topics = @(
            'Permitted application of proceeds'
            'Refinancing of existing indebtedness'
            'General corporate and working capital purposes'
            'Prohibited uses including sanctions-sensitive activities'
        ) }
        @{ Code = 'MOD-FOL-SEC-04'; Name = '4. Conditions of Utilisation'; Topics = @(
            'Initial conditions precedent to first Utilisation'
            'Repeating conditions for each Utilisation'
            'Legal opinions and corporate authorisations'
            'KYC/AML and sanctions compliance evidence'
        ) }
        @{ Code = 'MOD-FOL-SEC-05'; Name = '5. Utilisation'; Topics = @(
            'Delivery of Utilisation Requests'
            'Minimum amount and integral multiples'
            'Currency and account selection'
            'Agent notification to Lenders'
        ) }
        @{ Code = 'MOD-FOL-SEC-06'; Name = '6. Repayment'; Topics = @(
            'Scheduled amortisation instalments'
            'Final maturity bullet repayment'
            'Currency of repayment'
            'Application of prepayments'
        ) }
        @{ Code = 'MOD-FOL-SEC-07'; Name = '7. Prepayment and Cancellation'; Topics = @(
            'Voluntary prepayment notice and minimum amounts'
            'Mandatory prepayment on change of control'
            'Illegality and asset sale prepayment events'
            'Break Costs and prepayment fees'
        ) }
        @{ Code = 'MOD-FOL-SEC-08'; Name = '8. Interest'; Topics = @(
            'Calculation of interest on Loans'
            'Reference Rate plus Margin'
            'Default interest on overdue amounts'
            'Risk-free rate fallback and benchmark transition'
        ) }
        @{ Code = 'MOD-FOL-SEC-09'; Name = '9. Interest Periods'; Topics = @(
            'Selection and length of Interest Periods'
            'Non-Business Days and convention adjustments'
            'Broken period compensation'
            'Agent coordination of rate fixing'
        ) }
        @{ Code = 'MOD-FOL-SEC-10'; Name = '10. Changes to the Calculation of Interest'; Topics = @(
            'Market disruption events'
            'Cost of funds fallback (if applicable)'
            'Screen Rate unavailability'
            'Central bank rate fallback'
        ) }
        @{ Code = 'MOD-FOL-SEC-11'; Name = '11. Fees'; Topics = @(
            'Arrangement and underwriting fees'
            'Commitment fees on undrawn amounts'
            'Agency and security agency fees'
            'Utilisation and front-end fees'
        ) }
        @{ Code = 'MOD-FOL-SEC-12'; Name = '12. Tax Gross-Up and Indemnities'; Topics = @(
            'Tax gross-up obligations of Obligors'
            'Tax indemnities in favour of Finance Parties'
            'FATCA and CRS withholding'
            'Stamp duty and registration costs'
        ) }
        @{ Code = 'MOD-FOL-SEC-13'; Name = '13. Increased Costs'; Topics = @(
            'Compensation for increased costs of capital'
            'Change in law affecting Lenders'
            'Mitigation and replacement of Lender'
            'Claims certification procedure'
        ) }
        @{ Code = 'MOD-FOL-SEC-14'; Name = '14. Other Indemnities'; Topics = @(
            'Indemnity for third party claims'
            'Environmental and litigation indemnities'
            'Currency indemnity'
            'Enforcement costs indemnity'
        ) }
        @{ Code = 'MOD-FOL-SEC-15'; Name = '15. Mitigation by the Lenders'; Topics = @(
            'Lender mitigation steps'
            'Designation of different lending office'
            'Replacement of non-consenting Lender'
            'Costs of mitigation'
        ) }
        @{ Code = 'MOD-FOL-SEC-16'; Name = '16. Costs and Expenses'; Topics = @(
            'Upfront transaction costs'
            'Amendment and waiver costs'
            'Enforcement and preservation costs'
            'Registration and perfection costs'
        ) }
        @{ Code = 'MOD-FOL-SEC-17'; Name = '17. Guarantee and Indemnity'; Topics = @(
            'Guarantee of Obligor obligations'
            'Continuing guarantee and reinstatement'
            'Additional Guarantor accession'
            'Waiver of defences'
        ) }
        @{ Code = 'MOD-FOL-SEC-18'; Name = '18. Representations'; Topics = @(
            'Status and due authorisation'
            'No default and no litigation'
            'Financial statements fair presentation'
            'Sanctions and anti-corruption compliance'
        ) }
        @{ Code = 'MOD-FOL-SEC-19'; Name = '19. Information Undertakings'; Topics = @(
            'Annual audited financial statements'
            'Quarterly management accounts'
            'Compliance certificates and budget'
            'Notification of default and material events'
        ) }
        @{ Code = 'MOD-FOL-SEC-20'; Name = '20. Financial Covenants'; Topics = @(
            'Net leverage ratio covenant'
            'Interest cover ratio covenant'
            'Minimum liquidity covenant'
            'Equity cure and testing mechanics'
        ) }
        @{ Code = 'MOD-FOL-SEC-21'; Name = '21. General Undertakings'; Topics = @(
            'Negative pledge and permitted security'
            'Disposals and acquisitions restrictions'
            'Merger and change of business'
            'Pari passu ranking and ranking undertakings'
        ) }
        @{ Code = 'MOD-FOL-SEC-22'; Name = '22. Events of Default'; Topics = @(
            'Non-payment Events of Default'
            'Financial covenant breaches'
            'Cross-default and insolvency events'
            'Acceleration and enforcement remedies'
        ) }
        @{ Code = 'MOD-FOL-SEC-23'; Name = '23. Changes to the Lenders'; Topics = @(
            'Assignments and transfers by novation'
            'Disclosure of information to assignees'
            'Transfer certificates and procedures'
            'Pro rata sharing and sub-participations'
        ) }
        @{ Code = 'MOD-FOL-SEC-24'; Name = '24. The Agent and the Arrangers'; Topics = @(
            'Appointment and authority of the Agent'
            'Reliance on instructions and certificates'
            'Arranger role and no fiduciary duties'
            'Replacement of the Agent'
        ) }
        @{ Code = 'MOD-FOL-SEC-25'; Name = '25. Conduct of Business by the Finance Parties'; Topics = @(
            'No reliance on Finance Parties advice'
            'Arm''s length relationship'
            'Conflicts of interest disclosure'
            'Reference bank quotations'
        ) }
        @{ Code = 'MOD-FOL-SEC-26'; Name = '26. Sharing among the Finance Parties'; Topics = @(
            'Pro rata sharing of recoveries'
            'Redistribution of payments'
            'Agent redistribution account'
            'Turnover of recoveries'
        ) }
        @{ Code = 'MOD-FOL-SEC-27'; Name = '27. Payment Mechanics'; Topics = @(
            'Payments to Agent distribution account'
            'Currency of account and payment'
            'Partial payments and set-off'
            'Disruption to payment systems'
        ) }
        @{ Code = 'MOD-FOL-SEC-28'; Name = '28. Set-Off'; Topics = @(
            'Lender set-off rights'
            'Netting of matured obligations'
            'Notification of set-off'
            'Continuing obligations after set-off'
        ) }
        @{ Code = 'MOD-FOL-SEC-29'; Name = '29. Notices'; Topics = @(
            'Form and delivery of notices'
            'Electronic communication'
            'Change of address'
            'Deemed receipt timing'
        ) }
        @{ Code = 'MOD-FOL-SEC-30'; Name = '30. Governing Law and Jurisdiction'; Topics = @(
            'English law governing law clause'
            'Exclusive jurisdiction of English courts'
            'Service of process and process agent'
            'Waiver of immunity'
        ) }
        @{ Code = 'MOD-FOL-SCH-01'; Name = 'Schedule 1 - Facility Particulars'; Schedule = '1'; Extra = @(
            'Borrower: Pacific Rim Holdings Ltd. Total Commitments: USD 250,000,000. Margin: 185 bps over Term SOFR. Maturity: 1 July 2031.'
            'The syndicated lender matrix, tranche structure, amortisation profile and pricing grid in this Schedule are binding commercial terms of the Facility Offer Letter and prevail for Commitments, Margin and repayment calculations.'
        ) }
        @{ Code = 'MOD-FOL-SCH-02'; Name = 'Schedule 2 - Conditions Precedent'; Schedule = '2' }
        @{ Code = 'MOD-FOL-SCH-03'; Name = 'Schedule 3 - Representations'; Schedule = '3' }
        @{ Code = 'MOD-FOL-SCH-04'; Name = 'Schedule 4 - Form of Utilisation Request'; Schedule = '4' }
        @{ Code = 'MOD-FOL-SCH-05'; Name = 'Schedule 5 - Fees'; Schedule = '5' }
        @{ Code = 'MOD-FOL-SCH-06'; Name = 'Schedule 6 - Security Principles'; Schedule = '6'; Extra = @(
            'Security over shares in the Borrower, account charges, receivables assignments and intercreditor arrangements form the agreed Transaction Security package and shall be documented in the Security Documents listed with the Agent.'
        ) }
    )
}


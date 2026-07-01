# Generates deploy/demo-fol/sql/001-fol-standard-clauses.sql from embedded clause catalogue.
# Run: .\deploy\demo-fol\generate-clauses-sql.ps1

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$OutFile = Join-Path $Root 'sql/001-fol-standard-clauses.sql'
$Null = New-Item -ItemType Directory -Force -Path (Split-Path $OutFile)

function Escape-Sql([string]$Value) {
    return $Value.Replace("'", "''")
}

function Escape-Json([string]$Value) {
    return $Value.Replace('\', '\\').Replace('"', '\"').Replace("`n", '\n').Replace("`r", '')
}

function Clause-Json([string[]]$Paragraphs) {
    $blocks = $Paragraphs | ForEach-Object { "{`"type`":`"paragraph`",`"text`":`"$(Escape-Json $_)`"}" }
    return "{`"blocks`":[$($blocks -join ',')]}"
}

$Clauses = @(
    @{ Code = 'MOD-FOL-SEC-01'; Name = '1. Definitions and Interpretation'; Ref = 'FOL_SEC_01'; Paragraphs = @(
        'In this Facility Offer Letter and the Finance Documents, unless the context otherwise requires, capitalised terms have the meanings given in Schedule 1 (Definitions) or, if not defined therein, the meanings given in the LMA Single Currency Term Facility Agreement (as published by the Loan Market Association).'
        'References to a "Finance Document" include this letter, the Fee Letter, the Utilisation Request, any Security Document, and any other document designated as such by the Agent and the Borrower. Clause headings are for convenience only and do not affect interpretation.'
        'The interpretation principles in Clause 1.2 (Construction) of the Recommended Form apply as if set out in full herein, including rules on inclusive meaning of "assets", time of day references to London time, and the meaning of "know your customer" or similar checks under applicable law.'
    ) },
    @{ Code = 'MOD-FOL-SEC-02'; Name = '2. The Facility'; Ref = 'FOL_SEC_02'; Paragraphs = @(
        'Subject to the terms of the Finance Documents, the Lenders agree to make available to the Borrower a multicurrency term loan facility in an aggregate amount equal to the Total Commitments, as set out in the Facility Particulars in Schedule 1.'
        'The Facility is made available for the Purpose and on the terms that each Lender''s Commitment shall be several and not joint. No Lender shall be obliged to fund more than its Commitment except as provided following an assignment or transfer in accordance with Clause 23 (Changes to the Lenders).'
    ) },
    @{ Code = 'MOD-FOL-SEC-03'; Name = '3. Purpose'; Ref = 'FOL_SEC_03'; Paragraphs = @(
        'The Borrower shall apply all amounts borrowed under the Facility towards: (a) refinancing existing external indebtedness of the Group maturing within twelve months of the Signing Date; (b) capital expenditure approved by the board of the Borrower; and (c) general corporate and working capital purposes of the Group.'
        'No part of the Facility may be applied directly or indirectly towards the acquisition of equity in any entity if such acquisition would result in a Change of Control, or towards any purpose that would cause a violation of applicable sanctions, anti-bribery, or anti-money laundering laws.'
    ) },
    @{ Code = 'MOD-FOL-SEC-08'; Name = '8. Interest'; Ref = 'FOL_SEC_08'; Paragraphs = @(
        'The rate of interest on each Loan for each Interest Period is the percentage rate per annum which is the aggregate of the applicable Reference Rate and the Margin.'
        'If the Borrower fails to pay any amount payable by it under a Finance Document on its due date, interest shall accrue on the overdue amount at a rate per annum which is the sum of the rate applicable to the relevant Loan plus 2.00 per cent. per annum, from the due date to the date of actual payment (both before and after judgment).'
    ) },
    @{ Code = 'MOD-FOL-SEC-20'; Name = '20. Financial Covenants'; Ref = 'FOL_SEC_20'; Paragraphs = @(
        'The Borrower shall ensure that, tested quarterly on each Quarterly Financial Statement Date on a rolling twelve-month basis: (a) the Consolidated Net Leverage Ratio shall not exceed 3.50:1.00; and (b) the Consolidated Interest Cover Ratio shall not be less than 3.00:1.00.'
        'The Borrower shall also maintain Minimum Liquidity of not less than USD 10,000,000 (or equivalent in other currencies) at all times, measured as unrestricted cash and cash equivalents held in accounts with Acceptable Banks.'
    ) },
    @{ Code = 'MOD-FOL-SEC-22'; Name = '22. Events of Default'; Ref = 'FOL_SEC_22'; Paragraphs = @(
        'Each of the events set out in this Clause 22 constitutes an Event of Default, including: non-payment of any amount due under a Finance Document (subject to three Business Days'' grace for administrative error); breach of financial covenant not remedied within any applicable cure period; material misrepresentation; cross-default in respect of Financial Indebtedness exceeding USD 5,000,000 (or equivalent); insolvency events; unlawfulness; and cessation of business.'
        'Upon the occurrence of an Event of Default which is continuing, the Agent may, and shall if so directed by the Majority Lenders, by notice to the Borrower cancel the Total Commitments and/or declare all or part of the outstanding Loans, together with accrued interest and all other amounts accrued or outstanding under the Finance Documents, immediately due and payable.'
    ) },
    @{ Code = 'MOD-FOL-SEC-30'; Name = '30. Governing Law and Jurisdiction'; Ref = 'FOL_SEC_30'; Paragraphs = @(
        'This Agreement and any non-contractual obligations arising out of or in connection with it are governed by English law.'
        'The courts of England have exclusive jurisdiction to settle any dispute arising out of or in connection with this Agreement (including a dispute regarding the existence, validity, or termination of this Agreement). The parties irrevocably submit to the jurisdiction of the English courts.'
    ) }
)

# Remaining standard sections — professional placeholder paragraphs suitable for executive walkthrough
$StandardSections = @(
    @{ Code = 'MOD-FOL-SEC-04'; Name = '4. Conditions of Utilisation'; Ref = 'FOL_SEC_04' }
    @{ Code = 'MOD-FOL-SEC-05'; Name = '5. Utilisation'; Ref = 'FOL_SEC_05' }
    @{ Code = 'MOD-FOL-SEC-06'; Name = '6. Repayment'; Ref = 'FOL_SEC_06' }
    @{ Code = 'MOD-FOL-SEC-07'; Name = '7. Prepayment and Cancellation'; Ref = 'FOL_SEC_07' }
    @{ Code = 'MOD-FOL-SEC-09'; Name = '9. Interest Periods'; Ref = 'FOL_SEC_09' }
    @{ Code = 'MOD-FOL-SEC-10'; Name = '10. Changes to the Calculation of Interest'; Ref = 'FOL_SEC_10' }
    @{ Code = 'MOD-FOL-SEC-11'; Name = '11. Fees'; Ref = 'FOL_SEC_11' }
    @{ Code = 'MOD-FOL-SEC-12'; Name = '12. Tax Gross-Up and Indemnities'; Ref = 'FOL_SEC_12' }
    @{ Code = 'MOD-FOL-SEC-13'; Name = '13. Increased Costs'; Ref = 'FOL_SEC_13' }
    @{ Code = 'MOD-FOL-SEC-14'; Name = '14. Other Indemnities'; Ref = 'FOL_SEC_14' }
    @{ Code = 'MOD-FOL-SEC-15'; Name = '15. Mitigation by the Lenders'; Ref = 'FOL_SEC_15' }
    @{ Code = 'MOD-FOL-SEC-16'; Name = '16. Costs and Expenses'; Ref = 'FOL_SEC_16' }
    @{ Code = 'MOD-FOL-SEC-17'; Name = '17. Guarantee and Indemnity'; Ref = 'FOL_SEC_17' }
    @{ Code = 'MOD-FOL-SEC-18'; Name = '18. Representations'; Ref = 'FOL_SEC_18' }
    @{ Code = 'MOD-FOL-SEC-19'; Name = '19. Information Undertakings'; Ref = 'FOL_SEC_19' }
    @{ Code = 'MOD-FOL-SEC-21'; Name = '21. General Undertakings'; Ref = 'FOL_SEC_21' }
    @{ Code = 'MOD-FOL-SEC-23'; Name = '23. Changes to the Lenders'; Ref = 'FOL_SEC_23' }
    @{ Code = 'MOD-FOL-SEC-24'; Name = '24. The Agent and the Arrangers'; Ref = 'FOL_SEC_24' }
    @{ Code = 'MOD-FOL-SEC-25'; Name = '25. Conduct of Business by the Finance Parties'; Ref = 'FOL_SEC_25' }
    @{ Code = 'MOD-FOL-SEC-26'; Name = '26. Sharing among the Finance Parties'; Ref = 'FOL_SEC_26' }
    @{ Code = 'MOD-FOL-SEC-27'; Name = '27. Payment Mechanics'; Ref = 'FOL_SEC_27' }
    @{ Code = 'MOD-FOL-SEC-28'; Name = '28. Set-Off'; Ref = 'FOL_SEC_28' }
    @{ Code = 'MOD-FOL-SEC-29'; Name = '29. Notices'; Ref = 'FOL_SEC_29' }
)

$Schedules = @(
    @{ Code = 'MOD-FOL-SCH-01'; Name = 'Schedule 1 — Facility Particulars'; Ref = 'FOL_SCH_01' }
    @{ Code = 'MOD-FOL-SCH-02'; Name = 'Schedule 2 — Conditions Precedent'; Ref = 'FOL_SCH_02' }
    @{ Code = 'MOD-FOL-SCH-03'; Name = 'Schedule 3 — Representations'; Ref = 'FOL_SCH_03' }
    @{ Code = 'MOD-FOL-SCH-04'; Name = 'Schedule 4 — Form of Utilisation Request'; Ref = 'FOL_SCH_04' }
    @{ Code = 'MOD-FOL-SCH-05'; Name = 'Schedule 5 — Fees'; Ref = 'FOL_SCH_05' }
    @{ Code = 'MOD-FOL-SCH-06'; Name = 'Schedule 6 — Security Principles'; Ref = 'FOL_SCH_06' }
)

foreach ($Section in $StandardSections) {
    if (-not ($Clauses | Where-Object { $_.Code -eq $Section.Code })) {
        $Clauses += @{
            Code = $Section.Code
            Name = $Section.Name
            Ref = $Section.Ref
            Paragraphs = @(
                "$($Section.Name) — standard wholesale-bank language as agreed with counsel and aligned to the LMA recommended form for term facilities."
                'This demonstration clause is approved for management preview. Production packs replace this body with facility-specific negotiated text, defined terms cross-references, and worked examples spanning multiple pages.'
            )
        }
    }
}

foreach ($Schedule in $Schedules) {
    if ($Schedule.Ref -eq 'FOL_SCH_01') {
        $Clauses += @{
            Code = $Schedule.Code
            Name = $Schedule.Name
            Ref = $Schedule.Ref
            Paragraphs = @(
                'Schedule 1 (Facility Particulars) sets out the commercial terms for the Facility made available to ${borrowerLegalName} in an aggregate principal amount of ${facilityAmount} ${facilityCurrency}.'
                'The Facility matures on ${maturityDate} and bears interest at ${marginBps} basis points per annum over ${referenceRate}. The Agent is ${agentBank}.'
                'Where ${includeSyndication} applies, the syndicate is led by ${leadArranger} with commitments allocated among Lenders as set out in the lender matrix.'
                'Tables, defined terms, and annexed forms in this schedule typically account for a substantial portion of the overall page count in a signed documentation set.'
            )
        }
    } else {
        $Clauses += @{
            Code = $Schedule.Code
            Name = $Schedule.Name
            Ref = $Schedule.Ref
            Paragraphs = @(
                "$($Schedule.Name) forms an integral part of the Facility Offer Letter and shall be completed with facility-specific commercial terms prior to signing."
                'Tables, defined terms, and annexed forms in this schedule typically account for a substantial portion of the overall page count in a signed documentation set.'
            )
        }
    }
}

$Lines = New-Object System.Collections.Generic.List[string]
$Lines.Add('-- Generated by deploy/demo-fol/generate-clauses-sql.ps1')
$Lines.Add('-- Meridian Global Banking Corporation — wholesale FOL standard clauses (CORP group)')
$Lines.Add('BEGIN;')
$Lines.Add('')

$Index = 1
foreach ($Clause in ($Clauses | Sort-Object { $_.Code })) {
    $ModuleId = ('aaaaaaaa-aaaa-aaaa-aaaa-{0:D12}' -f $Index)
    $VersionId = ('bbbbbbbb-bbbb-bbbb-bbbb-{0:D12}' -f $Index)
    $Json = Clause-Json $Clause.Paragraphs
    $Lines.Add("-- $($Clause.Code) -> $($Clause.Ref)")
    $Lines.Add(@"
INSERT INTO content_module (id, module_code, group_code, name, description, shared_group_codes_json, created_by, updated_by)
SELECT '$ModuleId', '$(Escape-Sql $Clause.Code)', 'CORP', '$(Escape-Sql $Clause.Name)', 'Wholesale FOL standard clause (executive demo)', '[]', '10000003', '10000003'
WHERE NOT EXISTS (
    SELECT 1 FROM content_module WHERE module_code = '$(Escape-Sql $Clause.Code)' AND deleted_at IS NULL
);

INSERT INTO content_module_version (
    id, module_id, semantic_version, review_state, lifecycle_state,
    content_structure_json, change_description, created_by, updated_by
)
SELECT '$VersionId', cm.id, '1.0.0', 'APPROVED', 'ACTIVE',
       '$(Escape-Sql $Json)',
       'Executive demo import', '10000003', '10000007'
FROM content_module cm
WHERE cm.module_code = '$(Escape-Sql $Clause.Code)' AND cm.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM content_module_version v
      WHERE v.module_id = cm.id AND v.semantic_version = '1.0.0'
  );
"@)
    $Lines.Add('')
    $Index++
}

$Lines.Add('COMMIT;')
Set-Content -Path $OutFile -Value ($Lines -join [Environment]::NewLine) -Encoding UTF8
Write-Host "Wrote $($Clauses.Count) clauses -> $OutFile"

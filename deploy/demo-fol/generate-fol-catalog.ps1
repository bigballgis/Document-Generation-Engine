# Generates FOL executive demo catalog JSON artefacts (rich demo variables, rules, rich bindings).
# Run from repo root: .\deploy\demo-fol\generate-fol-catalog.ps1

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigDir = Join-Path $Root 'config'
$RepoRoot = Split-Path -Parent (Split-Path -Parent $Root)
$E2eFixturesDir = Join-Path $RepoRoot 'frontend/e2e/fixtures'

. (Join-Path $Root 'lma-clause-library.ps1')
. (Join-Path $Root 'fol-catalog-shared.ps1')

function Write-JsonFile([string]$Path, [object]$Data) {
    $Null = New-Item -ItemType Directory -Force -Path (Split-Path $Path)
    $json = $Data | ConvertTo-Json -Depth 100 -Compress:$false
    # FOS-W15-4: ConvertTo-Json flattens single-element nested arrays (@(@(...)) → [...]).
    # Re-nest headerRows / footerRows when the first element is a cell object.
    $json = Repair-DemoTableHeaderRowsJson -Json $json
    Set-Content -Path $Path -Value $json -Encoding UTF8
}

function Repair-DemoTableHeaderRowsJson([string]$Json) {
    try {
        $obj = $Json | ConvertFrom-Json
    } catch {
        return $Json
    }
    function Repair-Node($Node) {
        if ($null -eq $Node) { return }
        if ($Node -is [System.Collections.IEnumerable] -and -not ($Node -is [string])) {
            foreach ($child in @($Node)) { Repair-Node $child }
            return
        }
        if ($Node -isnot [pscustomobject] -and $Node -isnot [hashtable]) { return }
        foreach ($propName in @('headerRows', 'footerRows')) {
            $rows = $Node.$propName
            if ($null -eq $rows) { continue }
            $asArray = @($rows)
            if ($asArray.Count -gt 0 -and $null -ne $asArray[0].columnKey) {
                $Node.$propName = @(, $asArray)
            }
        }
        foreach ($p in $Node.PSObject.Properties) {
            Repair-Node $p.Value
        }
    }
    Repair-Node $obj
    return ($obj | ConvertTo-Json -Depth 100 -Compress:$false)
}

function New-Var([string]$Key, [string]$Type, [bool]$Required = $false, $Default = $null, $Enum = $null, [string]$Desc = '') {
    $v = [ordered]@{ key = $Key; type = $Type; required = $Required; description = $Desc }
    if ($null -ne $Default) { $v.defaultValue = [string]$Default }
    if ($null -ne $Enum) { $v.enumValues = $Enum }
    return [pscustomobject]$v
}

# Adds a LIST container plus its child field variables (DRY helper for array-like demo data).
function Add-ListWithFields(
    [System.Collections.Generic.List[object]]$Vars,
    [string]$ListKey,
    [string]$ListDesc,
    [array[]]$Fields
) {
    $Vars.Add((New-Var $ListKey 'LIST' $false $null $null $ListDesc))
    foreach ($f in $Fields) {
        $key = $f[0]; $type = $f[1]; $desc = $f[2]
        $default = if ($f.Count -gt 3) { $f[3] } else { $null }
        $enum = if ($f.Count -gt 4) { $f[4] } else { $null }
        $Vars.Add((New-Var $key $type $false $default $enum $desc))
    }
}

function Build-Variables {
    $vars = [System.Collections.Generic.List[object]]::new()

    # --- Core facility & parties ---
    $core = @(
        New-Var 'borrowerLegalName' 'TEXT' $true $null $null 'Borrower legal name'
        New-Var 'borrowerShortName' 'TEXT' $false 'Pacific Rim Holdings' $null 'Borrower short name'
        New-Var 'borrowerJurisdiction' 'TEXT' $false 'Cayman Islands' $null 'Borrower jurisdiction of incorporation'
        New-Var 'borrowerRegistrationNumber' 'TEXT' $false $null $null 'Borrower company registration number'
        New-Var 'borrowerRegisteredAddress' 'TEXT' $false $null $null 'Borrower registered office address'
        New-Var 'parentCompanyName' 'TEXT' $false $null $null 'Ultimate parent company'
        New-Var 'groupName' 'TEXT' $false 'Pacific Rim Group' $null 'Borrower group name'
        New-Var 'facilityAmount' 'AMOUNT' $true $null $null 'Total commitments'
        New-Var 'facilityCurrency' 'ENUM' $true 'USD' 'USD,EUR,GBP,HKD,SGD,JPY' 'Facility currency'
        New-Var 'facilityType' 'ENUM' $true 'TERM_LOAN' 'TERM_LOAN,REVOLVER,SYNDICATED,BRIDGE' 'Product type'
        New-Var 'facilityPurpose' 'TEXT' $false 'Refinancing and general corporate purposes' $null 'Stated purpose'
        New-Var 'offerDate' 'DATE' $true $null $null 'Offer letter date'
        New-Var 'signingDate' 'DATE' $false $null $null 'Expected signing date'
        New-Var 'firstUtilisationDate' 'DATE' $false $null $null 'Target first utilisation date'
        New-Var 'maturityDate' 'DATE' $true $null $null 'Final maturity date'
        New-Var 'availabilityPeriodEnd' 'DATE' $false $null $null 'Availability period end date'
        New-Var 'agentBank' 'TEXT' $false 'Meridian Global Banking Corporation' $null 'Facility agent'
        New-Var 'securityAgent' 'TEXT' $false 'Meridian Global Banking Corporation' $null 'Security agent / collateral agent'
        New-Var 'leadArranger' 'TEXT' $false 'Meridian Global Banking Corporation' $null 'Lead arranger'
        New-Var 'bookrunner' 'TEXT' $false 'Meridian Global Banking Corporation' $null 'Bookrunner'
        New-Var 'documentationAgent' 'TEXT' $false 'Meridian Global Banking Corporation' $null 'Documentation agent'
        New-Var 'referenceRate' 'ENUM' $false 'SOFR' 'SOFR,SONIA,EURIBOR,HIBOR' 'Reference rate benchmark'
        New-Var 'marginBps' 'NUMBER' $true '185' $null 'Margin over reference rate (bps)'
        New-Var 'commitmentFeeBps' 'NUMBER' $false '35' $null 'Commitment fee (bps p.a.)'
        New-Var 'utilisationFeeThresholdPct' 'NUMBER' $false '33.33' $null 'Utilisation fee threshold (%)'
        New-Var 'utilisationFeeBps' 'NUMBER' $false '10' $null 'Utilisation fee above threshold (bps)'
        New-Var 'defaultInterestMarginPct' 'NUMBER' $false '2.00' $null 'Default interest margin (%)'
        New-Var 'interestPeriodMonths' 'NUMBER' $false '3' $null 'Default interest period (months)'
        New-Var 'dayCountConvention' 'ENUM' $false 'ACT_360' 'ACT_360,ACT_365,30_360' 'Day count convention'
        New-Var 'governingLaw' 'TEXT' $false 'English law' $null 'Governing law'
        New-Var 'jurisdiction' 'TEXT' $false 'Courts of England and Wales' $null 'Exclusive jurisdiction'
        New-Var 'lmaFormReference' 'TEXT' $false 'LMA Single Currency Term Facility (recommended form)' $null 'LMA form reference'
    )
    foreach ($v in $core) { $vars.Add($v) }

    # --- Feature flags (composition + conditional display) ---
    $flags = @(
        'includeSyndication', 'includeHedge', 'includeGuarantee', 'includeESG', 'includeMultiCurrency',
        'includeAccordion', 'includeSwingline', 'includeLcSubline', 'includeSecurityPackage', 'includeIntercreditor',
        'includeUtilisationDate', 'includeCommitmentFee', 'includePrepaymentPenalty', 'includeFinancialCovenants',
        'includeEsgKpi', 'includeGreenUseOfProceeds', 'includeSanctionsRep', 'includeKycCondition'
    )
    foreach ($f in $flags) {
        $vars.Add((New-Var $f 'BOOLEAN' $false 'false' $null "Feature flag: $f"))
    }

    # --- LIST containers + child fields (array-like demo data) ---
    Add-ListWithFields $vars 'pricingTiers' 'Pricing margin grid tiers (loop source)' @(
        ,@('pricingTierLabel', 'TEXT', 'Pricing tier label')
        ,@('pricingTierMarginBps', 'NUMBER', 'Pricing tier margin (bps)')
        ,@('pricingTierFloorBps', 'NUMBER', 'Pricing tier floor (bps)')
    )
    Add-ListWithFields $vars 'milestones' 'Key milestone dates (loop source)' @(
        ,@('milestoneDate', 'DATE', 'Milestone date')
        ,@('milestoneDateDescription', 'TEXT', 'Milestone date description')
    )
    Add-ListWithFields $vars 'parties' 'Transaction parties roster (loop source)' @(
        ,@('partyRole', 'ENUM', 'Party role', $null, 'LENDER,GUARANTOR,SECURITY_PROVIDER,ARRANGER,AGENT')
        ,@('partyLegalName', 'TEXT', 'Party legal name')
        ,@('partyCountry', 'TEXT', 'Party country')
    )
    Add-ListWithFields $vars 'covenants' 'Financial covenant schedule (loop source)' @(
        ,@('covenantName', 'TEXT', 'Financial covenant name')
        ,@('covenantRatio', 'TEXT', 'Financial covenant ratio/threshold')
        ,@('covenantTestFrequency', 'ENUM', 'Covenant test frequency', 'QUARTERLY', 'QUARTERLY,SEMI_ANNUAL,ANNUAL')
    )
    Add-ListWithFields $vars 'securedAssets' 'Secured asset register (loop source)' @(
        ,@('securedAssetClass', 'ENUM', 'Security asset class', $null, 'REAL_ESTATE,SHARES,ACCOUNTS,RECEIVABLES,IP,OTHER')
        ,@('securedAssetDescription', 'TEXT', 'Security asset description')
        ,@('securedAssetJurisdiction', 'TEXT', 'Security asset jurisdiction')
        ,@('securedAssetPerfectionStatus', 'ENUM', 'Security perfection status', 'TO_BE_PERFECTED', 'PERFECTED,TO_BE_PERFECTED,NOT_REQUIRED')
    )
    Add-ListWithFields $vars 'legalReferences' 'Legal clause cross-references (loop source)' @(
        ,@('legalClauseRef', 'TEXT', 'Cross-reference legal clause')
        ,@('legalDefinedTerm', 'TEXT', 'Defined term reference')
    )
    Add-ListWithFields $vars 'lenders' 'Syndicate lender roster (loop source)' @(
        ,@('lenderName', 'TEXT', 'Lender legal name')
        ,@('lenderCommitment', 'AMOUNT', 'Lender commitment')
        ,@('lenderCommitmentPct', 'NUMBER', 'Lender commitment (%)')
        ,@('lenderCountry', 'TEXT', 'Lender country')
    )
    Add-ListWithFields $vars 'tranches' 'Facility tranches (loop source)' @(
        ,@('trancheName', 'TEXT', 'Tranche name')
        ,@('trancheAmount', 'AMOUNT', 'Tranche amount')
        ,@('trancheCurrency', 'ENUM', 'Tranche currency', $null, 'USD,EUR,GBP,HKD')
        ,@('trancheMarginBps', 'NUMBER', 'Tranche margin (bps)')
    )
    Add-ListWithFields $vars 'fees' 'Fee schedule rows (loop source)' @(
        ,@('feeItemDescription', 'TEXT', 'Fee line description')
        ,@('feeItemAmount', 'AMOUNT', 'Fee line amount')
        ,@('feeItemCurrency', 'ENUM', 'Fee line currency', $null, 'USD,EUR,GBP,HKD')
    )
    Add-ListWithFields $vars 'conditionsPrecedent' 'Conditions precedent checklist (loop source)' @(
        ,@('cpItemDescription', 'TEXT', 'CP item description')
        ,@('cpItemStatus', 'ENUM', 'CP item status', $null, 'PENDING,RECEIVED,WAIVED')
        ,@('cpItemResponsibleParty', 'TEXT', 'CP responsible party')
    )
    Add-ListWithFields $vars 'guarantors' 'Guarantor list (loop source)' @(
        ,@('guarantorName', 'TEXT', 'Guarantor legal name')
        ,@('guarantorJurisdiction', 'TEXT', 'Guarantor jurisdiction')
    )
    Add-ListWithFields $vars 'securityPackages' 'Security package list (loop source)' @(
        ,@('securityPackageName', 'TEXT', 'Security package name')
        ,@('securityPackageType', 'TEXT', 'Security package type')
    )
    Add-ListWithFields $vars 'amortisationSchedule' 'Amortisation schedule rows (loop source)' @(
        ,@('amortDate', 'DATE', 'Amortisation payment date')
        ,@('amortPrincipal', 'AMOUNT', 'Amortisation principal')
        ,@('amortBalance', 'AMOUNT', 'Outstanding balance after payment')
    )
    Add-ListWithFields $vars 'facilityParticulars' 'Facility particulars grid rows (loop source)' @(
        ,@('particularLabel', 'TEXT', 'Facility particular label')
        ,@('particularValue', 'TEXT', 'Facility particular value')
    )
    Add-ListWithFields $vars 'hedgeProviders' 'Hedge provider list (loop source)' @(
        ,@('hedgeProviderName', 'TEXT', 'Hedge provider name')
    )
    Add-ListWithFields $vars 'esgKpis' 'ESG KPI targets (loop source)' @(
        ,@('esgKpiName', 'TEXT', 'ESG KPI name')
        ,@('esgKpiTarget', 'TEXT', 'ESG KPI target value')
    )
    Add-ListWithFields $vars 'definedTerms' 'LMA-style defined terms catalogue (loop source)' @(
        ,@('definedTermName', 'TEXT', 'Defined term name')
        ,@('definedTermMeaning', 'TEXT', 'Defined term meaning / cross-reference')
    )
    Add-ListWithFields $vars 'representations' 'Representations & warranties checklist (loop source)' @(
        ,@('representationSummary', 'TEXT', 'Representation summary text')
        ,@('representationApplies', 'BOOLEAN', 'Representation applies (Y/N)', 'true')
    )
    Add-ListWithFields $vars 'infoUndertakings' 'Information undertakings / reporting (loop source)' @(
        ,@('infoUndertakingDescription', 'TEXT', 'Information undertaking description')
        ,@('infoUndertakingDueDays', 'NUMBER', 'Information undertaking due (days after period end)')
    )
    Add-ListWithFields $vars 'eodTriggers' 'Events of default triggers (loop source)' @(
        ,@('eodTriggerDescription', 'TEXT', 'Event of default trigger description')
        ,@('eodTriggerGraceDays', 'NUMBER', 'Event of default grace period (days)')
    )
    Add-ListWithFields $vars 'noticeParties' 'Notice party blocks (loop source)' @(
        ,@('noticePartyName', 'TEXT', 'Notice party name')
        ,@('noticePartyAddress', 'TEXT', 'Notice party address')
        ,@('noticePartyEmail', 'TEXT', 'Notice party email')
        ,@('noticePartyAttention', 'TEXT', 'Notice party attention / contact')
    )
    Add-ListWithFields $vars 'benchmarkFallbacks' 'Benchmark / rate fallback scenarios (loop source)' @(
        ,@('benchmarkFallbackScenario', 'TEXT', 'Benchmark fallback scenario')
        ,@('benchmarkFallbackRate', 'TEXT', 'Fallback rate description')
    )
    Add-ListWithFields $vars 'insurancePolicies' 'Insurance & collateral maintenance (loop source)' @(
        ,@('insurancePolicyType', 'ENUM', 'Insurance policy type', $null, 'PROPERTY,CASUALTY,LIABILITY,KEY_PERSON,OTHER')
        ,@('insurancePolicyMinimumCover', 'AMOUNT', 'Insurance policy minimum cover')
        ,@('insurancePolicyCurrency', 'ENUM', 'Insurance policy currency', 'USD', 'USD,EUR,GBP')
    )
    Add-ListWithFields $vars 'obligors' 'Group obligors / subsidiaries (loop source)' @(
        ,@('obligorLegalName', 'TEXT', 'Obligor legal name')
        ,@('obligorRole', 'ENUM', 'Obligor role', $null, 'BORROWER,GUARANTOR,SECURITY_PROVIDER,HOLDING')
        ,@('obligorJurisdiction', 'TEXT', 'Obligor jurisdiction')
    )
    Add-ListWithFields $vars 'prepaymentEvents' 'Prepayment / break cost events (loop source)' @(
        ,@('prepaymentEventDescription', 'TEXT', 'Prepayment event description')
        ,@('prepaymentEventPenaltyPct', 'NUMBER', 'Prepayment penalty (percent)')
    )

    # --- Tax, withholding & gross-up ---
    $taxFields = @(
        ,@('withholdingTaxRatePct', 'NUMBER', 'Standard withholding tax rate (percent)')
        ,@('grossUpRequired', 'BOOLEAN', 'Tax gross-up required')
        ,@('stampDutyJurisdiction', 'TEXT', 'Stamp duty jurisdiction')
        ,@('stampDutyAmount', 'AMOUNT', 'Estimated stamp duty amount')
        ,@('stampDutyCurrency', 'ENUM', 'Stamp duty currency')
        ,@('vatApplicable', 'BOOLEAN', 'VAT applicable on fees')
        ,@('fatcaStatus', 'ENUM', 'FATCA status of borrower')
        ,@('crsReportingRequired', 'BOOLEAN', 'CRS reporting required')
        ,@('taxIndemnityCap', 'AMOUNT', 'Tax indemnity cap amount')
        ,@('taxIndemnityCurrency', 'ENUM', 'Tax indemnity currency')
    )
    foreach ($tf in $taxFields) {
        $enum = if ($tf[1] -eq 'ENUM') {
            if ($tf[0] -like '*fatca*') { 'COMPLIANT,NON_COMPLIANT,PENDING' } else { 'USD,EUR,GBP' }
        } else { $null }
        $vars.Add((New-Var $tf[0] $tf[1] $false $null $enum $tf[2]))
    }

    # Summary totals
    $vars.Add((New-Var 'totalCommitments' 'AMOUNT' $false $null $null 'Total commitments (summary)'))
    $vars.Add((New-Var 'totalFees' 'AMOUNT' $false $null $null 'Total fees (summary)'))
    $vars.Add((New-Var 'minimumLiquidity' 'AMOUNT' $false '10000000' $null 'Minimum liquidity covenant'))
    $vars.Add((New-Var 'netLeverageMax' 'TEXT' $false '3.50:1.00' $null 'Maximum net leverage ratio'))
    $vars.Add((New-Var 'interestCoverMin' 'TEXT' $false '3.00:1.00' $null 'Minimum interest cover ratio'))

    return ,@($vars)
}

function Build-CompositionRules {
    $rules = @(
        @{ ruleId = 'rule-syndication-clause24'; conditionExpression = '${includeSyndication} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 24); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-hedge-clause11'; conditionExpression = '${includeHedge} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 11); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-guarantee-clause17'; conditionExpression = '${includeGuarantee} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 17); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-esg-clause21'; conditionExpression = '${includeESG} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 21); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-multicurrency-schedule01'; conditionExpression = '${includeMultiCurrency} == true'; targetAnchorId = (Get-FolAnchorIdForScheduleNumber 1); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-accordion-clause07'; conditionExpression = '${includeAccordion} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 7); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-swingline-clause05'; conditionExpression = '${includeSwingline} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 5); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-lc-subline-clause02'; conditionExpression = '${includeLcSubline} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 2); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-security-schedule06'; conditionExpression = '${includeSecurityPackage} == true'; targetAnchorId = (Get-FolAnchorIdForScheduleNumber 6); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-intercreditor-clause26'; conditionExpression = '${includeIntercreditor} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 26); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-prepayment-clause07'; conditionExpression = '${includePrepaymentPenalty} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 7); trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-covenants-clause20'; conditionExpression = '${includeFinancialCovenants} == true'; targetAnchorId = (Get-FolAnchorIdForSectionNumber 20); trueBranchRuleId = ''; falseBranchRuleId = '' }
    )
    return @{ rules = $rules }
}

function Para([object[]]$Children) { return @{ type = 'paragraph'; children = @($Children) } }
function TextRun([string]$Value) { return @{ type = 'textRun'; value = $Value } }
function VarNode([string]$Key) { return @{ type = 'variable'; key = $Key } }
function Emphasis([object[]]$Children) { return @{ type = 'emphasis'; children = @($Children) } }
function Text([string]$Value) { return @{ type = 'text'; value = $Value } }
function LineBreak() { return @{ type = 'lineBreak' } }
function Cond([string]$Expr, [object[]]$Children) { return @{ type = 'conditionBlock'; conditionExpression = $Expr; children = @($Children) } }
function Loop([string]$LoopVar, [object[]]$Children) { return @{ type = 'loopBlock'; loopVariable = $LoopVar; children = @($Children) } }
function Heading([string]$Title) { return @{ type = 'sectionHeading'; children = @(Text $Title) } }
function ModuleRef([string]$RefKey) { return @{ type = 'contentModuleRef'; referenceKey = $RefKey } }

function TableRef([string]$Key, [hashtable]$TableComponent) {
    return @{
        type = 'tableComponentRef'
        tableComponentRef = $Key
        tableComponent = $TableComponent
    }
}

function Build-TableComponents {
    $lenderMatrix = @{
        schemaVersion = '1.0'
        componentKey = 'FOL-TBL-LENDER-MATRIX'
        columnSchema = @(
            @{ columnKey = 'lender'; widthPct = 45 }
            @{ columnKey = 'commitment'; widthPct = 30 }
            @{ columnKey = 'pct'; widthPct = 25 }
        )
        headerRows = @(@(
            @{ columnKey = 'lender'; value = 'Lender' }
            @{ columnKey = 'commitment'; value = 'Commitment' }
            @{ columnKey = 'pct'; value = '% of Total' }
        ))
        repeatHeaderAcrossPages = $true
        loopRow = @{
            loopVariable = 'lenders'
            cells = @(
                @{ columnKey = 'lender'; variableKey = 'lenderName' }
                @{ columnKey = 'commitment'; variableKey = 'lenderCommitment' }
                @{ columnKey = 'pct'; variableKey = 'lenderCommitmentPct' }
            )
        }
        footerRows = @(@(
            @{ columnKey = 'lender'; value = 'Total' }
            @{ columnKey = 'commitment'; variableKey = 'totalCommitments' }
            @{ columnKey = 'pct'; value = '100%' }
        ))
    }

    $feeSchedule = @{
        schemaVersion = '1.0'
        componentKey = 'FOL-TBL-FEE-SCHEDULE'
        columnSchema = @(
            @{ columnKey = 'description'; widthPct = 55 }
            @{ columnKey = 'amount'; widthPct = 25 }
            @{ columnKey = 'currency'; widthPct = 20 }
        )
        headerRows = @(@(
            @{ columnKey = 'description'; value = 'Fee Description' }
            @{ columnKey = 'amount'; value = 'Amount' }
            @{ columnKey = 'currency'; value = 'Currency' }
        ))
        repeatHeaderAcrossPages = $true
        loopRow = @{
            loopVariable = 'fees'
            cells = @(
                @{ columnKey = 'description'; variableKey = 'feeItemDescription' }
                @{ columnKey = 'amount'; variableKey = 'feeItemAmount' }
                @{ columnKey = 'currency'; variableKey = 'feeItemCurrency' }
            )
        }
        footerRows = @(@(
            @{ columnKey = 'description'; value = 'Total Fees' }
            @{ columnKey = 'amount'; variableKey = 'totalFees' }
            @{ columnKey = 'currency'; variableKey = 'facilityCurrency' }
        ))
    }

    $amortisation = @{
        schemaVersion = '1.0'
        componentKey = 'FOL-TBL-AMORTISATION'
        columnSchema = @(
            @{ columnKey = 'date'; widthPct = 35 }
            @{ columnKey = 'principal'; widthPct = 35 }
            @{ columnKey = 'balance'; widthPct = 30 }
        )
        headerRows = @(@(
            @{ columnKey = 'date'; value = 'Payment Date' }
            @{ columnKey = 'principal'; value = 'Principal' }
            @{ columnKey = 'balance'; value = 'Outstanding Balance' }
        ))
        repeatHeaderAcrossPages = $true
        loopRow = @{
            loopVariable = 'amortisationSchedule'
            cells = @(
                @{ columnKey = 'date'; variableKey = 'amortDate' }
                @{ columnKey = 'principal'; variableKey = 'amortPrincipal' }
                @{ columnKey = 'balance'; variableKey = 'amortBalance' }
            )
        }
    }

    $cpChecklist = @{
        schemaVersion = '1.0'
        componentKey = 'FOL-TBL-CP-CHECKLIST'
        columnSchema = @(
            @{ columnKey = 'item'; widthPct = 50 }
            @{ columnKey = 'status'; widthPct = 20 }
            @{ columnKey = 'owner'; widthPct = 30 }
        )
        headerRows = @(@(
            @{ columnKey = 'item'; value = 'Condition Precedent' }
            @{ columnKey = 'status'; value = 'Status' }
            @{ columnKey = 'owner'; value = 'Responsible Party' }
        ))
        repeatHeaderAcrossPages = $true
        loopRow = @{
            loopVariable = 'conditionsPrecedent'
            cells = @(
                @{ columnKey = 'item'; variableKey = 'cpItemDescription' }
                @{ columnKey = 'status'; variableKey = 'cpItemStatus' }
                @{ columnKey = 'owner'; variableKey = 'cpItemResponsibleParty' }
            )
        }
    }

    $particularsGrid = @{
        schemaVersion = '1.0'
        componentKey = 'FOL-TBL-FACILITY-PARTICULARS'
        columnSchema = @(
            @{ columnKey = 'label'; widthPct = 40 }
            @{ columnKey = 'value'; widthPct = 60 }
        )
        headerRows = @(@(
            @{ columnKey = 'label'; value = 'Particular' }
            @{ columnKey = 'value'; value = 'Detail' }
        ))
        repeatHeaderAcrossPages = $true
        loopRow = @{
            loopVariable = 'facilityParticulars'
            cells = @(
                @{ columnKey = 'label'; variableKey = 'particularLabel' }
                @{ columnKey = 'value'; variableKey = 'particularValue' }
            )
        }
    }

    return @{
        LenderMatrix = $lenderMatrix
        FeeSchedule = $feeSchedule
        Amortisation = $amortisation
        CpChecklist = $cpChecklist
        ParticularsGrid = $particularsGrid
    }
}

function Build-DefaultSectionNodes([string]$Title, [string]$RefKey) {
    return @(
        Heading $Title
        Para @(
            TextRun 'Borrower: '; VarNode 'borrowerLegalName'; LineBreak
            TextRun 'Facility: '; VarNode 'facilityAmount'; TextRun ' '; VarNode 'facilityCurrency'; LineBreak
            TextRun 'Agent: '; VarNode 'agentBank'
        )
        ModuleRef $AnchorIdKey
    )
}

function Build-SectionOverlayNodes {
    param(
        [int]$SectionNumber,
        [string]$AnchorId,
        [string]$Title,
        [hashtable]$Tables
    )

    switch ($SectionNumber) {
        1 {
            return @(
                Heading $Title
                Para @(TextRun 'The following defined terms apply to this Facility Offer Letter and the Finance Documents:')
                Loop 'definedTerms' @(
                    Para @(Emphasis @(VarNode 'definedTermName')); TextRun ' means '; VarNode 'definedTermMeaning'
                )
                ModuleRef $AnchorId
            )
        }
        2 {
            return @(
                Heading $Title
                Para @(TextRun 'Total Commitments: '; VarNode 'facilityAmount'; TextRun ' '; VarNode 'facilityCurrency'; TextRun ' ('; VarNode 'facilityType'; TextRun ').')
                Cond '${includeLcSubline} == true' @(
                    Para @(TextRun 'Letter of credit sub-facility available within the Total Commitments as agreed with the Agent.')
                )
                Cond '${includeAccordion} == true' @(
                    Para @(TextRun 'Accordion: additional commitments may be requested subject to lender consent.')
                )
                Loop 'tranches' @(
                    Para @(TextRun 'Tranche '; VarNode 'trancheName'; TextRun ': '; VarNode 'trancheAmount'; TextRun ' '; VarNode 'trancheCurrency')
                )
                ModuleRef $AnchorId
            )
        }
        3 {
            return @(
                Heading $Title
                Para @(TextRun 'Purpose: '; VarNode 'facilityPurpose')
                Cond '${includeGreenUseOfProceeds} == true' @(
                    Para @(TextRun 'Green use of proceeds: eligible green expenditures as certified in accordance with the agreed green loan framework.')
                )
                ModuleRef $AnchorId
            )
        }
        4 {
            return @(
                Heading $Title
                Cond '${includeKycCondition} == true' @(
                    Para @(TextRun 'KYC/AML documentation satisfactory to all Finance Parties is a condition precedent to first Utilisation.')
                )
                TableRef 'FOL-TBL-CP-CHECKLIST' $Tables.CpChecklist
                Loop 'conditionsPrecedent' @(
                    Para @(TextRun '• '; VarNode 'cpItemDescription'; TextRun ' ['; VarNode 'cpItemStatus'; TextRun ']')
                )
                ModuleRef $AnchorId
            )
        }
        5 {
            return @(
                Heading $Title
                Cond '${includeUtilisationDate} == true' @(
                    Para @(TextRun 'Target first utilisation: '; VarNode 'firstUtilisationDate'; TextRun '; Availability period ends '; VarNode 'availabilityPeriodEnd')
                )
                Cond '${includeSwingline} == true' @(
                    Para @(TextRun 'Swingline sub-facility available for same-day utilisations subject to Agent approval.')
                )
                ModuleRef $AnchorId
            )
        }
        6 {
            return @(
                Heading $Title
                Para @(TextRun 'Final maturity: '; VarNode 'maturityDate'; TextRun '; Repayment currency: '; VarNode 'facilityCurrency')
                TableRef 'FOL-TBL-AMORTISATION' $Tables.Amortisation
                Loop 'amortisationSchedule' @(
                    Para @(VarNode 'amortDate'; TextRun ': '; VarNode 'amortPrincipal'; TextRun ' (balance '; VarNode 'amortBalance'; TextRun ')')
                )
                ModuleRef $AnchorId
            )
        }
        7 {
            return @(
                Heading $Title
                Loop 'prepaymentEvents' @(
                    Para @(VarNode 'prepaymentEventDescription'; TextRun ' — penalty '; VarNode 'prepaymentEventPenaltyPct'; TextRun '%')
                )
                Cond '${includePrepaymentPenalty} == true' @(
                    Para @(TextRun 'Voluntary prepayments subject to minimum notice periods and Break Costs compensation.')
                )
                Cond '${includeAccordion} == true' @(
                    Para @(TextRun 'Mandatory prepayment on change of control or asset sale as set out in the term sheet.')
                )
                ModuleRef $AnchorId
            )
        }
        8 {
            return @(
                Heading $Title
                Para @(TextRun 'Interest: '; VarNode 'referenceRate'; TextRun ' + '; VarNode 'marginBps'; TextRun ' bps; Default margin: '; VarNode 'defaultInterestMarginPct'; TextRun '%')
                Loop 'benchmarkFallbacks' @(
                    Para @(TextRun 'Fallback — '; VarNode 'benchmarkFallbackScenario'; TextRun ': '; VarNode 'benchmarkFallbackRate')
                )
                ModuleRef $AnchorId
            )
        }
        9 {
            return @(
                Heading $Title
                Para @(TextRun 'Interest period: '; VarNode 'interestPeriodMonths'; TextRun ' months; Day count: '; VarNode 'dayCountConvention')
                ModuleRef $AnchorId
            )
        }
        10 {
            return @(
                Heading $Title
                Loop 'benchmarkFallbacks' @(
                    Para @(VarNode 'benchmarkFallbackScenario'; TextRun ': '; VarNode 'benchmarkFallbackRate')
                )
                ModuleRef $AnchorId
            )
        }
        11 {
            return @(
                Heading $Title
                Cond '${includeCommitmentFee} == true' @(
                    Para @(TextRun 'Commitment fee: '; VarNode 'commitmentFeeBps'; TextRun ' bps on undrawn commitments.')
                )
                TableRef 'FOL-TBL-FEE-SCHEDULE' $Tables.FeeSchedule
                Loop 'fees' @(
                    Para @(VarNode 'feeItemDescription'; TextRun ': '; VarNode 'feeItemAmount'; TextRun ' '; VarNode 'feeItemCurrency')
                )
                Cond '${includeHedge} == true' @(
                    Loop 'hedgeProviders' @(
                        Para @(TextRun 'Approved hedge provider: '; VarNode 'hedgeProviderName')
                    )
                )
                ModuleRef $AnchorId
            )
        }
        12 {
            return @(
                Heading $Title
                Para @(TextRun 'Withholding tax rate: '; VarNode 'withholdingTaxRatePct'; TextRun '%; Gross-up required: '; VarNode 'grossUpRequired')
                Para @(TextRun 'Stamp duty — '; VarNode 'stampDutyJurisdiction'; TextRun ': '; VarNode 'stampDutyAmount'; TextRun ' '; VarNode 'stampDutyCurrency')
                Para @(TextRun 'FATCA status: '; VarNode 'fatcaStatus'; TextRun '; CRS reporting: '; VarNode 'crsReportingRequired'; TextRun '; VAT on fees: '; VarNode 'vatApplicable')
                Para @(TextRun 'Tax indemnity cap: '; VarNode 'taxIndemnityCap'; TextRun ' '; VarNode 'taxIndemnityCurrency')
                ModuleRef $AnchorId
            )
        }
        13 {
            return @(
                Heading $Title
                Para @(TextRun 'Increased costs claims certified by affected Lenders in accordance with LMA market practice.')
                ModuleRef $AnchorId
            )
        }
        14 {
            return @(
                Heading $Title
                Para @(TextRun 'Indemnities for third-party claims, currency losses and enforcement costs in favour of Finance Parties.')
                ModuleRef $AnchorId
            )
        }
        15 {
            return @(
                Heading $Title
                Para @(TextRun 'Lenders shall use reasonable endeavours to mitigate increased costs and may designate an alternative lending office.')
                ModuleRef $AnchorId
            )
        }
        16 {
            return @(
                Heading $Title
                Para @(TextRun 'Borrower to pay upfront transaction costs, amendment fees and security perfection costs.')
                ModuleRef $AnchorId
            )
        }
        17 {
            return @(
                Heading $Title
                Cond '${includeGuarantee} == true' @(
                    Loop 'guarantors' @(
                        Para @(TextRun 'Guarantor: '; VarNode 'guarantorName'; TextRun ' ('; VarNode 'guarantorJurisdiction'; TextRun ')')
                    )
                )
                ModuleRef $AnchorId
            )
        }
        18 {
            return @(
                Heading $Title
                Loop 'representations' @(
                    Para @(TextRun '• '; VarNode 'representationSummary')
                )
                Cond '${includeSanctionsRep} == true' @(
                    Para @(TextRun 'Sanctions: Borrower confirms compliance with applicable Sanctions regimes.')
                )
                ModuleRef $AnchorId
            )
        }
        19 {
            return @(
                Heading $Title
                Loop 'infoUndertakings' @(
                    Para @(VarNode 'infoUndertakingDescription'; TextRun ' — due within '; VarNode 'infoUndertakingDueDays'; TextRun ' days')
                )
                ModuleRef $AnchorId
            )
        }
        20 {
            return @(
                Heading $Title
                Cond '${includeFinancialCovenants} == true' @(
                    Para @(TextRun 'Net Leverage ≤ '; VarNode 'netLeverageMax'; TextRun '; Interest Cover ≥ '; VarNode 'interestCoverMin'; TextRun '; Minimum Liquidity USD '; VarNode 'minimumLiquidity')
                    Loop 'covenants' @(
                        Para @(VarNode 'covenantName'; TextRun ': '; VarNode 'covenantRatio'; TextRun ' ('; VarNode 'covenantTestFrequency'; TextRun ')')
                    )
                )
                ModuleRef $AnchorId
            )
        }
        21 {
            return @(
                Heading $Title
                Cond '${includeESG} == true' @(
                    Loop 'esgKpis' @(
                        Para @(VarNode 'esgKpiName'; TextRun ': '; VarNode 'esgKpiTarget')
                    )
                )
                Cond '${includeSecurityPackage} == true' @(
                    Loop 'securedAssets' @(
                        Para @(VarNode 'securedAssetDescription'; TextRun ' ('; VarNode 'securedAssetJurisdiction'; TextRun ')')
                    )
                )
                ModuleRef $AnchorId
            )
        }
        22 {
            return @(
                Heading $Title
                Loop 'eodTriggers' @(
                    Para @(VarNode 'eodTriggerDescription'; TextRun ' — grace period '; VarNode 'eodTriggerGraceDays'; TextRun ' days')
                )
                ModuleRef $AnchorId
            )
        }
        23 {
            return @(
                Heading $Title
                Para @(TextRun 'Assignments and transfers subject to LMA Transfer Certificate procedures and yank-a-bank provisions.')
                ModuleRef $AnchorId
            )
        }
        24 {
            return @(
                Heading $Title
                Para @(TextRun 'Facility Agent: '; VarNode 'agentBank'; LineBreak; TextRun 'Lead Arranger: '; VarNode 'leadArranger'; LineBreak; TextRun 'Documentation Agent: '; VarNode 'documentationAgent')
                Cond '${includeSyndication} == true' @(
                    TableRef 'FOL-TBL-LENDER-MATRIX' $Tables.LenderMatrix
                    Loop 'lenders' @(
                        Para @(VarNode 'lenderName'; TextRun ': '; VarNode 'lenderCommitment'; TextRun ' '; VarNode 'facilityCurrency'; TextRun ' ('; VarNode 'lenderCommitmentPct'; TextRun '%) — '; VarNode 'lenderCountry')
                    )
                )
                ModuleRef $AnchorId
            )
        }
        25 {
            return @(
                Heading $Title
                Para @(TextRun 'Finance Parties act at arm''s length; no Finance Party owes fiduciary duties to the Borrower.')
                ModuleRef $AnchorId
            )
        }
        26 {
            return @(
                Heading $Title
                Cond '${includeIntercreditor} == true' @(
                    Para @(TextRun 'Pro rata sharing and turnover of recoveries subject to the Intercreditor Agreement.')
                )
                ModuleRef $AnchorId
            )
        }
        27 {
            return @(
                Heading $Title
                Para @(TextRun 'All payments in '; VarNode 'facilityCurrency'; TextRun ' unless otherwise agreed; payments to Agent distribution account.')
                ModuleRef $AnchorId
            )
        }
        28 {
            return @(
                Heading $Title
                Para @(TextRun 'Each Lender may set off matured obligations owed by an Obligor against amounts due to that Lender.')
                ModuleRef $AnchorId
            )
        }
        29 {
            return @(
                Heading $Title
                Loop 'noticeParties' @(
                    Para @(VarNode 'noticePartyName'; LineBreak; VarNode 'noticePartyAddress'; LineBreak; TextRun 'Email: '; VarNode 'noticePartyEmail'; TextRun ' (Attn: '; VarNode 'noticePartyAttention'; TextRun ')')
                )
                ModuleRef $AnchorId
            )
        }
        30 {
            return @(
                Heading $Title
                Para @(TextRun 'Governing law: '; VarNode 'governingLaw'; LineBreak; TextRun 'Jurisdiction: '; VarNode 'jurisdiction')
                Para @(TextRun 'LMA form reference: '; VarNode 'lmaFormReference')
                ModuleRef $AnchorId
            )
        }
        default {
            return Build-DefaultSectionNodes -Title $Title -RefKey $AnchorId
        }
    }
}

function Build-BindingOverlays {
    $tables = Build-TableComponents
    $bindings = [ordered]@{}

    foreach ($entry in Get-LmaSectionCatalog) {
        $sectionNumber = Get-FolSectionNumber $entry
        if ($sectionNumber -gt 0) {
            $anchorId = Resolve-FolHybridAnchorId $entry
            $nodes = Build-SectionOverlayNodes -SectionNumber $sectionNumber -AnchorId $anchorId -Title $entry.Name -Tables $tables
            $bindings[$anchorId] = @{ schemaVersion = '1.0'; nodes = $nodes }
        }
    }

    $sch01Anchor = Get-FolAnchorIdForScheduleNumber 1
    $sch02Anchor = Get-FolAnchorIdForScheduleNumber 2
    $sch03Anchor = Get-FolAnchorIdForScheduleNumber 3
    $sch04Anchor = Get-FolAnchorIdForScheduleNumber 4
    $sch05Anchor = Get-FolAnchorIdForScheduleNumber 5
    $sch06Anchor = Get-FolAnchorIdForScheduleNumber 6

    $headerNodes = @(
        Para @(
            TextRun 'Date: '; VarNode 'offerDate'; LineBreak
            TextRun 'To: '; VarNode 'borrowerLegalName'; LineBreak
            TextRun 'Re: Facility Offer Letter — '; VarNode 'facilityType'; TextRun ' (Confidential)'; LineBreak
            TextRun 'Reference: '; VarNode 'borrowerRegistrationNumber'
        )
        Para @(
            TextRun 'Dear Sir or Madam,'; LineBreak
            TextRun 'Meridian Global Banking Corporation (the Arranger and Agent) is pleased to set out below the indicative terms and conditions of a senior secured term loan facility to be made available to '
            VarNode 'borrowerLegalName'
            TextRun ' (the Borrower), a company incorporated in '
            VarNode 'borrowerJurisdiction'
            TextRun '. This letter constitutes a mandate to arrange and does not constitute a commitment to lend until Finance Documents are executed.'
        )
        Cond '${includeSyndication} == true' @(
            Para @(TextRun 'Lead Arranger: '; VarNode 'leadArranger'; LineBreak; TextRun 'Bookrunner: '; VarNode 'bookrunner')
        )
        Cond '${includeMultiCurrency} == true' @(
            Para @(TextRun 'This facility supports multi-currency utilisations in: '; VarNode 'facilityCurrency'; TextRun ' and alternate currencies as agreed with the Agent.')
        )
        Cond '${includeESG} == true' @(
            Para @(TextRun 'ESG-linked pricing applies subject to achievement of agreed KPIs as set out in Schedule 1.')
        )
    )

    $summaryNodes = @(
        Heading 'Facility Summary'
        Para @(Emphasis @(Text 'Borrower: ')); VarNode 'borrowerLegalName'
        Para @(Emphasis @(Text 'Group: ')); VarNode 'groupName'
        Para @(Emphasis @(Text 'Total Commitments: ')); VarNode 'facilityAmount'; TextRun ' '; VarNode 'facilityCurrency'
        Para @(Emphasis @(Text 'Purpose: ')); VarNode 'facilityPurpose'
        Para @(Emphasis @(Text 'Margin: ')); VarNode 'marginBps'; TextRun ' bps per annum over '; VarNode 'referenceRate'
        Para @(Emphasis @(Text 'Final Maturity: ')); VarNode 'maturityDate'
        Para @(Emphasis @(Text 'Agent: ')); VarNode 'agentBank'
        Cond '${includeSyndication} == true' @(
            Para @(TextRun 'Lead Arranger: '; VarNode 'leadArranger'; LineBreak; TextRun 'Documentation Agent: '; VarNode 'documentationAgent')
        )
        Cond '${includeHedge} == true' @(
            Para @(TextRun 'Interest rate hedging: Borrower to enter into hedging arrangements with approved providers within 30 days of first utilisation.')
        )
        Cond '${includeAccordion} == true' @(
            Para @(TextRun 'Accordion feature: Borrower may request additional commitments up to an agreed accordion cap subject to lender consent.')
        )
        Cond '${includeCommitmentFee} == true' @(
            Para @(TextRun 'Commitment fee: '; VarNode 'commitmentFeeBps'; TextRun ' bps p.a. on undrawn commitments.')
        )
        Loop 'tranches' @(
            Para @(TextRun 'Tranche: '; VarNode 'trancheName'; TextRun ' — '; VarNode 'trancheAmount'; TextRun ' '; VarNode 'trancheCurrency'; TextRun ' @ '; VarNode 'trancheMarginBps'; TextRun ' bps')
        )
        Cond '${includeFinancialCovenants} == true' @(
            Para @(TextRun 'Financial covenants: Net Leverage ≤ '; VarNode 'netLeverageMax'; TextRun '; Interest Cover ≥ '; VarNode 'interestCoverMin'; TextRun '; Minimum Liquidity USD '; VarNode 'minimumLiquidity')
        )
        TableRef 'FOL-TBL-AMORTISATION' $tables.Amortisation
    )

    $sch01Nodes = @(
        Heading 'Schedule 1 — Facility Particulars'
        Para @(TextRun 'Borrower: '; VarNode 'borrowerLegalName'; LineBreak; TextRun 'Signing Date (target): '; VarNode 'signingDate'; LineBreak; TextRun 'First Utilisation (target): '; VarNode 'firstUtilisationDate')
        TableRef 'FOL-TBL-FACILITY-PARTICULARS' $tables.ParticularsGrid
        Cond '${includeMultiCurrency} == true' @(
            Para @(TextRun 'Multi-currency utilisations permitted in currencies agreed with the Agent from time to time.')
        )
        Loop 'tranches' @(
            Para @(TextRun 'Tranche '; VarNode 'trancheName'; TextRun ': '; VarNode 'trancheAmount'; TextRun ' '; VarNode 'trancheCurrency')
        )
        ModuleRef $sch01Anchor
    )

    $sch02Nodes = @(
        Heading 'Schedule 2 — Conditions Precedent'
        Cond '${includeKycCondition} == true' @(
            Para @(TextRun 'KYC/AML documentation satisfactory to all Finance Parties.')
        )
        TableRef 'FOL-TBL-CP-CHECKLIST' $tables.CpChecklist
        Loop 'conditionsPrecedent' @(
            Para @(TextRun '• '; VarNode 'cpItemDescription'; TextRun ' ['; VarNode 'cpItemStatus'; TextRun '] — '; VarNode 'cpItemResponsibleParty')
        )
        Cond '${includeSyndication} == true' @(
            Para @(TextRun 'Syndication: fee letters and lender accession documents from each Lender.')
        )
        ModuleRef $sch02Anchor
    )

    $sch03Nodes = @(
        Heading 'Schedule 3 — Representations'
        Cond '${includeSanctionsRep} == true' @(
            Para @(TextRun 'Sanctions: no Sanctions apply to the Borrower or its subsidiaries that would prevent utilisation.')
        )
        Cond '${includeGuarantee} == true' @(
            Loop 'guarantors' @(
                Para @(TextRun 'Guarantor representation — '; VarNode 'guarantorName'; TextRun ' ('; VarNode 'guarantorJurisdiction'; TextRun ')')
            )
        )
        ModuleRef $sch03Anchor
    )

    $sch04Nodes = @(
        Heading 'Schedule 4 — Form of Utilisation Request'
        Cond '${includeUtilisationDate} == true' @(
            Para @(TextRun 'Requested utilisation date: '; VarNode 'firstUtilisationDate'; TextRun '; Amount: '; VarNode 'facilityAmount'; TextRun ' '; VarNode 'facilityCurrency')
        )
        ModuleRef $sch04Anchor
    )

    $sch05Nodes = @(
        Heading 'Schedule 5 — Fees'
        Cond '${includeCommitmentFee} == true' @(
            Para @(TextRun 'Commitment fee: '; VarNode 'commitmentFeeBps'; TextRun ' bps on undrawn amounts.')
        )
        TableRef 'FOL-TBL-FEE-SCHEDULE' $tables.FeeSchedule
        Loop 'fees' @(
            Para @(VarNode 'feeItemDescription'; TextRun ': '; VarNode 'feeItemAmount'; TextRun ' '; VarNode 'feeItemCurrency')
        )
        ModuleRef $sch05Anchor
    )

    $sch06Nodes = @(
        Heading 'Schedule 6 — Security Principles'
        Cond '${includeSecurityPackage} == true' @(
            Loop 'securityPackages' @(
                Para @(TextRun 'Security package: '; VarNode 'securityPackageName'; TextRun ' — '; VarNode 'securityPackageType')
            )
        )
        Cond '${includeSyndication} == true' @(
            TableRef 'FOL-TBL-LENDER-MATRIX' $tables.LenderMatrix
            Loop 'lenders' @(
                Para @(VarNode 'lenderName'; TextRun ': '; VarNode 'lenderCommitment'; TextRun ' ('; VarNode 'lenderCommitmentPct'; TextRun '%)')
            )
        )
        Cond '${includeHedge} == true' @(
            Loop 'hedgeProviders' @(
                Para @(TextRun 'Approved hedge provider: '; VarNode 'hedgeProviderName')
            )
        )
        ModuleRef $sch06Anchor
    )

    $borrowerSig = @(
        Para @(TextRun 'Signed for and on behalf of'; LineBreak; VarNode 'borrowerLegalName')
        Para @(TextRun 'Name: _________________________  Title: Authorised Signatory  Date: _________')
    )
    $lenderSig = @(
        Para @(TextRun 'Signed for and on behalf of'; LineBreak; VarNode 'agentBank')
        Para @(TextRun 'Name: _________________________  Title: Authorised Signatory  Date: _________')
    )

    $bindings['FOL_HEADER'] = @{ schemaVersion = '1.0'; nodes = $headerNodes }
    $bindings['FOL_FACILITY_SUMMARY'] = @{ schemaVersion = '1.0'; nodes = $summaryNodes }
    $bindings[$sch01Anchor] = @{ schemaVersion = '1.0'; nodes = $sch01Nodes }
    $bindings[$sch02Anchor] = @{ schemaVersion = '1.0'; nodes = $sch02Nodes }
    $bindings[$sch03Anchor] = @{ schemaVersion = '1.0'; nodes = $sch03Nodes }
    $bindings[$sch04Anchor] = @{ schemaVersion = '1.0'; nodes = $sch04Nodes }
    $bindings[$sch05Anchor] = @{ schemaVersion = '1.0'; nodes = $sch05Nodes }
    $bindings[$sch06Anchor] = @{ schemaVersion = '1.0'; nodes = $sch06Nodes }
    $bindings['FOL_SIG_BORROWER'] = @{ schemaVersion = '1.0'; nodes = $borrowerSig }
    $bindings['FOL_SIG_LENDER'] = @{ schemaVersion = '1.0'; nodes = $lenderSig }

    return @{ bindings = $bindings }
}

function Build-DemoTestVariables {
    return @{
        borrowerLegalName = 'Pacific Rim Holdings Ltd.'
        borrowerShortName = 'Pacific Rim Holdings'
        borrowerJurisdiction = 'Cayman Islands'
        borrowerRegistrationNumber = 'CR-284719'
        borrowerRegisteredAddress = 'PO Box 309, Ugland House, Grand Cayman KY1-1104'
        parentCompanyName = 'Pacific Rim Group Holdings Ltd.'
        groupName = 'Pacific Rim Group'
        facilityAmount = '250000000'
        facilityCurrency = 'USD'
        facilityType = 'SYNDICATED'
        facilityPurpose = 'Refinancing existing external indebtedness and general corporate purposes'
        offerDate = '2026-07-01'
        signingDate = '2026-09-15'
        firstUtilisationDate = '2026-10-01'
        maturityDate = '2031-07-01'
        availabilityPeriodEnd = '2027-07-01'
        agentBank = 'Meridian Global Banking Corporation'
        securityAgent = 'Meridian Global Banking Corporation'
        leadArranger = 'Meridian Global Banking Corporation'
        bookrunner = 'Meridian Global Banking Corporation'
        documentationAgent = 'Meridian Global Banking Corporation'
        referenceRate = 'SOFR'
        marginBps = '185'
        commitmentFeeBps = '35'
        utilisationFeeThresholdPct = '33.33'
        utilisationFeeBps = '10'
        defaultInterestMarginPct = '2.00'
        interestPeriodMonths = '3'
        dayCountConvention = 'ACT_360'
        governingLaw = 'English law'
        jurisdiction = 'Courts of England and Wales'
        lmaFormReference = 'LMA Single Currency Term Facility (recommended form)'
        includeSyndication = $true
        includeHedge = $true
        includeGuarantee = $true
        includeESG = $true
        includeMultiCurrency = $true
        includeAccordion = $true
        includeSwingline = $false
        includeLcSubline = $false
        includeSecurityPackage = $true
        includeIntercreditor = $true
        includeUtilisationDate = $true
        includeCommitmentFee = $true
        includePrepaymentPenalty = $true
        includeFinancialCovenants = $true
        includeEsgKpi = $true
        includeGreenUseOfProceeds = $false
        includeSanctionsRep = $true
        includeKycCondition = $true
        totalCommitments = '250000000'
        totalFees = '3750000'
        minimumLiquidity = '10000000'
        netLeverageMax = '3.50:1.00'
        interestCoverMin = '3.00:1.00'
        lenders = @(
            @{ lenderName = 'Meridian Global Banking Corporation'; lenderCommitment = '75000000'; lenderCommitmentPct = '30'; lenderCountry = 'United Kingdom' }
            @{ lenderName = 'Continental Capital Bank AG'; lenderCommitment = '50000000'; lenderCommitmentPct = '20'; lenderCountry = 'Germany' }
            @{ lenderName = 'Pacific Trade Finance Ltd.'; lenderCommitment = '50000000'; lenderCommitmentPct = '20'; lenderCountry = 'Singapore' }
            @{ lenderName = 'Harbour Street Investments SA'; lenderCommitment = '37500000'; lenderCommitmentPct = '15'; lenderCountry = 'Luxembourg' }
            @{ lenderName = 'Summit Lending Partners LP'; lenderCommitment = '37500000'; lenderCommitmentPct = '15'; lenderCountry = 'United States' }
        )
        tranches = @(
            @{ trancheName = 'Tranche A (Term Loan)'; trancheAmount = '200000000'; trancheCurrency = 'USD'; trancheMarginBps = '185' }
            @{ trancheName = 'Tranche B (Delayed Draw)'; trancheAmount = '50000000'; trancheCurrency = 'USD'; trancheMarginBps = '195' }
        )
        fees = @(
            @{ feeItemDescription = 'Arrangement fee'; feeItemAmount = '2500000'; feeItemCurrency = 'USD' }
            @{ feeItemDescription = 'Commitment fee (undrawn)'; feeItemAmount = '875000'; feeItemCurrency = 'USD' }
            @{ feeItemDescription = 'Agency fee'; feeItemAmount = '250000'; feeItemCurrency = 'USD' }
            @{ feeItemDescription = 'Security agent fee'; feeItemAmount = '125000'; feeItemCurrency = 'USD' }
        )
        conditionsPrecedent = @(
            @{ cpItemDescription = 'Executed Finance Documents'; cpItemStatus = 'PENDING'; cpItemResponsibleParty = 'Borrower / Agent' }
            @{ cpItemDescription = 'Legal opinions (English and local counsel)'; cpItemStatus = 'PENDING'; cpItemResponsibleParty = 'Borrower' }
            @{ cpItemDescription = 'KYC/AML documentation'; cpItemStatus = 'RECEIVED'; cpItemResponsibleParty = 'All Lenders' }
            @{ cpItemDescription = 'Perfection of security over shares'; cpItemStatus = 'PENDING'; cpItemResponsibleParty = 'Security Agent' }
            @{ cpItemDescription = 'Evidence of repayment of existing RCF'; cpItemStatus = 'PENDING'; cpItemResponsibleParty = 'Borrower' }
        )
        guarantors = @(
            @{ guarantorName = 'Pacific Rim Group Holdings Ltd.'; guarantorJurisdiction = 'Cayman Islands' }
            @{ guarantorName = 'Pacific Rim Operations Pte. Ltd.'; guarantorJurisdiction = 'Singapore' }
        )
        securityPackages = @(
            @{ securityPackageName = 'Share charge over borrower'; securityPackageType = 'English law share charge' }
            @{ securityPackageName = 'Account charge'; securityPackageType = 'Debenture over material bank accounts' }
            @{ securityPackageName = 'Receivables assignment'; securityPackageType = 'Assignment of material trade receivables' }
        )
        amortisationSchedule = @(
            @{ amortDate = '2028-07-01'; amortPrincipal = '25000000'; amortBalance = '225000000' }
            @{ amortDate = '2029-07-01'; amortPrincipal = '25000000'; amortBalance = '200000000' }
            @{ amortDate = '2030-07-01'; amortPrincipal = '25000000'; amortBalance = '175000000' }
            @{ amortDate = '2031-07-01'; amortPrincipal = '175000000'; amortBalance = '0' }
        )
        facilityParticulars = @(
            @{ particularLabel = 'Borrower'; particularValue = 'Pacific Rim Holdings Ltd.' }
            @{ particularLabel = 'Total Commitments'; particularValue = 'USD 250,000,000' }
            @{ particularLabel = 'Purpose'; particularValue = 'Refinancing and general corporate purposes' }
            @{ particularLabel = 'Margin'; particularValue = '185 bps over Term SOFR' }
            @{ particularLabel = 'Maturity'; particularValue = '1 July 2031' }
            @{ particularLabel = 'Repayment'; particularValue = 'Amortising — see schedule' }
            @{ particularLabel = 'Governing Law'; particularValue = 'English law' }
        )
        hedgeProviders = @(
            @{ hedgeProviderName = 'Meridian Global Markets Ltd.' }
            @{ hedgeProviderName = 'Continental Capital Bank AG' }
        )
        esgKpis = @(
            @{ esgKpiName = 'Scope 1 & 2 emissions reduction'; esgKpiTarget = '5% YoY vs baseline' }
            @{ esgKpiName = 'Renewable energy share'; esgKpiTarget = '>= 40% by 2028' }
        )
        pricingTiers = @(
            @{ pricingTierLabel = 'Tier 1 (utilisation <= 33%)'; pricingTierMarginBps = '185'; pricingTierFloorBps = '0' }
            @{ pricingTierLabel = 'Tier 2 (utilisation 33–66%)'; pricingTierMarginBps = '195'; pricingTierFloorBps = '0' }
            @{ pricingTierLabel = 'Tier 3 (utilisation > 66%)'; pricingTierMarginBps = '205'; pricingTierFloorBps = '0' }
        )
        milestones = @(
            @{ milestoneDate = '2026-09-15'; milestoneDateDescription = 'Target signing date' }
            @{ milestoneDate = '2026-10-01'; milestoneDateDescription = 'First utilisation date' }
            @{ milestoneDate = '2027-07-01'; milestoneDateDescription = 'Availability period end' }
            @{ milestoneDate = '2031-07-01'; milestoneDateDescription = 'Final maturity date' }
        )
        parties = @(
            @{ partyRole = 'AGENT'; partyLegalName = 'Meridian Global Banking Corporation'; partyCountry = 'United Kingdom' }
            @{ partyRole = 'ARRANGER'; partyLegalName = 'Meridian Global Banking Corporation'; partyCountry = 'United Kingdom' }
            @{ partyRole = 'LENDER'; partyLegalName = 'Continental Capital Bank AG'; partyCountry = 'Germany' }
            @{ partyRole = 'LENDER'; partyLegalName = 'Pacific Trade Finance Ltd.'; partyCountry = 'Singapore' }
            @{ partyRole = 'GUARANTOR'; partyLegalName = 'Pacific Rim Group Holdings Ltd.'; partyCountry = 'Cayman Islands' }
        )
        covenants = @(
            @{ covenantName = 'Net Leverage Ratio'; covenantRatio = '3.50:1.00 maximum'; covenantTestFrequency = 'QUARTERLY' }
            @{ covenantName = 'Interest Cover Ratio'; covenantRatio = '3.00:1.00 minimum'; covenantTestFrequency = 'QUARTERLY' }
            @{ covenantName = 'Minimum Liquidity'; covenantRatio = 'USD 10,000,000 minimum'; covenantTestFrequency = 'QUARTERLY' }
        )
        securedAssets = @(
            @{ securedAssetClass = 'SHARES'; securedAssetDescription = '100% shares in Pacific Rim Holdings Ltd.'; securedAssetJurisdiction = 'Cayman Islands'; securedAssetPerfectionStatus = 'TO_BE_PERFECTED' }
            @{ securedAssetClass = 'ACCOUNTS'; securedAssetDescription = 'Material bank accounts with Agent'; securedAssetJurisdiction = 'Singapore'; securedAssetPerfectionStatus = 'TO_BE_PERFECTED' }
            @{ securedAssetClass = 'RECEIVABLES'; securedAssetDescription = 'Trade receivables over USD 500,000'; securedAssetJurisdiction = 'Singapore'; securedAssetPerfectionStatus = 'TO_BE_PERFECTED' }
        )
        legalReferences = @(
            @{ legalClauseRef = 'Clause 22.1 (Events of Default)'; legalDefinedTerm = 'Event of Default' }
            @{ legalClauseRef = 'Clause 19.1 (Financial Covenants)'; legalDefinedTerm = 'Financial Covenant' }
            @{ legalClauseRef = 'Schedule 1 (Definitions)'; legalDefinedTerm = 'Finance Document' }
        )
        definedTerms = @(
            @{ definedTermName = 'Borrower'; definedTermMeaning = 'Pacific Rim Holdings Ltd., a company incorporated in the Cayman Islands' }
            @{ definedTermName = 'Finance Documents'; definedTermMeaning = 'This Agreement, the Fee Letters and any other document designated as a Finance Document' }
            @{ definedTermName = 'Material Adverse Effect'; definedTermMeaning = 'A material adverse effect on the business, assets or financial condition of the Group taken as a whole' }
        )
        representations = @(
            @{ representationSummary = 'Due incorporation and valid existence of the Borrower'; representationApplies = $true }
            @{ representationSummary = 'No default under existing indebtedness'; representationApplies = $true }
            @{ representationSummary = 'Sanctions compliance — no Sanctions apply to the Borrower or its subsidiaries'; representationApplies = $true }
        )
        infoUndertakings = @(
            @{ infoUndertakingDescription = 'Annual audited consolidated financial statements'; infoUndertakingDueDays = '120' }
            @{ infoUndertakingDescription = 'Quarterly management accounts'; infoUndertakingDueDays = '45' }
            @{ infoUndertakingDescription = 'Compliance certificate with covenant calculations'; infoUndertakingDueDays = '30' }
        )
        eodTriggers = @(
            @{ eodTriggerDescription = 'Non-payment of principal or interest'; eodTriggerGraceDays = '3' }
            @{ eodTriggerDescription = 'Breach of financial covenant'; eodTriggerGraceDays = '15' }
            @{ eodTriggerDescription = 'Cross-default under other indebtedness exceeding USD 5m'; eodTriggerGraceDays = '0' }
        )
        noticeParties = @(
            @{ noticePartyName = 'Pacific Rim Holdings Ltd.'; noticePartyAddress = 'PO Box 309, Ugland House, Grand Cayman KY1-1104'; noticePartyEmail = 'legal@pacificrimholdings.com'; noticePartyAttention = 'General Counsel' }
            @{ noticePartyName = 'Meridian Global Banking Corporation'; noticePartyAddress = '1 Canary Wharf, London E14 5AB'; noticePartyEmail = 'agency@meridianglobal.com'; noticePartyAttention = 'Agency Team' }
        )
        benchmarkFallbacks = @(
            @{ benchmarkFallbackScenario = 'Term SOFR unavailable'; benchmarkFallbackRate = 'Compounded SOFR in arrears plus 0.26161% adjustment' }
            @{ benchmarkFallbackScenario = 'Benchmark discontinuation'; benchmarkFallbackRate = 'Fallback rate agreed by Majority Lenders and Borrower' }
        )
        insurancePolicies = @(
            @{ insurancePolicyType = 'PROPERTY'; insurancePolicyMinimumCover = '50000000'; insurancePolicyCurrency = 'USD' }
            @{ insurancePolicyType = 'LIABILITY'; insurancePolicyMinimumCover = '25000000'; insurancePolicyCurrency = 'USD' }
        )
        obligors = @(
            @{ obligorLegalName = 'Pacific Rim Holdings Ltd.'; obligorRole = 'BORROWER'; obligorJurisdiction = 'Cayman Islands' }
            @{ obligorLegalName = 'Pacific Rim Group Holdings Ltd.'; obligorRole = 'GUARANTOR'; obligorJurisdiction = 'Cayman Islands' }
            @{ obligorLegalName = 'Pacific Rim Operations Pte. Ltd.'; obligorRole = 'GUARANTOR'; obligorJurisdiction = 'Singapore' }
        )
        prepaymentEvents = @(
            @{ prepaymentEventDescription = 'Voluntary prepayment of Term Loan A'; prepaymentEventPenaltyPct = '1.00' }
            @{ prepaymentEventDescription = 'Change of control mandatory prepayment'; prepaymentEventPenaltyPct = '1.00' }
            @{ prepaymentEventDescription = 'Asset sale excess cash sweep'; prepaymentEventPenaltyPct = '0.00' }
        )
        withholdingTaxRatePct = '0.00'
        grossUpRequired = $true
        stampDutyJurisdiction = 'England and Wales'
        stampDutyAmount = '1250'
        stampDutyCurrency = 'GBP'
        vatApplicable = $false
        fatcaStatus = 'COMPLIANT'
        crsReportingRequired = $true
        taxIndemnityCap = '5000000'
        taxIndemnityCurrency = 'USD'
    }
}

# --- Generate ---
$variables = Build-Variables
$compositionRules = Build-CompositionRules
$bindingOverlays = Build-BindingOverlays
$demoTestVariables = Build-DemoTestVariables
$compositionRuleTargets = @($compositionRules.rules | ForEach-Object { $_.targetAnchorId })
$manifest = Build-CatalogManifest -CompositionRuleTargets $compositionRuleTargets

Write-JsonFile (Join-Path $ConfigDir 'fol-variables.json') @{ variables = $variables; generatedAt = (Get-Date -Format 'o'); variableCount = $variables.Count }
Write-JsonFile (Join-Path $ConfigDir 'fol-composition-rules.json') $compositionRules
Write-JsonFile (Join-Path $ConfigDir 'fol-binding-overlays.json') $bindingOverlays
Write-JsonFile (Join-Path $ConfigDir 'fol-catalog-manifest.json') $manifest
$masterSectionManifest = Get-FolMasterSectionManifest
$backendTestResourceDir = Join-Path $RepoRoot 'backend/src/test/resources/demo'
$Null = New-Item -ItemType Directory -Force -Path $backendTestResourceDir
Write-JsonFile (Join-Path $ConfigDir 'fol-master-anchor-ids.json') @{
    anchorIds = ($masterSectionManifest | ForEach-Object { $_.anchorId })
    sections = $masterSectionManifest
    generatedAt = (Get-Date -Format 'o')
}
Write-JsonFile (Join-Path $backendTestResourceDir 'fol-master-anchor-ids.json') @{
    anchorIds = ($masterSectionManifest | ForEach-Object { $_.anchorId })
    sections = $masterSectionManifest
    generatedAt = (Get-Date -Format 'o')
}
Write-JsonFile (Join-Path $ConfigDir 'fol-demo-test-variables.json') @{
    name = 'Executive walkthrough - Pacific Rim Holdings'
    scenarioName = 'Syndicated term loan - Pacific Rim USD 250m (LMA IG baseline)'
    required = $true
    coverageTags = @('executive-demo', 'fol', 'syndicated', 'happy-path')
    variables = $demoTestVariables
}

$Null = New-Item -ItemType Directory -Force -Path $E2eFixturesDir
Write-JsonFile (Join-Path $E2eFixturesDir 'fol-catalog-manifest.json') $manifest
Write-JsonFile (Join-Path $E2eFixturesDir 'fol-demo-test-variables.json') @{
    variables = $demoTestVariables
}

$overlayJson = ($bindingOverlays | ConvertTo-Json -Depth 100)
$features = @{
    conditionBlocks = ([regex]::Matches($overlayJson, '"type"\s*:\s*"conditionBlock"')).Count
    loopBlocks = ([regex]::Matches($overlayJson, '"type"\s*:\s*"loopBlock"')).Count
    tableComponentRefs = ([regex]::Matches($overlayJson, '"type"\s*:\s*"tableComponentRef"')).Count
}

Write-Host "Generated FOL catalog artefacts in $ConfigDir"
Write-Host "  variables:          $($variables.Count)"
Write-Host "  composition rules:  $($compositionRules.rules.Count)"
Write-Host "  binding overlays:   $($bindingOverlays.bindings.Keys.Count) anchors"
Write-Host "  clause bindings:    $($manifest.clauseBindings.Count)"
Write-Host "  conditionBlocks:    $($features.conditionBlocks)"
Write-Host "  loopBlocks:         $($features.loopBlocks)"
Write-Host "  tableComponentRefs: $($features.tableComponentRefs)"
Write-Host "  e2e fixtures:       $E2eFixturesDir"
exit 0

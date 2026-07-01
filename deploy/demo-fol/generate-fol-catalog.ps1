# Generates FOL executive demo catalog JSON artefacts (rich demo variables, rules, rich bindings).
# Run from repo root: .\deploy\demo-fol\generate-fol-catalog.ps1

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigDir = Join-Path $Root 'config'

function Write-JsonFile([string]$Path, [object]$Data) {
    $Null = New-Item -ItemType Directory -Force -Path (Split-Path $Path)
    $json = $Data | ConvertTo-Json -Depth 100 -Compress:$false
    Set-Content -Path $Path -Value $json -Encoding UTF8
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
        @{ ruleId = 'rule-syndication-sec24'; conditionExpression = '${includeSyndication} == true'; targetAnchorId = 'FOL_SEC_24'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-hedge-sec11'; conditionExpression = '${includeHedge} == true'; targetAnchorId = 'FOL_SEC_11'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-guarantee-sec17'; conditionExpression = '${includeGuarantee} == true'; targetAnchorId = 'FOL_SEC_17'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-esg-sec21'; conditionExpression = '${includeESG} == true'; targetAnchorId = 'FOL_SEC_21'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-multicurrency-sch01'; conditionExpression = '${includeMultiCurrency} == true'; targetAnchorId = 'FOL_SCH_01'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-accordion-sec07'; conditionExpression = '${includeAccordion} == true'; targetAnchorId = 'FOL_SEC_07'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-swingline-sec05'; conditionExpression = '${includeSwingline} == true'; targetAnchorId = 'FOL_SEC_05'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-lc-subline-sec02'; conditionExpression = '${includeLcSubline} == true'; targetAnchorId = 'FOL_SEC_02'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-security-sch06'; conditionExpression = '${includeSecurityPackage} == true'; targetAnchorId = 'FOL_SCH_06'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-intercreditor-sec26'; conditionExpression = '${includeIntercreditor} == true'; targetAnchorId = 'FOL_SEC_26'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-prepayment-sec07'; conditionExpression = '${includePrepaymentPenalty} == true'; targetAnchorId = 'FOL_SEC_07'; trueBranchRuleId = ''; falseBranchRuleId = '' }
        @{ ruleId = 'rule-covenants-sec20'; conditionExpression = '${includeFinancialCovenants} == true'; targetAnchorId = 'FOL_SEC_20'; trueBranchRuleId = ''; falseBranchRuleId = '' }
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

function Build-BindingOverlays {
    $tables = Build-TableComponents

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
        ModuleRef 'FOL_SCH_01'
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
        ModuleRef 'FOL_SCH_02'
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
        ModuleRef 'FOL_SCH_03'
    )

    $sch04Nodes = @(
        Heading 'Schedule 4 — Form of Utilisation Request'
        Cond '${includeUtilisationDate} == true' @(
            Para @(TextRun 'Requested utilisation date: '; VarNode 'firstUtilisationDate'; TextRun '; Amount: '; VarNode 'facilityAmount'; TextRun ' '; VarNode 'facilityCurrency')
        )
        ModuleRef 'FOL_SCH_04'
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
        ModuleRef 'FOL_SCH_05'
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
        ModuleRef 'FOL_SCH_06'
    )

    $borrowerSig = @(
        Para @(TextRun 'Signed for and on behalf of'; LineBreak; VarNode 'borrowerLegalName')
        Para @(TextRun 'Name: _________________________  Title: Authorised Signatory  Date: _________')
    )
    $lenderSig = @(
        Para @(TextRun 'Signed for and on behalf of'; LineBreak; VarNode 'agentBank')
        Para @(TextRun 'Name: _________________________  Title: Authorised Signatory  Date: _________')
    )

    return @{
        bindings = @{
            FOL_HEADER = @{ schemaVersion = '1.0'; nodes = $headerNodes }
            FOL_FACILITY_SUMMARY = @{ schemaVersion = '1.0'; nodes = $summaryNodes }
            FOL_SCH_01 = @{ schemaVersion = '1.0'; nodes = $sch01Nodes }
            FOL_SCH_02 = @{ schemaVersion = '1.0'; nodes = $sch02Nodes }
            FOL_SCH_03 = @{ schemaVersion = '1.0'; nodes = $sch03Nodes }
            FOL_SCH_04 = @{ schemaVersion = '1.0'; nodes = $sch04Nodes }
            FOL_SCH_05 = @{ schemaVersion = '1.0'; nodes = $sch05Nodes }
            FOL_SCH_06 = @{ schemaVersion = '1.0'; nodes = $sch06Nodes }
            FOL_SIG_BORROWER = @{ schemaVersion = '1.0'; nodes = $borrowerSig }
            FOL_SIG_LENDER = @{ schemaVersion = '1.0'; nodes = $lenderSig }
        }
    }
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
    }
}

# --- Generate ---
$variables = Build-Variables
Write-JsonFile (Join-Path $ConfigDir 'fol-variables.json') @{ variables = $variables; generatedAt = (Get-Date -Format 'o'); variableCount = $variables.Count }

Write-JsonFile (Join-Path $ConfigDir 'fol-composition-rules.json') (Build-CompositionRules)
Write-JsonFile (Join-Path $ConfigDir 'fol-binding-overlays.json') (Build-BindingOverlays)
Write-JsonFile (Join-Path $ConfigDir 'fol-demo-test-variables.json') @{
    name = 'Executive walkthrough - Pacific Rim Holdings'
    scenarioName = 'Syndicated term loan - Pacific Rim USD 250m (LMA IG baseline)'
    required = $true
    coverageTags = @('executive-demo', 'fol', 'syndicated', 'happy-path')
    variables = (Build-DemoTestVariables)
}

Write-Host "Generated FOL catalog artefacts in $ConfigDir"
Write-Host "  variables:          $($variables.Count)"
Write-Host "  composition rules:  $((Build-CompositionRules).rules.Count)"
Write-Host "  binding overlays:   $((Build-BindingOverlays).bindings.Keys.Count) anchors"
exit 0

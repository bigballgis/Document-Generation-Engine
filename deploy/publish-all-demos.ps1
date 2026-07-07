# Publish all imported demo templates through lifecycle + API policy + credential.
# Prerequisites: deploy/import-all-demos.ps1 completed; backend healthy on :8080.
#
# Usage (from repo root):
#   .\deploy\publish-all-demos.ps1
#   .\deploy\publish-all-demos.ps1 -BackendUrl http://localhost:8080

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$ReleaseVersion = '1.0.0'
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $RepoRoot 'demo-import-shared.ps1')

function Write-PublishStep([string]$Message) { Write-Host "==> publish-all-demos: $Message" }

function Get-AllowedAdGroups([string]$GroupCode) {
    if ($GroupCode -eq 'CORP') { return @('CORP_API') }
    return @('RETAIL_API')
}

function Invoke-MgmtApi {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body = $null
    )
    Invoke-DemoApi -ApiBase "$BackendUrl/api/management/v1" -Method $Method -Path $Path -Token $Token -Body $Body
}

function Get-TemplateDetail {
    param([string]$ExternalId, [string]$AccessToken)
    $list = Invoke-MgmtApi GET '/templates?size=200' $AccessToken
    $content = if ($list.result.content) { @($list.result.content) } else { @($list.result) }
    return $content | Where-Object { $_.externalId -eq $ExternalId } | Select-Object -First 1
}

function Resolve-TemplateAccessToken {
    param([string]$GroupCode)
    if ($GroupCode -in @('TRADE', 'WEALTH')) { return $script:GlobalAdminToken }
    return $script:AuthorToken
}

function Resolve-TesterToken {
    param([string]$GroupCode)
    if ($GroupCode -in @('TRADE', 'WEALTH')) { return $script:GlobalAdminToken }
    return $script:TesterToken
}

function Ensure-TestingReady {
    param([string]$TemplateId, [string]$AccessToken, [string]$TesterToken)
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    $status = [string]$detail.result.lifecycleStatus
    if (@('APPROVAL', 'PENDING_RELEASE', 'PUBLISHED') -contains $status) { return }

    Write-PublishStep "Validating bindings for $TemplateId ..."
    Invoke-MgmtApi POST "/templates/$TemplateId/bindings/validate" $AccessToken @{} | Out-Null

    $setsResp = Invoke-MgmtApi GET "/templates/$TemplateId/test-data-sets" $AccessToken
    $sets = @($setsResp.result)
    if ($sets.Count -eq 0) { throw "No test data sets for template $TemplateId" }
    $dataSet = $sets | Where-Object { $_.required -eq $true } | Select-Object -First 1
    if (-not $dataSet) { $dataSet = $sets[0] }

    Write-PublishStep "Running preview + batch test ($($dataSet.name)) ..."
    Invoke-MgmtApi POST "/templates/$TemplateId/previews/test-generate" $AccessToken @{
        variables = $dataSet.variables
    } | Out-Null
    Invoke-MgmtApi POST "/templates/$TemplateId/previews/batch-test" $AccessToken @{
        testDataSetIds = @($dataSet.testDataSetId)
    } | Out-Null

    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    if ([string]$detail.result.lifecycleStatus -notin @('TESTING', 'APPROVAL', 'PENDING_RELEASE', 'PUBLISHED')) {
        Invoke-MgmtApi POST "/templates/$TemplateId/lifecycle/submit-test" $AccessToken @{
            commentSummary = 'Demo publish-all-demos automated submit'
        } | Out-Null
    }
}

function Ensure-ApprovalPending {
    param([string]$TemplateId, [string]$AccessToken, [string]$TesterToken)
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    $status = [string]$detail.result.lifecycleStatus
    $sub = [string]$detail.result.approvalSubState
    if ($status -eq 'APPROVAL' -and $sub -eq 'PENDING_DECISION') { return }
    if (@('PENDING_RELEASE', 'PUBLISHED') -contains $status) { return }

    Ensure-TestingReady -TemplateId $TemplateId -AccessToken $AccessToken -TesterToken $TesterToken
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    if ([string]$detail.result.lifecycleStatus -eq 'TESTING') {
        Invoke-MgmtApi POST "/templates/$TemplateId/lifecycle/test-decision" $TesterToken @{
            decision = 'PASSED'
            commentSummary = 'Demo publish-all-demos test pass'
            fidelityViewedConfirmed = $true
            coverageViewedConfirmed = $true
            previewViewedConfirmed = $true
        } | Out-Null
    }
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    if ([string]$detail.result.lifecycleStatus -eq 'APPROVAL' -and [string]$detail.result.approvalSubState -eq 'PENDING_SUBMIT') {
        Invoke-MgmtApi POST "/templates/$TemplateId/lifecycle/submit-approval" $AccessToken @{
            commentSummary = 'Demo publish-all-demos submit approval'
        } | Out-Null
    }
}

function Ensure-PendingRelease {
    param([string]$TemplateId, [string]$AccessToken, [string]$TesterToken, [string]$ApproverToken)
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    if ([string]$detail.result.lifecycleStatus -eq 'PENDING_RELEASE') { return }
    if ([string]$detail.result.lifecycleStatus -eq 'PUBLISHED') { return }

    Ensure-ApprovalPending -TemplateId $TemplateId -AccessToken $AccessToken -TesterToken $TesterToken
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $ApproverToken
    if ([string]$detail.result.lifecycleStatus -eq 'APPROVAL' -and [string]$detail.result.approvalSubState -eq 'PENDING_DECISION') {
        Invoke-MgmtApi POST "/templates/$TemplateId/lifecycle/approval-decision" $ApproverToken @{
            decision = 'APPROVED'
            commentSummary = 'Demo publish-all-demos approval'
            keyEvidenceConfirmed = $true
        } | Out-Null
    }
}

function Publish-DemoTemplate {
    param(
        [string]$ExternalId,
        [string]$GroupAdminToken,
        [string]$ApproverToken
    )
    $template = Get-TemplateDetail -ExternalId $ExternalId -AccessToken $script:GlobalAdminToken
    if (-not $template) {
        $template = Get-TemplateDetail -ExternalId $ExternalId -AccessToken $script:AuthorToken
    }
    if (-not $template) {
        Write-Warning "SKIP $ExternalId — template not found (run import-all-demos.ps1 first)"
        return
    }

    $templateId = [string]$template.id
    $groupCode = [string]$template.groupCode
    $accessToken = Resolve-TemplateAccessToken -GroupCode $groupCode
    $testerToken = Resolve-TesterToken -GroupCode $groupCode
    Write-PublishStep "Processing $ExternalId ($groupCode, status=$($template.lifecycleStatus)) ..."

    if ([string]$template.lifecycleStatus -ne 'PUBLISHED') {
        Ensure-PendingRelease -TemplateId $templateId -AccessToken $accessToken -TesterToken $testerToken -ApproverToken $ApproverToken
        $allowed = Get-AllowedAdGroups $groupCode
        try {
            Invoke-MgmtApi PUT "/templates/$templateId/api/policy" $GroupAdminToken @{
                allowedAdGroups = $allowed
                defaultRouteReleaseVersion = $ReleaseVersion
                outputFormats = @('DOCX')
                outputModes = @('SYNC_STREAM')
                batchEnabled = $false
                maxBatchSize = 10
                docxEncryptionEnabled = $false
                pdfEncryptionEnabled = $false
            } | Out-Null
        } catch {
            Write-Warning "API policy PUT for $ExternalId failed (continuing to publish): $($_.Exception.Message)"
        }
        Invoke-MgmtApi POST "/templates/$templateId/lifecycle/publish" $GroupAdminToken @{
            releaseVersion = $ReleaseVersion
        } | Out-Null
    } else {
        Write-PublishStep "$ExternalId already PUBLISHED — ensuring API policy ..."
        try {
            Invoke-MgmtApi GET "/templates/$templateId/api/policy" $GroupAdminToken | Out-Null
        } catch {
            $allowed = Get-AllowedAdGroups $groupCode
            try {
                Invoke-MgmtApi PUT "/templates/$templateId/api/policy" $GroupAdminToken @{
                    allowedAdGroups = $allowed
                    defaultRouteReleaseVersion = $ReleaseVersion
                    outputFormats = @('DOCX')
                    outputModes = @('SYNC_STREAM')
                    batchEnabled = $false
                    maxBatchSize = 10
                    docxEncryptionEnabled = $false
                    pdfEncryptionEnabled = $false
                } | Out-Null
            } catch {
                Write-Warning "API policy ensure for $ExternalId failed: $($_.Exception.Message)"
            }
        }
    }

    $final = Invoke-MgmtApi GET "/templates/$templateId" $accessToken
    if ([string]$final.result.lifecycleStatus -ne 'PUBLISHED') {
        throw "Failed to publish $ExternalId (status=$($final.result.lifecycleStatus))"
    }
    Write-PublishStep "$ExternalId PUBLISHED (release=$ReleaseVersion)"
}

if (-not (Wait-DemoBackendHealthy -BackendUrl $BackendUrl)) {
    throw "Backend not healthy at $BackendUrl/healthz"
}

$ApiBase = "$BackendUrl/api/management/v1"
$script:GlobalAdminToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000001' -Password 'ChangeMe123!'
$script:AuthorToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000003' -Password 'ChangeMe123!'
$GroupAdminToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000002' -Password 'ChangeMe123!'
$ApproverToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000007' -Password 'ChangeMe123!'
$script:TesterToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000006' -Password 'ChangeMe123!'

$DemoExternalIds = @(
    'CORP-FOL-OFFER',
    'DEMO-FULL-FLOW-LETTER',
    'DEMO-RETAIL-ACCOUNT-OPEN',
    'DEMO-RETAIL-ACCOUNT-BALANCE',
    'DEMO-MORTGAGE-APPROVAL',
    'DEMO-CREDIT-LIMIT-CONFIRM',
    'DEMO-TRADE-LC-NOTICE',
    'DEMO-TRADE-GUARANTEE-NOTICE',
    'DEMO-RATE-CHANGE-NOTICE',
    'DEMO-OVERDUE-COLLECTION',
    'DEMO-ANNUAL-REVIEW',
    'DEMO-FACILITY-RENEWAL',
    'DEMO-WEALTH-STATEMENT'
)

Write-PublishStep 'starting publish chain'
foreach ($externalId in $DemoExternalIds) {
    try {
        Publish-DemoTemplate -ExternalId $externalId -GroupAdminToken $GroupAdminToken -ApproverToken $ApproverToken
    } catch {
        Write-Warning "FAILED $externalId — $($_.Exception.Message)"
    }
}
Write-PublishStep 'completed (failures logged as warnings)'

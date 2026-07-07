# Publish all imported demo templates through lifecycle + API policy + credential.
# P23-T12 — publish orchestration for all 8 demo families + full-flow letter.
#
# Prerequisites:
#   1. Backend healthy on :8080 (Docker deploy or local spring-boot:run)
#   2. deploy/import-all-demos.ps1 completed (8 deploy/demo-* packages)
#   3. DEMO-FULL-FLOW-LETTER present (docgen.demo-catalog.seed-enabled=true on first boot, or E2E seed)
#
# Templates covered (13 external IDs — registry: Get-DemoPublishExternalIds):
#   CORP-FOL-OFFER                          | CORP   | CORP_API
#   DEMO-FULL-FLOW-LETTER                   | RETAIL | RETAIL_API
#   DEMO-RETAIL-ACCOUNT-OPEN/BALANCE        | RETAIL | RETAIL_API
#   DEMO-MORTGAGE-APPROVAL                  | RETAIL | RETAIL_API
#   DEMO-CREDIT-LIMIT-CONFIRM               | CORP   | CORP_API
#   DEMO-TRADE-LC-NOTICE / GUARANTEE-NOTICE | TRADE  | RETAIL_API
#   DEMO-RATE-CHANGE / OVERDUE-COLLECTION   | RETAIL | RETAIL_API
#   DEMO-ANNUAL-REVIEW / FACILITY-RENEWAL   | CORP   | CORP_API
#   DEMO-WEALTH-STATEMENT                   | WEALTH | RETAIL_API
#
# AD group alignment: template groupCode CORP → policy CORP_API; all others → RETAIL_API.
# Runtime callers svc-caller / e2e-runtime-caller hold both groups (application.yml).
#
# Usage (from repo root):
#   .\deploy\import-all-demos.ps1
#   .\deploy\publish-all-demos.ps1
#   .\deploy\publish-all-demos.ps1 -BackendUrl http://localhost:8080 -ReleaseVersion 1.0.0
#
# Outputs:
#   .tmp/credentials/<externalId>.json          — runtime credential bundles
#   .tmp/evidence/all-demos-publish-summary.json — publish evidence table

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$ReleaseVersion = '1.0.0',
    [string]$CredentialDir = '',
    [string]$EvidenceDir = ''
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceRoot = Split-Path -Parent $RepoRoot
if (-not $CredentialDir) { $CredentialDir = Join-Path $WorkspaceRoot '.tmp/credentials' }
if (-not $EvidenceDir) { $EvidenceDir = Join-Path $WorkspaceRoot '.tmp/evidence' }
. (Join-Path $RepoRoot 'demo-import-shared.ps1')

function Write-PublishStep([string]$Message) { Write-Host "==> publish-all-demos: $Message" }

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

function Ensure-DemoApiPolicy {
    param(
        [string]$TemplateId,
        [string]$GroupCode,
        [string]$GroupAdminToken
    )
    $allowed = Get-DemoAllowedApiAdGroups -GroupCode $GroupCode
    $body = @{
        allowedAdGroups = $allowed
        defaultRouteReleaseVersion = $ReleaseVersion
        outputFormats = @('DOCX')
        outputModes = @('SYNC_STREAM')
        batchEnabled = $false
        maxBatchSize = 10
        docxEncryptionEnabled = $false
        pdfEncryptionEnabled = $false
    }
    try {
        Invoke-MgmtApi PUT "/templates/$TemplateId/api/policy" $GroupAdminToken $body | Out-Null
    } catch {
        Write-Warning "API policy PUT failed (continuing): $($_.Exception.Message)"
    }
    return $allowed
}

function Ensure-DemoRuntimeCredential {
    param(
        [string]$TemplateId,
        [string]$ExternalId,
        [string]$GroupAdminToken
    )
    if (-not (Test-Path $CredentialDir)) {
        New-Item -ItemType Directory -Path $CredentialDir -Force | Out-Null
    }
    $credPath = Join-Path $CredentialDir "$ExternalId.json"
    $listResp = Invoke-MgmtApi GET "/templates/$TemplateId/api/credentials" $GroupAdminToken
    $activeIds = @($listResp.result | ForEach-Object { [string]$_.externalId })

    if (Test-Path $credPath) {
        $saved = Get-Content $credPath -Raw | ConvertFrom-Json
        if ($saved.secret -and $activeIds -contains [string]$saved.externalId) {
            Write-PublishStep "Reusing credential for $ExternalId ($($saved.externalId))"
            return $saved
        }
    }

    Write-PublishStep "Issuing API credential for $ExternalId ..."
    $created = Invoke-MgmtApi POST "/templates/$TemplateId/api/credentials" $GroupAdminToken @{}
    $bundle = [ordered]@{
        externalId = [string]$created.result.externalId
        secret = [string]$created.result.secret
        templateExternalId = $ExternalId
        templateId = $TemplateId
        issuedAt = (Get-Date).ToUniversalTime().ToString('o')
    }
    ($bundle | ConvertTo-Json) | Set-Content $credPath -Encoding UTF8
    return $bundle
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
        Write-Warning "SKIP $ExternalId — template not found (run import-all-demos.ps1 first; full-flow needs demo-catalog seed)"
        return $null
    }

    $templateId = [string]$template.id
    $groupCode = [string]$template.groupCode
    $accessToken = Resolve-DemoPublishAccessToken -GroupCode $groupCode
    $testerToken = Resolve-DemoPublishTesterToken -GroupCode $groupCode
    Write-PublishStep "Processing $ExternalId ($groupCode, status=$($template.lifecycleStatus)) ..."

    if ([string]$template.lifecycleStatus -ne 'PUBLISHED') {
        Ensure-PendingRelease -TemplateId $templateId -AccessToken $accessToken -TesterToken $testerToken -ApproverToken $ApproverToken
        Ensure-DemoApiPolicy -TemplateId $templateId -GroupCode $groupCode -GroupAdminToken $GroupAdminToken | Out-Null
        Invoke-MgmtApi POST "/templates/$templateId/lifecycle/publish" $GroupAdminToken @{
            releaseVersion = $ReleaseVersion
        } | Out-Null
    } else {
        Write-PublishStep "$ExternalId already PUBLISHED — ensuring API policy ..."
        Ensure-DemoApiPolicy -TemplateId $templateId -GroupCode $groupCode -GroupAdminToken $GroupAdminToken | Out-Null
    }

    $final = Invoke-MgmtApi GET "/templates/$templateId" $accessToken
    if ([string]$final.result.lifecycleStatus -ne 'PUBLISHED') {
        throw "Failed to publish $ExternalId (status=$($final.result.lifecycleStatus))"
    }

    $allowed = Get-DemoAllowedApiAdGroups -GroupCode $groupCode
    $credential = Ensure-DemoRuntimeCredential -TemplateId $templateId -ExternalId $ExternalId -GroupAdminToken $GroupAdminToken
    Write-PublishStep "$ExternalId PUBLISHED (release=$ReleaseVersion, adGroups=$($allowed -join ','), credential=$($credential.externalId))"

    return [ordered]@{
        externalId = $ExternalId
        templateId = $templateId
        groupCode = $groupCode
        lifecycleStatus = [string]$final.result.lifecycleStatus
        releaseVersion = $ReleaseVersion
        allowedAdGroups = $allowed
        credentialExternalId = [string]$credential.externalId
        catalogMarker = if ($final.result.description -match '\[([^\]]+)\]') { $Matches[1] } else { $null }
    }
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

$DemoExternalIds = Get-DemoPublishExternalIds -DeployRoot $RepoRoot
$summaryRows = @()

Write-PublishStep "starting publish chain ($($DemoExternalIds.Count) templates)"
foreach ($externalId in $DemoExternalIds) {
    try {
        $row = Publish-DemoTemplate -ExternalId $externalId -GroupAdminToken $GroupAdminToken -ApproverToken $ApproverToken
        if ($row) { $summaryRows += $row }
    } catch {
        Write-Warning "FAILED $externalId — $($_.Exception.Message)"
    }
}

if (-not (Test-Path $EvidenceDir)) {
    New-Item -ItemType Directory -Path $EvidenceDir -Force | Out-Null
}
$summaryPath = Join-Path $EvidenceDir 'all-demos-publish-summary.json'
$summary = [ordered]@{
    generatedAt = (Get-Date).ToUniversalTime().ToString('o')
    backendUrl = $BackendUrl
    releaseVersion = $ReleaseVersion
    templateCount = $summaryRows.Count
    expectedCount = $DemoExternalIds.Count
    templates = $summaryRows
}
($summary | ConvertTo-Json -Depth 10) | Set-Content $summaryPath -Encoding UTF8
Write-PublishStep "evidence written to $summaryPath ($($summaryRows.Count)/$($DemoExternalIds.Count) published)"
Write-PublishStep 'completed (failures logged as warnings)'

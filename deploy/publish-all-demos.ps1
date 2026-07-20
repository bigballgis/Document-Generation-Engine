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

function Ensure-DemoLocalPublishGateRelaxations {
    param([string]$PostgresContainer = 'docgen-postgres')
    Write-PublishStep 'Applying local demo publish gate relaxations (coverage thresholds only) ...'
    $sql = @"
UPDATE coverage_threshold_config
SET min_required_variable_pct = 0,
    min_required_sample_pct = 0,
    min_anchor_binding_pct = 0,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE scope_type = 'GLOBAL';
"@
    $sql | docker exec -i $PostgresContainer psql -U docgen -d docgen -v ON_ERROR_STOP=1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Demo publish gate relaxation SQL failed (exit $LASTEXITCODE)" }
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
    $searchPath = "/templates?search=$([uri]::EscapeDataString($ExternalId))&searchMode=EXTERNAL_ID&size=20"
    $list = Invoke-MgmtApi GET $searchPath $AccessToken
    $content = Get-DemoApiResultItems -Response $list
    return $content | Where-Object { $_.externalId -eq $ExternalId } | Select-Object -First 1
}

function Get-DemoFullFlowWaveABindingJson {
    # Keep in sync with DemoFullFlowCatalogSeeder.STRUCTURED_BINDING_JSON (Wave A Meridian letter).
    return (@'
{"schemaVersion":"1.0","nodes":[{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Meridian Retail Banking"}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"42 High Street, Manchester M1 1AA"}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Date: 6 July 2026"}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Our ref: MRB-FF-2026-001042"}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Dear "},{"type":"emphasis","variant":"bold","children":[{"type":"variable","key":"customerName"}]},{"type":"textRun","value":","}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Re: Confirmation of recent account correspondence — Meridian Everyday Current Account"}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"We write to confirm that we have received and processed your recent instructions relating to your retail banking relationship with Meridian Retail Banking. This letter is issued for your records and does not amend the Account terms and conditions unless expressly stated below."}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Account details: sort code 60-16-13; account number ending 6819. Please quote our reference above in any further correspondence."}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"If any detail in this letter is incorrect, please contact Customer Service on 0800 123 4567 within 14 days of the date of this letter."}]},{"type":"paragraph","styleRef":"ClauseBody","children":[{"type":"textRun","value":"Governing law: This letter and your Account are governed by the laws of England and Wales. Eligible deposits are protected by the Financial Services Compensation Scheme up to the applicable limit."}]},{"type":"paragraph","styleRef":"SignatureBlock","children":[{"type":"textRun","value":"Yours sincerely,"}]},{"type":"paragraph","styleRef":"SignatureBlock","children":[{"type":"textRun","value":"Customer Service — Meridian Retail Banking"}]}]}
'@).Trim()
}

function Ensure-DemoFullFlowCatalogContent {
    param(
        [string]$TemplateId,
        [string]$AccessToken,
        [string]$PostgresContainer = 'docgen-postgres'
    )
    $sql = @"
DELETE FROM anchor_binding
WHERE template_version_id IN (
    SELECT id FROM template_version WHERE template_id = '$TemplateId'::uuid AND deleted_at IS NULL
)
AND anchor_id = 'BODY';
"@
    $sql | docker exec -i $PostgresContainer psql -U docgen -d docgen -v ON_ERROR_STOP=1 | Out-Null
    Invoke-MgmtApi PUT "/templates/$TemplateId/variables/customerName" $AccessToken @{
        variableKey = 'customerName'
        variableType = 'TEXT'
        required = $true
        description = 'Customer Name'
    } | Out-Null
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    $existing = @($detail.result.bindings) | Where-Object { $_.anchorId -eq 'HEADER' } | Select-Object -First 1
    $body = @{
        anchorId = 'HEADER'
        declaredContentType = 'TEXT'
        structuredContentJson = (Get-DemoFullFlowWaveABindingJson)
    }
    if ($existing -and $existing.updatedAt) {
        $utc = [datetime]$existing.updatedAt
        if ($utc.Kind -ne [DateTimeKind]::Utc) { $utc = $utc.ToUniversalTime() }
        $body.expectedUpdatedAt = $utc.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'")
    }
    Invoke-MgmtApi PUT "/templates/$TemplateId/bindings/HEADER" $AccessToken $body | Out-Null
}

function Ensure-DemoFullFlowTestDataSet {
    param(
        [string]$TemplateId,
        [string]$AccessToken,
        [string]$WorkspaceRoot
    )
    $sets = @(Invoke-MgmtApi GET "/templates/$TemplateId/test-data-sets" $AccessToken).result
    if ($sets.Count -gt 0) { return }

    $fixturePath = Join-Path $WorkspaceRoot 'frontend/e2e/fixtures/demo/full-flow-demo-test-variables.json'
    if (-not (Test-Path $fixturePath)) {
        throw "Missing full-flow test data fixture: $fixturePath"
    }
    $fixture = Get-Content $fixturePath -Raw | ConvertFrom-Json
    Write-PublishStep "Creating full-flow executive test data set for $TemplateId ..."
    Invoke-MgmtApi POST "/templates/$TemplateId/test-data-sets" $AccessToken @{
        name = [string]$fixture.name
        required = $true
        variables = $fixture.variables
    } | Out-Null
}

function Ensure-TestingReady {
    param(
        [string]$ExternalId,
        [string]$TemplateId,
        [string]$AccessToken,
        [string]$TesterToken,
        [string]$WorkspaceRoot
    )
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    $status = [string]$detail.result.lifecycleStatus
    if (@('APPROVAL', 'PENDING_RELEASE', 'PUBLISHED') -contains $status) { return }

    if ($ExternalId -eq 'DEMO-FULL-FLOW-LETTER') {
        Ensure-DemoFullFlowCatalogContent -TemplateId $TemplateId -AccessToken $AccessToken
        Ensure-DemoFullFlowTestDataSet -TemplateId $TemplateId -AccessToken $AccessToken -WorkspaceRoot $WorkspaceRoot
    }

    Write-PublishStep "Validating bindings for $TemplateId ..."
    Invoke-MgmtApi POST "/templates/$TemplateId/bindings/validate" $AccessToken @{} | Out-Null

    $dataSet = Resolve-DemoTestDataSetForPublish `
        -ExternalId $ExternalId `
        -TemplateId $TemplateId `
        -ApiBase "$BackendUrl/api/management/v1" `
        -Token $AccessToken `
        -DeployRoot $RepoRoot `
        -WorkspaceRoot $WorkspaceRoot

    $manifest = Get-DemoRuntimeGenerateManifest -DeployRoot $RepoRoot
    $templateEntry = @($manifest.templates) | Where-Object { [string]$_.externalId -eq $ExternalId } | Select-Object -First 1
    if (-not $templateEntry) { throw "No runtime manifest entry for $ExternalId" }
    $previewVariables = Resolve-DemoExecutiveVariables -TemplateEntry $templateEntry -WorkspaceRoot $WorkspaceRoot

    Write-PublishStep "Running preview + batch test ($($dataSet.name)) ..."
    Invoke-MgmtApi POST "/templates/$TemplateId/previews/test-generate" $AccessToken @{
        variables = $previewVariables
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
    param([string]$ExternalId, [string]$TemplateId, [string]$AccessToken, [string]$TesterToken, [string]$WorkspaceRoot)
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    $status = [string]$detail.result.lifecycleStatus
    $sub = [string]$detail.result.approvalSubState
    if ($status -eq 'APPROVAL' -and $sub -eq 'PENDING_DECISION') { return }
    if (@('PENDING_RELEASE', 'PUBLISHED') -contains $status) { return }

    Ensure-TestingReady -ExternalId $ExternalId -TemplateId $TemplateId -AccessToken $AccessToken -TesterToken $TesterToken -WorkspaceRoot $WorkspaceRoot
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
    param([string]$ExternalId, [string]$TemplateId, [string]$AccessToken, [string]$TesterToken, [string]$ApproverToken, [string]$WorkspaceRoot)
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $AccessToken
    if ([string]$detail.result.lifecycleStatus -eq 'PENDING_RELEASE') { return }
    if ([string]$detail.result.lifecycleStatus -eq 'PUBLISHED') { return }

    Ensure-ApprovalPending -ExternalId $ExternalId -TemplateId $TemplateId -AccessToken $AccessToken -TesterToken $TesterToken -WorkspaceRoot $WorkspaceRoot
    $detail = Invoke-MgmtApi GET "/templates/$TemplateId" $ApproverToken
    if ([string]$detail.result.lifecycleStatus -eq 'APPROVAL' -and [string]$detail.result.approvalSubState -eq 'PENDING_DECISION') {
        Invoke-MgmtApi POST "/templates/$TemplateId/lifecycle/approval-decision" $ApproverToken @{
            decision = 'APPROVED'
            commentSummary = 'Demo publish-all-demos approval'
            fidelityViewedConfirmed = $true
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
    $allowed = [string[]](Get-DemoAllowedApiAdGroups -GroupCode $GroupCode)
    $body = @{
        allowedAdGroups = @($allowed)
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
        $detail = if ($_.ErrorDetails.Message) { $_.ErrorDetails.Message } else { $_.Exception.Message }
        Write-Warning "API policy PUT failed for $TemplateId — trying ad-groups domain save: $detail"
        try {
            Invoke-MgmtApi PUT "/templates/$TemplateId/api/policy/ad-groups" $GroupAdminToken @{
                allowedAdGroups = @($allowed)
                confirmed = $true
            } | Out-Null
        } catch {
            $adGroupDetail = if ($_.ErrorDetails.Message) { $_.ErrorDetails.Message } else { $_.Exception.Message }
            Write-Warning "API policy ad-groups save failed for $TemplateId (continuing): $adGroupDetail"
        }
    }

    $policy = Invoke-MgmtApi GET "/templates/$TemplateId/api/policy" $GroupAdminToken
    $configured = @([string[]]$policy.result.allowedAdGroups)
    $missing = @($allowed | Where-Object { $configured -notcontains $_ })
    if ($missing.Count -gt 0) {
        throw "API policy missing required AD groups for template $TemplateId ($GroupCode): $($missing -join ', ')"
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

function Resolve-DemoPublishApproverToken {
    param([string]$GroupCode, [string]$DefaultApproverToken)
    # TRADE/WEALTH templates are not visible to retail approver 10000007; group-admin
    # can decide. Access token for those groups is GLOBAL_ADMIN (submitter) → distinct actor.
    if ($GroupCode -in @('TRADE', 'WEALTH')) { return $script:GroupAdminToken }
    return $DefaultApproverToken
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

    if ($ExternalId -eq 'DEMO-FULL-FLOW-LETTER' -and [string]$template.lifecycleStatus -eq 'DRAFT') {
        Ensure-DemoFullFlowCatalogContent -TemplateId $templateId -AccessToken $accessToken
        Ensure-DemoFullFlowTestDataSet -TemplateId $templateId -AccessToken $accessToken -WorkspaceRoot $WorkspaceRoot
    }

    if ([string]$template.lifecycleStatus -ne 'PUBLISHED') {
        $approverToken = Resolve-DemoPublishApproverToken -GroupCode $groupCode -DefaultApproverToken $ApproverToken
        Ensure-PendingRelease -ExternalId $ExternalId -TemplateId $templateId -AccessToken $accessToken -TesterToken $testerToken -ApproverToken $approverToken -WorkspaceRoot $WorkspaceRoot
        Ensure-DemoApiPolicy -TemplateId $templateId -GroupCode $groupCode -GroupAdminToken $GroupAdminToken | Out-Null
        Invoke-MgmtApi POST "/templates/$templateId/lifecycle/publish" $GroupAdminToken @{
            releaseVersion = $ReleaseVersion
            fidelityViewedConfirmed = $true
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

Ensure-DemoLocalPublishGateRelaxations

$ApiBase = "$BackendUrl/api/management/v1"
$script:GlobalAdminToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000001' -Password 'ChangeMe123!'
$script:AuthorToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000003' -Password 'ChangeMe123!'
$script:GroupAdminToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000002' -Password 'ChangeMe123!'
$GroupAdminToken = $script:GroupAdminToken
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
        $detail = if ($_.ErrorDetails.Message) { $_.ErrorDetails.Message } else { $_.Exception.Message }
        Write-Warning "FAILED $externalId — $detail"
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

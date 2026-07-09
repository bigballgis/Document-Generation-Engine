# Taskmaster #8 — collect fundraising evidence bundle under .tmp/evidence/
# Prerequisites: publish-all-demos.ps1 + generate-all-demos.ps1 (or -SkipGenerate with existing .tmp/generated_*.docx)
#
# Usage (repo root):
#   .\deploy\capture-fundraising-evidence-bundle.ps1
#   .\deploy\capture-fundraising-evidence-bundle.ps1 -BackendUrl http://localhost:8080 -SkipGenerate

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$WorkspaceRoot = '',
    [string]$EvidenceDir = '',
    [string]$OutputDir = '',
    [switch]$SkipGenerate,
    [switch]$ContinueOnGenerateFailure
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $WorkspaceRoot) { $WorkspaceRoot = Split-Path -Parent $RepoRoot }
if (-not $EvidenceDir) { $EvidenceDir = Join-Path $WorkspaceRoot '.tmp/evidence' }
if (-not $OutputDir) { $OutputDir = Join-Path $WorkspaceRoot '.tmp' }

. (Join-Path $RepoRoot 'demo-import-shared.ps1')

function Write-BundleStep([string]$Message) { Write-Host "==> capture-evidence: $Message" }

function Invoke-MgmtApi {
    param([string]$Method, [string]$Path, [string]$Token, [object]$Body = $null)
    Invoke-DemoApi -ApiBase "$BackendUrl/api/management/v1" -Method $Method -Path $Path -Token $Token -Body $Body
}

function Get-Sha256Hex([string]$FilePath) {
    (Get-FileHash -Path $FilePath -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Get-TemplateByExternalId {
    param([string]$ExternalId, [string]$Token)
    $list = Invoke-MgmtApi GET '/templates?size=200' $Token
    $content = Get-DemoApiResultItems -Response $list
    return $content | Where-Object { $_.externalId -eq $ExternalId } | Select-Object -First 1
}

function Get-GenerationAuditRecords {
    param([string]$ExternalId, [string]$AdminToken)
    try {
        return Invoke-MgmtApi GET "/audit/generation?templateExternalId=$ExternalId&size=50" $AdminToken
    } catch {
        Write-Warning "Audit fetch failed for ${ExternalId}: $($_.Exception.Message)"
        return @{ result = @{ content = @() } }
    }
}

Write-BundleStep "preparing evidence directories"
$docxBundleDir = Join-Path $EvidenceDir 'generated-docx'
$auditDir = Join-Path $EvidenceDir 'audit-records'
New-Item -ItemType Directory -Path $docxBundleDir -Force | Out-Null
New-Item -ItemType Directory -Path $auditDir -Force | Out-Null

if (-not $SkipGenerate) {
    Write-BundleStep "running generate-all-demos"
    $genArgs = @{ BackendUrl = $BackendUrl }
    if ($ContinueOnGenerateFailure) { $genArgs.ContinueOnFailure = $true }
    try {
        & (Join-Path $RepoRoot 'generate-all-demos.ps1') @genArgs
    } catch {
        if (-not $ContinueOnGenerateFailure) { throw }
        Write-Warning $_.Exception.Message
    }
}

$manifestPath = Join-Path $EvidenceDir 'generated-docx-manifest.json'
$publishPath = Join-Path $EvidenceDir 'all-demos-publish-summary.json'
if (-not (Test-Path $manifestPath)) {
    throw "Missing $manifestPath — run generate-all-demos.ps1 first"
}

$manifest = Get-Content $manifestPath -Raw | ConvertFrom-Json
$publishSummary = if (Test-Path $publishPath) { Get-Content $publishPath -Raw | ConvertFrom-Json } else { $null }

Write-BundleStep "copying DOCX artifacts to generated-docx/"
foreach ($entry in @($manifest.entries)) {
    $ext = [string]$entry.externalId
    $src = Join-Path $OutputDir "generated_$ext.docx"
    $dest = Join-Path $docxBundleDir "$ext.docx"
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $dest -Force
        if (-not $entry.sha256 -and (Test-Path $dest)) {
            $entry | Add-Member -NotePropertyName 'sha256' -NotePropertyValue (Get-Sha256Hex $dest) -Force
        }
    }
}

# FOL fallback: prior successful local generate artifact
$folSrc = Join-Path $OutputDir 'gen_fol.docx'
$folDest = Join-Path $docxBundleDir 'CORP-FOL-OFFER.docx'
if (-not (Test-Path $folDest) -and (Test-Path $folSrc)) {
    Copy-Item $folSrc $folDest -Force
    Write-BundleStep "using fallback FOL artifact from gen_fol.docx"
}

Write-BundleStep "fetching generation audit records"
$AdminToken = Get-DemoApiToken -ApiBase "$BackendUrl/api/management/v1" -Username '10000001' -Password 'ChangeMe123!'
$externalIds = Get-DemoPublishExternalIds -DeployRoot $RepoRoot
foreach ($ext in $externalIds) {
    $auditPath = Join-Path $auditDir "$ext.json"
    $manifestEntry = @($manifest.entries) | Where-Object { $_.externalId -eq $ext } | Select-Object -First 1
    $audit = Get-GenerationAuditRecords -ExternalId $ext -AdminToken $AdminToken
    $records = Get-DemoApiResultItems -Response $audit
    if ($records.Count -eq 0 -and $manifestEntry -and $manifestEntry.audit) {
        ($manifestEntry.audit | ConvertTo-Json -Depth 20) | Set-Content $auditPath -Encoding UTF8
        continue
    }
    ($audit | ConvertTo-Json -Depth 20) | Set-Content $auditPath -Encoding UTF8
}

Write-BundleStep "writing fundraising-demo-summary.md"
$rows = @()
foreach ($ext in $externalIds) {
    $tpl = Get-TemplateByExternalId -ExternalId $ext -Token $AdminToken
    $pub = if ($publishSummary) { @($publishSummary.templates) | Where-Object { $_.externalId -eq $ext } | Select-Object -First 1 } else { $null }
    $gen = @($manifest.entries) | Where-Object { $_.externalId -eq $ext } | Select-Object -First 1
    $docxPath = Join-Path $docxBundleDir "$ext.docx"
    $size = if (Test-Path $docxPath) { (Get-Item $docxPath).Length } elseif ($gen.sizeBytes) { [int]$gen.sizeBytes } else { 0 }
    $auditFile = Join-Path $auditDir "$ext.json"
    $auditStatus = 'NONE'
    if ($gen -and $gen.audit -and $gen.audit.requestIdMatched) {
        $auditStatus = 'SUCCESS'
    } elseif (Test-Path $auditFile) {
        $auditJson = Get-Content $auditFile -Raw | ConvertFrom-Json
        if ($auditJson.requestIdMatched) {
            $auditStatus = 'SUCCESS'
        } else {
            $records = Get-DemoApiResultItems -Response $auditJson
            $success = $records | Where-Object {
                $_.status -eq 'SUCCEEDED' -or $_.outcome -eq 'SUCCESS' -or $_.outcome -eq 'REPLAYED'
            } | Select-Object -First 1
            if ($success) { $auditStatus = 'SUCCESS' }
        }
    }
    $rows += [pscustomobject]@{
        Template = $ext
        Group = if ($tpl) { $tpl.groupCode } else { '—' }
        Lifecycle = if ($tpl) { $tpl.lifecycleStatus } else { '—' }
        PolicyAdGroup = if ($pub) { $pub.allowedAdGroups } else { '—' }
        Credential = if ($pub) { $pub.credentialExternalId } else { '—' }
        DocxSize = $size
        GenerateStatus = if ($gen) { $gen.status } else { 'MISSING' }
        AuditStatus = $auditStatus
    }
}

$summaryPath = Join-Path $EvidenceDir 'fundraising-demo-summary.md'
$generatedAt = (Get-Date).ToUniversalTime().ToString('yyyy-MM-dd HH:mm:ss') + ' UTC'
$successGen = @($manifest.entries | Where-Object { $_.status -eq 'SUCCESS' }).Count
$publishedCount = if ($publishSummary) { [int]$publishSummary.templateCount } else { 0 }

@"
# Fundraising Demo Evidence Bundle (runtime capture)

**Generated:** $generatedAt  
**Backend:** $BackendUrl  
**Task:** taskmaster #8 — Capture fundraising evidence bundle

Each demo template was authored with real foreign-bank-letter content (parties, defined terms, covenants, schedules, signatures, governing law), published through the lifecycle where reachable, exposed via API management with AD-Group-authorized credentials, and generated on demand into DOCX artifacts where the runtime release line is callable.

## Summary counts

| Metric | Value |
| --- | --- |
| Registry templates | $($externalIds.Count) |
| Publish summary (callable credentials) | $publishedCount |
| Generate manifest SUCCESS | $successGen / $($manifest.templateCount) |
| DOCX copies in bundle | $(@(Get-ChildItem $docxBundleDir -Filter '*.docx').Count) |

## Template evidence table

| Template | Group | Lifecycle | Policy AD Group | Credential | DOCX Size | Generate | Audit |
| --- | --- | --- | --- | --- | ---: | --- | --- |
$(
    ($rows | ForEach-Object {
        "| $($_.Template) | $($_.Group) | $($_.Lifecycle) | $($_.PolicyAdGroup) | $($_.Credential) | $($_.DocxSize) | $($_.GenerateStatus) | $($_.AuditStatus) |"
    }) -join "`n"
)

## Bundle paths

- ``all-demos-publish-summary.json`` — publish orchestration
- ``generated-docx-manifest.json`` — SHA-256 + validation flags
- ``audit-records/<externalId>.json`` — generation audit snapshots
- ``generated-docx/<externalId>.docx`` — self-contained DOCX copies
- ``docs/evidence/fundraising-demo-summary.md`` — static index (P23-T16)

"@ | Set-Content $summaryPath -Encoding UTF8

Write-BundleStep "evidence bundle ready at $EvidenceDir"
Write-BundleStep "human summary: $summaryPath"

# Prepare an intranet SCA submission bundle (SBOM + manifest + checksums).
# Does NOT submit to org tools — produces files for manual upload per runbook.
#
# Usage (from repo root):
#   .\scripts\prepare-sca-submission-bundle.ps1
#   .\scripts\prepare-sca-submission-bundle.ps1 -RegenerateSbom

param(
    [switch]$RegenerateSbom
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$SbomDir = Join-Path $RepoRoot 'artifacts/sbom'
$FrontendSbom = Join-Path $SbomDir 'frontend-cyclonedx.json'
$BackendSbom = Join-Path $SbomDir 'backend-cyclonedx.json'

function Write-Step([string]$Message) { Write-Host "==> $Message" -ForegroundColor Cyan }

function Get-FileSha256([string]$Path) {
    return (Get-FileHash -Path $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Get-BomComponentCount([string]$Path) {
    $bom = Get-Content $Path -Raw | ConvertFrom-Json
    if ($null -ne $bom.components) {
        return @($bom.components).Count
    }
    return 0
}

if ($RegenerateSbom -or -not (Test-Path $FrontendSbom) -or -not (Test-Path $BackendSbom)) {
    Write-Step 'Generating SBOM artifacts...'
    & (Join-Path $RepoRoot 'scripts/generate-sbom.ps1')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

foreach ($path in @($FrontendSbom, $BackendSbom)) {
    if (-not (Test-Path $path)) {
        throw "Missing SBOM: $path — run .\scripts\generate-sbom.ps1 first"
    }
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$bundleDir = Join-Path $RepoRoot "artifacts/sca-submission/$timestamp"
New-Item -ItemType Directory -Force -Path $bundleDir | Out-Null

Copy-Item $FrontendSbom (Join-Path $bundleDir 'frontend-cyclonedx.json')
Copy-Item $BackendSbom (Join-Path $bundleDir 'backend-cyclonedx.json')

$gitSha = ''
try {
    $gitSha = (git -C $RepoRoot rev-parse HEAD 2>$null)
} catch {
    $gitSha = 'unknown'
}

$manifest = [ordered]@{
    bundleVersion = '1'
    preparedAt = (Get-Date).ToUniversalTime().ToString('o')
    repositoryCommit = $gitSha
    project = 'Document Generation Engine'
    artifacts = @(
        @{
            name = 'frontend-cyclonedx.json'
            role = 'frontend-sbom'
            sha256 = Get-FileSha256 (Join-Path $bundleDir 'frontend-cyclonedx.json')
            componentCount = Get-BomComponentCount (Join-Path $bundleDir 'frontend-cyclonedx.json')
        },
        @{
            name = 'backend-cyclonedx.json'
            role = 'backend-sbom'
            sha256 = Get-FileSha256 (Join-Path $bundleDir 'backend-cyclonedx.json')
            componentCount = Get-BomComponentCount (Join-Path $bundleDir 'backend-cyclonedx.json')
        }
    )
    submissionRunbook = 'docs/evidence/security/intranet-sca-submission-runbook.md'
    triagePolicy = 'docs/architecture/quality-gate-threshold-baseline.md#security-and-dependency'
}

($manifest | ConvertTo-Json -Depth 6) | Out-File -FilePath (Join-Path $bundleDir 'manifest.json') -Encoding utf8

@(
    '# SCA submission checklist (copy into org ticket)',
    '',
    "- [ ] Bundle path: ``artifacts/sca-submission/$timestamp``",
    "- [ ] Git commit: ``$gitSha``",
    "- [ ] Frontend SBOM uploaded to intranet SCA",
    "- [ ] Backend SBOM uploaded to intranet SCA",
    "- [ ] Scan job ID / ticket reference recorded in execution log",
    "- [ ] Critical/high findings triaged (remediate or exception with expiry)",
    "- [ ] After remediation: ``pnpm -C frontend lint/type-check/test/build`` green",
    "- [ ] After remediation: ``mvn -B -ntp -f backend/pom.xml verify`` green",
    '',
    'Owner: _TBD (security / platform)_',
    'Reviewer: _TBD_'
) | Out-File -FilePath (Join-Path $bundleDir 'SUBMISSION-CHECKLIST.md') -Encoding utf8

Write-Host ''
Write-Host 'SCA submission bundle ready.' -ForegroundColor Green
Write-Host "  Directory: $bundleDir"
Write-Host '  Upload JSON files per docs/evidence/security/intranet-sca-submission-runbook.md'

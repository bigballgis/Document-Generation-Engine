# CORE-FORTRESS evidence bundle — gate logs + runtime snapshots
param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$IncludeRuntimeSnapshots,
    [string]$BackendUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$timestampUtc = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
$folderTimestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$evidenceDir = Join-Path $root "artifacts/core-fortress-evidence/$folderTimestamp"
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null

$status = 'PASSED'
$failReason = $null

function Write-EvidenceFile($name, $content) {
    $path = Join-Path $evidenceDir $name
    if ($content -is [string]) {
        $content | Out-File -FilePath $path -Encoding utf8
    } else {
        $content | ConvertTo-Json -Depth 8 | Out-File -FilePath $path -Encoding utf8
    }
    Write-Host "Evidence: $path"
}

if (-not $SkipBackend) {
    Write-Host '=== Evidence Bundle: Backend verify ===' -ForegroundColor Cyan
    Push-Location (Join-Path $root 'backend')
    $backendLog = Join-Path $evidenceDir 'backend-verify.log'
    mvn -B -ntp verify 2>&1 | Tee-Object -FilePath $backendLog
    if ($LASTEXITCODE -ne 0) {
        $status = 'FAILED'
        $failReason = 'backend verify failed'
    }
    Pop-Location
}

if ($status -eq 'PASSED' -and -not $SkipFrontend) {
    Write-Host '=== Evidence Bundle: Frontend gates ===' -ForegroundColor Cyan
    Push-Location (Join-Path $root 'frontend')
    if (-not (Test-Path 'node_modules')) { pnpm install }
    $frontendLog = Join-Path $evidenceDir 'frontend-gates.log'
    {
        pnpm lint
        pnpm type-check
        pnpm test
        pnpm build
    } 2>&1 | Tee-Object -FilePath $frontendLog
    if ($LASTEXITCODE -ne 0) {
        $status = 'FAILED'
        $failReason = 'frontend gates failed'
    }
    Pop-Location
}

if ($IncludeRuntimeSnapshots) {
    Write-Host '=== Evidence Bundle: Runtime snapshots ===' -ForegroundColor Cyan
    try {
        $healthz = Invoke-WebRequest -Uri "$BackendUrl/healthz" -UseBasicParsing -TimeoutSec 10
        Write-EvidenceFile 'healthz.txt' $healthz.Content
    } catch {
        Write-EvidenceFile 'healthz.txt' "FAILED: $($_.Exception.Message)"
        if ($status -eq 'PASSED') { $status = 'FAILED'; $failReason = 'healthz unreachable' }
    }
    try {
        $readyz = Invoke-WebRequest -Uri "$BackendUrl/readyz" -UseBasicParsing -TimeoutSec 10
        Write-EvidenceFile 'readyz.json' $readyz.Content
    } catch {
        Write-EvidenceFile 'readyz.json' "{ `"error`": `"$($_.Exception.Message)`" }"
    }
    try {
        $prom = Invoke-WebRequest -Uri "$BackendUrl/actuator/prometheus" -UseBasicParsing -TimeoutSec 15
        $sample = ($prom.Content -split "`n" | Select-String -Pattern 'docgen_generation|docgen\.generation|docgen_pdf_conversion' | Select-Object -First 40) -join "`n"
        if (-not $sample) { $sample = '# no docgen SLO series found in first pass' }
        Write-EvidenceFile 'prometheus-sample.txt' $sample
    } catch {
        Write-EvidenceFile 'prometheus-sample.txt' "# FAILED: $($_.Exception.Message)"
    }
}

$gitSha = ''
Push-Location $root
try { $gitSha = (git rev-parse HEAD).Trim() } catch { $gitSha = 'unknown' }
Pop-Location

$summary = [ordered]@{
    gateVersion = 'core-fortress-evidence-v1'
    timestamp   = $timestampUtc
    gitSha      = $gitSha
    status      = $status
    failReason  = $failReason
    evidenceDir = $evidenceDir
}
Write-EvidenceFile 'summary.json' $summary

Copy-Item (Join-Path $root 'docs/operations/core-fortress-release-checklist.md') (Join-Path $evidenceDir 'CHECKLIST.md')

Write-Host "=== Evidence Bundle: $status ===" -ForegroundColor $(if ($status -eq 'PASSED') { 'Green' } else { 'Red' })
Write-Host "Directory: $evidenceDir"

if ($status -ne 'PASSED') { exit 1 }

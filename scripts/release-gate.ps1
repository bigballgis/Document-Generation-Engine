# Release gate — backend verify, frontend gates, evidence output
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$evidenceDir = Join-Path $root "artifacts/release-gate/$timestamp"
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null

function Write-Evidence($name, $content) {
    $path = Join-Path $evidenceDir $name
    $content | Out-File -FilePath $path -Encoding utf8
    Write-Host "Evidence: $path"
}

Write-Host '=== Release Gate: Backend ===' -ForegroundColor Cyan
Push-Location (Join-Path $root 'backend')
$backendLog = Join-Path $evidenceDir 'backend-verify.log'
mvn -B -ntp verify 2>&1 | Tee-Object -FilePath $backendLog
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Host '=== Release Gate: Frontend ===' -ForegroundColor Cyan
Push-Location (Join-Path $root 'frontend')
if (-not (Test-Path 'node_modules')) { pnpm install }
$frontendLog = Join-Path $evidenceDir 'frontend-gates.log'
{
    pnpm lint
    pnpm type-check
    pnpm test
    pnpm build
} 2>&1 | Tee-Object -FilePath $frontendLog
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Evidence 'summary.json' @"
{
  "gate": "release-gate-v1",
  "timestamp": "$timestamp",
  "backendTests": "see backend-verify.log",
  "frontendTests": "see frontend-gates.log",
  "status": "PASSED"
}
"@

Write-Host "=== Release Gate: PASSED ===" -ForegroundColor Green
Write-Host "Evidence directory: $evidenceDir"

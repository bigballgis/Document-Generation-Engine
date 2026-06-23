# P0 quality gate — backend verify + frontend lint/type-check/test/build
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

Write-Host '=== P0 Gate: Backend ===' -ForegroundColor Cyan
Push-Location (Join-Path $root 'backend')
mvn -B -ntp verify
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Host '=== P0 Gate: Frontend ===' -ForegroundColor Cyan
Push-Location (Join-Path $root 'frontend')
if (-not (Test-Path 'node_modules')) {
    pnpm install
}
pnpm lint
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
pnpm type-check
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
pnpm test
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
pnpm build
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Host '=== P0 Gate: PASSED ===' -ForegroundColor Green

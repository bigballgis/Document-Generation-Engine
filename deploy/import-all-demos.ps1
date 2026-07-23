# Import keep-set demo packages only (TM #164 / demo-catalog-keep-bank-letters).
# Seven packages → eight Live bank-letter templates. Authoritative load path is
# deploy package + PowerShell import (not Java ApplicationRunner seed).
#
# After import, run deploy/publish-all-demos.ps1 to publish + API credentials.
#
# Usage (from repo root):
#   .\deploy\import-all-demos.ps1
#   .\deploy\import-all-demos.ps1 -SkipSql

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$PostgresContainer = 'docgen-postgres',
    [switch]$SkipSql,
    [switch]$SkipApi,
    [switch]$RegenerateCatalog,
    [switch]$SkipMasterRefresh
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

$DemoScripts = @(
    @{ Name = 'FOL Wholesale'; Script = 'demo-fol/import-fol-demo.ps1' },
    @{ Name = 'Credit Limit'; Script = 'demo-credit-limit/import-credit-limit-demo.ps1' },
    @{ Name = 'Annual Review'; Script = 'demo-annual-review/import-annual-review-demo.ps1' },
    @{ Name = 'Facility Amendment'; Script = 'demo-facility-amendment/import-facility-amendment-demo.ps1' },
    @{ Name = 'Commitment'; Script = 'demo-commitment/import-commitment-demo.ps1' },
    @{ Name = 'Formal Demand'; Script = 'demo-formal-demand/import-formal-demand-demo.ps1' },
    @{ Name = 'Covenant Waiver'; Script = 'demo-covenant-waiver/import-covenant-waiver-demo.ps1' }
)

Write-Host '==> import-all-demos: starting keep-set import chain (8 templates / 7 packages)'
foreach ($demo in $DemoScripts) {
    $scriptPath = Join-Path $RepoRoot $demo.Script
    if (-not (Test-Path $scriptPath)) {
        Write-Warning "SKIP $($demo.Name): script not found at $scriptPath"
        continue
    }
    Write-Host "==> import-all-demos: $($demo.Name)"
    $importParams = @{
        BackendUrl         = $BackendUrl
        PostgresContainer  = $PostgresContainer
    }
    if ($SkipSql) { $importParams.SkipSql = $true }
    if ($SkipApi) { $importParams.SkipApi = $true }
    if ($RegenerateCatalog) { $importParams.RegenerateCatalog = $true }
    if ($SkipMasterRefresh) { $importParams.SkipMasterRefresh = $true }
    & $scriptPath @importParams
    if ($LASTEXITCODE -ne 0) { throw "Import failed for $($demo.Name) (exit $LASTEXITCODE)" }
}
Write-Host '==> import-all-demos: completed successfully'

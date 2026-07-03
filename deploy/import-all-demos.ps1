# Import all demo packages in priority order (P22 R5).
# Usage (from repo root):
#   .\deploy\import-all-demos.ps1
#   .\deploy\import-all-demos.ps1 -SkipSql

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$PostgresContainer = 'docgen-postgres',
    [switch]$SkipSql,
    [switch]$RegenerateCatalog
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

$DemoScripts = @(
    @{ Name = 'FOL Wholesale'; Script = 'demo-fol/import-fol-demo.ps1' },
    @{ Name = 'Retail Account'; Script = 'demo-retail-account/import-retail-account-demo.ps1' },
    @{ Name = 'Mortgage'; Script = 'demo-mortgage/import-mortgage-demo.ps1' },
    @{ Name = 'Credit Limit'; Script = 'demo-credit-limit/import-credit-limit-demo.ps1' },
    @{ Name = 'Trade LC'; Script = 'demo-trade-lc/import-trade-lc-demo.ps1' },
    @{ Name = 'Collection'; Script = 'demo-collection/import-collection-demo.ps1' },
    @{ Name = 'Annual Review'; Script = 'demo-annual-review/import-annual-review-demo.ps1' },
    @{ Name = 'Wealth'; Script = 'demo-wealth/import-wealth-demo.ps1' }
)

Write-Host '==> import-all-demos: starting priority import chain'
foreach ($demo in $DemoScripts) {
    $scriptPath = Join-Path $RepoRoot $demo.Script
    if (-not (Test-Path $scriptPath)) {
        Write-Warning "SKIP $($demo.Name): script not found at $scriptPath"
        continue
    }
    Write-Host "==> import-all-demos: $($demo.Name)"
    $args = @('-BackendUrl', $BackendUrl, '-PostgresContainer', $PostgresContainer)
    if ($SkipSql) { $args += '-SkipSql' }
    if ($RegenerateCatalog) { $args += '-RegenerateCatalog' }
    & $scriptPath @args
    if ($LASTEXITCODE -ne 0) { throw "Import failed for $($demo.Name) (exit $LASTEXITCODE)" }
}
Write-Host '==> import-all-demos: completed successfully'

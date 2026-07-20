# Import all demo packages in priority order (P22 R5 / P23-T12 / Wave B #142).
# Covers Wave A 8 deploy/demo-* families + Wave B 7 families (19 package templates).
# DEMO-FULL-FLOW-LETTER is seeded separately via docgen.demo-catalog.seed-enabled=true
# (DemoFullFlowCatalogSeeder) or E2E helpers — registry total 20 external IDs after publish.
# After import, run deploy/publish-all-demos.ps1 to publish all templates + API credentials.
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
    @{ Name = 'Retail Account'; Script = 'demo-retail-account/import-retail-account-demo.ps1' },
    @{ Name = 'Mortgage'; Script = 'demo-mortgage/import-mortgage-demo.ps1' },
    @{ Name = 'Credit Limit'; Script = 'demo-credit-limit/import-credit-limit-demo.ps1' },
    @{ Name = 'Trade LC'; Script = 'demo-trade-lc/import-trade-lc-demo.ps1' },
    @{ Name = 'Collection'; Script = 'demo-collection/import-collection-demo.ps1' },
    @{ Name = 'Annual Review'; Script = 'demo-annual-review/import-annual-review-demo.ps1' },
    @{ Name = 'Wealth'; Script = 'demo-wealth/import-wealth-demo.ps1' },
    @{ Name = 'Facility Amendment'; Script = 'demo-facility-amendment/import-facility-amendment-demo.ps1' },
    @{ Name = 'KYC CDD'; Script = 'demo-kyc-cdd/import-kyc-cdd-demo.ps1' },
    @{ Name = 'Account Closure'; Script = 'demo-account-closure/import-account-closure-demo.ps1' },
    @{ Name = 'Commitment'; Script = 'demo-commitment/import-commitment-demo.ps1' },
    @{ Name = 'Formal Demand'; Script = 'demo-formal-demand/import-formal-demand-demo.ps1' },
    @{ Name = 'Covenant Waiver'; Script = 'demo-covenant-waiver/import-covenant-waiver-demo.ps1' },
    @{ Name = 'Insurance Endorsement'; Script = 'demo-insurance-endorsement/import-insurance-endorsement-demo.ps1' }
)

Write-Host '==> import-all-demos: starting priority import chain'
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

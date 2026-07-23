# OBSOLETE for slim bank-letter catalog (TM #164).
#
# This script historically kept ONLY CORP-FOL-OFFER and deleted all other templates.
# Running it against the keep-set of 8 would delete the other seven Live bank letters.
#
# Use instead (from repo root):
#   .\deploy\cleanup-demo-catalog-keep-list.ps1
#   .\deploy\cleanup-demo-catalog-keep-list.ps1 -WhatIf
#
# This wrapper refuses to run the FOL-only cleanup and redirects operators.

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$PostgresContainer = 'docgen-postgres',
    [string]$PostgresUser = $(if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'docgen' }),
    [string]$PostgresDb = $(if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'docgen' }),
    [switch]$WhatIf,
    [switch]$ForceFolOnlyUnsafe
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$KeepListScript = Join-Path $RepoRoot 'cleanup-demo-catalog-keep-list.ps1'

if (-not $ForceFolOnlyUnsafe) {
    Write-Host "==> cleanup-catalog-except-fol.ps1 is superseded by keep-set cleanup (8 templates)."
    Write-Host "    Redirecting to: $KeepListScript"
    if (-not (Test-Path $KeepListScript)) {
        throw "Missing keep-list cleanup script: $KeepListScript"
    }
    & $KeepListScript `
        -BackendUrl $BackendUrl `
        -PostgresContainer $PostgresContainer `
        -PostgresUser $PostgresUser `
        -PostgresDb $PostgresDb `
        -WhatIf:$WhatIf
    exit $LASTEXITCODE
}

throw "ForceFolOnlyUnsafe is blocked for demo-catalog-keep-bank-letters — use cleanup-demo-catalog-keep-list.ps1"

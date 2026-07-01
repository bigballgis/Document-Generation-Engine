# Remove all masters and templates except the wholesale FOL executive demo catalog.
#
# Usage (from repo root):
#   .\deploy\demo-fol\cleanup-catalog-except-fol.ps1
#   .\deploy\demo-fol\cleanup-catalog-except-fol.ps1 -BackendUrl http://localhost:8080 -PostgresContainer docgen-postgres
#   .\deploy\demo-fol\cleanup-catalog-except-fol.ps1 -WhatIf

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$PostgresContainer = 'docgen-postgres',
    [string]$PostgresUser = $(if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'docgen' }),
    [string]$PostgresDb = $(if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'docgen' }),
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'
$DemoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigPath = Join-Path $DemoRoot 'config/fol-template-config.json'
$ApiBase = "$BackendUrl/api/management/v1"

function Write-Step([string]$Message) { Write-Host "==> $Message" }

function Get-Token([string]$Username, [string]$Password) {
    $login = Invoke-RestMethod -Method POST -Uri "$ApiBase/auth/login" -ContentType 'application/json' -Body (@{ username = $Username; password = $Password } | ConvertTo-Json)
    return $login.result.accessToken
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body = $null
    )
    $Headers = @{ Authorization = "Bearer $Token" }
    $Uri = "$ApiBase$Path"
    if ($Body -ne $null) {
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 20)
    }
    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
}

if (-not (Test-Path $ConfigPath)) { throw "Missing config: $ConfigPath" }
$Config = Get-Content $ConfigPath -Raw | ConvertFrom-Json
$KeepTemplateExternalId = $Config.templateExternalId
$KeepMasterName = $Config.masterName

Write-Step "Keeping FOL demo only:"
Write-Host "  Master : $KeepMasterName"
Write-Host "  Template: $KeepTemplateExternalId"

Write-Step "Waiting for backend $BackendUrl/healthz ..."
$healthy = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri "$BackendUrl/healthz" -UseBasicParsing -TimeoutSec 5
        if ($resp.StatusCode -eq 200) { $healthy = $true; break }
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $healthy) { throw "Backend not healthy at $BackendUrl/healthz" }

$AdminToken = Get-Token '10000001' 'ChangeMe123!'

$templates = @(Invoke-Api GET '/templates' $AdminToken).result
$toDelete = $templates | Where-Object { $_.externalId -ne $KeepTemplateExternalId }

Write-Step "Templates: $($templates.Count) total, $($toDelete.Count) to remove, 1 to keep"
foreach ($tpl in $toDelete) {
    $label = "$($tpl.externalId) ($($tpl.id))"
    if ($WhatIf) {
        Write-Host "  [WhatIf] DELETE template $label"
        continue
    }
    try {
        Invoke-RestMethod -Method DELETE -Uri "$ApiBase/templates/$($tpl.id)" -Headers @{ Authorization = "Bearer $AdminToken" } -ContentType 'application/json' -Body (@{ reason = 'FOL-only catalog cleanup'; confirmed = $true } | ConvertTo-Json)
        Write-Host "  Deleted template $label"
    } catch {
        Write-Warning "  Failed to delete template $label : $($_.Exception.Message)"
    }
}

$remainingTemplates = @(Invoke-Api GET '/templates' $AdminToken).result
$folTemplate = $remainingTemplates | Where-Object { $_.externalId -eq $KeepTemplateExternalId } | Select-Object -First 1
if (-not $folTemplate) {
    Write-Warning "FOL template '$KeepTemplateExternalId' not found after cleanup. Run .\deploy\demo-fol\import-fol-demo.ps1"
}

$masters = @(Invoke-Api GET '/masters' $AdminToken).result
$mastersToRemove = $masters | Where-Object { $_.name -ne $KeepMasterName }

Write-Step "Masters: $($masters.Count) total, $($mastersToRemove.Count) to remove, 1 to keep"
if ($mastersToRemove.Count -gt 0) {
    $ids = ($mastersToRemove | ForEach-Object { "'$($_.id)'" }) -join ', '
    $names = ($mastersToRemove | ForEach-Object { $_.name }) -join '; '
    $sql = @"
UPDATE master_document
SET deleted_at = (NOW() AT TIME ZONE 'UTC'),
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE deleted_at IS NULL
  AND id IN ($ids);
"@
    if ($WhatIf) {
        Write-Host "  [WhatIf] Soft-delete masters: $names"
    } else {
        Write-Step "Soft-deleting non-FOL masters in postgres..."
        $sql | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDb -v ON_ERROR_STOP=1
        if ($LASTEXITCODE -ne 0) { throw "Master cleanup SQL failed (exit $LASTEXITCODE)" }
        Write-Host "  Removed masters: $names"
    }
}

$remainingMasters = @(Invoke-Api GET '/masters' $AdminToken).result
Write-Step "Done. Remaining catalog:"
Write-Host "  Templates: $(@($remainingTemplates | ForEach-Object { $_.externalId }) -join ', ')"
Write-Host "  Masters  : $(@($remainingMasters | ForEach-Object { $_.name }) -join ', ')"
if (-not $WhatIf) {
    Write-Host ""
    Write-Host "Tip: set DOCGEN_SEED_DEMO_CATALOG=false on docgen-backend so RETAIL demo is not re-seeded on restart."
}

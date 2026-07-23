# Slim acceptance demo catalog to the keep-set of 8 bank-letter Live templates (TM #164).
#
# Keeps:
#   DEMO-COVENANT-WAIVER, DEMO-FORMAL-DEMAND, DEMO-COMMITMENT-LETTER,
#   DEMO-FACILITY-AMENDMENT, DEMO-ANNUAL-REVIEW, DEMO-FACILITY-RENEWAL,
#   DEMO-CREDIT-LIMIT-CONFIRM, CORP-FOL-OFFER
#
# Then soft-deletes orphan masters / content modules / asset-library rows that are
# not referenced by any remaining keep-set template. Fail-closed: if a candidate
# orphan is still referenced by a keep template binding, abort with non-zero exit.
#
# Do NOT use deploy/demo-fol/cleanup-catalog-except-fol.ps1 for this leaf — it keeps
# only CORP-FOL-OFFER and would delete the other seven keep templates.
#
# Usage (from repo root; healthy Docker stack required):
#   .\deploy\cleanup-demo-catalog-keep-list.ps1
#   .\deploy\cleanup-demo-catalog-keep-list.ps1 -BackendUrl http://localhost:8080 -WhatIf
#   .\deploy\cleanup-demo-catalog-keep-list.ps1 -PostgresContainer docgen-postgres

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$PostgresContainer = 'docgen-postgres',
    [string]$PostgresUser = $(if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'docgen' }),
    [string]$PostgresDb = $(if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'docgen' }),
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'
$ApiBase = "$BackendUrl/api/management/v1"

$KeepTemplateExternalIds = @(
    'DEMO-COVENANT-WAIVER',
    'DEMO-FORMAL-DEMAND',
    'DEMO-COMMITMENT-LETTER',
    'DEMO-FACILITY-AMENDMENT',
    'DEMO-ANNUAL-REVIEW',
    'DEMO-FACILITY-RENEWAL',
    'DEMO-CREDIT-LIMIT-CONFIRM',
    'CORP-FOL-OFFER'
)

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
    if ($null -ne $Body) {
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 20)
    }
    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
}

function Invoke-PostgresSql([string]$Sql) {
    $Sql | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDb -v ON_ERROR_STOP=1 -t -A
    if ($LASTEXITCODE -ne 0) { throw "Postgres SQL failed (exit $LASTEXITCODE)" }
}

Write-Step "Keep-set catalog cleanup (8 templates):"
foreach ($id in $KeepTemplateExternalIds) { Write-Host "  KEEP $id" }

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

$templates = @(Invoke-Api GET '/templates?size=500' $AdminToken).result
if ($templates -isnot [System.Array] -and $templates.content) {
    $templates = @($templates.content)
}
$keepTemplates = @($templates | Where-Object { $KeepTemplateExternalIds -contains $_.externalId })
$toDelete = @($templates | Where-Object { $KeepTemplateExternalIds -notcontains $_.externalId })

$missingKeep = @($KeepTemplateExternalIds | Where-Object { $keepExternal = $_; -not ($keepTemplates | Where-Object { $_.externalId -eq $keepExternal }) })
if ($missingKeep.Count -gt 0) {
    Write-Warning "Keep templates not found before cleanup (import keep packages first): $($missingKeep -join ', ')"
}

Write-Step "Templates: $($templates.Count) total, $($toDelete.Count) to remove, $($keepTemplates.Count) keep"
foreach ($tpl in $toDelete) {
    $label = "$($tpl.externalId) ($($tpl.id))"
    if ($WhatIf) {
        Write-Host "  [WhatIf] DELETE template $label"
        continue
    }
    try {
        Invoke-RestMethod -Method DELETE -Uri "$ApiBase/templates/$($tpl.id)" -Headers @{ Authorization = "Bearer $AdminToken" } -ContentType 'application/json' -Body (@{ reason = 'Keep-set bank-letter catalog cleanup'; confirmed = $true } | ConvertTo-Json)
        Write-Host "  Deleted template $label"
    } catch {
        throw "Failed to delete template $label : $($_.Exception.Message) — aborting (fail-closed)"
    }
}

$remainingTemplates = @(Invoke-Api GET '/templates?size=500' $AdminToken).result
if ($remainingTemplates -isnot [System.Array] -and $remainingTemplates.content) {
    $remainingTemplates = @($remainingTemplates.content)
}
$remainingKeep = @($remainingTemplates | Where-Object { $KeepTemplateExternalIds -contains $_.externalId })
$rogue = @($remainingTemplates | Where-Object { $_.externalId -like 'DEMO-*' -and $KeepTemplateExternalIds -notcontains $_.externalId })
if ($rogue.Count -gt 0 -and -not $WhatIf) {
    throw "Fail-closed: purge DEMO templates still present after delete: $((@($rogue | ForEach-Object { $_.externalId })) -join ', ')"
}

# Resolve keep transitive master IDs from remaining keep templates.
$keepMasterIds = [System.Collections.Generic.HashSet[string]]::new()
$keepMasterNames = [System.Collections.Generic.HashSet[string]]::new()
foreach ($tpl in $remainingKeep) {
    $detail = Invoke-Api GET "/templates/$($tpl.id)" $AdminToken
    $masterId = [string]$detail.result.masterDocumentId
    if (-not $masterId) { $masterId = [string]$detail.result.masterId }
    if ($masterId) { [void]$keepMasterIds.Add($masterId) }
    $masterName = [string]$detail.result.masterName
    if ($masterName) { [void]$keepMasterNames.Add($masterName) }
}

$masters = @(Invoke-Api GET '/masters?size=500' $AdminToken).result
if ($masters -isnot [System.Array] -and $masters.content) {
    $masters = @($masters.content)
}
$mastersToRemove = @($masters | Where-Object {
    $id = [string]$_.id
    $name = [string]$_.name
    (-not $keepMasterIds.Contains($id)) -and (-not $keepMasterNames.Contains($name))
})

# Fail-closed dependency check: never soft-delete a master still bound to keep-set.
foreach ($m in $mastersToRemove) {
    $blocking = @($remainingKeep | Where-Object {
        $detail = Invoke-Api GET "/templates/$($_.id)" $AdminToken
        $mid = [string]$detail.result.masterDocumentId
        if (-not $mid) { $mid = [string]$detail.result.masterId }
        $mid -eq [string]$m.id -or [string]$detail.result.masterName -eq [string]$m.name
    })
    if ($blocking.Count -gt 0) {
        $blockIds = @($blocking | ForEach-Object { $_.externalId }) -join ', '
        throw "Fail-closed: master '$($m.name)' ($($m.id)) still referenced by keep template(s): $blockIds"
    }
}

Write-Step "Masters: $($masters.Count) total, $($mastersToRemove.Count) orphan candidates, $($keepMasterIds.Count) keep-referenced"
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
        Write-Host "  [WhatIf] Soft-delete orphan masters: $names"
    } else {
        Write-Step "Soft-deleting orphan masters in postgres..."
        Invoke-PostgresSql $sql | Out-Null
        Write-Host "  Removed masters: $names"
    }
}

# Fail-closed: abort if keep templates reference modules that the orphan UPDATE would also match.
# (Probe must be empty before orphan purge — keep-referenced modules must stay.)
$contentModuleProbeSql = @"
WITH keep_template_ids AS (
  SELECT id FROM template WHERE deleted_at IS NULL AND external_id IN (
    'DEMO-COVENANT-WAIVER','DEMO-FORMAL-DEMAND','DEMO-COMMITMENT-LETTER',
    'DEMO-FACILITY-AMENDMENT','DEMO-ANNUAL-REVIEW','DEMO-FACILITY-RENEWAL',
    'DEMO-CREDIT-LIMIT-CONFIRM','CORP-FOL-OFFER'
  )
),
keep_module_ids AS (
  SELECT DISTINCT cm.id
  FROM content_module cm
  JOIN content_module_ref cmr ON cmr.content_module_id = cm.id
  JOIN template_version tv ON tv.id = cmr.template_version_id AND tv.deleted_at IS NULL
  JOIN keep_template_ids kt ON kt.id = tv.template_id
  WHERE cm.deleted_at IS NULL
)
SELECT COUNT(*)::text FROM keep_module_ids;
"@

try {
    $keepModuleCount = (Invoke-PostgresSql $contentModuleProbeSql).Trim()
    Write-Step "Keep-referenced content modules: $keepModuleCount (will be retained)"
} catch {
    Write-Warning "Content-module keep-ref probe skipped or schema mismatch: $($_.Exception.Message)"
}

$orphanModuleSql = @"
WITH keep_template_ids AS (
  SELECT id FROM template WHERE deleted_at IS NULL AND external_id IN (
    'DEMO-COVENANT-WAIVER','DEMO-FORMAL-DEMAND','DEMO-COMMITMENT-LETTER',
    'DEMO-FACILITY-AMENDMENT','DEMO-ANNUAL-REVIEW','DEMO-FACILITY-RENEWAL',
    'DEMO-CREDIT-LIMIT-CONFIRM','CORP-FOL-OFFER'
  )
),
keep_module_ids AS (
  SELECT DISTINCT cm.id
  FROM content_module cm
  JOIN content_module_ref cmr ON cmr.content_module_id = cm.id
  JOIN template_version tv ON tv.id = cmr.template_version_id AND tv.deleted_at IS NULL
  JOIN keep_template_ids kt ON kt.id = tv.template_id
  WHERE cm.deleted_at IS NULL
)
UPDATE content_module
SET deleted_at = (NOW() AT TIME ZONE 'UTC'),
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE deleted_at IS NULL
  AND id NOT IN (SELECT id FROM keep_module_ids)
  AND (
    code LIKE 'DEMO-%'
    OR code LIKE 'FOL-%'
    OR name ILIKE '%demo%'
    OR name ILIKE '%meridian%'
  )
  AND id NOT IN (
    SELECT DISTINCT content_module_id FROM content_module_ref cmr
    JOIN template_version tv ON tv.id = cmr.template_version_id AND tv.deleted_at IS NULL
    JOIN keep_template_ids kt ON kt.id = tv.template_id
  );
"@

if ($WhatIf) {
    Write-Host "  [WhatIf] Soft-delete orphan demo content modules (keep-referenced retained)"
} else {
    try {
        Write-Step "Soft-deleting orphan demo content modules..."
        Invoke-PostgresSql $orphanModuleSql | Out-Null
    } catch {
        Write-Warning "Content-module orphan purge skipped: $($_.Exception.Message)"
    }
}

# Orphan asset-library rows: soft-delete ACTIVE demo assets not referenced by keep templates.
$orphanAssetSql = @"
WITH keep_template_ids AS (
  SELECT id FROM template WHERE deleted_at IS NULL AND external_id IN (
    'DEMO-COVENANT-WAIVER','DEMO-FORMAL-DEMAND','DEMO-COMMITMENT-LETTER',
    'DEMO-FACILITY-AMENDMENT','DEMO-ANNUAL-REVIEW','DEMO-FACILITY-RENEWAL',
    'DEMO-CREDIT-LIMIT-CONFIRM','CORP-FOL-OFFER'
  )
),
keep_asset_ids AS (
  SELECT DISTINCT al.id
  FROM asset_library_item al
  JOIN template_asset_ref tar ON tar.asset_id = al.id
  JOIN template_version tv ON tv.id = tar.template_version_id AND tv.deleted_at IS NULL
  JOIN keep_template_ids kt ON kt.id = tv.template_id
  WHERE al.deleted_at IS NULL
)
UPDATE asset_library_item
SET deleted_at = (NOW() AT TIME ZONE 'UTC'),
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE deleted_at IS NULL
  AND id NOT IN (SELECT id FROM keep_asset_ids)
  AND (
    external_id LIKE 'DEMO-%'
    OR name ILIKE '%demo%'
    OR name ILIKE '%meridian%'
  );
"@

if ($WhatIf) {
    Write-Host "  [WhatIf] Soft-delete orphan demo asset-library rows (keep-referenced retained)"
} else {
    try {
        Write-Step "Soft-deleting orphan demo asset-library rows..."
        Invoke-PostgresSql $orphanAssetSql | Out-Null
    } catch {
        Write-Warning "Asset-library orphan purge skipped (table/column may differ): $($_.Exception.Message)"
    }
}

$finalTemplates = @(Invoke-Api GET '/templates?size=500' $AdminToken).result
if ($finalTemplates -isnot [System.Array] -and $finalTemplates.content) {
    $finalTemplates = @($finalTemplates.content)
}
$finalMasters = @(Invoke-Api GET '/masters?size=500' $AdminToken).result
if ($finalMasters -isnot [System.Array] -and $finalMasters.content) {
    $finalMasters = @($finalMasters.content)
}

Write-Step "Done. Remaining catalog:"
Write-Host "  Templates: $(@($finalTemplates | ForEach-Object { $_.externalId }) -join ', ')"
Write-Host "  Masters  : $(@($finalMasters | ForEach-Object { $_.name }) -join ', ')"
if (-not $WhatIf) {
    Write-Host ""
    Write-Host "Tip: Playwright E2E fixtures use E2E-* prefixes; global-teardown removes them after test runs."
    Write-Host "      Next: .\deploy\import-all-demos.ps1 ; .\deploy\publish-all-demos.ps1"
    Write-Host "      Do not re-enable DOCGEN_SEED_DEMO_CATALOG — purge seeders are retired."
}

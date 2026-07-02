# Remove duplicate FOL executive demo test data sets; keep one canonical row.
# Also optional: remove non-FOL templates (E2E leftovers) via cleanup-catalog-except-fol.ps1.
#
# Usage:
#   .\deploy\demo-fol\cleanup-fol-test-data-sets.ps1
#   .\deploy\demo-fol\cleanup-fol-test-data-sets.ps1 -AlsoRemoveNonFolTemplates

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$PostgresContainer = 'docgen-postgres',
    [string]$PostgresUser = $(if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'docgen' }),
    [string]$PostgresDb = $(if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'docgen' }),
    [string]$KeepTemplateExternalId = 'CORP-FOL-OFFER',
    [string]$KeepScenarioName = 'Syndicated term loan - Pacific Rim USD 250m (LMA IG baseline)',
    [switch]$AlsoRemoveNonFolTemplates,
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'
$DemoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Write-Step([string]$Message) { Write-Host "==> $Message" }

Write-Step "Inspecting executive demo test data sets for $KeepTemplateExternalId..."
$before = docker exec $PostgresContainer psql -U $PostgresUser -d $PostgresDb -t -A -c @"
SELECT COUNT(*) FROM template_test_data_set tds
JOIN template t ON t.id = tds.template_id
WHERE t.external_id = '$KeepTemplateExternalId' AND t.deleted_at IS NULL
  AND (
      tds.scenario_name = '$KeepScenarioName'
      OR tds.name LIKE 'Executive walkthrough%'
      OR tds.coverage_tags_json LIKE '%executive-demo%'
  );
"@

Write-Host "  Before: $before row(s)"

if ($WhatIf) {
    docker exec $PostgresContainer psql -U $PostgresUser -d $PostgresDb -c @"
SELECT tds.external_id, tds.name, tds.scenario_name, tds.locked, tds.updated_at
FROM template_test_data_set tds
JOIN template t ON t.id = tds.template_id
WHERE t.external_id = '$KeepTemplateExternalId' AND t.deleted_at IS NULL
ORDER BY tds.updated_at DESC;
"@
    Write-Host "[WhatIf] Would keep newest row matching scenario or executive-demo tag; delete the rest."
    exit 0
}

$sql = @"
WITH fol AS (
    SELECT id FROM template WHERE external_id = '$KeepTemplateExternalId' AND deleted_at IS NULL LIMIT 1
),
keeper AS (
    SELECT id, external_id FROM template_test_data_set
    WHERE template_id = (SELECT id FROM fol)
      AND (
          scenario_name = '$KeepScenarioName'
          OR name LIKE 'Executive walkthrough%'
          OR coverage_tags_json LIKE '%executive-demo%'
      )
    ORDER BY
        CASE WHEN scenario_name = '$KeepScenarioName' THEN 0 ELSE 1 END,
        updated_at DESC
    LIMIT 1
)
UPDATE template_test_data_set child
SET derived_from_id = NULL,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE child.template_id = (SELECT id FROM fol)
  AND child.derived_from_id IN (
      SELECT id FROM template_test_data_set
      WHERE template_id = (SELECT id FROM fol)
        AND id NOT IN (SELECT id FROM keeper)
  );
WITH fol AS (
    SELECT id FROM template WHERE external_id = '$KeepTemplateExternalId' AND deleted_at IS NULL LIMIT 1
),
keeper AS (
    SELECT id FROM template_test_data_set
    WHERE template_id = (SELECT id FROM fol)
      AND (
          scenario_name = '$KeepScenarioName'
          OR name LIKE 'Executive walkthrough%'
          OR coverage_tags_json LIKE '%executive-demo%'
      )
    ORDER BY
        CASE WHEN scenario_name = '$KeepScenarioName' THEN 0 ELSE 1 END,
        updated_at DESC
    LIMIT 1
)
DELETE FROM template_test_data_set
WHERE template_id = (SELECT id FROM fol)
  AND id NOT IN (SELECT id FROM keeper)
  AND (
      scenario_name = '$KeepScenarioName'
      OR name LIKE 'Executive walkthrough%'
      OR coverage_tags_json LIKE '%executive-demo%'
  );
SELECT tds.external_id, tds.name, tds.scenario_name, tds.locked
FROM template_test_data_set tds
JOIN template t ON t.id = tds.template_id
WHERE t.external_id = '$KeepTemplateExternalId' AND t.deleted_at IS NULL
ORDER BY tds.updated_at DESC;
"@

$sql | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDb -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { throw "Cleanup SQL failed (exit $LASTEXITCODE)" }

if ($AlsoRemoveNonFolTemplates) {
    Write-Step "Removing non-FOL templates (E2E leftovers)..."
    & (Join-Path $DemoRoot 'cleanup-catalog-except-fol.ps1') -BackendUrl $BackendUrl -PostgresContainer $PostgresContainer
}

Write-Step "Done. Re-import demo variables with: .\deploy\demo-fol\import-fol-demo.ps1 -SkipSql"

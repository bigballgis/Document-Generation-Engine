# Purge preview/runtime generated DOCX/PDF artifacts from Postgres + MinIO.
# Keeps master documents, template config, and test data sets.
#
# Usage (from repo root):
#   .\scripts\cleanup-generated-artifacts.ps1
#   .\scripts\cleanup-generated-artifacts.ps1 -WhatIf

param(
    [string]$PostgresContainer = 'docgen-postgres',
    [string]$MinioContainer = 'docgen-minio',
    [string]$PostgresUser = $(if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'docgen' }),
    [string]$PostgresDb = $(if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'docgen' }),
    [string]$MinioUser = $(if ($env:MINIO_ROOT_USER) { $env:MINIO_ROOT_USER } else { 'docgen' }),
    [string]$MinioPassword = $(if ($env:MINIO_ROOT_PASSWORD) { $env:MINIO_ROOT_PASSWORD } else { 'docgen_local_pwd' }),
    [string]$StorageBucket = $(if ($env:STORAGE_BUCKET) { $env:STORAGE_BUCKET } else { 'docgen-artifacts' }),
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'

function Write-Step([string]$Message) { Write-Host "==> $Message" }

function Invoke-PostgresSql([string]$Sql) {
    docker exec $PostgresContainer psql -U $PostgresUser -d $PostgresDb -t -A -c $Sql
}

function Invoke-Minio([string[]]$Args) {
    $inner = ($Args | ForEach-Object {
        if ($_ -match "[\s'`"`$]") { "'$($_ -replace "'", "'\\''")'" } else { $_ }
    }) -join ' '
    docker exec $MinioContainer sh -c "/bin/mc alias set local http://127.0.0.1:9000 $MinioUser $MinioPassword >/dev/null 2>&1; /bin/mc $inner"
}

Write-Step "Counting generated artifact rows..."
$previewCount = [int](Invoke-PostgresSql 'SELECT COUNT(*) FROM preview_record;')
$batchCount = [int](Invoke-PostgresSql 'SELECT COUNT(*) FROM template_batch_test_run;')
$idempotencyCount = [int](Invoke-PostgresSql 'SELECT COUNT(*) FROM generation_idempotency WHERE response_storage_key IS NOT NULL;')

Write-Host "  preview_record rows              : $previewCount"
Write-Host "  template_batch_test_run rows     : $batchCount"
Write-Host "  generation_idempotency artifacts : $idempotencyCount"

if ($WhatIf) {
    Write-Step "WhatIf only — no changes made"
    Write-Host "  Would delete MinIO prefixes: previews/, generated/"
    Write-Host "  Would truncate preview_record and template_batch_test_run"
    Write-Host "  Would delete generation_idempotency rows with stored artifacts"
    exit 0
}

Write-Step "Removing MinIO objects under previews/ and generated/ ..."
try {
    Invoke-Minio @('rm', '--recursive', '--force', "local/$StorageBucket/previews/") | Out-Null
} catch {
    Write-Host "  previews/ prefix already empty or absent"
}
try {
    Invoke-Minio @('rm', '--recursive', '--force', "local/$StorageBucket/generated/") | Out-Null
} catch {
    Write-Host "  generated/ prefix already empty or absent"
}

Write-Step "Purging Postgres preview and runtime artifact metadata ..."
$purgeSql = @"
BEGIN;
DELETE FROM preview_record;
DELETE FROM template_batch_test_run;
DELETE FROM generation_idempotency WHERE response_storage_key IS NOT NULL;
COMMIT;
"@
docker exec $PostgresContainer psql -U $PostgresUser -d $PostgresDb -c $purgeSql | Out-Null

$previewAfter = [int](Invoke-PostgresSql 'SELECT COUNT(*) FROM preview_record;')
$batchAfter = [int](Invoke-PostgresSql 'SELECT COUNT(*) FROM template_batch_test_run;')
$idempotencyAfter = [int](Invoke-PostgresSql 'SELECT COUNT(*) FROM generation_idempotency WHERE response_storage_key IS NOT NULL;')

Write-Step "Cleanup complete"
Write-Host "  preview_record remaining           : $previewAfter"
Write-Host "  template_batch_test_run remaining  : $batchAfter"
Write-Host "  generation_idempotency artifacts   : $idempotencyAfter"
Write-Host "  MinIO kept                         : masters/ and other non-generated prefixes"

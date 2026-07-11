# Non-destructive backup of Postgres + MinIO for DR drill / ops.
# Usage (from repo root):
#   .\scripts\backup-stack.ps1
#   .\scripts\backup-stack.ps1 -PostgresContainer docgen-postgres -MinioVolume <volume>
#
# Prefer dump-inside-container + docker cp (PowerShell '>' corrupts -Fc dumps).
# MinIO: volume tar fallback when minio/mc image is unavailable (registry blocked).

param(
    [string]$PostgresContainer = "docgen-postgres",
    [string]$MinioContainer = "docgen-minio",
    [string]$MinioVolume = "",
    [string]$BackupRoot = "backups",
    [string]$Stamp = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RepoRoot

if (-not $Stamp) {
    $Stamp = Get-Date -Format "yyyy-MM-dd"
}

New-Item -ItemType Directory -Force -Path $BackupRoot | Out-Null

$backupStarted = (Get-Date).ToUniversalTime()
Write-Host "==> Backup start (UTC): $($backupStarted.ToString('o'))"

# Resolve MinIO volume from running container if not provided
if (-not $MinioVolume) {
    $mountsJson = docker inspect $MinioContainer --format '{{json .Mounts}}'
    if ($LASTEXITCODE -ne 0) { throw "Cannot inspect $MinioContainer" }
    $mounts = $mountsJson | ConvertFrom-Json
    $dataMount = $mounts | Where-Object { $_.Destination -eq "/data" } | Select-Object -First 1
    if (-not $dataMount -or -not $dataMount.Name) {
        throw "Could not resolve MinIO Docker volume for $MinioContainer"
    }
    $MinioVolume = $dataMount.Name
    Write-Host "    MinIO volume: $MinioVolume"
}

$dumpName = "docgen-$Stamp.dump"
$dumpHost = Join-Path $BackupRoot $dumpName
$minioTarName = "minio-$Stamp.tgz"
$minioTarHost = Join-Path $BackupRoot $minioTarName

Write-Host "==> PostgreSQL logical dump ($PostgresContainer)..."
$pgStart = Get-Date
docker exec $PostgresContainer pg_dump -U docgen -Fc -f "/tmp/$dumpName" docgen
if ($LASTEXITCODE -ne 0) { throw "pg_dump failed" }
docker cp "${PostgresContainer}:/tmp/$dumpName" $dumpHost
if ($LASTEXITCODE -ne 0) { throw "docker cp dump failed" }
docker exec $PostgresContainer rm -f "/tmp/$dumpName" | Out-Null
$pgDurationSec = [math]::Round(((Get-Date) - $pgStart).TotalSeconds, 2)

if (-not (Test-Path $dumpHost) -or (Get-Item $dumpHost).Length -lt 100) {
    throw "Dump missing or too small: $dumpHost"
}
Write-Host "    Dump: $dumpHost ($((Get-Item $dumpHost).Length) bytes) in ${pgDurationSec}s"

Write-Host "==> MinIO volume tar ($MinioVolume)..."
$minioStart = Get-Date
# Use cached alpine; do not require minio/mc registry pull.
docker run --rm `
    -v "${MinioVolume}:/data:ro" `
    -v "${RepoRoot}/${BackupRoot}:/backup" `
    alpine:latest `
    tar czf "/backup/$minioTarName" -C /data .
if ($LASTEXITCODE -ne 0) { throw "MinIO volume tar failed" }
$minioDurationSec = [math]::Round(((Get-Date) - $minioStart).TotalSeconds, 2)

if (-not (Test-Path $minioTarHost) -or (Get-Item $minioTarHost).Length -lt 100) {
    throw "MinIO tar missing or too small: $minioTarHost"
}
Write-Host "    Tar: $minioTarHost ($((Get-Item $minioTarHost).Length) bytes) in ${minioDurationSec}s"

# Capture a sample object path for round-trip evidence (best-effort)
$sampleObject = ""
try {
    $sampleObject = (docker exec $MinioContainer sh -c "ls /data/docgen-artifacts/generated 2>/dev/null | head -1").Trim()
} catch {
    $sampleObject = ""
}

$backupCompleted = (Get-Date).ToUniversalTime()
$manifest = [ordered]@{
    stamp                 = $Stamp
    backupStartedUtc      = $backupStarted.ToString("o")
    backupCompletedUtc    = $backupCompleted.ToString("o")
    postgresDumpPath      = $dumpHost.Replace("\", "/")
    postgresDumpBytes     = (Get-Item $dumpHost).Length
    postgresDurationSec   = $pgDurationSec
    minioTarPath          = $minioTarHost.Replace("\", "/")
    minioTarBytes         = (Get-Item $minioTarHost).Length
    minioDurationSec      = $minioDurationSec
    minioVolume           = $MinioVolume
    sampleGeneratedPrefix = $sampleObject
    method                = "pg_dump -Fc + MinIO Docker volume tar (mc fallback)"
}
$manifestPath = Join-Path $BackupRoot "backup-manifest-$Stamp.json"
$manifest | ConvertTo-Json | Set-Content -Path $manifestPath -Encoding utf8
Write-Host "==> Backup complete. Manifest: $manifestPath"
Write-Host "BACKUP_OK stamp=$Stamp"

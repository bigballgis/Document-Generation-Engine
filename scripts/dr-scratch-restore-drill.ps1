# Scratch-stack restore drill with mandatory confirmation gate.
# Destructive against the *scratch* project only — never auto-confirms.
#
# Usage (from repo root):
#   .\scripts\dr-scratch-restore-drill.ps1 `
#     -ConfirmPhrase "RESTORE-CONFIRM docgen-scratch 2026-07-12" `
#     -Stamp 2026-07-12
#
# Preconditions:
#   - backups/docgen-<stamp>.dump and backups/minio-<stamp>.tgz exist (see backup-stack.ps1)
#   - Shared acceptance containers stopped (fixed container_name collision) WITHOUT -v
#   - Host ports 8080/4173/5432/9000 free
#
# Forbidden: piping yes, default ConfirmPhrase, down -v on acceptance volumes,
# ScratchProject equal to the acceptance compose project (hard abort).

param(
    [Parameter(Mandatory = $true)]
    [string]$ConfirmPhrase,

    [string]$ScratchProject = "docgen-scratch",
    [string]$Stamp = "",
    [string]$BackupRoot = "backups",
    [string]$EvidenceRoot = "artifacts/dr-drill",
    [string]$Verifier = "deploy-engineer",
    [switch]$SkipAcceptanceStop,
    [switch]$SkipAcceptanceRestart,
    [string]$AcceptanceProject = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RepoRoot

if (-not $Stamp) {
    $Stamp = Get-Date -Format "yyyy-MM-dd"
}

# Detect acceptance project from running postgres (if any) for safe stop/restart
# and for defense-in-depth: never allow ScratchProject == acceptance project.
function Get-ComposeProjectFromContainer([string]$Name) {
    try {
        $proj = docker inspect $Name --format '{{index .Config.Labels "com.docker.compose.project"}}' 2>$null
        if ($LASTEXITCODE -eq 0 -and $proj) { return $proj.Trim() }
    } catch { }
    return ""
}

if (-not $AcceptanceProject) {
    $AcceptanceProject = Get-ComposeProjectFromContainer "docgen-postgres"
}

# Defense-in-depth: refuse wipe of acceptance volumes even if ConfirmPhrase names that project.
if ($AcceptanceProject -and ($ScratchProject -ieq $AcceptanceProject)) {
    Write-Error @"
Scratch/acceptance collision — hard abort.
  ScratchProject:     $ScratchProject
  AcceptanceProject:  $AcceptanceProject
Refusing to run destructive restore against the acceptance compose project.
Use a distinct scratch project (e.g. docgen-scratch).
"@
    exit 3
}

$expectedPhrase = "RESTORE-CONFIRM $ScratchProject $Stamp"
if ($ConfirmPhrase -ne $expectedPhrase) {
    Write-Error @"
Confirmation gate FAILED.
  Expected: $expectedPhrase
  Received: $ConfirmPhrase
Aborting — no destructive restore executed.
"@
    exit 2
}

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "Created .env from .env.example"
}

# BDD-OPS-KAFKA-REGISTRY-001: docgen-kafka requires explicit KAFKA_IMAGE (compose fail-closed; no Hub silent default).
# Prefer process env; else read from .env (copied from .env.example for local/scratch).
function Get-DotEnvValue {
    param([Parameter(Mandatory)][string]$Key)
    if (-not (Test-Path ".env")) { return $null }
    foreach ($line in Get-Content ".env") {
        $trimmed = $line.Trim()
        if ($trimmed.StartsWith('#') -or -not $trimmed.Contains('=')) { continue }
        $name, $value = $trimmed.Split('=', 2)
        if ($name.Trim() -eq $Key) {
            return $value.Trim().Trim('"').Trim("'")
        }
    }
    return $null
}
$kafkaImage = if (-not [string]::IsNullOrWhiteSpace($env:KAFKA_IMAGE)) {
    $env:KAFKA_IMAGE.Trim()
} else {
    Get-DotEnvValue -Key 'KAFKA_IMAGE'
}
if ([string]::IsNullOrWhiteSpace($kafkaImage)) {
    Write-Error @"
KAFKA_IMAGE must be set before bringing up docgen-kafka (scratch drill).
Set it in the environment or .env — see .env.example (LOCAL/DEV ONLY Hub example) or runbook for company-approved coords.
"@
    exit 1
}
$env:KAFKA_IMAGE = $kafkaImage

$dumpHost = Join-Path $BackupRoot "docgen-$Stamp.dump"
$minioTarHost = Join-Path $BackupRoot "minio-$Stamp.tgz"
if (-not (Test-Path $dumpHost)) { throw "Missing dump: $dumpHost — run backup-stack.ps1 first" }
if (-not (Test-Path $minioTarHost)) { throw "Missing MinIO tar: $minioTarHost — run backup-stack.ps1 first" }

$evidenceDir = Join-Path $EvidenceRoot $Stamp
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null
$logPath = Join-Path $evidenceDir "restore-log.txt"

function Write-Log([string]$Message) {
    $line = "{0:o}  {1}" -f (Get-Date).ToUniversalTime(), $Message
    Add-Content -Path $logPath -Value $line -Encoding utf8
    Write-Host $Message
}

Write-Log "Confirmation gate PASSED: $ConfirmPhrase (verifier=$Verifier); acceptanceProject=$AcceptanceProject"

$composeBase = @("-p", $ScratchProject, "-f", "docker-compose.yml")
$composeProd = $composeBase + @("-f", "docker-compose.prod.yml", "--profile", "prod")

# --- Stop acceptance (containers only; NEVER -v) ---
if (-not $SkipAcceptanceStop) {
    Write-Log "Stopping shared acceptance containers (no -v; volumes preserved). project=$AcceptanceProject"
    $names = @("docgen-backend", "docgen-frontend", "docgen-postgres", "docgen-redis", "docgen-minio", "docgen-kafka", "docgen-libreoffice")
    foreach ($n in $names) {
        docker stop $n 2>$null | Out-Null
        docker rm $n 2>$null | Out-Null
    }
    # Also try compose stop if project known
    if ($AcceptanceProject) {
        docker compose -p $AcceptanceProject -f docker-compose.yml -f docker-compose.prod.yml --profile prod stop 2>$null | Out-Null
    }
}

# Ensure no name collision
$conflict = docker ps -a --format '{{.Names}}' | Where-Object { $_ -match '^docgen-' }
if ($conflict) {
    Write-Log "Removing leftover containers for name reuse: $($conflict -join ', ')"
    $conflict | ForEach-Object { docker rm -f $_ 2>$null | Out-Null }
}

# --- Bring up scratch deps ---
Write-Log "Starting scratch deps: $ScratchProject"
docker compose @composeBase up -d docgen-postgres docgen-redis docgen-minio docgen-kafka
if ($LASTEXITCODE -ne 0) { throw "Failed to start scratch deps" }

Write-Log "Waiting for scratch postgres + minio healthy..."
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
    $pg = docker inspect docgen-postgres --format '{{.State.Health.Status}}' 2>$null
    $mn = docker inspect docgen-minio --format '{{.State.Health.Status}}' 2>$null
    if ($pg -eq "healthy" -and $mn -eq "healthy") { $ready = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $ready) { throw "Scratch postgres/minio not healthy in time" }
Write-Log "Scratch deps healthy"

# Resolve scratch MinIO volume
$mounts = (docker inspect docgen-minio --format '{{json .Mounts}}') | ConvertFrom-Json
$scratchMinioVol = ($mounts | Where-Object { $_.Destination -eq "/data" } | Select-Object -First 1).Name
Write-Log "Scratch MinIO volume: $scratchMinioVol"

# --- Timed restore window (RTO clock) ---
$rtoStart = (Get-Date).ToUniversalTime()
Write-Log "RTO clock start (UTC): $($rtoStart.ToString('o'))"

# Postgres restore (destructive --clean on scratch DB only)
Write-Log "Restoring PostgreSQL (pg_restore --clean --if-exists)..."
$pgRestoreStart = Get-Date
docker compose @composeBase cp $dumpHost "docgen-postgres:/tmp/restore.dump"
if ($LASTEXITCODE -ne 0) { throw "compose cp dump failed" }
docker compose @composeBase exec -T docgen-postgres `
    pg_restore -U docgen -d docgen --clean --if-exists /tmp/restore.dump
# pg_restore may exit non-zero on benign warnings; verify with a query
$pgRestoreExit = $LASTEXITCODE
docker compose @composeBase exec -T docgen-postgres rm -f /tmp/restore.dump | Out-Null
$tplCount = (docker compose @composeBase exec -T docgen-postgres `
    psql -U docgen -d docgen -t -A -c "SELECT count(*) FROM template;").Trim()
if (-not $tplCount -or $tplCount -eq "0") {
    throw "Postgres restore verification failed (template count=$tplCount, pg_restore_exit=$pgRestoreExit)"
}
$pgRestoreSec = [math]::Round(((Get-Date) - $pgRestoreStart).TotalSeconds, 2)
Write-Log "Postgres restore OK templates=$tplCount durationSec=$pgRestoreSec exit=$pgRestoreExit"

# MinIO restore via volume tar (stop MinIO, extract, start)
Write-Log "Restoring MinIO volume from tar (confirmation-gated scratch volume only)..."
$minioRestoreStart = Get-Date
docker compose @composeBase stop docgen-minio
if ($LASTEXITCODE -ne 0) { throw "stop minio failed" }
docker run --rm `
    -v "${scratchMinioVol}:/data" `
    -v "${RepoRoot}/${BackupRoot}:/backup" `
    alpine:latest `
    sh -c "rm -rf /data/* /data/.[!.]* /data/..?* 2>/dev/null; tar xzf /backup/minio-$Stamp.tgz -C /data"
if ($LASTEXITCODE -ne 0) { throw "MinIO tar extract failed" }
docker compose @composeBase start docgen-minio
if ($LASTEXITCODE -ne 0) { throw "start minio failed" }
for ($i = 0; $i -lt 30; $i++) {
    $mn = docker inspect docgen-minio --format '{{.State.Health.Status}}' 2>$null
    if ($mn -eq "healthy") { break }
    Start-Sleep -Seconds 2
}
$minioRestoreSec = [math]::Round(((Get-Date) - $minioRestoreStart).TotalSeconds, 2)
Write-Log "MinIO restore OK durationSec=$minioRestoreSec"

# Start apps — retag known-good images so compose project name does not force rebuild
Write-Log "Starting scratch apps (prod profile)..."
$backendSrcCandidates = @(
    "dge-lrp-d6-load-smoke-docgen-backend:latest",
    "documentgenerationengine-docgen-backend:latest",
    "docgen-app:latest"
)
$frontendSrcCandidates = @(
    "dge-lrp-d6-load-smoke-docgen-frontend:latest",
    "documentgenerationengine-docgen-frontend:latest",
    "docgen-frontend:latest"
)
$backendSrc = $backendSrcCandidates | Where-Object { docker images -q $_ } | Select-Object -First 1
$frontendSrc = $frontendSrcCandidates | Where-Object { docker images -q $_ } | Select-Object -First 1
if (-not $backendSrc -or -not $frontendSrc) {
    throw "No reusable backend/frontend images found to tag for scratch project (avoid full rebuild during drill)"
}
$backendDst = "${ScratchProject}-docgen-backend:latest"
$frontendDst = "${ScratchProject}-docgen-frontend:latest"
Write-Log "Retagging images: $backendSrc -> $backendDst ; $frontendSrc -> $frontendDst"
docker tag $backendSrc $backendDst
if ($LASTEXITCODE -ne 0) { throw "docker tag backend failed" }
docker tag $frontendSrc $frontendDst
if ($LASTEXITCODE -ne 0) { throw "docker tag frontend failed" }

docker compose @composeProd up -d --no-build docgen-backend docgen-frontend
if ($LASTEXITCODE -ne 0) { throw "Failed to start scratch apps with retagged images" }

# Wait healthz
Write-Log "Waiting for /healthz..."
$healthzStatus = $null
$healthzBody = $null
$healthzOkAt = $null
for ($i = 0; $i -lt 90; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:8080/healthz" -UseBasicParsing -TimeoutSec 5
        if ($resp.StatusCode -eq 200) {
            $healthzStatus = 200
            $healthzBody = $resp.Content
            $healthzOkAt = (Get-Date).ToUniversalTime()
            break
        }
    } catch {
        # retry
    }
    Start-Sleep -Seconds 2
}
if ($healthzStatus -ne 200) { throw "/healthz did not return 200" }
$healthzSec = [math]::Round(($healthzOkAt - $rtoStart).TotalSeconds, 2)
Write-Log "/healthz 200 after ${healthzSec}s from RTO start. body=$healthzBody"
Set-Content -Path (Join-Path $evidenceDir "healthz.txt") -Value $healthzBody -Encoding utf8

# readyz
try {
    $readyz = Invoke-WebRequest -Uri "http://localhost:8080/readyz" -UseBasicParsing -TimeoutSec 10
    Set-Content -Path (Join-Path $evidenceDir "readyz.json") -Value $readyz.Content -Encoding utf8
    Write-Log "/readyz $($readyz.StatusCode) $($readyz.Content)"
} catch {
    Write-Log "/readyz failed: $($_.Exception.Message)"
}

# Document round-trip — retrieve previously generated object from restored MinIO
Write-Log "Document round-trip: locate restored generated artifact..."
$roundTripStart = Get-Date
$docId = (docker exec docgen-minio sh -c "ls /data/docgen-artifacts/generated 2>/dev/null | head -1").Trim()
$objectOk = $false
$objectBytes = 0
$hostCopy = Join-Path $evidenceDir "restored-sample.bin"
if ($docId) {
    $extractSh = @"
set -e
DOC=/data/docgen-artifacts/generated/$docId
FILE=`$(find "`$DOC" -type f -name 'output.docx' | head -1)
if [ -z "`$FILE" ]; then FILE=`$(find "`$DOC" -type f | head -1); fi
echo SAMPLE=`$FILE
cp "`$FILE" /out/restored-sample.bin
wc -c < "`$FILE"
"@
    $extractPath = Join-Path $evidenceDir "_extract-sample.sh"
    Set-Content -Path $extractPath -Value $extractSh -Encoding ascii -NoNewline
    # Normalize to LF for alpine sh
    $bytes = [System.IO.File]::ReadAllBytes($extractPath)
    $text = [System.Text.Encoding]::ASCII.GetString($bytes) -replace "`r`n", "`n" -replace "`r", "`n"
    [System.IO.File]::WriteAllText($extractPath, $text)
    docker run --rm `
        -v "${scratchMinioVol}:/data:ro" `
        -v "${RepoRoot}/${evidenceDir}:/out" `
        alpine:latest `
        sh /out/_extract-sample.sh 2>&1 | Tee-Object -Variable probeOut | Out-Host
    if (Test-Path $hostCopy) {
        $objectBytes = (Get-Item $hostCopy).Length
        if ($objectBytes -gt 0) { $objectOk = $true }
    }
    Write-Log "Sample DOC folder=$docId bytes=$objectBytes probe=$($probeOut -join ' | ')"
    Remove-Item $extractPath -Force -ErrorAction SilentlyContinue
}
# Also confirm DB still knows this document
$dbDoc = ""
if ($docId) {
    $dbDoc = (docker compose @composeBase exec -T docgen-postgres `
        psql -U docgen -d docgen -t -A -c "SELECT count(*) FROM runtime_generation_audit_event WHERE document_id = '$docId';").Trim()
}
$roundTripSec = [math]::Round(((Get-Date) - $roundTripStart).TotalSeconds, 2)
$roundTripOutcome = if ($objectOk) { "PASS — retrieved restored object $docId ($objectBytes bytes); audit_rows=$dbDoc" } else { "FAIL — no retrievable generated object after restore" }
Write-Log "Document round-trip: $roundTripOutcome (${roundTripSec}s)"

$rtoEnd = (Get-Date).ToUniversalTime()
$rtoMinutes = [math]::Round(($rtoEnd - $rtoStart).TotalMinutes, 3)
Write-Log "RTO clock end (UTC): $($rtoEnd.ToString('o')) observedRtoMinutes=$rtoMinutes"

# RPO from backup manifest age at cutover (restore start)
$manifestPath = Join-Path $BackupRoot "backup-manifest-$Stamp.json"
$rpoMinutes = $null
$backupCompletedUtc = $null
if (Test-Path $manifestPath) {
    $manifest = Get-Content $manifestPath -Raw | ConvertFrom-Json
    # Prefer raw string parse — ConvertFrom-Json may coerce ISO timestamps to local DateTime.
    $rawManifest = Get-Content $manifestPath -Raw
    if ($rawManifest -match '"backupCompletedUtc"\s*:\s*"([^"]+)"') {
        $backupCompletedUtc = [datetime]::Parse($Matches[1], [System.Globalization.CultureInfo]::InvariantCulture, [System.Globalization.DateTimeStyles]::RoundtripKind).ToUniversalTime()
    } else {
        $backupCompletedUtc = [datetime]::Parse($manifest.backupCompletedUtc.ToString(), [System.Globalization.CultureInfo]::InvariantCulture, [System.Globalization.DateTimeStyles]::AssumeUniversal).ToUniversalTime()
    }
    $rpoMinutes = [math]::Round(($rtoStart - $backupCompletedUtc).TotalMinutes, 3)
    if ($rpoMinutes -lt 0) { $rpoMinutes = 0 }
}
Write-Log "Observed RPO (backup age at restore start) minutes=$rpoMinutes"

$meetsRpo = ($null -ne $rpoMinutes) -and ($rpoMinutes -le 15)
$meetsRto = $rtoMinutes -le 30
$meetsTargets = $meetsRpo -and $meetsRto -and $objectOk -and ($healthzStatus -eq 200)

$smoke = @"
# DR drill smoke notes — $Stamp

## Scope
- Environment: local Docker **scratch** compose project ``$ScratchProject``
- Not production; lighter than prod (logical dump + volume tar; no WAL/PITR)
- MinIO method: Docker volume tar (minio/mc registry pull unavailable during drill)

## Confirmation
- Phrase: ``$ConfirmPhrase``
- Verifier: $Verifier

## Document round-trip
- $roundTripOutcome
- Object prefix: $docId
- Bytes: $objectBytes
- Duration: ${roundTripSec}s

## Health
- /healthz: $healthzStatus
- See healthz.txt / readyz.json

## Durations
- Postgres restore: ${pgRestoreSec}s
- MinIO restore: ${minioRestoreSec}s
- Time to /healthz 200 from RTO start: ${healthzSec}s
- Observed RTO: ${rtoMinutes} min (target ≤ 30)
- Observed RPO (backup age proxy): ${rpoMinutes} min (target ≤ 15)
- Meets ADR-0030 targets (this drill scope): $meetsTargets

## Honesty note
This drill validates the **local Compose scratch path** only. Do **not** claim production RPO/RTO compliance from these numbers alone.
"@
Set-Content -Path (Join-Path $evidenceDir "smoke-notes.md") -Value $smoke -Encoding utf8

$rpoRto = [ordered]@{
    drillDate            = $Stamp
    verifier             = $Verifier
    scratchProject       = $ScratchProject
    confirmationGate     = $ConfirmPhrase
    rpoObservedMinutes   = $rpoMinutes
    rtoObservedMinutes   = $rtoMinutes
    targets              = @{ rpoMinutes = 15; rtoMinutes = 30 }
    meetsTargets         = $meetsTargets
    healthzHttpStatus    = $healthzStatus
    documentRoundTrip    = $roundTripOutcome
    postgresRestoreSec   = $pgRestoreSec
    minioRestoreSec      = $minioRestoreSec
    healthzFromRtoStartSec = $healthzSec
    backupCompletedUtc   = if ($backupCompletedUtc) { $backupCompletedUtc.ToString("o") } else { $null }
    rtoStartUtc          = $rtoStart.ToString("o")
    rtoEndUtc            = $rtoEnd.ToString("o")
    scope                = "local-docker-scratch; pg_dump+volume-tar; no-WAL-PITR; not-production"
    sampleDocumentId     = $docId
}
$rpoRto | ConvertTo-Json -Depth 5 | Set-Content -Path (Join-Path $evidenceDir "rpo-rto.json") -Encoding utf8
Write-Log "Evidence written under $evidenceDir"

if (-not $meetsTargets) {
    Write-Log "DRILL_RESULT=FAIL (see rpo-rto.json / smoke-notes.md)"
} else {
    Write-Log "DRILL_RESULT=PASS (scratch-scope; see honesty note)"
}

# --- Tear down scratch (volumes optional; gated) ---
Write-Log "Stopping scratch stack (containers only by default; volumes removed only with matching ConfirmPhrase already used)"
docker compose @composeProd stop 2>$null | Out-Null
docker compose @composeBase down --remove-orphans
# Remove scratch volumes explicitly — still under same ConfirmPhrase for this drill session
Write-Log "Removing scratch volumes for project $ScratchProject (confirmation already recorded)"
docker volume rm "${ScratchProject}_docgen-postgres-data" 2>$null | Out-Null
docker volume rm "${ScratchProject}_docgen-minio-data" 2>$null | Out-Null

# Restart prior acceptance if known
if (-not $SkipAcceptanceRestart -and $AcceptanceProject) {
    Write-Log "Restarting prior acceptance project=$AcceptanceProject (SkipBuild path)"
    docker compose -p $AcceptanceProject -f docker-compose.yml up -d docgen-postgres docgen-redis docgen-minio docgen-kafka
    docker compose -p $AcceptanceProject -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d --no-build docgen-backend docgen-frontend
    Write-Log "Acceptance restart issued; verify /healthz externally if needed"
} elseif (-not $SkipAcceptanceRestart) {
    Write-Log "No AcceptanceProject detected — operator should run .\scripts\docker-deploy-queue.ps1 -SkipBuild from MAIN"
}

Write-Host "DRILL_DONE evidence=$evidenceDir meetsTargets=$meetsTargets"
exit $(if ($meetsTargets) { 0 } else { 1 })

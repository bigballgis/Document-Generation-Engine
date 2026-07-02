# P15-T01 smoke: verify hardened images start with read-only root FS + non-root user.
# Prerequisite: host-built artifacts + images (run docker-deploy.ps1 or build steps first).
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $RepoRoot

$composeArgs = @(
    "-f", "docker-compose.yml",
    "-f", "docker-compose.prod.yml",
    "--profile", "prod"
)

function Test-HttpOk {
    param([string]$Url, [int]$TimeoutSec = 5)
    try {
        $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec $TimeoutSec
        return $resp.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [int]$Attempts = 40,
        [int]$SleepSec = 3
    )
    for ($i = 0; $i -lt $Attempts; $i++) {
        if (Test-HttpOk -Url $Url) { return $true }
        Start-Sleep -Seconds $SleepSec
    }
    return $false
}

function Remove-SmokeContainers {
    param([Parameter(Mandatory)][string[]]$Names)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'
    try {
        foreach ($name in $Names) {
            if ([string]::IsNullOrWhiteSpace($name)) { continue }
            # Native docker stderr must not surface under $ErrorActionPreference = Stop.
            docker rm -f $name 2>&1 | Out-Null
        }
    } finally {
        $ErrorActionPreference = $prev
    }
}

Write-Host "==> Ensuring infrastructure (postgres, redis, minio)..."
docker compose up -d docgen-postgres docgen-redis docgen-minio
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not $SkipBuild) {
    if (-not (Get-ChildItem -Path "backend/target/docgen-backend-*.jar" -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch 'original' })) {
        Write-Error "Backend JAR missing. Run .\scripts\docker-deploy.ps1 or mvn package first."
        exit 1
    }
    if (-not (Test-Path "frontend/dist/index.html")) {
        Write-Error "frontend/dist missing. Run .\scripts\docker-deploy.ps1 or pnpm build first."
        exit 1
    }

    Write-Host "==> Building hardened runtime images..."
    docker compose @composeArgs build --pull=false docgen-backend docgen-frontend
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

$backendImage = "documentgenerationengine-docgen-backend:latest"
$frontendImage = "documentgenerationengine-docgen-frontend:latest"
if (-not (docker image inspect $backendImage 2>$null)) {
    Write-Error "Backend image $backendImage not found. Build images first."
    exit 1
}
if (-not (docker image inspect $frontendImage 2>$null)) {
    Write-Error "Frontend image $frontendImage not found. Build images first."
    exit 1
}
$backendId = docker images -q $backendImage
$frontendId = docker images -q $frontendImage
Write-Host "    Backend image:  $backendImage ($backendId)"
Write-Host "    Frontend image: $frontendImage ($frontendId)"

$networkName = docker inspect docgen-postgres --format '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' 2>$null
if (-not $networkName) {
    Write-Error "Could not resolve compose network from docgen-postgres. Start infrastructure first."
    exit 1
}

$backendPort = 18080
$frontendPort = 14173
$backendName = "docgen-hardening-backend-smoke"
$frontendName = "docgen-hardening-frontend-smoke"

Remove-SmokeContainers -Names @($backendName, $frontendName)

Write-Host "==> Smoke: backend (read-only + non-root UID 65532 + /tmp tmpfs)..."
docker run -d --name $backendName `
    --network $networkName `
    --network-alias docgen-backend `
    --read-only `
    --user 65532:65532 `
    --tmpfs /tmp:rw,noexec,nosuid,size=256m `
    --security-opt no-new-privileges:true `
    -e SPRING_PROFILES_ACTIVE=prod `
    -e POSTGRES_HOST=docgen-postgres `
    -e POSTGRES_PORT=5432 `
    -e POSTGRES_DB=$(if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'docgen' }) `
    -e POSTGRES_USER=$(if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'docgen' }) `
    -e POSTGRES_PASSWORD=$(if ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { 'docgen_local_pwd' }) `
    -e REDIS_HOST=docgen-redis `
    -e REDIS_PORT=6379 `
    -e MINIO_ENDPOINT=http://docgen-minio:9000 `
    -e JWT_SECRET=$(if ($env:JWT_SECRET) { $env:JWT_SECRET } else { 'prod-change-me-32-bytes-minimum-secret' }) `
    -e APP_ENVIRONMENT=prod `
    -e DOCGEN_SEED_DEMO_CATALOG=false `
    -e JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/tmp `
    -p "${backendPort}:8080" `
    $backendImage
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not (Wait-HttpOk -Url "http://localhost:${backendPort}/healthz")) {
    Write-Error "Backend hardened smoke failed: /healthz not healthy on port $backendPort"
    docker logs $backendName
    Remove-SmokeContainers -Names @($backendName)
    exit 1
}
Write-Host "    OK: http://localhost:${backendPort}/healthz"

Write-Host "==> Smoke: frontend (read-only + non-root nginx + /tmp tmpfs)..."
docker run -d --name $frontendName `
    --network $networkName `
    --read-only `
    --user nginx `
    --tmpfs /tmp:rw,noexec,nosuid,size=64m `
    --security-opt no-new-privileges:true `
    -p "${frontendPort}:8080" `
    $frontendImage
if ($LASTEXITCODE -ne 0) {
    Remove-SmokeContainers -Names @($backendName)
    exit $LASTEXITCODE
}

if (-not (Wait-HttpOk -Url "http://localhost:${frontendPort}/healthz")) {
    Write-Error "Frontend hardened smoke failed: /healthz not healthy on port $frontendPort"
    docker logs $frontendName
    Remove-SmokeContainers -Names @($backendName, $frontendName)
    exit 1
}
if (-not (Wait-HttpOk -Url "http://localhost:${frontendPort}/")) {
    Write-Error "Frontend hardened smoke failed: SPA root not reachable on port $frontendPort"
    docker logs $frontendName
    Remove-SmokeContainers -Names @($backendName, $frontendName)
    exit 1
}
Write-Host "    OK: http://localhost:${frontendPort}/healthz and SPA root"

$frontendUser = docker exec $frontendName id 2>$null
Write-Host "    Backend user: 65532:65532 (minimal non-root JRE)"
Write-Host "    Frontend user: nginx ($frontendUser)"

Remove-SmokeContainers -Names @($backendName, $frontendName)

Write-Host ""
Write-Host "P15-T01 container hardening smoke: PASSED"
Write-Host "  Evidence: read-only root FS + non-root + documented tmpfs mounts"
Write-Host "  See deploy/container-hardening.md for writable path list."

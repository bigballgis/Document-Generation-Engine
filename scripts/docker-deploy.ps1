# Canonical Docker-only deployment for local validation.
# Usage (from repo root):
#   .\scripts\docker-deploy.ps1              # rebuild app images (use local cache, no registry pull)
#   .\scripts\docker-deploy.ps1 -SkipBuild   # restart containers only, no compile
#   .\scripts\docker-deploy.ps1 -ForceRebuild # full rebuild without layer cache

param(
    [switch]$SkipBuild,
    [switch]$ForceRebuild
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RepoRoot

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "Created .env from .env.example"
}

$composeArgs = @(
    "-f", "docker-compose.yml",
    "-f", "docker-compose.prod.yml",
    "--profile", "prod"
)

# Backend multi-stage build needs these base images once; skip pull if already local.
$baseImages = @(
    "maven:3.9.9-eclipse-temurin-21-alpine",
    "eclipse-temurin:21-jre-alpine",
    "node:22-alpine",
    "nginx:1.27-alpine"
)
foreach ($image in $baseImages) {
    $imageId = docker images -q $image
    if (-not $imageId) {
        Write-Host "==> First-time pull: $image"
        docker pull $image
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
}

Write-Host "==> Starting infrastructure (postgres, redis, minio)..."
docker compose up -d docgen-postgres docgen-redis docgen-minio
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if (-not $SkipBuild) {
    Write-Host "==> Building application images (local layer cache; --pull=false skips registry re-download)..."
    $buildArgs = @("compose") + $composeArgs + @("build", "--pull=false")
    if ($ForceRebuild) {
        $buildArgs += "--no-cache"
    }
    $buildArgs += @("docgen-backend", "docgen-frontend")
    docker @buildArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host "==> Skipping image build (-SkipBuild). Restarting existing images only."
}

Write-Host "==> Starting application containers..."
docker compose @composeArgs up -d --remove-orphans docgen-backend docgen-frontend
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$backendPort = if ($env:BACKEND_PORT) { $env:BACKEND_PORT } else { "8080" }
$frontendPort = if ($env:FRONTEND_PORT) { $env:FRONTEND_PORT } else { "4173" }

Write-Host "==> Waiting for backend health..."
$healthy = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:$backendPort/healthz" -UseBasicParsing -TimeoutSec 5
        if ($resp.StatusCode -eq 200) {
            $healthy = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 3
    }
}

if (-not $healthy) {
    Write-Error "Backend health check failed on http://localhost:$backendPort/healthz"
    docker compose @composeArgs ps
    exit 1
}

Write-Host ""
Write-Host "Deployment ready."
Write-Host "  Frontend: http://localhost:$frontendPort"
Write-Host "  Backend:  http://localhost:$backendPort/healthz"
Write-Host "  Login:    10000001 / ChangeMe123! (GLOBAL_ADMIN)"
Write-Host ""
docker compose @composeArgs ps

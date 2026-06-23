# Canonical deployment: compile on host, run in Docker (no Maven inside image build).
# Usage (from repo root):
#   .\scripts\docker-deploy.ps1              # local mvn + pnpm build, then docker images
#   .\scripts\docker-deploy.ps1 -SkipBuild   # restart containers only
#   .\scripts\docker-deploy.ps1 -ForceRebuild # docker build --no-cache

param(
    [switch]$SkipBuild,
    [switch]$ForceRebuild
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RepoRoot

$env:DOCKER_BUILDKIT = "1"
$env:COMPOSE_DOCKER_CLI_BUILD = "1"

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "Created .env from .env.example"
}

$composeArgs = @(
    "-f", "docker-compose.yml",
    "-f", "docker-compose.prod.yml",
    "--profile", "prod"
)

$runtimeImages = @(
    "eclipse-temurin:21-jre-alpine",
    "nginx:1.27-alpine"
)
foreach ($image in $runtimeImages) {
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
    Write-Host "==> Building backend JAR locally (uses your ~/.m2 cache)..."
    mvn -B -ntp -f backend/pom.xml package "-Dmaven.test.skip=true"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    $jar = Get-ChildItem -Path "backend/target/docgen-backend-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'original' } |
        Select-Object -First 1
    if (-not $jar) {
        Write-Error "Backend JAR not found under backend/target after Maven package."
        exit 1
    }
    Write-Host "    JAR: $($jar.FullName)"

    Write-Host "==> Building frontend assets locally..."
    Push-Location frontend
    try {
        pnpm install --frozen-lockfile
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        pnpm build
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    } finally {
        Pop-Location
    }

    if (-not (Test-Path "frontend/dist/index.html")) {
        Write-Error "frontend/dist not found after pnpm build."
        exit 1
    }

    Write-Host "==> Packaging Docker images (copy pre-built artifacts only; --pull=false)..."
    $buildArgs = @("compose") + $composeArgs + @("build", "--pull=false")
    if ($ForceRebuild) {
        $buildArgs += "--no-cache"
    }
    $buildArgs += @("docgen-backend", "docgen-frontend")
    docker @buildArgs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Host "==> Skipping compile and image build (-SkipBuild). Restarting existing images only."
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

# Generate CycloneDX SBOM artifacts for release / intranet SCA submission.
#
# Usage (from repo root):
#   .\scripts\generate-sbom.ps1
#   .\scripts\generate-sbom.ps1 -FrontendOnly
#   .\scripts\generate-sbom.ps1 -BackendOnly
#
# Outputs (gitignored under artifacts/):
#   artifacts/sbom/frontend-cyclonedx.json
#   artifacts/sbom/backend-cyclonedx.json

param(
    [switch]$FrontendOnly,
    [switch]$BackendOnly
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $PSScriptRoot
$SbomDir = Join-Path $RepoRoot 'artifacts/sbom'
New-Item -ItemType Directory -Force -Path $SbomDir | Out-Null

function Write-Step([string]$Message) { Write-Host "==> $Message" -ForegroundColor Cyan }

$runFrontend = -not $BackendOnly
$runBackend = -not $FrontendOnly

if ($runFrontend) {
    Write-Step 'Generating frontend CycloneDX SBOM (pnpm lock + node_modules; npm ls warnings ignored)...'
    Push-Location (Join-Path $RepoRoot 'frontend')
    try {
        if (-not (Test-Path 'node_modules')) {
            pnpm install --frozen-lockfile
            if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        }
        $outFile = Join-Path $SbomDir 'frontend-cyclonedx.json'
        pnpm exec cyclonedx-npm `
            --ignore-npm-errors `
            --no-validate `
            --spec-version 1.6 `
            --output-file $outFile
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        if (-not (Test-Path $outFile)) {
            throw "Frontend SBOM not written: $outFile"
        }
        Write-Host "  Wrote $outFile ($((Get-Item $outFile).Length) bytes)"
    } finally {
        Pop-Location
    }
}

if ($runBackend) {
    Write-Step 'Generating backend CycloneDX SBOM (Maven -Psbom)...'
    Push-Location (Join-Path $RepoRoot 'backend')
    try {
        mvn -B -ntp -Psbom package "-Dmaven.test.skip=true"
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        $candidates = @(
            (Join-Path $RepoRoot 'backend/target/bom.json'),
            (Join-Path $RepoRoot 'backend/target/classes/META-INF/sbom/application.cdx.json')
        )
        $attached = Get-ChildItem (Join-Path $RepoRoot 'backend/target') -Filter '*cyclonedx.json' -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($attached) { $candidates += $attached.FullName }
        $source = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
        if (-not $source) {
            throw "Backend SBOM not found under backend/target (expected bom.json or *cyclonedx.json)"
        }
        $dest = Join-Path $SbomDir 'backend-cyclonedx.json'
        Copy-Item -Force $source $dest
        Write-Host "  Wrote $dest ($((Get-Item $dest).Length) bytes)"
    } finally {
        Pop-Location
    }
}

Write-Host ''
Write-Host 'SBOM generation complete.' -ForegroundColor Green
Write-Host "  Directory: $SbomDir"
Write-Host '  Next (org gate): submit artifacts to approved intranet SCA per docs/architecture/m9-t02-closure-plan.md'

<#
.SYNOPSIS
  IBL-D3 k6 smoke runner against acceptance stack :8080 (healthz only).

.DESCRIPTION
  Resolves host `k6` or Docker `grafana/k6`. Writes evidence under
  docs/plan/evidence/ibl-d3-k6-nfr-path/. Soft script thresholds are NOT SLOs.
  Use -DryRun when k6 cannot execute — records honest presence evidence.

.PARAMETER BaseUrl
  Target base URL (default http://localhost:8080).

.PARAMETER DryRun
  Do not execute load; verify script presence + record install guidance.

.PARAMETER SkipHealthCheck
  Skip preflight GET /healthz.
#>
[CmdletBinding()]
param(
  [string]$BaseUrl = 'http://localhost:8080',
  [switch]$DryRun,
  [switch]$SkipHealthCheck
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
Set-Location $repoRoot

$scriptRel = 'perf/k6/smoke-healthz.js'
$scriptPath = Join-Path $repoRoot $scriptRel
$evidenceDir = Join-Path $repoRoot 'docs/plan/evidence/ibl-d3-k6-nfr-path'
$timestamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')

if (-not (Test-Path -LiteralPath $scriptPath)) {
  throw "Missing k6 script: $scriptPath"
}

New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null

function Write-EvidenceSummary {
  param(
    [hashtable]$Payload
  )
  $jsonPath = Join-Path $evidenceDir 'latest-summary.json'
  $mdPath = Join-Path $evidenceDir 'latest-summary.md'
  $stampJson = Join-Path $evidenceDir ("run-{0}.json" -f $Payload.timestampUtc)
  $json = $Payload | ConvertTo-Json -Depth 8
  [System.IO.File]::WriteAllText($jsonPath, $json, [System.Text.UTF8Encoding]::new($false))
  Copy-Item -LiteralPath $jsonPath -Destination $stampJson -Force

  $lines = @(
    '# IBL-D3 k6 smoke - latest summary',
    '',
    '| Field | Value |',
    '| --- | --- |',
    ("| Timestamp (UTC) | {0} |" -f $Payload.timestampUtc),
    ("| Mode | `{0}` |" -f $Payload.mode),
    ("| Base URL | `{0}` |" -f $Payload.baseUrl),
    ("| k6 available | {0} |" -f $Payload.k6Available),
    ("| Runner | `{0}` |" -f $Payload.runner),
    ("| Exit code | {0} |" -f $Payload.exitCode),
    ("| healthz preflight | {0} |" -f $Payload.healthzPreflight),
    ("| Script | `{0}` |" -f $Payload.script),
    ("| NFR status | **{0}** |" -f $Payload.nfrStatus),
    '',
    '## Honesty',
    '',
    '- Numbers are **measured-input / proposed** for LR-D5 only.',
    '- **Do not** promote to confirmed SLO from this run.',
    '- Soft k6 thresholds are not product SLOs.',
    '',
    '## Notes',
    '',
    ($Payload.notes -join "`n"),
    ''
  )
  [System.IO.File]::WriteAllLines($mdPath, $lines, [System.Text.UTF8Encoding]::new($false))
}

function Test-Healthz {
  param([string]$Url)
  try {
    $r = Invoke-WebRequest -Uri ($Url.TrimEnd('/') + '/healthz') -UseBasicParsing -TimeoutSec 10
    if ($r.StatusCode -eq 200) { return "OK $($r.StatusCode)" }
    return "UNEXPECTED $($r.StatusCode)"
  } catch {
    return "UNREACHABLE: $($_.Exception.Message)"
  }
}

function Resolve-K6Runner {
  $cmd = Get-Command k6 -ErrorAction SilentlyContinue
  if ($cmd) {
    return @{ kind = 'host'; path = $cmd.Source; available = $true; detail = $cmd.Source }
  }
  # Portable / worktree-local binary (gitignored .tools/)
  $portable = Join-Path $repoRoot '.tools/k6'
  $portableExe = Get-ChildItem -Path $portable -Filter 'k6.exe' -Recurse -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty FullName
  if ($portableExe) {
    return @{ kind = 'portable'; path = $portableExe; available = $true; detail = $portableExe }
  }
  $docker = Get-Command docker -ErrorAction SilentlyContinue
  if ($docker) {
    $img = & docker images grafana/k6 --format '{{.Repository}}:{{.Tag}}' 2>$null
    if ($img) {
      return @{ kind = 'docker'; path = 'docker run --rm -i grafana/k6:latest'; available = $true; detail = ($img | Select-Object -First 1) }
    }
    return @{
      kind      = 'docker-missing-image'
      path      = $null
      available = $false
      detail    = 'docker CLI present but grafana/k6 image not local (pull required)'
    }
  }
  return @{ kind = 'none'; path = $null; available = $false; detail = 'no host/portable k6 and no docker' }
}

$base = $BaseUrl.TrimEnd('/')
$healthz = if ($SkipHealthCheck) { 'SKIPPED' } else { Test-Healthz -Url $base }
$runner = Resolve-K6Runner

Write-Host "[k6-smoke] repo=$repoRoot"
Write-Host "[k6-smoke] script=$scriptRel"
Write-Host "[k6-smoke] baseUrl=$base"
Write-Host "[k6-smoke] healthz=$healthz"
Write-Host "[k6-smoke] runner=$($runner.kind)"

if ($DryRun -or -not $runner.available) {
  $notes = @(
    '- Mode: dry-run / presence evidence (k6 load **not** executed).',
    '- Install host k6: `winget install --id GrafanaLabs.k6 -e` then re-run without `-DryRun`.',
    '- Or: `docker pull grafana/k6:latest` and re-run (BASE_URL=http://host.docker.internal:8080 on Docker Desktop).',
    '- Suite checked in under `perf/k6/`; see `perf/k6/README.md`.',
    "- Preflight healthz: $healthz"
  )
  if (-not $runner.available) {
    $notes += '- Host `k6` and Docker runner both unavailable or not selected; honesty: no load metrics invented.'
  }
  $payload = @{
    slice            = 'ibl-d3-k6-nfr-path'
    taskIds          = @('125')
    finding          = 'F22'
    timestampUtc     = $timestamp
    mode             = 'dry-run'
    baseUrl          = $base
    k6Available      = [bool]$runner.available
    runner           = $runner.kind
    exitCode         = 0
    healthzPreflight = $healthz
    script           = $scriptRel
    nfrStatus        = 'measured-input path only - proposed awaiting confirmation (no confirmed SLO)'
    notes            = $notes
    metrics          = $null
    runnerDetail     = $runner.detail
  }
  Write-EvidenceSummary -Payload $payload
  Write-Host "[k6-smoke] DRY_RUN_OK evidence=$evidenceDir"
  exit 0
}

if (-not $SkipHealthCheck -and $healthz -notmatch '^OK') {
  throw "Acceptance stack healthz preflight failed ($healthz). Deploy with .\scripts\docker-deploy-queue.ps1 or pass -SkipHealthCheck."
}

$exportPath = Join-Path $evidenceDir 'k6-summary-export.json'
$logPath = Join-Path $evidenceDir ("k6-run-{0}.log" -f $timestamp)

if ($runner.kind -eq 'host' -or $runner.kind -eq 'portable') {
  $k6Exe = if ($runner.kind -eq 'portable') { $runner.path } else { 'k6' }
  $args = @(
    'run',
    '-e', "BASE_URL=$base",
    '--summary-export', $exportPath,
    $scriptRel
  )
  Write-Host "[k6-smoke] exec: $k6Exe $($args -join ' ')"
  & $k6Exe @args 2>&1 | Tee-Object -FilePath $logPath
  $exit = $LASTEXITCODE
} elseif ($runner.kind -eq 'docker') {
  # Docker Desktop: map host port via host.docker.internal when BASE_URL is localhost
  $dockerBase = $base
  if ($dockerBase -match 'localhost|127\.0\.0\.1') {
    $dockerBase = $dockerBase -replace 'localhost|127\.0\.0\.1', 'host.docker.internal'
  }
  $containerScript = '/scripts/smoke-healthz.js'
  $vol = "${repoRoot}/perf/k6:/scripts:ro"
  $exportVol = "${evidenceDir}:/evidence"
  $dockerArgs = @(
    'run', '--rm', '-i',
    '-v', $vol,
    '-v', $exportVol,
    '-e', "BASE_URL=$dockerBase",
    'grafana/k6:latest',
    'run',
    '-e', "BASE_URL=$dockerBase",
    '--summary-export', '/evidence/k6-summary-export.json',
    $containerScript
  )
  Write-Host "[k6-smoke] exec: docker $($dockerArgs -join ' ')"
  & docker @dockerArgs 2>&1 | Tee-Object -FilePath $logPath
  $exit = $LASTEXITCODE
} else {
  throw "No executable k6 runner (kind=$($runner.kind); detail=$($runner.detail))"
}

$metrics = $null
if (Test-Path -LiteralPath $exportPath) {
  try {
    $metrics = Get-Content -LiteralPath $exportPath -Raw -Encoding utf8 | ConvertFrom-Json
  } catch {
    $metrics = @{ parseError = $_.Exception.Message }
  }
}

$notes = @(
  "- Mode: executed via $($runner.kind).",
  "- Log: $(Split-Path -Leaf $logPath)",
  '- Soft thresholds in smoke-healthz.js are smoke usability only — not NFR SLOs.',
  '- Feed LR-D5 as measured-input; do not flip proposed → confirmed.'
)

$payload = @{
  slice            = 'ibl-d3-k6-nfr-path'
  taskIds          = @('125')
  finding          = 'F22'
  timestampUtc     = $timestamp
  mode             = 'executed'
  baseUrl          = $base
  k6Available      = $true
  runner           = $runner.kind
  exitCode         = $exit
  healthzPreflight = $healthz
  script           = $scriptRel
  summaryExport    = 'k6-summary-export.json'
  nfrStatus        = 'measured-input path only - proposed awaiting confirmation (no confirmed SLO)'
  notes            = $notes
  metricsHint      = 'See k6-summary-export.json for raw k6 metrics; do not invent confirmed SLOs from p95/error rate.'
}

Write-EvidenceSummary -Payload $payload

if ($exit -ne 0) {
  Write-Error "[k6-smoke] k6 exited $exit — see $logPath"
  exit $exit
}

Write-Host "[k6-smoke] RUN_OK evidence=$evidenceDir"
exit 0

# Run frontend Knip dead-code scan and refresh evidence artifacts.
# Usage (from repo root):
#   .\scripts\knip-scan.ps1
#   .\scripts\knip-scan.ps1 -Production
#
# Exit code 1 from Knip (findings remain) is expected and treated as success for this
# informational tooling slice — do not enable as a blocking CI gate without a follow-up.

[CmdletBinding()]
param(
    [switch]$Production
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$frontend = Join-Path $repoRoot 'frontend'
$outDir = Join-Path $repoRoot 'docs\evidence\slim-knip-scan'

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Push-Location $frontend
try {
    $pnpmScript = if ($Production) { 'knip:prod' } else { 'knip' }

    Write-Host "Running: pnpm $pnpmScript"
    $reportPath = Join-Path $outDir 'knip-report.txt'
    $summaryPath = Join-Path $outDir 'knip-summary.txt'
    $jsonPath = Join-Path $outDir 'knip-report.json'

    # Knip exits 1 when findings exist — capture output regardless.
    $reportLines = & pnpm $pnpmScript 2>&1 | ForEach-Object { "$_" }
    $exit = $LASTEXITCODE
    $reportLines | Set-Content -Path $reportPath -Encoding utf8

    if ($exit -gt 1) {
        throw "Knip failed with exit code $exit"
    }

    # Best-effort machine JSON (may also exit 1 with findings).
    & pnpm exec knip --reporter json 2>$null | Set-Content -Path $jsonPath -Encoding utf8
    $jsonExit = $LASTEXITCODE
    if ($jsonExit -gt 1) {
        Write-Warning "Knip JSON reporter exited $jsonExit — keeping prior/partial $jsonPath"
    }

    # Rolled-up summary from the human report.
    $unusedFiles = @($reportLines | Where-Object { $_ -match '^src/' -or $_ -match '^e2e/' -or $_ -match '^tests/' -or $_ -match '^build/' })
    # After "Unused files (N)" header, file paths appear until the next section.
    $inUnusedFiles = $false
    $unusedFilePaths = [System.Collections.Generic.List[string]]::new()
    $unusedExports = 0
    $unusedTypes = 0
    $dupExports = 0
    foreach ($line in $reportLines) {
        if ($line -match '^Unused files \((\d+)\)') {
            $inUnusedFiles = $true
            continue
        }
        if ($line -match '^Unused (exports|exported types|dependencies|devDependencies|files)|^Duplicate exports|^Unlisted|^Unresolved') {
            $inUnusedFiles = $false
        }
        if ($inUnusedFiles -and ($line -match '^\S')) {
            $unusedFilePaths.Add(($line -replace '\s+$', ''))
        }
        if ($line -match '^Unused exports \((\d+)\)') { $unusedExports = [int]$Matches[1] }
        if ($line -match '^Unused exported types \((\d+)\)') { $unusedTypes = [int]$Matches[1] }
        if ($line -match '^Duplicate exports \((\d+)\)') { $dupExports = [int]$Matches[1] }
    }

    $exportHits = @{}
    $typeHits = @{}
    $section = ''
    foreach ($line in $reportLines) {
        if ($line -match '^Unused exports \(') { $section = 'exports'; continue }
        if ($line -match '^Unused exported types \(') { $section = 'types'; continue }
        if ($line -match '^(Unused |Duplicate |Unlisted|Unresolved)') {
            if ($line -notmatch '^Unused exports' -and $line -notmatch '^Unused exported types') {
                $section = ''
            }
        }
        if (($section -eq 'exports' -or $section -eq 'types') -and $line -match '\s(src/[^\s]+|e2e/[^\s]+)') {
            $file = $Matches[1] -replace ':\d+:\d+$', ''
            if ($section -eq 'exports') {
                if (-not $exportHits.ContainsKey($file)) { $exportHits[$file] = 0 }
                $exportHits[$file]++
            } else {
                if (-not $typeHits.ContainsKey($file)) { $typeHits[$file] = 0 }
                $typeHits[$file]++
            }
        }
    }

    $summary = New-Object System.Collections.Generic.List[string]
    [void]$summary.Add('=== SUMMARY ===')
    [void]$summary.Add("Unused files: $($unusedFilePaths.Count)")
    [void]$summary.Add("Unused exports: $unusedExports")
    [void]$summary.Add("Unused exported types: $unusedTypes")
    [void]$summary.Add("Duplicate exports: $dupExports")
    [void]$summary.Add('')
    [void]$summary.Add('=== Unused files ===')
    if ($unusedFilePaths.Count -eq 0) {
        [void]$summary.Add('(none)')
    } else {
        foreach ($p in $unusedFilePaths) { [void]$summary.Add($p) }
    }
    [void]$summary.Add('')
    [void]$summary.Add('=== Unused / unlisted deps ===')
    [void]$summary.Add('')
    [void]$summary.Add('=== Unused exports by file (top 15) ===')
    $exportHits.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 15 | ForEach-Object {
        [void]$summary.Add(('{0,3}  {1}' -f $_.Value, $_.Key))
    }
    [void]$summary.Add('')
    [void]$summary.Add('=== Unused exported types by file (top 10) ===')
    $typeHits.GetEnumerator() | Sort-Object Value -Descending | Select-Object -First 10 | ForEach-Object {
        [void]$summary.Add(('{0,3}  {1}' -f $_.Value, $_.Key))
    }
    $summary | Set-Content -Path $summaryPath -Encoding utf8

    Write-Host "Report: $reportPath (knip exit=$exit)"
    Write-Host "Summary: $summaryPath"
    Write-Host "JSON: $jsonPath"
    Write-Host "Done. See docs/evidence/slim-knip-scan/README.md"
    exit 0
}
finally {
    Pop-Location
}

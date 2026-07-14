#Requires -Version 5.1
<#
.SYNOPSIS
  Lightweight documentation structure anti-drift checks (ADR-0024 / ADR-0055).

.DESCRIPTION
  Validates:
  - docs/README.md source-of-truth order matches document-as-code constitution
  - README.md / AGENTS.md agent count = 18
  - No root CLAUDE.md dual-agent leftovers; no tracked .claude/ refs expected
  - Behavior specs under docs/behavior/ are indexed in docs/README.md (best-effort)

.PARAMETER StrictMetadata
  Reserved for future frontmatter checks (no-op today; accepted for call-site compat).

.EXAMPLE
  pwsh -NoProfile -File ./scripts/validate-doc-structure.ps1
#>
[CmdletBinding()]
param(
    [switch]$StrictMetadata
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$failures = New-Object System.Collections.Generic.List[string]

function Add-Fail([string]$msg) { [void]$failures.Add($msg) }

# --- SoT order (canonical, ADR-0055 aligned) ---
$canonicalSot = @(
    'Latest explicit user confirmation',
    '.taskmaster/tasks/tasks.json',
    'docs/plan/',
    'docs/requirements/requirements-plan.md',
    'docs/product/PRD.md',
    'docs/domain/domain-model.md',
    'docs/security/permission-matrix.md',
    'ADRs'
)

$docsReadme = Join-Path $root 'docs/README.md'
$constitution = Join-Path $root '.cursor/rules/document-as-code-constitution.mdc'
if (-not (Test-Path $docsReadme)) { Add-Fail "Missing docs/README.md" }
if (-not (Test-Path $constitution)) { Add-Fail "Missing document-as-code-constitution.mdc" }

if ((Test-Path $docsReadme) -and (Test-Path $constitution)) {
    $readmeText = Get-Content -Raw -Path $docsReadme
    $constText = Get-Content -Raw -Path $constitution
    foreach ($token in @(
            '.taskmaster/tasks/tasks.json',
            'docs/plan/',
            'requirements-plan.md',
            'PRD.md',
            'domain-model.md',
            'permission-matrix.md'
        )) {
        if ($readmeText -notmatch [regex]::Escape($token) -and $readmeText -notmatch [regex]::Escape(($token -replace '\\', '/'))) {
            # allow link forms without full path prefix
            $short = Split-Path $token -Leaf
            if ($short -and $readmeText -notmatch [regex]::Escape($short)) {
                Add-Fail "docs/README.md SoT section missing token: $token"
            }
        }
        if ($constText -notmatch [regex]::Escape($token) -and $token -ne 'docs/plan/') {
            if ($token -eq 'docs/plan/' -and $constText -match 'docs/plan/') { continue }
            if ($constText -notmatch [regex]::Escape(($token -replace '^docs/', ''))) {
                # constitution uses backtick paths — check leaf
                $leaf = ($token -split '/')[-1]
                if ($leaf -and $constText -notmatch [regex]::Escape($leaf) -and $token -ne 'docs/plan/') {
                    Add-Fail "constitution SoT missing token: $token"
                }
            }
        }
    }
    # Ordered check only inside the SoT conflict section (not earlier hub links)
    $sotIdx = $readmeText.IndexOf('Source-of-truth order')
    if ($sotIdx -lt 0) {
        Add-Fail "docs/README.md missing 'Source-of-truth order' section"
    } else {
        $sotSlice = $readmeText.Substring($sotIdx)
        $rmTm = $sotSlice.IndexOf('taskmaster/tasks/tasks.json')
        $rmReq = $sotSlice.IndexOf('requirements-plan.md')
        if ($rmTm -lt 0 -or $rmReq -lt 0 -or $rmTm -gt $rmReq) {
            Add-Fail "docs/README.md SoT section: tasks.json must appear before requirements-plan.md"
        }
    }
    $cTm = $constText.IndexOf('taskmaster/tasks/tasks.json')
    $cReq = $constText.IndexOf('requirements-plan.md')
    if ($cTm -lt 0 -or $cReq -lt 0 -or $cTm -gt $cReq) {
        Add-Fail "constitution SoT order: tasks.json must appear before requirements-plan.md"
    }
}

# --- Agent count 18 ---
foreach ($hub in @('README.md', 'AGENTS.md')) {
    $path = Join-Path $root $hub
    if (-not (Test-Path $path)) { Add-Fail "Missing $hub"; continue }
    $text = Get-Content -Raw -Path $path
    if ($text -match 'Agents\s*\(17\)' -or $text -match '\b17 agents\b') {
        Add-Fail "$hub still says 17 agents (expected 18)"
    }
    if ($hub -eq 'README.md' -and $text -notmatch 'Agents\s*\(18\)' -and $text -notmatch '\b18\b.*agents|Agents.*\b18\b') {
        # AGENTS.md uses "Agents (18)" in a table heading style differently
        if ($text -notmatch 'Agents \(18\)') {
            Add-Fail "README.md should mention Agents (18)"
        }
    }
}

$agentDir = Join-Path $root '.cursor/agents'
if (Test-Path $agentDir) {
    $agentCount = @(Get-ChildItem -Path $agentDir -Filter '*.md' | Where-Object { $_.Name -ne 'MODEL-STRATEGY.md' }).Count
    if ($agentCount -ne 18) {
        Add-Fail ".cursor/agents has $agentCount specialist md files (expected 18; exclude MODEL-STRATEGY.md)"
    }
}

# --- Claude dual-agent leftovers ---
$claudeMd = Join-Path $root 'CLAUDE.md'
if (Test-Path $claudeMd) {
    $claudeBody = Get-Content -Raw -Path $claudeMd
    if ($claudeBody -match 'dual-agent|Claude Code|cloude-code-toolbox') {
        Add-Fail "Root CLAUDE.md contains dual-agent / Claude Code narrative (delete or slim Cursor pointer only)"
    }
}
$claudeDir = Join-Path $root '.claude'
if (Test-Path $claudeDir) {
    Add-Fail "Tracked .claude/ directory should not exist (Cursor-only; ADR-0055)"
}
$rootMcp = Join-Path $root '.mcp.json'
if (Test-Path $rootMcp) {
    Add-Fail "Root .mcp.json should not exist; use .cursor/mcp.json only"
}
if (Test-Path (Join-Path $root '.taskmaster/CLAUDE.md')) {
    Add-Fail ".taskmaster/CLAUDE.md should be renamed to TASKMASTER.md"
}

# --- Behavior specs indexed (best-effort) ---
$behaviorDir = Join-Path $root 'docs/behavior'
if ((Test-Path $behaviorDir) -and (Test-Path $docsReadme)) {
    $readmeText = Get-Content -Raw -Path $docsReadme
    $orphans = @()
    Get-ChildItem -Path $behaviorDir -Filter '*.md' | ForEach-Object {
        $name = $_.Name
        $stem = [IO.Path]::GetFileNameWithoutExtension($name)
        if ($readmeText -notmatch [regex]::Escape($name) -and $readmeText -notmatch [regex]::Escape($stem)) {
            $orphans += $name
        }
    }
    if ($orphans.Count -gt 0) {
        $sample = ($orphans | Select-Object -First 8) -join ', '
        Add-Fail ("Behavior specs not linked from docs/README.md ({0}): {1}" -f $orphans.Count, $sample)
    }
}

if ($StrictMetadata) {
    Write-Host "StrictMetadata: reserved (no additional checks yet)"
}

Write-Host "validate-doc-structure: root=$root"
if ($failures.Count -eq 0) {
    Write-Host "PASS"
    exit 0
}

Write-Host "FAIL ($($failures.Count))"
foreach ($f in $failures) { Write-Host " - $f" }
exit 1

#Requires -Version 5.1
<#
.SYNOPSIS
  Blocking CI gate for Kubernetes manifest validation (P15-T09 / ADR-0030).

.DESCRIPTION
  Entry point for push-to-main and PR pipelines when deploy/ or Helm chart files change.
  Runs scripts/helm-validate.ps1 (helm lint, helm template, kubeconform). Exits non-zero on failure.

  Wire this script into your CI provider as a blocking job on changes under deploy/ or scripts/helm-validate.ps1.
#>
param(
    [switch]$SkipKubeconform
)

$ErrorActionPreference = "Stop"
$validateScript = Join-Path $PSScriptRoot "helm-validate.ps1"

if (-not (Test-Path $validateScript)) {
    Write-Error "Missing helm validation script: $validateScript"
}

$validateParams = @{}
if ($SkipKubeconform) {
    $validateParams['SkipKubeconform'] = $true
}

& $validateScript @validateParams
exit $LASTEXITCODE

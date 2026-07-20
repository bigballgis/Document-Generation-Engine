# Wave B demo import — mirrors demo-fol / demo-credit-limit contract.
param(
    [string]$BackendUrl = 'http://localhost:8080',
    [string]$PostgresContainer = 'docgen-postgres',
    [switch]$SkipSql,
    [switch]$SkipApi,
    [switch]$SkipMasterRefresh
)
$ErrorActionPreference = 'Stop'
$DemoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Shared = Join-Path (Split-Path -Parent $DemoRoot) 'demo-import-shared.ps1'
if (-not (Test-Path $Shared)) { throw "Missing shared import helper: $Shared" }
. $Shared
Import-DemoPackage -DemoRoot $DemoRoot -ConfigPath (Join-Path $DemoRoot 'config/insurance-endorsement-template-config.json') -BackendUrl $BackendUrl -PostgresContainer $PostgresContainer -SkipSql:$SkipSql -SkipApi:$SkipApi -SkipMasterRefresh:$SkipMasterRefresh

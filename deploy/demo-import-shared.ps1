# Shared demo import helpers for deploy/demo-* packages.
param()

function Import-DemoPackage {
    param(
        [string]$DemoRoot,
        [string]$ConfigPath,
        [string]$BackendUrl,
        [string]$PostgresContainer,
        [switch]$SkipSql
    )
    if (-not (Test-Path $ConfigPath)) { throw "Config not found: $ConfigPath" }
    $Config = Get-Content $ConfigPath -Raw | ConvertFrom-Json
    Write-Host "==> Demo import marker: $($Config.catalogMarker) layout: $($Config.masterLayoutVersion)"
    if (-not $SkipSql) {
        $sqlDir = Join-Path $DemoRoot 'sql'
        if (Test-Path $sqlDir) {
            Get-ChildItem $sqlDir -Filter '*.sql' | Sort-Object Name | ForEach-Object {
                Write-Host "==> Applying SQL $($_.Name)"
                Get-Content $_.FullName -Raw | docker exec -i $PostgresContainer psql -U docgen -d docgen -v ON_ERROR_STOP=1
            }
        }
    }
    Write-Host "==> Demo package validated (catalogMarker=$($Config.catalogMarker))"
    Write-Host "    Management API import requires running backend at $BackendUrl"
    Write-Host "    Run full API import when Docker stack is healthy."
}

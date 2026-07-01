# Import wholesale FOL executive demo data:
#   1) SQL — standard clauses (content_module)
#   2) Generated catalog JSON — variables, rules, rich bindings, test sample
#   3) Management API — master DOCX upload, template, bindings, test sample
#
# Usage (from repo root):
#   .\deploy\demo-fol\import-fol-demo.ps1
#   .\deploy\demo-fol\import-fol-demo.ps1 -BackendUrl http://localhost:8080 -PostgresContainer docgen-postgres
#   .\deploy\demo-fol\import-fol-demo.ps1 -RegenerateCatalog

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$PostgresContainer = 'docgen-postgres',
    [string]$PostgresUser = $(if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'docgen' }),
    [string]$PostgresDb = $(if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'docgen' }),
    [switch]$SkipSql,
    [switch]$SkipApi,
    [switch]$RegenerateCatalog
)

$ErrorActionPreference = 'Stop'
$DemoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent (Split-Path -Parent $DemoRoot)
$ConfigPath = Join-Path $DemoRoot 'config/fol-template-config.json'
$SqlPath = Join-Path $DemoRoot 'sql/001-fol-standard-clauses.sql'
$GenerateSql = Join-Path $DemoRoot 'generate-clauses-sql.ps1'
$GenerateCatalog = Join-Path $DemoRoot 'generate-fol-catalog.ps1'
$ApiBase = "$BackendUrl/api/management/v1"

function Write-Step([string]$Message) { Write-Host "==> $Message" }

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body = $null,
        [hashtable]$MultipartFields = $null
    )
    $Headers = @{ Authorization = "Bearer $Token" }
    $Uri = "$ApiBase$Path"
    if ($MultipartFields) {
        $curlArgs = @('-sS', '-X', $Method, $Uri, '-H', "Authorization: Bearer $Token")
        foreach ($key in $MultipartFields.Keys) {
            $value = $MultipartFields[$key]
            if ($value -is [System.IO.FileInfo]) {
                $curlArgs += @('-F', "$key=@$($value.FullName)")
            } else {
                $curlArgs += @('-F', "$key=$value")
            }
        }
        $raw = curl.exe @curlArgs
        if ($LASTEXITCODE -ne 0) { throw "curl failed ($Method $Path): $raw" }
        return ($raw | ConvertFrom-Json)
    }
    if ($Body -ne $null) {
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 100)
    }
    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
}

function Get-Token([string]$Username, [string]$Password) {
    $login = Invoke-RestMethod -Method POST -Uri "$ApiBase/auth/login" -ContentType 'application/json' -Body (@{ username = $Username; password = $Password } | ConvertTo-Json)
    return $login.result.accessToken
}

function Resolve-CatalogPath([string]$RelativePath) {
    return Join-Path $DemoRoot ($RelativePath -replace '^config/', 'config/')
}

function Ensure-CatalogGenerated {
    param([object]$Config)
    $files = @(
        (Resolve-CatalogPath $Config.generatedCatalogFiles.variables),
        (Resolve-CatalogPath $Config.generatedCatalogFiles.compositionRules),
        (Resolve-CatalogPath $Config.generatedCatalogFiles.bindingOverlays),
        (Resolve-CatalogPath $Config.generatedCatalogFiles.demoTestVariables)
    )
    $missing = $files | Where-Object { -not (Test-Path $_) }
    if ($RegenerateCatalog -or $missing.Count -gt 0) {
        Write-Step "Generating FOL catalog JSON ($($files.Count) files)..."
        & $GenerateCatalog
        if (-not (Test-Path (Resolve-CatalogPath $Config.generatedCatalogFiles.variables))) {
            throw "Catalog generation failed — fol-variables.json not created"
        }
    }
}

function Count-StructuredFeatures([string]$Json) {
    $conditionBlocks = ([regex]::Matches($Json, '"type"\s*:\s*"conditionBlock"')).Count
    $loopBlocks = ([regex]::Matches($Json, '"type"\s*:\s*"loopBlock"')).Count
    $tableRefs = ([regex]::Matches($Json, '"type"\s*:\s*"tableComponentRef"')).Count
    return @{ conditionBlocks = $conditionBlocks; loopBlocks = $loopBlocks; tableComponentRefs = $tableRefs }
}

function Set-Binding([string]$TemplateId, [string]$Token, [string]$AnchorId, [object]$StructuredContent) {
    Invoke-Api PUT "/templates/$TemplateId/bindings/$AnchorId" $Token -Body @{
        anchorId = $AnchorId
        declaredContentType = 'TEXT'
        structuredContentJson = ($StructuredContent | ConvertTo-Json -Depth 100 -Compress)
    } | Out-Null
}

function Merge-ClauseBinding([string]$AnchorId, [string]$ReferenceKey, [object]$Overlay) {
    if ($Overlay) {
        return $Overlay
    }
    $title = $ReferenceKey -replace '_', ' '
    return @{
        schemaVersion = '1.0'
        nodes = @(
            @{ type = 'sectionHeading'; children = @(@{ type = 'text'; value = $title }) }
            @{ type = 'contentModuleRef'; referenceKey = $ReferenceKey }
        )
    }
}

function Upsert-TestDataSet([string]$TemplateId, [string]$Token, [object]$TestDataConfig) {
    $name = $TestDataConfig.name
    $sets = Invoke-Api GET "/templates/$TemplateId/test-data-sets" $Token
    $existing = @($sets.result) | Where-Object { $_.name -eq $name } | Select-Object -First 1
    $body = @{
        name = $name
        required = [bool]$TestDataConfig.required
        variables = $TestDataConfig.variables
    }
    if (-not $existing) {
        Invoke-Api POST "/templates/$TemplateId/test-data-sets" $Token -Body $body | Out-Null
        return
    }
    $targetId = $existing.testDataSetId
    if ($existing.locked) {
        $derived = Invoke-Api POST "/templates/$TemplateId/test-data-sets/$targetId/derive" $Token
        $targetId = $derived.result.testDataSetId
    }
    try {
        Invoke-Api PUT "/templates/$TemplateId/test-data-sets/$targetId" $Token -Body $body | Out-Null
    } catch {
        Write-Host "  (test data set update skipped: $($_.Exception.Message))"
    }
}

if (-not (Test-Path $ConfigPath)) { throw "Missing config: $ConfigPath" }
$Config = Get-Content $ConfigPath -Raw | ConvertFrom-Json
Ensure-CatalogGenerated -Config $Config

$VariablesPath = Resolve-CatalogPath $Config.generatedCatalogFiles.variables
$RulesPath = Resolve-CatalogPath $Config.generatedCatalogFiles.compositionRules
$OverlaysPath = Resolve-CatalogPath $Config.generatedCatalogFiles.bindingOverlays
$TestDataPath = Resolve-CatalogPath $Config.generatedCatalogFiles.demoTestVariables

$CatalogVariables = (Get-Content $VariablesPath -Raw | ConvertFrom-Json).variables
$CompositionRules = (Get-Content $RulesPath -Raw | ConvertFrom-Json).rules
$BindingOverlays = (Get-Content $OverlaysPath -Raw | ConvertFrom-Json).bindings
$TestDataConfig = Get-Content $TestDataPath -Raw | ConvertFrom-Json

if (-not $SkipSql) {
    if (-not (Test-Path $SqlPath)) {
        Write-Step "Generating SQL from clause catalogue..."
        & $GenerateSql
    }
    Write-Step "Applying FOL standard clauses SQL via postgres container '$PostgresContainer'..."
    Get-Content $SqlPath -Raw | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDb -v ON_ERROR_STOP=1 -f -
    if ($LASTEXITCODE -ne 0) { throw "SQL import failed (exit $LASTEXITCODE)" }
}

if ($SkipApi) {
    Write-Host "API import skipped (-SkipApi)."
    Write-Host "Catalog ready: $($CatalogVariables.Count) variables, $($CompositionRules.Count) rules."
    exit 0
}

Write-Step "Waiting for backend $BackendUrl/healthz ..."
$healthy = $false
for ($i = 0; $i -lt 40; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri "$BackendUrl/healthz" -UseBasicParsing -TimeoutSec 5
        if ($resp.StatusCode -eq 200) { $healthy = $true; break }
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $healthy) { throw "Backend not healthy at $BackendUrl/healthz" }

$AdminToken = Get-Token '10000001' 'ChangeMe123!'
$GroupAdminToken = Get-Token '10000002' 'ChangeMe123!'
$AuthorToken = Get-Token '10000003' 'ChangeMe123!'

# --- Master (DOCX in MinIO — must go through API) ---
$DocxPath = Join-Path $DemoRoot $Config.masterDocx
if (-not (Test-Path $DocxPath)) {
    throw "Missing master DOCX: $DocxPath`nRun: mvn -f backend/pom.xml test -Dtest=FolMasterDocxAssetGeneratorTest"
}

$masters = Invoke-Api GET '/masters' $AdminToken
$master = $masters.result | Where-Object { $_.name -eq $Config.masterName } | Select-Object -First 1
if (-not $master) {
    Write-Step "Uploading FOL master DOCX..."
    $created = Invoke-Api POST '/masters' $GroupAdminToken -MultipartFields @{
        groupCode = $Config.groupCode
        name = $Config.masterName
        description = $Config.masterDescription
        file = (Get-Item $DocxPath)
    }
    $masterId = $created.result.id
    Invoke-Api POST "/masters/$masterId/submit-review" $GroupAdminToken -Body @{ changeSummary = 'FOL executive demo import' }
    $master = (Invoke-Api POST "/masters/$masterId/review" $AdminToken -Body @{ decision = 'APPROVED'; commentSummary = 'Executive demo approved' }).result
} else {
    $needsLayoutRefresh = ($master.description -notlike "*$($Config.masterLayoutVersion)*")
    $anchorCount = @($master.anchors).Count
    if ($needsLayoutRefresh -or $anchorCount -lt 40) {
        Write-Step "Refreshing FOL master (layout=$needsLayoutRefresh anchors=$anchorCount)..."
        Invoke-Api PUT "/masters/$($master.id)/file" $GroupAdminToken -MultipartFields @{
            file = (Get-Item $DocxPath)
        }
        Invoke-Api POST "/masters/$($master.id)/submit-review" $GroupAdminToken -Body @{ changeSummary = 'FOL master refresh' }
        $master = (Invoke-Api POST "/masters/$($master.id)/review" $AdminToken -Body @{ decision = 'APPROVED'; commentSummary = 'Executive demo approved' }).result
    }
}
$masterId = $master.id
Write-Step "Master ready: $($Config.masterName) ($masterId)"

# --- Template ---
$templates = Invoke-Api GET '/templates' $AuthorToken
$template = $templates.result | Where-Object { $_.externalId -eq $Config.templateExternalId } | Select-Object -First 1
if (-not $template) {
    Write-Step "Creating FOL template $($Config.templateExternalId)..."
    $created = Invoke-Api POST '/templates' $AuthorToken -Body @{
        externalId = $Config.templateExternalId
        groupCode = $Config.groupCode
        name = $Config.templateName
        description = $Config.templateDescription
        masterId = $masterId
    }
    $templateId = $created.result.id
} else {
    $templateId = $template.id
    if ($template.description -notlike "*$($Config.catalogMarker)*") {
        Invoke-Api PATCH "/templates/$templateId" $AuthorToken -Body @{
            name = $Config.templateName
            description = $Config.templateDescription
        }
    }
}

# --- Variables (>=500) ---
Write-Step "Upserting $($CatalogVariables.Count) template variables..."
foreach ($var in $CatalogVariables) {
    $body = @{
        variableKey = $var.key
        variableType = $var.type
        required = [bool]$var.required
        description = $var.description
    }
    if ($null -ne $var.PSObject.Properties['defaultValue'] -and $var.defaultValue -ne '') {
        $body.defaultValue = [string]$var.defaultValue
    }
    if ($null -ne $var.PSObject.Properties['enumValues'] -and $var.enumValues -ne '') {
        $body.enumValues = [string]$var.enumValues
    }
    Invoke-Api PUT "/templates/$templateId/variables/$($var.key)" $AuthorToken -Body $body | Out-Null
}

# --- Bindings: overlays + clause module refs ---
Write-Step "Applying rich binding overlays and clause references..."
$overlayKeys = @{}
$BindingOverlays.PSObject.Properties | ForEach-Object { $overlayKeys[$_.Name] = $_.Value }

# Signature and header anchors from overlays (not in clauseBindings)
$overlayOnlyAnchors = @('FOL_HEADER', 'FOL_FACILITY_SUMMARY', 'FOL_SIG_BORROWER', 'FOL_SIG_LENDER')
foreach ($anchorId in $overlayOnlyAnchors) {
    if ($overlayKeys.ContainsKey($anchorId)) {
        Set-Binding -TemplateId $templateId -Token $AuthorToken -AnchorId $anchorId -StructuredContent $overlayKeys[$anchorId]
    }
}

foreach ($clause in $Config.clauseBindings) {
    $anchorId = $clause.anchorId
    $refKey = $clause.referenceKey
    $overlay = $null
    if ($overlayKeys.ContainsKey($anchorId)) {
        $overlay = $overlayKeys[$anchorId]
    }
    $structured = Merge-ClauseBinding -AnchorId $anchorId -ReferenceKey $refKey -Overlay $overlay
    Set-Binding -TemplateId $templateId -Token $AuthorToken -AnchorId $anchorId -StructuredContent $structured
    Invoke-Api PUT "/templates/$templateId/content-module-references/$refKey" $AuthorToken -Body @{
        referenceKey = $refKey
        moduleId = $clause.moduleCode
        semanticVersion = '1.0.0'
    } | Out-Null
}

Invoke-Api POST "/templates/$templateId/bindings/validate" $AuthorToken -Body @{} | Out-Null

# --- Composition rules (>=10) ---
Write-Step "Applying $($CompositionRules.Count) composition rules..."
Invoke-Api PUT "/templates/$templateId/rules" $AuthorToken -Body @{ rules = @($CompositionRules) } | Out-Null

# --- Test data set ---
Write-Step "Refreshing executive demo test data set..."
Upsert-TestDataSet -TemplateId $templateId -Token $AuthorToken -TestDataConfig $TestDataConfig

# --- Verification ---
$detail = Invoke-Api GET "/templates/$templateId" $AuthorToken
$variableCount = @($detail.result.variables).Count
$ruleCount = @($detail.result.rules).Count
$bindingJson = ($detail.result.bindings | ForEach-Object { $_.structuredContentJson }) -join ''
$features = Count-StructuredFeatures $bindingJson

Write-Host ""
Write-Host "FOL executive demo import complete ($($Config.catalogMarker))."
Write-Host "  Master:            $($Config.masterName)"
Write-Host "  Template:            $($Config.templateExternalId)  id=$templateId"
Write-Host "  Variables (API):     $variableCount"
Write-Host "  Composition rules:   $ruleCount"
Write-Host "  Bindings:            $(@($detail.result.bindings).Count)"
Write-Host "  conditionBlocks:     $($features.conditionBlocks)"
Write-Host "  loopBlocks:          $($features.loopBlocks)"
Write-Host "  tableComponentRefs:  $($features.tableComponentRefs)"
Write-Host "  Clauses (SQL):       $($Config.clauseBindings.Count)"
Write-Host "  UI:                  http://localhost:4173/templates/$templateId"
Write-Host ""

if ($variableCount -lt 500) { Write-Warning "Variable count $variableCount is below target (500)" }
if ($ruleCount -lt 10) { Write-Warning "Rule count $ruleCount is below target (10)" }
if ($features.conditionBlocks -lt 15) { Write-Warning "conditionBlocks $($features.conditionBlocks) below target (15)" }
if ($features.loopBlocks -lt 8) { Write-Warning "loopBlocks $($features.loopBlocks) below target (8)" }
if ($features.tableComponentRefs -lt 5) { Write-Warning "tableComponentRefs $($features.tableComponentRefs) below target (5)" }

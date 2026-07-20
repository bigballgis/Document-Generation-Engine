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
$CatalogShared = Join-Path $DemoRoot 'fol-catalog-shared.ps1'
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

function Get-ApiListItems([object]$EnvelopeResult) {
    if ($null -eq $EnvelopeResult) { return @() }
    if ($EnvelopeResult -is [System.Array]) { return $EnvelopeResult }
    if ($null -ne $EnvelopeResult.PSObject.Properties['content']) {
        return @($EnvelopeResult.content)
    }
    return @($EnvelopeResult)
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
        (Resolve-CatalogPath $Config.generatedCatalogFiles.demoTestVariables),
        (Resolve-CatalogPath $Config.generatedCatalogFiles.catalogManifest)
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

function Format-BindingExpectedUpdatedAt([object]$UpdatedAt) {
    if ($null -eq $UpdatedAt) { return $null }
    if ($UpdatedAt -is [string]) { return [string]$UpdatedAt }
    $utc = [datetime]$UpdatedAt
    if ($utc.Kind -ne [DateTimeKind]::Utc) { $utc = $utc.ToUniversalTime() }
    return $utc.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'")
}

function Set-Binding([string]$TemplateId, [string]$Token, [string]$AnchorId, [object]$StructuredContent) {
    $detail = Invoke-Api GET "/templates/$TemplateId" $Token
    $existing = @($detail.result.bindings) | Where-Object { $_.anchorId -eq $AnchorId } | Select-Object -First 1
    $body = @{
        anchorId = $AnchorId
        declaredContentType = 'TEXT'
        structuredContentJson = ($StructuredContent | ConvertTo-Json -Depth 100 -Compress)
    }
    if ($existing -and $existing.updatedAt) {
        $body.expectedUpdatedAt = Format-BindingExpectedUpdatedAt $existing.updatedAt
    }
    Invoke-Api PUT "/templates/$TemplateId/bindings/$AnchorId" $Token -Body $body | Out-Null
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

function Remove-StaleLegacyFolBindings([string]$TemplateId) {
    $sql = @"
BEGIN;
DELETE FROM template_content_module_reference tcmr
USING template_version tv
WHERE tcmr.template_version_id = tv.id
  AND tv.template_id = '$TemplateId'::uuid
  AND tv.lifecycle_status = 'DRAFT'
  AND (tcmr.reference_key LIKE 'FOL_SEC_%' OR tcmr.reference_key LIKE 'FOL_SCH_%');

DELETE FROM anchor_binding ab
USING template_version tv
WHERE ab.template_version_id = tv.id
  AND tv.template_id = '$TemplateId'::uuid
  AND tv.lifecycle_status = 'DRAFT'
  AND (ab.anchor_id LIKE 'FOL_SEC_%' OR ab.anchor_id LIKE 'FOL_SCH_%');
COMMIT;
"@
    Write-Step "Removing legacy FOL_SEC_/FOL_SCH_ bindings and module references..."
    $sql | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDb -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Legacy FOL binding cleanup failed (exit $LASTEXITCODE)" }
}

function Remove-StaleCatalogVariables([string]$TemplateId, [string]$Token, [string[]]$CatalogKeys) {
    $detail = Invoke-Api GET "/templates/$TemplateId" $Token
    $existing = @($detail.result.variables)
    $catalogSet = [System.Collections.Generic.HashSet[string]]::new([string[]]$CatalogKeys)
    $stale = $existing | Where-Object { -not $catalogSet.Contains($_.variableKey) }
    if ($stale.Count -eq 0) {
        return
    }
    Write-Step "Removing $($stale.Count) stale template variables not in catalog..."
    foreach ($var in $stale) {
        try {
            Invoke-RestMethod -Method DELETE -Uri "$ApiBase/templates/$TemplateId/variables/$($var.variableKey)" -Headers @{ Authorization = "Bearer $Token" } | Out-Null
        } catch {
            Write-Warning "  Failed to delete $($var.variableKey): $($_.Exception.Message)"
        }
    }
}

function Get-VersionLines([object]$VersionLinesResponse) {
    $payload = $VersionLinesResponse.result
    if ($null -eq $payload) {
        return @()
    }
    if ($null -ne $payload.PSObject.Properties['content']) {
        return @($payload.content)
    }
    return @($payload)
}

function Ensure-FolTemplateDraftForImport([string]$TemplateId, [string]$Token) {
    $detail = Invoke-Api GET "/templates/$TemplateId" $Token
    $status = [string]$detail.result.lifecycleStatus
    if ($status -eq 'DRAFT') {
        return
    }

    $versionLines = Get-VersionLines (Invoke-Api GET "/templates/$TemplateId/version-lines" $Token)
    $inFlightLine = $versionLines | Where-Object { $_.lineKind -eq 'IN_FLIGHT' } | Select-Object -First 1

    if (-not $inFlightLine) {
        $releaseLine = $versionLines | Where-Object { $_.releaseVersion } | Select-Object -First 1
        $releaseVersion = if ($releaseLine) { [string]$releaseLine.releaseVersion } else { [string]$detail.result.releaseVersion }
        if (-not $releaseVersion) {
            throw "FOL template $TemplateId is $status with no in-flight or published release line to clone for demo refresh."
        }
        Write-Step "Cloning release $releaseVersion to new dev line for FOL catalog refresh (was $status)..."
        Invoke-Api POST "/templates/$TemplateId/release-versions/$releaseVersion/clone" $Token | Out-Null
        return
    }

    Write-Step "Resetting FOL template $TemplateId from $status to DRAFT for catalog refresh (local demo only)..."
    $sql = @"
BEGIN;
UPDATE template
SET lifecycle_status = 'DRAFT',
    release_version = NULL,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE id = '$TemplateId'::uuid
  AND deleted_at IS NULL;
UPDATE template_version
SET lifecycle_status = 'DRAFT',
    release_version = NULL,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE template_id = '$TemplateId'::uuid
  AND deleted_at IS NULL;
UPDATE template_content_module_reference
SET locked_flag = FALSE
WHERE template_version_id IN (
    SELECT id FROM template_version WHERE template_id = '$TemplateId'::uuid AND deleted_at IS NULL
);
COMMIT;
"@
    $sql | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDb -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "FOL template draft reset failed (exit $LASTEXITCODE)" }
}

function Find-ExecutiveDemoTestDataSet([object[]]$Sets, [object]$TestDataConfig) {
    $scenarioName = if ($TestDataConfig.PSObject.Properties['scenarioName']) { [string]$TestDataConfig.scenarioName } else { '' }
    if ($scenarioName) {
        $byScenario = @($Sets) | Where-Object { $_.scenarioName -eq $scenarioName } |
            Sort-Object { [datetime]$_.updatedAt } -Descending |
            Select-Object -First 1
        if ($byScenario) { return $byScenario }
    }
    $byTag = @($Sets) | Where-Object {
        $_.coverageTags -contains 'executive-demo' -or $_.name -like 'Executive walkthrough*'
    } | Sort-Object { [datetime]$_.updatedAt } -Descending | Select-Object -First 1
    if ($byTag) { return $byTag }
    $configName = if ($TestDataConfig.PSObject.Properties['name']) { [string]$TestDataConfig.name } else { '' }
    if ($configName) {
        return @($Sets) | Where-Object { $_.name -eq $configName } | Select-Object -First 1
    }
    return $null
}

function Invoke-ExecutiveDemoTestDataSetSql {
    param(
        [string]$TemplateId,
        [string]$KeepExternalId = $null,
        [ValidateSet('unlock', 'prune')]
        [string]$Action
    )
    $keepClause = if ($KeepExternalId) { "AND external_id <> '$KeepExternalId'" } else { '' }
    if ($Action -eq 'unlock' -and $KeepExternalId) {
        $sql = @"
UPDATE template_test_data_set
SET locked = FALSE,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE template_id = '$TemplateId'::uuid
  AND external_id = '$KeepExternalId';
"@
    } else {
        $sql = @"
UPDATE template_test_data_set child
SET derived_from_id = NULL,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE child.template_id = '$TemplateId'::uuid
  AND child.derived_from_id IN (
      SELECT id FROM template_test_data_set parent
      WHERE parent.template_id = '$TemplateId'::uuid
        AND (
            parent.scenario_name = 'Syndicated term loan - Pacific Rim USD 250m (LMA IG baseline)'
            OR parent.name LIKE 'Executive walkthrough%'
            OR parent.coverage_tags_json LIKE '%executive-demo%'
        )
        $keepClause
  );
DELETE FROM template_test_data_set
WHERE template_id = '$TemplateId'::uuid
  AND (
      scenario_name = 'Syndicated term loan - Pacific Rim USD 250m (LMA IG baseline)'
      OR name LIKE 'Executive walkthrough%'
      OR coverage_tags_json LIKE '%executive-demo%'
  )
  $keepClause;
"@
    }
    $sql | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDb -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Executive demo test data set SQL ($Action) failed (exit $LASTEXITCODE)" }
}

function Upsert-TestDataSet([string]$TemplateId, [string]$Token, [object]$TestDataConfig) {
    $name = $TestDataConfig.name
    $sets = @(Invoke-Api GET "/templates/$TemplateId/test-data-sets" $Token).result
    $existing = Find-ExecutiveDemoTestDataSet -Sets $sets -TestDataConfig $TestDataConfig
    $body = @{
        name = $name
        required = [bool]$TestDataConfig.required
        variables = $TestDataConfig.variables
    }
    if ($null -ne $TestDataConfig.PSObject.Properties['scenarioName'] -and $TestDataConfig.scenarioName) {
        $body.scenarioName = [string]$TestDataConfig.scenarioName
    }
    if ($null -ne $TestDataConfig.PSObject.Properties['coverageTags'] -and $TestDataConfig.coverageTags) {
        $body.coverageTags = @($TestDataConfig.coverageTags)
    }
    if (-not $existing) {
        $created = Invoke-Api POST "/templates/$TemplateId/test-data-sets" $Token -Body $body
        $keeperId = $created.result.testDataSetId
    } else {
        $keeperId = $existing.testDataSetId
        if ($existing.locked) {
            Write-Step "Unlocking locked executive demo test data set $keeperId for in-place refresh..."
            Invoke-ExecutiveDemoTestDataSetSql -TemplateId $TemplateId -KeepExternalId $keeperId -Action unlock
        }
        Invoke-Api PUT "/templates/$TemplateId/test-data-sets/$keeperId" $Token -Body $body | Out-Null
    }
    $staleCount = @($sets | Where-Object { $_.testDataSetId -ne $keeperId }).Count
    if ($staleCount -gt 0) {
        Write-Step "Removing $staleCount stale executive demo test data set(s) on template $TemplateId..."
        Invoke-ExecutiveDemoTestDataSetSql -TemplateId $TemplateId -KeepExternalId $keeperId -Action prune
    }
}

if (-not (Test-Path $ConfigPath)) { throw "Missing config: $ConfigPath" }
$Config = Get-Content $ConfigPath -Raw | ConvertFrom-Json
Ensure-CatalogGenerated -Config $Config

$VariablesPath = Resolve-CatalogPath $Config.generatedCatalogFiles.variables
$RulesPath = Resolve-CatalogPath $Config.generatedCatalogFiles.compositionRules
$OverlaysPath = Resolve-CatalogPath $Config.generatedCatalogFiles.bindingOverlays
$TestDataPath = Resolve-CatalogPath $Config.generatedCatalogFiles.demoTestVariables
$ManifestPath = Resolve-CatalogPath $Config.generatedCatalogFiles.catalogManifest

$CatalogVariables = (Get-Content $VariablesPath -Raw | ConvertFrom-Json).variables
$CompositionRules = (Get-Content $RulesPath -Raw | ConvertFrom-Json).rules
$BindingOverlays = (Get-Content $OverlaysPath -Raw | ConvertFrom-Json).bindings
$TestDataConfig = Get-Content $TestDataPath -Raw | ConvertFrom-Json
$CatalogManifest = Get-Content $ManifestPath -Raw | ConvertFrom-Json
$ClauseBindings = @($CatalogManifest.clauseBindings)

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

$masters = Invoke-Api GET '/masters?size=200' $AdminToken
$master = Get-ApiListItems $masters.result | Where-Object { $_.name -eq $Config.masterName } | Select-Object -First 1
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
$needsDescriptionPatch = $false
$folExternalId = [string]$Config.templateExternalId
$templates = Invoke-Api GET "/templates?search=$([uri]::EscapeDataString($folExternalId))&searchMode=EXTERNAL_ID&size=20" $AuthorToken
$template = Get-ApiListItems $templates.result | Where-Object { $_.externalId -eq $folExternalId } | Select-Object -First 1
if (-not $template) {
    Write-Step "Creating FOL template $($Config.templateExternalId)..."
    $folLocale = if ($Config.PSObject.Properties['locale'] -and $Config.locale) { [string]$Config.locale } else { 'zh-CN' }
    $created = Invoke-Api POST '/templates' $AuthorToken -Body @{
        externalId = $Config.templateExternalId
        groupCode = $Config.groupCode
        name = $Config.templateName
        description = $Config.templateDescription
        masterId = $masterId
        locale = $folLocale
    }
    $templateId = $created.result.id
} else {
    $templateId = $template.id
    if ($template.masterId -ne $masterId) {
        Write-Step "Re-linking template to current FOL master (was $($template.masterId))..."
        $relinkSql = @"
UPDATE template
SET master_id = '$masterId'::uuid,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE id = '$templateId'::uuid
  AND deleted_at IS NULL;
"@
        $relinkSql | docker exec -i $PostgresContainer psql -U $PostgresUser -d $PostgresDb -v ON_ERROR_STOP=1
        if ($LASTEXITCODE -ne 0) { throw "Template master re-link failed (exit $LASTEXITCODE)" }
    }
    if ($template.description -notlike "*$($Config.catalogMarker)*") {
        $needsDescriptionPatch = $true
    }
}

Ensure-FolTemplateDraftForImport -TemplateId $templateId -Token $AuthorToken

if ($needsDescriptionPatch) {
    Invoke-Api PATCH "/templates/$templateId" $AuthorToken -Body @{
        name = $Config.templateName
        description = $Config.templateDescription
    }
}

# --- Variables ---
$catalogKeys = @($CatalogVariables | ForEach-Object { $_.key })
Remove-StaleCatalogVariables -TemplateId $templateId -Token $AuthorToken -CatalogKeys $catalogKeys
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
Remove-StaleLegacyFolBindings -TemplateId $templateId
$overlayKeys = @{}
$BindingOverlays.PSObject.Properties | ForEach-Object { $overlayKeys[$_.Name] = $_.Value }

# Signature and header anchors from overlays (not in clauseBindings)
$overlayOnlyAnchors = @($CatalogManifest.overlayOnlyAnchors)
foreach ($anchorId in $overlayOnlyAnchors) {
    if ($overlayKeys.ContainsKey($anchorId)) {
        Set-Binding -TemplateId $templateId -Token $AuthorToken -AnchorId $anchorId -StructuredContent $overlayKeys[$anchorId]
    }
}

foreach ($clause in $ClauseBindings) {
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
$sanitizedRules = @($CompositionRules | ForEach-Object {
    $rule = [ordered]@{
        ruleId = $_.ruleId
        conditionExpression = $_.conditionExpression
        targetAnchorId = $_.targetAnchorId
    }
    if ($_.trueBranchRuleId -and -not [string]::IsNullOrWhiteSpace([string]$_.trueBranchRuleId)) {
        $rule.trueBranchRuleId = [string]$_.trueBranchRuleId
    }
    if ($_.falseBranchRuleId -and -not [string]::IsNullOrWhiteSpace([string]$_.falseBranchRuleId)) {
        $rule.falseBranchRuleId = [string]$_.falseBranchRuleId
    }
    $rule
})
Invoke-Api PUT "/templates/$templateId/rules" $AuthorToken -Body @{ rules = $sanitizedRules } | Out-Null

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
Write-Host "  Clauses (SQL):       $($ClauseBindings.Count)"
Write-Host "  UI:                  http://localhost:4173/templates/$templateId"
Write-Host ""

if ($variableCount -lt 100) { Write-Warning "Variable count $variableCount is unusually low for FOL demo" }
if ($ruleCount -lt 10) { Write-Warning "Rule count $ruleCount is below target (10)" }
if ($features.conditionBlocks -lt 15) { Write-Warning "conditionBlocks $($features.conditionBlocks) below target (15)" }
if ($features.loopBlocks -lt 8) { Write-Warning "loopBlocks $($features.loopBlocks) below target (8)" }
if ($features.tableComponentRefs -lt 5) { Write-Warning "tableComponentRefs $($features.tableComponentRefs) below target (5)" }

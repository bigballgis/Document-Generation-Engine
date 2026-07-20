# Shared demo import helpers for deploy/demo-* packages.
param()

function Write-DemoStep([string]$Message) { Write-Host "==> $Message" }

function Get-DemoApiResultItems {
    param([object]$Response)
    if ($null -eq $Response -or $null -eq $Response.result) { return @() }
    if ($null -ne $Response.result.PSObject.Properties['content'] -and $Response.result.content) {
        return @($Response.result.content)
    }
    if ($null -ne $Response.result.PSObject.Properties['events'] -and $Response.result.events) {
        return @($Response.result.events)
    }
    if ($Response.result -is [System.Array]) { return @($Response.result) }
    return @($Response.result)
}

function Invoke-DemoApi {
    param(
        [Parameter(Mandatory = $true)][string]$ApiBase,
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Token,
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
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType 'application/json' -Body (ConvertTo-DemoApiJson -Body $Body)
    }
    return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers
}

function Get-DemoApiToken {
    param([string]$ApiBase, [string]$Username, [string]$Password)
    $login = Invoke-RestMethod -Method POST -Uri "$ApiBase/auth/login" -ContentType 'application/json' -Body (@{ username = $Username; password = $Password } | ConvertTo-Json)
    return $login.result.accessToken
}

function Wait-DemoBackendHealthy {
    param([string]$BackendUrl, [int]$MaxAttempts = 40)
    Write-DemoStep "Waiting for backend $BackendUrl/healthz ..."
    for ($i = 0; $i -lt $MaxAttempts; $i++) {
        try {
            $resp = Invoke-WebRequest -Uri "$BackendUrl/healthz" -UseBasicParsing -TimeoutSec 5
            if ($resp.StatusCode -eq 200) { return $true }
        } catch { Start-Sleep -Seconds 2 }
    }
    return $false
}

function Resolve-DemoCatalogPath {
    param([string]$DemoRoot, [string]$RelativePath)
    return Join-Path $DemoRoot ($RelativePath -replace '^config/', 'config/')
}

function Get-DemoTemplateDefinitions {
    param([object]$Config)
    if ($Config.PSObject.Properties['templates'] -and $Config.templates) {
        return @($Config.templates)
    }
    return @([ordered]@{
        externalId = $Config.templateExternalId
        name = $Config.templateName
        description = if ($Config.PSObject.Properties['templateDescription']) { $Config.templateDescription } else { $Config.masterDescription }
        masterDocx = $Config.masterDocx
    })
}

function Get-DemoTemplateAnchorIds {
    param(
        [string]$DemoRoot,
        [object]$Manifest,
        [string]$TemplateExternalId
    )
    if ($Manifest -and $Manifest.PSObject.Properties['templates']) {
        foreach ($entry in @($Manifest.templates)) {
            if ($entry -is [string]) {
                if ($entry -ne $TemplateExternalId) { continue }
            } elseif ([string]$entry.externalId -ne $TemplateExternalId) {
                continue
            } else {
                if ($entry.PSObject.Properties['anchorIds'] -and $entry.anchorIds) {
                    return @($entry.anchorIds)
                }
            }
        }
    }
    $configDir = Join-Path $DemoRoot 'config'
    if (Test-Path $configDir) {
        $anchorFile = Get-ChildItem $configDir -Filter '*-master-anchor-ids.json' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($anchorFile) {
            $data = Get-Content $anchorFile.FullName -Raw | ConvertFrom-Json
            if ($data.PSObject.Properties['templates']) {
                $matched = @($data.templates) | Where-Object { [string]$_.externalId -eq $TemplateExternalId } | Select-Object -First 1
                if ($matched -and $matched.anchorIds) { return @($matched.anchorIds) }
            }
        }
    }
    return $null
}

function Get-DemoBindingsForTemplate {
    param(
        [object]$BindingOverlays,
        [object]$Manifest,
        [string]$TemplateExternalId,
        [string]$DemoRoot = ''
    )
    $allBindings = @{}
    if ($BindingOverlays -and $BindingOverlays.PSObject.Properties['bindings']) {
        $BindingOverlays.bindings.PSObject.Properties | ForEach-Object { $allBindings[$_.Name] = $_.Value }
    }
    $anchorIds = Get-DemoTemplateAnchorIds -DemoRoot $DemoRoot -Manifest $Manifest -TemplateExternalId $TemplateExternalId
    if (-not $anchorIds -or $anchorIds.Count -eq 0) {
        return $allBindings
    }
    $filtered = @{}
    foreach ($anchorId in @($anchorIds)) {
        if ($allBindings.ContainsKey($anchorId)) {
            $filtered[$anchorId] = $allBindings[$anchorId]
        }
    }
    return $filtered
}

function Remove-DemoStaleBindings {
    param(
        [string]$TemplateId,
        [string[]]$AllowedAnchorIds,
        [string]$PostgresContainer = 'docgen-postgres'
    )
    if (-not $AllowedAnchorIds -or $AllowedAnchorIds.Count -eq 0) { return }
    $quoted = ($AllowedAnchorIds | ForEach-Object { "'$($_ -replace "'","''")'" }) -join ','
    $sql = @"
DELETE FROM anchor_binding
WHERE template_version_id IN (
    SELECT id FROM template_version
    WHERE template_id = '$TemplateId'::uuid
      AND release_version IS NULL
)
AND anchor_id NOT IN ($quoted);
"@
    $sql | docker exec -i $PostgresContainer psql -U docgen -d docgen -v ON_ERROR_STOP=1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Remove stale bindings failed for template $TemplateId (exit $LASTEXITCODE)" }
}

function Get-DemoTestDataSets {
    param(
        [object]$TestDataConfig,
        [string]$TemplateExternalId,
        [string]$DeployRoot = ''
    )
    if ($DeployRoot) {
        try {
            $runtimeManifest = Get-DemoRuntimeGenerateManifest -DeployRoot $DeployRoot
            $entry = @($runtimeManifest.templates) | Where-Object { [string]$_.externalId -eq $TemplateExternalId } | Select-Object -First 1
            if ($entry -and $entry.PSObject.Properties['testDataSetId'] -and $entry.testDataSetId -and $TestDataConfig.PSObject.Properties['testDataSets']) {
                $byId = @($TestDataConfig.testDataSets) | Where-Object { [string]$_.id -eq [string]$entry.testDataSetId } | Select-Object -First 1
                if ($byId) { return @($byId) }
            }
        } catch {
            Write-Warning "Runtime manifest test data lookup skipped for ${TemplateExternalId}: $($_.Exception.Message)"
        }
    }
    if ($TestDataConfig.PSObject.Properties['testDataSets']) {
        $matched = @($TestDataConfig.testDataSets) | Where-Object {
            $_.id -like "*$($TemplateExternalId.ToLower() -replace 'demo-','')*" -or
            $_.id -like "*$($TemplateExternalId.Split('-')[-1].ToLower())*"
        }
        if ($matched.Count -gt 0) { return @($matched) }
        return @($TestDataConfig.testDataSets)
    }
    return @([ordered]@{
        id = "$TemplateExternalId-executive"
        label = $TestDataConfig.name
        variables = $TestDataConfig.variables
    })
}

function Resolve-DemoContentModuleId {
    param(
        [string]$ApiBase,
        [string]$Token,
        [string]$GroupCode,
        [string]$ModuleCode
    )
    $resp = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path "/content-modules?groupCode=$GroupCode&size=200" -Token $Token
    $modules = Get-DemoApiResultItems -Response $resp
    $match = $modules | Where-Object { [string]$_.moduleCode -eq $ModuleCode } | Select-Object -First 1
    if (-not $match) {
        throw "Content module not found for moduleCode=$ModuleCode (groupCode=$GroupCode)"
    }
    return [string]$match.moduleId
}

function Resolve-DemoContentModuleSemanticVersion {
    param([object]$Ref)
    if ($Ref.PSObject.Properties['semanticVersion'] -and $Ref.semanticVersion) {
        return [string]$Ref.semanticVersion
    }
    return '1.0.0'
}

function Get-DemoStructuredContentVariableKeys {
    param([string]$StructuredContentJson)
    $keys = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    $loopVars = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    if ([string]::IsNullOrWhiteSpace($StructuredContentJson)) { return @{ keys = @(); loopVariables = @() } }
    try {
        $root = $StructuredContentJson | ConvertFrom-Json
        function Walk-DemoContentNode {
            param([object]$Node)
            if ($null -eq $Node) { return }
            if ($Node -is [System.Collections.IDictionary] -or ($Node.PSObject.Properties.Name -contains 'type')) {
                $type = [string]$Node.type
                if ($type -eq 'variable' -and $Node.key) { [void]$keys.Add([string]$Node.key) }
                if ($type -eq 'loopBlock' -and $Node.loopVariable) { [void]$loopVars.Add([string]$Node.loopVariable) }
                if ($type -eq 'conditionBlock' -and $Node.conditionExpression) {
                    foreach ($match in [regex]::Matches([string]$Node.conditionExpression, '\$\{([^}]+)\}')) {
                        [void]$keys.Add($match.Groups[1].Value.Trim())
                    }
                }
                if ($Node.tableComponent) {
                    $loopRow = $Node.tableComponent.loopRow
                    if ($loopRow) {
                        if ($loopRow.loopVariable) { [void]$loopVars.Add([string]$loopRow.loopVariable) }
                        foreach ($cell in @($loopRow.cells)) {
                            if ($cell.variableKey) { [void]$keys.Add([string]$cell.variableKey) }
                        }
                    }
                }
                foreach ($child in @($Node.children)) { Walk-DemoContentNode -Node $child }
            }
        }
        foreach ($node in @($root.nodes)) { Walk-DemoContentNode -Node $node }
    } catch {
        Write-Warning "Unable to scan structured content for variable keys: $($_.Exception.Message)"
    }
    return @{ keys = @($keys); loopVariables = @($loopVars) }
}

function Expand-DemoCatalogVariablesFromBindings {
    param(
        [object[]]$CatalogVariables,
        [hashtable]$Bindings
    )
    $known = @{}
    foreach ($var in @($CatalogVariables)) { $known[[string]$var.key] = $var }
    foreach ($entry in $Bindings.GetEnumerator()) {
        $json = ($entry.Value | ConvertTo-Json -Depth 100 -Compress)
        $scan = Get-DemoStructuredContentVariableKeys -StructuredContentJson $json
        foreach ($loopVar in @($scan.loopVariables)) {
            if (-not $known.ContainsKey($loopVar)) {
                $known[$loopVar] = [ordered]@{ key = $loopVar; label = $loopVar; type = 'LIST'; required = $false }
            }
        }
        foreach ($key in @($scan.keys)) {
            if (-not $known.ContainsKey($key)) {
                $varType = if ($key -like 'include*' -or $key -like 'has*') { 'BOOLEAN' } else { 'TEXT' }
                $known[$key] = [ordered]@{ key = $key; label = $key; type = $varType; required = $false }
            }
        }
    }
    return @($known.Values)
}

function Format-DemoBindingExpectedUpdatedAt {
    param([object]$UpdatedAt)
    if ($null -eq $UpdatedAt) { return $null }
    if ($UpdatedAt -is [string]) { return [string]$UpdatedAt }
    $utc = [datetime]$UpdatedAt
    if ($utc.Kind -ne [DateTimeKind]::Utc) { $utc = $utc.ToUniversalTime() }
    return $utc.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'")
}

function Set-DemoBinding {
    param(
        [string]$ApiBase,
        [string]$TemplateId,
        [string]$Token,
        [string]$AnchorId,
        [object]$StructuredContent
    )
    $detail = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path "/templates/$TemplateId" -Token $Token
    $existing = @($detail.result.bindings) | Where-Object { $_.anchorId -eq $AnchorId } | Select-Object -First 1
    $body = @{
        anchorId = $AnchorId
        declaredContentType = 'TEXT'
        structuredContentJson = ($StructuredContent | ConvertTo-Json -Depth 100 -Compress)
    }
    if ($existing -and $existing.updatedAt) {
        $body.expectedUpdatedAt = Format-DemoBindingExpectedUpdatedAt $existing.updatedAt
    }
    Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/templates/$TemplateId/bindings/$AnchorId" -Token $Token -Body $body | Out-Null
}

function Get-DemoMasterList {
    param([object]$MastersResponse)
    return Get-DemoApiResultItems -Response $MastersResponse
}

function Unlock-DemoTestDataSetIfLocked {
    param(
        [string]$TemplateId,
        [string]$TestDataSetId,
        [string]$PostgresContainer
    )
    $sql = @"
UPDATE template_test_data_set
SET locked = FALSE,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE template_id = '$TemplateId'::uuid
  AND external_id = '$TestDataSetId';
"@
    $sql | docker exec -i $PostgresContainer psql -U docgen -d docgen -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Unlock test data set $TestDataSetId failed (exit $LASTEXITCODE)" }
}

function Resolve-DemoMasterId {
    param([object]$Master)
    if ($null -eq $Master) { return $null }
    if ($Master.PSObject.Properties['id'] -and $Master.id) { return [string]$Master.id.Trim() }
    if ($Master.PSObject.Properties['masterId'] -and $Master.masterId) { return [string]$Master.masterId.Trim() }
    return $null
}

function Ensure-DemoMaster {
    param(
        [string]$ApiBase,
        [object]$Config,
        [string]$MasterDocxRelative,
        [string]$AdminToken,
        [string]$GroupAdminToken,
        [string]$MasterNameOverride = '',
        [switch]$SkipRefresh
    )
    $DemoRoot = $Config._demoRoot
    $DocxPath = Join-Path $DemoRoot $MasterDocxRelative
    if (-not (Test-Path $DocxPath)) {
        throw "Missing master DOCX: $DocxPath (run *MasterDocxAssetGeneratorTest first)"
    }
    $masterName = if ($MasterNameOverride) {
        [string]$MasterNameOverride.Trim()
    } elseif ($Config.PSObject.Properties['masterName'] -and $Config.masterName) {
        [string]$Config.masterName.Trim()
    } else {
        [System.IO.Path]::GetFileNameWithoutExtension($MasterDocxRelative)
    }
    $mastersResp = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path '/masters?size=200' -Token $GroupAdminToken
    $masterList = Get-DemoMasterList $mastersResp
    $master = $masterList | Where-Object { $_.name -eq $masterName } | Sort-Object { [datetime]$_.updatedAt } -Descending | Select-Object -First 1
    if (-not $master) {
        Write-DemoStep "Uploading master $masterName ..."
        $created = Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path '/masters' -Token $GroupAdminToken -MultipartFields @{
            groupCode = $Config.groupCode
            name = $masterName
            description = "$($Config.masterDescription) ($($Config.masterLayoutVersion))"
            file = (Get-Item $DocxPath)
        }
        $masterId = [string]$created.result.id.Trim()
        Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/masters/$masterId/submit-review" -Token $GroupAdminToken -Body @{ changeSummary = 'Demo import' }
        $master = (Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/masters/$masterId/review" -Token $AdminToken -Body @{ decision = 'APPROVED'; commentSummary = 'Demo approved' }).result
    } else {
        $needsRefresh = (-not $SkipRefresh) -and ($master.description -notlike "*$($Config.masterLayoutVersion)*")
        if ($needsRefresh) {
            Write-DemoStep "Refreshing master layout $masterName ($($Config.masterLayoutVersion)) ..."
            $currentMasterId = [string]$master.id.Trim()
            try {
                Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/masters/$currentMasterId/file" -Token $GroupAdminToken -MultipartFields @{ file = (Get-Item $DocxPath) }
                Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/masters/$currentMasterId/submit-review" -Token $GroupAdminToken -Body @{ changeSummary = 'Demo layout refresh' }
                $reviewed = (Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/masters/$currentMasterId/review" -Token $AdminToken -Body @{ decision = 'APPROVED'; commentSummary = 'Demo approved' }).result
                if ($reviewed -and (Resolve-DemoMasterId $reviewed)) {
                    $master = $reviewed
                }
            } catch {
                Write-Warning "Master layout refresh skipped for ${masterName}: $($_.Exception.Message)"
            }
            $mastersResp = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path '/masters?size=200' -Token $GroupAdminToken
            $master = Get-DemoMasterList $mastersResp | Where-Object { [string]$_.name.Trim() -eq $masterName } | Sort-Object { [datetime]$_.updatedAt } -Descending | Select-Object -First 1
        } else {
            Write-DemoStep "SKIP master upload (layout unchanged): $masterName"
        }
    }
    if (-not $master -or -not (Resolve-DemoMasterId $master)) {
        $mastersResp = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path '/masters?size=200' -Token $GroupAdminToken
        $master = Get-DemoMasterList $mastersResp | Where-Object { $_.name -eq $masterName } | Sort-Object { [datetime]$_.updatedAt } -Descending | Select-Object -First 1
    }
    $resolvedId = Resolve-DemoMasterId $master
    if (-not $resolvedId) {
        throw "Master unavailable after ensure: $masterName"
    }
    $master | Add-Member -NotePropertyName 'id' -NotePropertyValue $resolvedId -Force
    return $master
}

function Get-DemoVersionLines {
    param([object]$VersionLinesResponse)
    $payload = $VersionLinesResponse.result
    if ($null -eq $payload) { return @() }
    if ($null -ne $payload.PSObject.Properties['content']) { return @($payload.content) }
    return @($payload)
}

# Local demo only — reset non-DRAFT templates so bindings/variables can be refreshed (mirrors FOL import).
function Set-DemoTemplateMasterId {
    param(
        [string]$TemplateId,
        [string]$MasterId,
        [string]$ExternalId,
        [string]$MasterName,
        [string]$PostgresContainer = 'docgen-postgres'
    )
    $templateId = [string]$TemplateId.Trim()
    $masterId = [string]$MasterId.Trim()
    Write-DemoStep "Rebinding $ExternalId to master $MasterName ($masterId) via SQL ..."
    $sql = @"
UPDATE template
SET master_id = '$MasterId'::uuid,
    updated_at = (NOW() AT TIME ZONE 'UTC')
WHERE id = '$TemplateId'::uuid
  AND deleted_at IS NULL;
"@
    $sql | docker exec -i $PostgresContainer psql -U docgen -d docgen -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Template master rebind failed for $ExternalId (exit $LASTEXITCODE)" }
}

function Ensure-DemoTemplateDraftForImport {
    param(
        [string]$ApiBase,
        [string]$TemplateId,
        [string]$ExternalId,
        [string]$Token,
        [string]$PostgresContainer = 'docgen-postgres'
    )
    $detail = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path "/templates/$TemplateId" -Token $Token
    $status = [string]$detail.result.lifecycleStatus
    if ($status -eq 'DRAFT') { return }

    $versionLines = Get-DemoVersionLines (Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path "/templates/$TemplateId/version-lines" -Token $Token)
    $inFlightLine = $versionLines | Where-Object { $_.lineKind -eq 'IN_FLIGHT' } | Select-Object -First 1

    if (-not $inFlightLine) {
        $releaseLine = $versionLines | Where-Object { $_.releaseVersion } | Select-Object -First 1
        $releaseVersion = if ($releaseLine) { [string]$releaseLine.releaseVersion } else { [string]$detail.result.releaseVersion }
        if (-not $releaseVersion) {
            throw "Template $ExternalId is $status with no in-flight or published release line to clone for demo refresh."
        }
        Write-DemoStep "Cloning release $releaseVersion to new dev line for $ExternalId catalog refresh (was $status)..."
        try {
            Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/templates/$TemplateId/release-versions/$releaseVersion/clone" -Token $Token | Out-Null
            return
        } catch {
            Write-Warning "Clone failed for ${ExternalId}, falling back to SQL draft reset: $($_.Exception.Message)"
        }
    }

    Write-DemoStep "Resetting $ExternalId from $status to DRAFT for catalog refresh (local demo only)..."
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
    $sql | docker exec -i $PostgresContainer psql -U docgen -d docgen -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw "Template draft reset failed for $ExternalId (exit $LASTEXITCODE)" }
}

function Import-DemoPackage {
    param(
        [string]$DemoRoot,
        [string]$ConfigPath,
        [string]$BackendUrl,
        [string]$PostgresContainer,
        [switch]$SkipSql,
        [switch]$SkipApi,
        [switch]$SkipMasterRefresh
    )
    if (-not (Test-Path $ConfigPath)) { throw "Config not found: $ConfigPath" }
    $Config = Get-Content $ConfigPath -Raw | ConvertFrom-Json
    $Config | Add-Member -NotePropertyName '_demoRoot' -NotePropertyValue $DemoRoot -Force
    $marker = $Config.catalogMarker
    Write-DemoStep "Demo import marker: $marker layout: $($Config.masterLayoutVersion)"

    if (-not $SkipSql) {
        $sqlDir = Join-Path $DemoRoot 'sql'
        if (Test-Path $sqlDir) {
            Get-ChildItem $sqlDir -Filter '*.sql' | Sort-Object Name | ForEach-Object {
                Write-DemoStep "Applying SQL $($_.Name)"
                Get-Content $_.FullName -Raw | docker exec -i $PostgresContainer psql -U docgen -d docgen -v ON_ERROR_STOP=1
                if ($LASTEXITCODE -ne 0) { throw "SQL failed for $($_.Name)" }
            }
        }
    }

    if ($SkipApi) {
        Write-DemoStep "API import skipped (-SkipApi). SQL/catalogMarker=$marker validated."
        return
    }

    if (-not (Wait-DemoBackendHealthy -BackendUrl $BackendUrl)) {
        throw "Backend not healthy at $BackendUrl/healthz — use -SkipApi for SQL-only import"
    }

    $ApiBase = "$BackendUrl/api/management/v1"
    $AdminToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000001' -Password 'ChangeMe123!'
    $GroupAdminToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000002' -Password 'ChangeMe123!'
    $AuthorToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000003' -Password 'ChangeMe123!'

    $catalogFiles = $Config.generatedCatalogFiles
    $VariablesPath = Resolve-DemoCatalogPath -DemoRoot $DemoRoot -RelativePath $catalogFiles.variables
    $OverlaysPath = Resolve-DemoCatalogPath -DemoRoot $DemoRoot -RelativePath $catalogFiles.bindingOverlays
    $TestDataPath = Resolve-DemoCatalogPath -DemoRoot $DemoRoot -RelativePath $catalogFiles.demoTestVariables
    $ManifestPath = Resolve-DemoCatalogPath -DemoRoot $DemoRoot -RelativePath $catalogFiles.catalogManifest

    $CatalogVariables = (Get-Content $VariablesPath -Raw | ConvertFrom-Json).variables
    $BindingOverlays = Get-Content $OverlaysPath -Raw | ConvertFrom-Json
    $TestDataConfig = Get-Content $TestDataPath -Raw | ConvertFrom-Json
    $Manifest = Get-Content $ManifestPath -Raw | ConvertFrom-Json

    $templateDefs = Get-DemoTemplateDefinitions -Config $Config
    foreach ($templateDef in $templateDefs) {
        $externalId = $templateDef.externalId
        $masterNameOverride = if ($templateDef.PSObject.Properties['masterName'] -and $templateDef.masterName) { [string]$templateDef.masterName } else { '' }
        $master = Ensure-DemoMaster -ApiBase $ApiBase -Config $Config -MasterDocxRelative $templateDef.masterDocx -AdminToken $AdminToken -GroupAdminToken $GroupAdminToken -MasterNameOverride $masterNameOverride -SkipRefresh:$SkipMasterRefresh
        $masterId = Resolve-DemoMasterId $master

        $searchPath = "/templates?search=$([uri]::EscapeDataString($externalId))&searchMode=EXTERNAL_ID&size=20"
        $templatesResp = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path $searchPath -Token $AuthorToken
        $templateList = Get-DemoApiResultItems -Response $templatesResp
        $template = $templateList | Where-Object { $_.externalId -eq $externalId } | Select-Object -First 1
        if (-not $template) {
            Write-DemoStep "Creating template $externalId ..."
            $templateLocale = if ($templateDef.PSObject.Properties['locale'] -and $templateDef.locale) {
                [string]$templateDef.locale
            } elseif ($Config.PSObject.Properties['locale'] -and $Config.locale) {
                [string]$Config.locale
            } else {
                'zh-CN'
            }
            $created = Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path '/templates' -Token $AuthorToken -Body @{
                externalId = $externalId
                groupCode = $Config.groupCode
                name = $templateDef.name
                description = "$($templateDef.description) [$marker]"
                masterId = $masterId
                locale = $templateLocale
            }
            $templateId = $created.result.id
        } else {
            $templateId = $template.id
            if ([string](Resolve-DemoMasterId $master) -and [string]$template.masterId -ne [string]$masterId) {
                Set-DemoTemplateMasterId -TemplateId $templateId -MasterId $masterId -ExternalId $externalId -MasterName ([string]$master.name).Trim() -PostgresContainer $PostgresContainer
            }
            if ($template.description -and $template.description.Contains("[$marker]")) {
                Write-DemoStep "SKIP template create (catalogMarker present): $externalId"
            } else {
                Invoke-DemoApi -ApiBase $ApiBase -Method PATCH -Path "/templates/$templateId" -Token $AuthorToken -Body @{
                    description = "$($templateDef.description) [$marker]"
                } | Out-Null
            }
        }

        Ensure-DemoTemplateDraftForImport -ApiBase $ApiBase -TemplateId $templateId -ExternalId $externalId -Token $AuthorToken -PostgresContainer $PostgresContainer

        $bindings = Get-DemoBindingsForTemplate -BindingOverlays $BindingOverlays -Manifest $Manifest -TemplateExternalId $externalId -DemoRoot $DemoRoot
        $anchorIds = @($bindings.Keys)
        Remove-DemoStaleBindings -TemplateId $templateId -AllowedAnchorIds $anchorIds -PostgresContainer $PostgresContainer
        $allVariables = Expand-DemoCatalogVariablesFromBindings -CatalogVariables @($CatalogVariables) -Bindings $bindings
        Write-DemoStep "Upserting $($allVariables.Count) variables for $externalId ..."
        foreach ($var in @($allVariables)) {
            Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/templates/$templateId/variables/$($var.key)" -Token $AuthorToken -Body @{
                variableKey = $var.key
                variableType = $var.type
                required = [bool]$var.required
                description = if ($var.PSObject.Properties['label']) { $var.label } else { $var.key }
            } | Out-Null
        }

        Write-DemoStep "Applying $($bindings.Count) bindings for $externalId ..."
        foreach ($entry in $bindings.GetEnumerator()) {
            Set-DemoBinding -ApiBase $ApiBase -TemplateId $templateId -Token $AuthorToken -AnchorId $entry.Key -StructuredContent $entry.Value
        }

        if ($Manifest.PSObject.Properties['contentModuleRefs']) {
            foreach ($ref in @($Manifest.contentModuleRefs)) {
                try {
                    $moduleUuid = Resolve-DemoContentModuleId -ApiBase $ApiBase -Token $GroupAdminToken -GroupCode $Config.groupCode -ModuleCode ([string]$ref.moduleCode)
                    $semanticVersion = Resolve-DemoContentModuleSemanticVersion -Ref $ref
                    Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/templates/$templateId/content-module-references/$($ref.referenceKey)" -Token $AuthorToken -Body @{
                        referenceKey = $ref.referenceKey
                        moduleId = $moduleUuid
                        semanticVersion = $semanticVersion
                    } | Out-Null
                } catch {
                    Write-Warning "Module ref $($ref.referenceKey): $($_.Exception.Message)"
                }
            }
        }

        $testSets = Get-DemoTestDataSets -TestDataConfig $TestDataConfig -TemplateExternalId $externalId -DeployRoot (Split-Path -Parent $DemoRoot)
        foreach ($set in $testSets) {
            $body = @{
                name = if ($set.label) { $set.label } else { $set.name }
                required = $true
                variables = $set.variables
            }
            $existing = @(Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path "/templates/$templateId/test-data-sets" -Token $AuthorToken).result |
                Where-Object { $_.name -eq $body.name } | Select-Object -First 1
            if ($existing) {
                if ($existing.locked) {
                    Write-DemoStep "Unlocking locked test data set $($existing.testDataSetId) ..."
                    Unlock-DemoTestDataSetIfLocked -TemplateId $templateId -TestDataSetId $existing.testDataSetId -PostgresContainer $PostgresContainer
                }
                Write-DemoStep "Updating test data set $($body.name) ..."
                Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/templates/$templateId/test-data-sets/$($existing.testDataSetId)" -Token $AuthorToken -Body $body | Out-Null
            } else {
                Write-DemoStep "Creating test data set $($body.name) ..."
                Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/templates/$templateId/test-data-sets" -Token $AuthorToken -Body $body | Out-Null
            }
        }

        try {
            Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/templates/$templateId/bindings/validate" -Token $AuthorToken -Body @{} | Out-Null
        } catch {
            Write-Warning ("Binding validation for {0}: {1}" -f $externalId, $_.Exception.Message)
        }
    }

    Write-DemoStep "Demo package import complete (catalogMarker=$marker)"
}

# P23-T12 — canonical publish registry (mirrored by DemoPublishRegistry / DemoPublishOrchestrationContractTest).
function Get-DemoAllowedApiAdGroups {
    param([Parameter(Mandatory = $true)][string]$GroupCode)
    # Unary comma keeps a one-element array from being unwrapped when assigned (PS assignment quirk).
    if ($GroupCode -eq 'CORP') { return ,@('CORP_API') }
    return ,@('RETAIL_API')
}

function ConvertTo-DemoApiJson {
    param([Parameter(Mandatory = $true)][object]$Body)
    if ($Body -is [hashtable]) {
        $normalized = @{}
        foreach ($key in $Body.Keys) {
            $value = $Body[$key]
            if ($null -ne $value -and ($value -is [System.Array] -or $value.GetType().IsArray)) {
                $normalized[$key] = @($value)
            } else {
                $normalized[$key] = $value
            }
        }
        return ($normalized | ConvertTo-Json -Depth 100 -Compress)
    }
    return ($Body | ConvertTo-Json -Depth 100 -Compress)
}

function Get-DemoPublishExternalIds {
    param([string]$DeployRoot)
    if (-not $DeployRoot) {
        $DeployRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
    }
    $ids = [System.Collections.Generic.List[string]]::new()
    $packageOrder = @(
        'demo-fol',
        'demo-retail-account',
        'demo-mortgage',
        'demo-credit-limit',
        'demo-trade-lc',
        'demo-collection',
        'demo-annual-review',
        'demo-wealth'
    )
    foreach ($packageDir in $packageOrder) {
        $configDir = Join-Path $DeployRoot $packageDir 'config'
        if (-not (Test-Path $configDir)) { continue }
        Get-ChildItem $configDir -Filter '*-template-config.json' | ForEach-Object {
            $config = Get-Content $_.FullName -Raw | ConvertFrom-Json
            foreach ($def in (Get-DemoTemplateDefinitions -Config $config)) {
                if ($def.externalId) { $ids.Add([string]$def.externalId) }
            }
        }
    }
    if ($ids -notcontains 'DEMO-FULL-FLOW-LETTER') {
        $ids.Insert(1, 'DEMO-FULL-FLOW-LETTER')
    }
    return @($ids | Select-Object -Unique)
}

function Resolve-DemoPublishAccessToken {
    param([string]$GroupCode)
    if ($GroupCode -in @('TRADE', 'WEALTH')) { return $script:GlobalAdminToken }
    return $script:AuthorToken
}

function Resolve-DemoPublishTesterToken {
    param([string]$GroupCode)
    if ($GroupCode -in @('TRADE', 'WEALTH')) { return $script:GlobalAdminToken }
    return $script:TesterToken
}

# P23-T14 — runtime generate manifest + executive variable resolution (mirrored by DemoRuntimeGenerateManifest).
function Get-DemoRuntimeGenerateManifest {
    param([string]$DeployRoot)
    if (-not $DeployRoot) {
        $DeployRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
    }
    $manifestPath = Join-Path $DeployRoot 'demo-shared/demo-runtime-generate-manifest.json'
    if (-not (Test-Path $manifestPath)) {
        throw "Missing runtime generate manifest: $manifestPath"
    }
    return (Get-Content $manifestPath -Raw | ConvertFrom-Json)
}

function Resolve-DemoExecutiveVariables {
    param(
        [Parameter(Mandatory = $true)][object]$TemplateEntry,
        [Parameter(Mandatory = $true)][string]$WorkspaceRoot
    )
    $fixturePath = Join-Path $WorkspaceRoot ([string]$TemplateEntry.variablesFixture)
    if (-not (Test-Path $fixturePath)) {
        throw "Missing variables fixture for $($TemplateEntry.externalId): $fixturePath"
    }
    $fixture = Get-Content $fixturePath -Raw | ConvertFrom-Json
    if ($TemplateEntry.PSObject.Properties['testDataSetId'] -and $TemplateEntry.testDataSetId) {
        $dataSetId = [string]$TemplateEntry.testDataSetId
        $matched = @($fixture.testDataSets) | Where-Object { [string]$_.id -eq $dataSetId } | Select-Object -First 1
        if (-not $matched) {
            throw "testDataSet '$dataSetId' not found in $fixturePath"
        }
        return $matched.variables
    }
    if ($fixture.PSObject.Properties['variables'] -and $fixture.variables) {
        return $fixture.variables
    }
    throw "Fixture $fixturePath has no variables or testDataSet match for $($TemplateEntry.externalId)"
}

function Resolve-DemoTestDataSetForPublish {
    param(
        [Parameter(Mandatory = $true)][string]$ExternalId,
        [Parameter(Mandatory = $true)][string]$TemplateId,
        [Parameter(Mandatory = $true)][string]$ApiBase,
        [Parameter(Mandatory = $true)][string]$Token,
        [Parameter(Mandatory = $true)][string]$DeployRoot,
        [Parameter(Mandatory = $true)][string]$WorkspaceRoot
    )
    $manifest = Get-DemoRuntimeGenerateManifest -DeployRoot $DeployRoot
    $templateEntry = @($manifest.templates) | Where-Object { [string]$_.externalId -eq $ExternalId } | Select-Object -First 1
    if (-not $templateEntry) {
        throw "No runtime manifest entry for $ExternalId"
    }
    $fixturePath = Join-Path $WorkspaceRoot ([string]$templateEntry.variablesFixture)
    if (-not (Test-Path $fixturePath)) {
        throw "Missing variables fixture for $ExternalId : $fixturePath"
    }
    $fixture = Get-Content $fixturePath -Raw | ConvertFrom-Json
    $expectedLabel = $null
    if ($templateEntry.PSObject.Properties['testDataSetId'] -and $templateEntry.testDataSetId) {
        $matchedFixture = @($fixture.testDataSets) | Where-Object { [string]$_.id -eq [string]$templateEntry.testDataSetId } | Select-Object -First 1
        if ($matchedFixture -and $matchedFixture.label) {
            $expectedLabel = [string]$matchedFixture.label
        } elseif ($matchedFixture -and $matchedFixture.name) {
            $expectedLabel = [string]$matchedFixture.name
        }
    }
    if (-not $expectedLabel -and $fixture.PSObject.Properties['name']) {
        $expectedLabel = [string]$fixture.name
    }
    $setsResp = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path "/templates/$TemplateId/test-data-sets" -Token $Token
    $sets = Get-DemoApiResultItems -Response $setsResp
    if ($sets.Count -eq 0) { throw "No test data sets for template $ExternalId ($TemplateId)" }
    if ($expectedLabel) {
        $byLabel = $sets | Where-Object { [string]$_.name -eq $expectedLabel } | Select-Object -First 1
        if ($byLabel) { return $byLabel }
    }
    $required = $sets | Where-Object { $_.required -eq $true } | Select-Object -First 1
    if ($required) { return $required }
    return $sets[0]
}

# Shared demo import helpers for deploy/demo-* packages.
param()

function Write-DemoStep([string]$Message) { Write-Host "==> $Message" }

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
        return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $Headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 100)
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

function Get-DemoBindingsForTemplate {
    param(
        [object]$BindingOverlays,
        [object]$Manifest,
        [string]$TemplateExternalId
    )
    $allBindings = @{}
    if ($BindingOverlays -and $BindingOverlays.PSObject.Properties['bindings']) {
        $BindingOverlays.bindings.PSObject.Properties | ForEach-Object { $allBindings[$_.Name] = $_.Value }
    }
    if (-not $Manifest -or -not $Manifest.PSObject.Properties['templates']) {
        return $allBindings
    }
    $templateEntry = @($Manifest.templates) | Where-Object { $_.externalId -eq $TemplateExternalId } | Select-Object -First 1
    if (-not $templateEntry -or -not $templateEntry.anchorIds) {
        return $allBindings
    }
    $filtered = @{}
    foreach ($anchorId in @($templateEntry.anchorIds)) {
        if ($allBindings.ContainsKey($anchorId)) {
            $filtered[$anchorId] = $allBindings[$anchorId]
        }
    }
    return $filtered
}

function Get-DemoTestDataSets {
    param([object]$TestDataConfig, [string]$TemplateExternalId)
    if ($TestDataConfig.PSObject.Properties['testDataSets']) {
        $matched = @($TestDataConfig.testDataSets) | Where-Object {
            $_.id -like "*$($TemplateExternalId.ToLower() -replace 'demo-','')*" -or
            $_.id -like "*$($TemplateExternalId.Split('-')[-1].ToLower())*"
        }
        if ($matched.Count -gt 0) { return $matched }
        return @($TestDataConfig.testDataSets)
    }
    return @([ordered]@{
        id = "$TemplateExternalId-executive"
        label = $TestDataConfig.name
        variables = $TestDataConfig.variables
    })
}

function Set-DemoBinding {
    param(
        [string]$ApiBase,
        [string]$TemplateId,
        [string]$Token,
        [string]$AnchorId,
        [object]$StructuredContent
    )
    Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/templates/$TemplateId/bindings/$AnchorId" -Token $Token -Body @{
        anchorId = $AnchorId
        declaredContentType = 'TEXT'
        structuredContentJson = ($StructuredContent | ConvertTo-Json -Depth 100 -Compress)
    } | Out-Null
}

function Ensure-DemoMaster {
    param(
        [string]$ApiBase,
        [object]$Config,
        [string]$MasterDocxRelative,
        [string]$AdminToken,
        [string]$GroupAdminToken
    )
    $DemoRoot = $Config._demoRoot
    $DocxPath = Join-Path $DemoRoot $MasterDocxRelative
    if (-not (Test-Path $DocxPath)) {
        throw "Missing master DOCX: $DocxPath (run *MasterDocxAssetGeneratorTest first)"
    }
    $masterName = if ($Config.PSObject.Properties['masterName'] -and $Config.masterName) {
        $Config.masterName
    } else {
        [System.IO.Path]::GetFileNameWithoutExtension($MasterDocxRelative)
    }
    $masters = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path '/masters' -Token $GroupAdminToken
    $master = $masters.result | Where-Object { $_.name -eq $masterName } | Select-Object -First 1
    if (-not $master) {
        Write-DemoStep "Uploading master $masterName ..."
        $created = Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path '/masters' -Token $GroupAdminToken -MultipartFields @{
            groupCode = $Config.groupCode
            name = $masterName
            description = "$($Config.masterDescription) ($($Config.masterLayoutVersion))"
            file = (Get-Item $DocxPath)
        }
        $masterId = $created.result.id
        Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/masters/$masterId/submit-review" -Token $GroupAdminToken -Body @{ changeSummary = 'Demo import' }
        $master = (Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/masters/$masterId/review" -Token $AdminToken -Body @{ decision = 'APPROVED'; commentSummary = 'Demo approved' }).result
    } else {
        $needsRefresh = ($master.description -notlike "*$($Config.masterLayoutVersion)*")
        if ($needsRefresh) {
            Write-DemoStep "Refreshing master layout $masterName ($($Config.masterLayoutVersion)) ..."
            Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/masters/$($master.id)/file" -Token $GroupAdminToken -MultipartFields @{ file = (Get-Item $DocxPath) }
            Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/masters/$($master.id)/submit-review" -Token $GroupAdminToken -Body @{ changeSummary = 'Demo layout refresh' }
            $master = (Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path "/masters/$($master.id)/review" -Token $AdminToken -Body @{ decision = 'APPROVED'; commentSummary = 'Demo approved' }).result
        } else {
            Write-DemoStep "SKIP master upload (layout unchanged): $masterName"
        }
    }
    return $master
}

function Import-DemoPackage {
    param(
        [string]$DemoRoot,
        [string]$ConfigPath,
        [string]$BackendUrl,
        [string]$PostgresContainer,
        [switch]$SkipSql,
        [switch]$SkipApi
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
        $master = Ensure-DemoMaster -ApiBase $ApiBase -Config $Config -MasterDocxRelative $templateDef.masterDocx -AdminToken $AdminToken -GroupAdminToken $GroupAdminToken
        $masterId = $master.id

        $templates = Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path '/templates' -Token $AuthorToken
        $template = $templates.result | Where-Object { $_.externalId -eq $externalId } | Select-Object -First 1
        if (-not $template) {
            Write-DemoStep "Creating template $externalId ..."
            $created = Invoke-DemoApi -ApiBase $ApiBase -Method POST -Path '/templates' -Token $AuthorToken -Body @{
                externalId = $externalId
                groupCode = $Config.groupCode
                name = $templateDef.name
                description = "$($templateDef.description) [$marker]"
                masterId = $masterId
            }
            $templateId = $created.result.id
        } else {
            $templateId = $template.id
            if ($template.description -like "*[$marker]*") {
                Write-DemoStep "SKIP template create (catalogMarker present): $externalId"
            } else {
                Invoke-DemoApi -ApiBase $ApiBase -Method PATCH -Path "/templates/$templateId" -Token $AuthorToken -Body @{
                    description = "$($templateDef.description) [$marker]"
                } | Out-Null
            }
        }

        Write-DemoStep "Upserting variables for $externalId ..."
        foreach ($var in @($CatalogVariables)) {
            Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/templates/$templateId/variables/$($var.key)" -Token $AuthorToken -Body @{
                variableKey = $var.key
                variableType = $var.type
                required = [bool]$var.required
                description = if ($var.PSObject.Properties['label']) { $var.label } else { $var.key }
            } | Out-Null
        }

        $bindings = Get-DemoBindingsForTemplate -BindingOverlays $BindingOverlays -Manifest $Manifest -TemplateExternalId $externalId
        Write-DemoStep "Applying $($bindings.Count) bindings for $externalId ..."
        foreach ($entry in $bindings.GetEnumerator()) {
            Set-DemoBinding -ApiBase $ApiBase -TemplateId $templateId -Token $AuthorToken -AnchorId $entry.Key -StructuredContent $entry.Value
        }

        if ($Manifest.PSObject.Properties['contentModuleRefs']) {
            foreach ($ref in @($Manifest.contentModuleRefs)) {
                try {
                    Invoke-DemoApi -ApiBase $ApiBase -Method PUT -Path "/templates/$templateId/content-module-references/$($ref.referenceKey)" -Token $AuthorToken -Body @{
                        referenceKey = $ref.referenceKey
                        moduleId = $ref.moduleCode
                        semanticVersion = '1.0.0'
                    } | Out-Null
                } catch {
                    Write-Warning "Module ref $($ref.referenceKey): $($_.Exception.Message)"
                }
            }
        }

        $testSets = Get-DemoTestDataSets -TestDataConfig $TestDataConfig -TemplateExternalId $externalId
        foreach ($set in $testSets) {
            $body = @{
                name = if ($set.label) { $set.label } else { $set.name }
                required = $true
                variables = $set.variables
            }
            $existing = @(Invoke-DemoApi -ApiBase $ApiBase -Method GET -Path "/templates/$templateId/test-data-sets" -Token $AuthorToken).result |
                Where-Object { $_.name -eq $body.name } | Select-Object -First 1
            if ($existing) {
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
    if ($GroupCode -eq 'CORP') { return @('CORP_API') }
    return @('RETAIL_API')
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

# Runtime generate all published demo templates + SHA-256 evidence manifest.
# P23-T14 — BDD-DEMO-TYP-011/012/013 runtime proof driver.
#
# Prerequisites:
#   1. Backend healthy on :8080 (Docker deploy or local spring-boot:run)
#   2. deploy/import-all-demos.ps1 completed
#   3. deploy/publish-all-demos.ps1 completed (credentials in .tmp/credentials/)
#
# Usage (from repo root):
#   .\deploy\publish-all-demos.ps1
#   .\deploy\generate-all-demos.ps1
#   .\deploy\generate-all-demos.ps1 -BackendUrl http://localhost:8080 -Environment dev
#
# Outputs:
#   .tmp/generated_<externalId>.docx
#   .tmp/evidence/generated-docx-manifest.json
#   .tmp/evidence/audit-records/<externalId>.json

param(
    [string]$BackendUrl = $(if ($env:BACKEND_PORT) { "http://localhost:$($env:BACKEND_PORT)" } else { 'http://localhost:8080' }),
    [string]$Environment = 'dev',
    [string]$CredentialDir = '',
    [string]$OutputDir = '',
    [string]$EvidenceDir = '',
    [switch]$SkipAuditFetch,
    [switch]$ContinueOnFailure
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$WorkspaceRoot = Split-Path -Parent $RepoRoot
if (-not $CredentialDir) { $CredentialDir = Join-Path $WorkspaceRoot '.tmp/credentials' }
if (-not $OutputDir) { $OutputDir = Join-Path $WorkspaceRoot '.tmp' }
if (-not $EvidenceDir) { $EvidenceDir = Join-Path $WorkspaceRoot '.tmp/evidence' }
. (Join-Path $RepoRoot 'demo-import-shared.ps1')

function Write-GenerateStep([string]$Message) { Write-Host "==> generate-all-demos: $Message" }

function Invoke-MgmtApi {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body = $null
    )
    Invoke-DemoApi -ApiBase "$BackendUrl/api/management/v1" -Method $Method -Path $Path -Token $Token -Body $Body
}

function Get-TemplateDetailByExternalId {
    param([string]$ExternalId, [string]$AccessToken)
    $list = Invoke-MgmtApi GET '/templates?size=200' $AccessToken
    $content = Get-DemoApiResultItems -Response $list
    return $content | Where-Object { $_.externalId -eq $ExternalId } | Select-Object -First 1
}

function Get-Sha256Hex([string]$FilePath) {
    $hash = Get-FileHash -Path $FilePath -Algorithm SHA256
    return $hash.Hash.ToLowerInvariant()
}

function Test-DocxMagicBytes([string]$FilePath) {
    $bytes = [System.IO.File]::ReadAllBytes($FilePath)
    if ($bytes.Length -lt 4) { return $false }
    return ($bytes[0] -eq 0x50 -and $bytes[1] -eq 0x4B -and $bytes[2] -eq 0x03 -and $bytes[3] -eq 0x04)
}

function Get-DocxDocumentXmlText([string]$FilePath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($FilePath)
    try {
        $entry = $zip.GetEntry('word/document.xml')
        if (-not $entry) {
            throw "DOCX missing word/document.xml: $FilePath"
        }
        $reader = New-Object System.IO.StreamReader($entry.Open())
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } finally {
        $zip.Dispose()
    }
}

function Test-DocxArtifact {
    param(
        [string]$FilePath,
        [int]$MinDocxBytes,
        [string[]]$ContentMarkers,
        [string[]]$ForbiddenPatterns
    )
    $sizeBytes = (Get-Item $FilePath).Length
    $result = [ordered]@{
        minDocxBytesPassed = ($sizeBytes -ge $MinDocxBytes)
        contentMarkersPassed = $true
        forbiddenPatternScanPassed = $true
        missingMarkers = @()
        forbiddenHits = @()
    }

    if (-not (Test-DocxMagicBytes $FilePath)) {
        throw "Not a valid DOCX (missing PK magic bytes): $FilePath"
    }

    $documentXml = Get-DocxDocumentXmlText $FilePath
    foreach ($pattern in $ForbiddenPatterns) {
        if ($documentXml.Contains($pattern)) {
            $result.forbiddenPatternScanPassed = $false
            $result.forbiddenHits += $pattern
        }
    }
    foreach ($marker in $ContentMarkers) {
        if (-not $documentXml.Contains($marker)) {
            $result.contentMarkersPassed = $false
            $result.missingMarkers += $marker
        }
    }
    return $result
}

function Invoke-RuntimeGenerateDocx {
    param(
        [string]$ExternalId,
        [object]$Credential,
        [object]$Variables,
        [string]$RequestId,
        [string]$IdempotencyKey,
        [string]$OutFile
    )
    $runtimeBase = "$BackendUrl/api/$Environment/v1"
    $uri = "$runtimeBase/templates/$ExternalId/default/generate"
    $body = @{
        output = @{ format = 'DOCX'; mode = 'SYNC_STREAM' }
        variables = $Variables
        requestId = $RequestId
        idempotencyKey = $IdempotencyKey
    } | ConvertTo-Json -Depth 100 -Compress

    $headersFile = [System.IO.Path]::GetTempFileName()
    $bodyFile = [System.IO.Path]::GetTempFileName()
    try {
        [System.IO.File]::WriteAllText($bodyFile, $body, [System.Text.UTF8Encoding]::new($false))
        $curlArgs = @(
            '-sS', '-D', $headersFile, '-o', $OutFile,
            '-X', 'POST', $uri,
            '-H', "X-Api-Credential-Id: $($Credential.externalId)",
            '-H', "X-Api-Credential-Secret: $($Credential.secret)",
            '-H', 'X-Access-Account: e2e-runtime-caller',
            '-H', 'Content-Type: application/json',
            '--data-binary', "@$bodyFile"
        )
        & curl.exe @curlArgs
        if ($LASTEXITCODE -ne 0) {
            throw "curl runtime generate failed for $ExternalId (exit $LASTEXITCODE)"
        }

        $statusLine = (Get-Content $headersFile -TotalCount 1)
        $httpStatus = 0
        if ($statusLine -match '^HTTP/\S+\s+(\d+)') {
            $httpStatus = [int]$Matches[1]
        }

        $documentId = $null
        Get-Content $headersFile | ForEach-Object {
            if ($_ -match '^(?i)document-?id:\s*(.+)$') {
                $documentId = $Matches[1].Trim()
            }
        }

        return [ordered]@{
            httpStatus = $httpStatus
            documentId = $documentId
        }
    } finally {
        if (Test-Path $headersFile) { Remove-Item $headersFile -Force }
        if (Test-Path $bodyFile) { Remove-Item $bodyFile -Force }
    }
}

function Get-RuntimeInvocationAudit {
    param(
        [string]$TemplateId,
        [string]$RequestId,
        [string]$AdminToken
    )
    $resp = Invoke-MgmtApi GET "/templates/$TemplateId/api/invocations/recent?limit=30" $AdminToken
    $items = @($resp.result)
    $matched = $items | Where-Object {
        [string]$_.requestId -eq $RequestId -and [string]$_.status -eq 'SUCCEEDED'
    } | Select-Object -First 1

    return [ordered]@{
        queryPath = "/templates/$TemplateId/api/invocations/recent"
        requestId = $RequestId
        invocationStatus = if ($matched) { [string]$matched.status } else { $null }
        requestIdMatched = [bool]$matched
        invocationId = if ($matched) { [string]$matched.invocationId } else { $null }
        runtimeAuditOutcome = 'SUCCESS'
        itemsReturned = $items.Count
        records = $items
    }
}

function Load-DemoCredential {
    param([string]$ExternalId)
    $credPath = Join-Path $CredentialDir "$ExternalId.json"
    if (-not (Test-Path $credPath)) {
        throw "Missing credential bundle for $ExternalId at $credPath (run publish-all-demos.ps1 first)"
    }
    return (Get-Content $credPath -Raw | ConvertFrom-Json)
}

if (-not (Wait-DemoBackendHealthy -BackendUrl $BackendUrl)) {
    throw "Backend not healthy at $BackendUrl/healthz"
}

$manifest = Get-DemoRuntimeGenerateManifest -DeployRoot $RepoRoot
$publishIds = Get-DemoPublishExternalIds -DeployRoot $RepoRoot
$manifestIds = @($manifest.templates | ForEach-Object { [string]$_.externalId })
$registryMismatch = Compare-Object -ReferenceObject $publishIds -DifferenceObject $manifestIds
if ($registryMismatch) {
    throw "Runtime generate manifest external IDs do not match Get-DemoPublishExternalIds"
}

$ApiBase = "$BackendUrl/api/management/v1"
$AdminToken = Get-DemoApiToken -ApiBase $ApiBase -Username '10000002' -Password 'ChangeMe123!'
$forbiddenPatterns = @($manifest.forbiddenPatterns | ForEach-Object { [string]$_ })
$runStamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddHHmmss')
$auditDir = Join-Path $EvidenceDir 'audit-records'

if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null }
if (-not (Test-Path $EvidenceDir)) { New-Item -ItemType Directory -Path $EvidenceDir -Force | Out-Null }
if (-not $SkipAuditFetch -and -not (Test-Path $auditDir)) {
    New-Item -ItemType Directory -Path $auditDir -Force | Out-Null
}

$entries = @()
$successCount = 0
$failureCount = 0

Write-GenerateStep "starting runtime generate chain ($($manifest.templates.Count) templates)"
foreach ($templateEntry in @($manifest.templates)) {
    $externalId = [string]$templateEntry.externalId
    $outFile = Join-Path $OutputDir "generated_$externalId.docx"
    $requestId = "req-p23-t14-$externalId"
    $idempotencyKey = "idem-p23-t14-$externalId-$runStamp"

    try {
        Write-GenerateStep "generating $externalId ..."
        $credential = Load-DemoCredential -ExternalId $externalId
        $variables = Resolve-DemoExecutiveVariables -TemplateEntry $templateEntry -WorkspaceRoot $WorkspaceRoot
        $generateResult = Invoke-RuntimeGenerateDocx `
            -ExternalId $externalId `
            -Credential $credential `
            -Variables $variables `
            -RequestId $requestId `
            -IdempotencyKey $idempotencyKey `
            -OutFile $outFile

        if ($generateResult.httpStatus -ne 200) {
            throw "HTTP $($generateResult.httpStatus) for $externalId"
        }
        if (-not (Test-Path $outFile)) {
            throw "DOCX output missing: $outFile"
        }

        $artifactChecks = Test-DocxArtifact `
            -FilePath $outFile `
            -MinDocxBytes ([int]$templateEntry.minDocxBytes) `
            -ContentMarkers (@($templateEntry.contentMarkers | ForEach-Object { [string]$_ })) `
            -ForbiddenPatterns $forbiddenPatterns

        if (-not $artifactChecks.minDocxBytesPassed) {
            throw "DOCX below size floor for $externalId"
        }
        if (-not $artifactChecks.contentMarkersPassed) {
            throw "DOCX missing content markers for ${externalId}: $($artifactChecks.missingMarkers -join ', ')"
        }
        if (-not $artifactChecks.forbiddenPatternScanPassed) {
            throw "DOCX contains forbidden patterns for ${externalId}: $($artifactChecks.forbiddenHits -join ', ')"
        }

        $auditSummary = $null
        if (-not $SkipAuditFetch) {
            $template = Get-TemplateDetailByExternalId -ExternalId $externalId -AccessToken $AdminToken
            if (-not $template) {
                throw "Template not found for audit fetch: $externalId"
            }
            $auditSummary = Get-RuntimeInvocationAudit `
                -TemplateId ([string]$template.id) `
                -RequestId $requestId `
                -AdminToken $AdminToken
            $auditPath = Join-Path $auditDir "$externalId.json"
            ($auditSummary | ConvertTo-Json -Depth 20) | Set-Content $auditPath -Encoding UTF8
            $auditSummary.recordsSavedTo = $auditPath
            if (-not $auditSummary.requestIdMatched) {
                throw "No SUCCEEDED invocation audit record for requestId $requestId"
            }
        }

        $sizeBytes = (Get-Item $outFile).Length
        $entry = [ordered]@{
            externalId = $externalId
            fileName = "generated_$externalId.docx"
            filePath = $outFile
            sizeBytes = $sizeBytes
            sha256 = (Get-Sha256Hex $outFile)
            httpStatus = $generateResult.httpStatus
            documentId = $generateResult.documentId
            requestId = $requestId
            idempotencyKey = $idempotencyKey
            minDocxBytes = [int]$templateEntry.minDocxBytes
            minDocxBytesPassed = $artifactChecks.minDocxBytesPassed
            contentMarkersPassed = $artifactChecks.contentMarkersPassed
            forbiddenPatternScanPassed = $artifactChecks.forbiddenPatternScanPassed
            audit = $auditSummary
            status = 'SUCCESS'
        }
        $entries += $entry
        $successCount++
        Write-GenerateStep "$externalId OK ($sizeBytes bytes, sha256=$($entry.sha256.Substring(0, 12))...)"
    } catch {
        $failureCount++
        Write-Warning "FAILED $externalId — $($_.Exception.Message)"
        $entries += [ordered]@{
            externalId = $externalId
            fileName = "generated_$externalId.docx"
            filePath = $outFile
            status = 'FAILED'
            error = $_.Exception.Message
            requestId = $requestId
        }
    }
}

$evidence = [ordered]@{
    manifestVersion = [string]$manifest.manifestVersion
    bddIds = @($manifest.bddIds | ForEach-Object { [string]$_ })
    generatedAt = (Get-Date).ToUniversalTime().ToString('o')
    backendUrl = $BackendUrl
    runtimeApiPath = "/api/$Environment/v1"
    templateCount = $manifest.templates.Count
    successCount = $successCount
    failureCount = $failureCount
    entries = $entries
}
$manifestPath = Join-Path $EvidenceDir 'generated-docx-manifest.json'
($evidence | ConvertTo-Json -Depth 20) | Set-Content $manifestPath -Encoding UTF8
Write-GenerateStep "evidence written to $manifestPath ($successCount/$($manifest.templates.Count) succeeded)"

if ($failureCount -gt 0 -and -not $ContinueOnFailure) {
    throw "generate-all-demos completed with $failureCount failure(s); see $manifestPath"
}
if ($failureCount -gt 0) {
    Write-Warning "generate-all-demos completed with $failureCount failure(s); see $manifestPath"
} else {
    Write-GenerateStep 'completed successfully'
}

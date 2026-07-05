param([string]$Action = 'inspect', [string]$ExternalId = '', [string]$OutFile = '')
$ErrorActionPreference = 'Stop'
$BASE = if ($env:BACKEND_URL) { $env:BACKEND_URL } else { 'http://localhost:8080' }
$MGMT = "$BASE/api/management/v1"
$RUNTIME = "$BASE/api/dev/v1"

function Get-Token([string]$role) {
    $map = @{ admin='10000001'; group_admin='10000002'; author='10000003' }
    $u = $map[$role]; $p = 'ChangeMe123!'
    $r = Invoke-RestMethod -Method POST -Uri "$MGMT/auth/login" -ContentType 'application/json' -Body (@{username=$u;password=$p} | ConvertTo-Json -Compress)
    return $r.result.accessToken
}

function Call([string]$Method, [string]$Path, [string]$Token, $Body, [string]$Base = $MGMT) {
    $h = @{ Authorization = "Bearer $Token" }
    if ($Body) {
        return Invoke-RestMethod -Method $Method -Uri "$Base$Path" -Headers $h -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 100 -Compress)
    }
    return Invoke-RestMethod -Method $Method -Uri "$Base$Path" -Headers $h
}

switch ($Action) {
    'inspect' {
        $tok = Get-Token group_admin
        $tpl = Call GET '/templates?size=200' $tok
        Write-Host '=== TEMPLATES ==='
        foreach ($t in $tpl.result) {
            $name = if ($t.name) { $t.name } else { '' }
            Write-Host ("{0,-32} | {1,-14} | {2,-8} | {3}" -f $t.externalId, $t.lifecycleStatus, $t.groupCode, $name)
        }
        $m = Call GET '/masters?size=200' $tok
        Write-Host '=== MASTERS ==='
        foreach ($x in $m.result) {
            $rs = if ($x.reviewState) { $x.reviewState } else { 'NONE' }
            $gc = if ($x.groupCode) { $x.groupCode } else { '?' }
            $name = if ($x.name) { $x.name } else { '' }
            Write-Host ("{0,-50} | {1,-10} | {2}" -f $name, $rs, $gc)
        }
    }
    'generate' {
        $ga = Get-Token group_admin
        $tpl = Call GET '/templates?size=200' $ga
        $target = $tpl.result | Where-Object { $_.externalId -eq $ExternalId } | Select-Object -First 1
        if (-not $target) { throw "Template $ExternalId not found" }
        $tid = $target.id
        Write-Host "template: $ExternalId id=$tid status=$($target.lifecycleStatus)"
        $cred = Call POST "/templates/$tid/api/credentials" $ga
        Write-Host ("credential externalId={0}" -f $cred.result.externalId)
        $headers = @{
            'X-Api-Credential-Id' = $cred.result.externalId
            'X-Api-Credential-Secret' = $cred.result.secret
            'X-Access-Account' = 'demo-real-letter-caller'
        }
        $vars = @{}
        switch ($ExternalId) {
            'DEMO-CREDIT-LIMIT-CONFIRM' { $vars = @{ customerName='Northgate Manufacturing Ltd.'; facilityReference='CORP-CL-2026-88421'; creditLimit='GBP 12,500,000.00'; reviewDate='2026-06-30'; includeOverdraft=$true; overdraftLimit='GBP 1,000,000.00' } }
            'DEMO-MORTGAGE-APPROVAL' { $vars = @{ borrowerName='Mr Oliver Hartley & Mrs Sarah Hartley'; propertyAddress='14 Willow Close, Bristol BS8 4QT'; loanAmount='GBP 385,000.00'; interestRate='4.29% fixed for 5 years'; termYears=25; approvalDate='2026-06-20' } }
            default { $vars = @{ customerName='Executive Sample Customer' } }
        }
        $body = @{
            output = @{ format='DOCX'; mode='SYNC_STREAM' }
            variables = $vars
            requestId = "req-demo-$(Get-Date -Format 'yyyyMMddHHmmss')"
            idempotencyKey = "idem-demo-$(Get-Date -Format 'yyyyMMddHHmmss')"
        }
        $bodyJson = $body | ConvertTo-Json -Depth 100 -Compress
        $uri = "$RUNTIME/templates/$ExternalId/default/generate"
        Write-Host "POST $uri"
        try {
            $resp = Invoke-WebRequest -Method POST -Uri $uri -Headers $headers -ContentType 'application/json' -Body $bodyJson -UseBasicParsing
            Write-Host "GENERATE status=$($resp.StatusCode)"
            $docId = $resp.Headers['documentId']
            Write-Host "documentId=$docId"
            if ($OutFile -and $resp.Content) {
                [System.IO.File]::WriteAllBytes((Resolve-Path -LiteralPath '.').Path + "\$OutFile", $resp.Content)
                Write-Host "saved $OutFile ($($resp.Content.Length) bytes)"
            }
        } catch {
            $r = $_.Exception.Response
            if ($r) {
                $sr = New-Object System.IO.StreamReader($r.GetResponseStream())
                $body2 = $sr.ReadToEnd()
                Write-Host "GENERATE FAILED status=$([int]$r.StatusCode) body=$body2"
            } else {
                Write-Host "GENERATE FAILED: $($_.Exception.Message)"
            }
        }
    }
    default { Write-Host "Unknown action: $Action" }
}

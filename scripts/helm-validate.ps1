#Requires -Version 5.1
<#
.SYNOPSIS
  CI-friendly Helm chart validation for deploy/helm/docgen (P15-T02–T09 / ADR-0030).

.DESCRIPTION
  Runs helm lint and helm template for the docgen chart using default and per-environment
  values files. Validates rendered manifests for security context, secrets wiring,
  ingress/TLS, HPA, NetworkPolicy, health probes, and (prod) blue-green selectors.
  Runs kubeconform on rendered manifests (P15-T09). Exits non-zero on failure.
  Does not require a Kubernetes cluster.

  Prerequisites: helm CLI v3+ on PATH, or Docker for alpine/helm fallback.
  kubeconform: native binary on PATH, or Docker (ghcr.io/yannh/kubeconform).
#>
param(
    [string]$ChartPath = (Join-Path $PSScriptRoot "..\deploy\helm\docgen"),
    [string]$OutputDir = (Join-Path $env:TEMP "docgen-helm-render"),
    [string]$HelmDockerImage = "alpine/helm:3.14.4",
    [string]$KubeconformDockerImage = "yannh/kubeconform:v0.6.7",
    [string]$KubernetesVersion = "1.29.0",
    [switch]$SkipKubeconform
)

$ErrorActionPreference = "Stop"

function Resolve-HelmCommand {
    $wingetHelm = Get-ChildItem (Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages") -Recurse -Filter "helm.exe" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match "Helm\.Helm" } |
        Select-Object -First 1

    $candidates = @(
        $wingetHelm,
        (Get-Command helm -ErrorAction SilentlyContinue),
        (Get-Command "$env:LOCALAPPDATA\Microsoft\WinGet\Links\helm.exe" -ErrorAction SilentlyContinue),
        (Get-Command "$env:ProgramFiles\Helm\helm.exe" -ErrorAction SilentlyContinue)
    ) | Where-Object { $_ -ne $null }

    if ($candidates.Count -gt 0) {
        $resolved = $candidates[0]
        $command = if ($resolved -is [System.Management.Automation.ApplicationInfo] -or $resolved -is [System.Management.Automation.CommandInfo]) {
            $resolved.Source
        } else {
            $resolved.FullName
        }
        return @{ Mode = "native"; Command = $command }
    }

    if (Get-Command docker -ErrorAction SilentlyContinue) {
        return @{ Mode = "docker"; Command = "docker" }
    }

    Write-Error "Neither helm CLI nor Docker found. Install Helm 3+ or Docker to run chart validation."
}

function Invoke-Helm {
    param(
        [hashtable]$Helm,
        [string]$Chart,
        [string[]]$HelmArgs
    )

    if ($Helm.Mode -eq "native") {
        & $Helm.Command @HelmArgs $Chart 2>&1 | Out-Host
        return $LASTEXITCODE
    }

    $chartMount = (Resolve-Path $Chart).Path -replace '\\', '/'
    $dockerArgs = @(
        "run", "--rm",
        "-v", "${chartMount}:/chart:ro",
        $HelmDockerImage
    ) + $HelmArgs + @("/chart")
    & docker @dockerArgs
    return $LASTEXITCODE
}

function Invoke-HelmTemplate {
    param(
        [hashtable]$Helm,
        [string]$Chart,
        [string]$ReleaseName,
        [string[]]$ExtraArgs,
        [string]$OutFile
    )

    $templateArgs = @("template", $ReleaseName) + $ExtraArgs + @("--namespace", "docgen")

    if ($Helm.Mode -eq "native") {
        $previousErrorAction = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        & $Helm.Command @templateArgs $Chart 2>&1 | Out-File -FilePath $OutFile -Encoding utf8
        $exitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorAction
        return $exitCode
    }
    else {
        $chartMount = $Chart -replace '\\', '/'
        $dockerArgs = @(
            "run", "--rm",
            "-v", "${chartMount}:/chart:ro",
            $HelmDockerImage,
            "template"
        ) + $templateArgs[1..($templateArgs.Length - 1)] + @("/chart")
        & docker @dockerArgs | Out-File -FilePath $OutFile -Encoding utf8
    }

    return $LASTEXITCODE
}

function Get-ConfigMapDataSection {
    param([string]$Content)

    if ($Content -notmatch '(?ms)# Source: docgen/templates/configmap\.yaml\r?\n.*?^data:\r?\n(.*?)(?=^---|\z)') {
        throw "render content missing application ConfigMap (templates/configmap.yaml)"
    }
    return $Matches[1]
}

function Assert-T03ConfigSecrets {
    param(
        [string]$ProfileName,
        [string]$Content,
        [string]$ExpectedSecretName
    )

    if ($Content -match 'kind:\s*StatefulSet') {
        throw "render-$ProfileName.yaml contains StatefulSet — external managed services only (P15-T03c)"
    }

    $configData = Get-ConfigMapDataSection -Content $Content

    foreach ($requiredKey in @(
            'POSTGRES_HOST', 'POSTGRES_PORT', 'POSTGRES_DB',
            'REDIS_HOST', 'REDIS_PORT',
            'KAFKA_BOOTSTRAP_SERVERS', 'MINIO_ENDPOINT'
        )) {
        if ($configData -notmatch "(?m)^\s+${requiredKey}:") {
            throw "render-$ProfileName.yaml ConfigMap missing external endpoint key $requiredKey (P15-T03c)"
        }
    }

    foreach ($forbiddenKey in @(
            'POSTGRES_USER', 'POSTGRES_PASSWORD',
            'MINIO_ROOT_USER', 'MINIO_ROOT_PASSWORD', 'JWT_SECRET'
        )) {
        if ($configData -match "(?m)^\s+${forbiddenKey}:") {
            throw "render-$ProfileName.yaml ConfigMap contains credential key $forbiddenKey (P15-T03a — secrets belong in Secret)"
        }
    }

    if ($Content -match '(?ms)kind:\s*Secret\r?\n.*?^stringData:') {
        throw "render-$ProfileName.yaml renders chart-managed Secret stringData — use secrets.create=false + existingSecretName (P15-T03b)"
    }

    if ($Content -notmatch "name:\s*$([regex]::Escape($ExpectedSecretName))") {
        throw "render-$ProfileName.yaml missing secretRef to existingSecretName '$ExpectedSecretName' (P15-T03b)"
    }
}

function Assert-T04IngressTls {
    param(
        [string]$ProfileName,
        [string]$Content,
        [bool]$ExpectIngress = $false,
        [bool]$ExpectCertificate = $false
    )

    # T04a: ClusterIP Services on port 8080 with K8s DNS annotations
    if ($Content -notmatch '(?ms)kind:\s*Service\r?\nmetadata:\r?\n\s+name:\s*(?<BackendSvc>[^\r\n]+-backend)\r?\n.*?docgen\.io/cluster-dns:\s*"(?<BackendDns>[^"]+\.svc\.cluster\.local)"\r?\n.*?type:\s*ClusterIP\r?\n.*?port:\s*8080') {
        throw "render-$ProfileName.yaml missing backend ClusterIP Service on port 8080 with docgen.io/cluster-dns (P15-T04a)"
    }
    $backendSvc = $Matches['BackendSvc']
    $backendDns = $Matches['BackendDns']

    if ($Content -notmatch '(?ms)kind:\s*Service\r?\nmetadata:\r?\n\s+name:\s*(?<FrontendSvc>[^\r\n]+-frontend)\r?\n.*?docgen\.io/cluster-dns:\s*"(?<FrontendDns>[^"]+\.svc\.cluster\.local)"\r?\n.*?type:\s*ClusterIP\r?\n.*?port:\s*8080') {
        throw "render-$ProfileName.yaml missing frontend ClusterIP Service on port 8080 with docgen.io/cluster-dns (P15-T04a)"
    }
    $frontendSvc = $Matches['FrontendSvc']
    $frontendDns = $Matches['FrontendDns']

    if ($backendDns -notmatch [regex]::Escape($backendSvc)) {
        throw "render-$ProfileName.yaml backend Service DNS '$backendDns' does not include service name '$backendSvc' (P15-T04a)"
    }
    if ($frontendDns -notmatch [regex]::Escape($frontendSvc)) {
        throw "render-$ProfileName.yaml frontend Service DNS '$frontendDns' does not include service name '$frontendSvc' (P15-T04a)"
    }

    if (-not $ExpectIngress) {
        if ($Content -match 'kind:\s*Ingress') {
            throw "render-$ProfileName.yaml renders Ingress but ingress.enabled is false for this profile (P15-T04b)"
        }
        return
    }

    # T04b: NGINX Ingress class, host/path routing (API + SPA), TLS secret reference
    if ($Content -notmatch '(?ms)kind:\s*Ingress\r?\n.*?ingressClassName:\s*nginx') {
        throw "render-$ProfileName.yaml missing Ingress with ingressClassName: nginx (P15-T04b)"
    }
    if ($Content -notmatch 'cert-manager\.io/cluster-issuer') {
        throw "render-$ProfileName.yaml missing cert-manager.io/cluster-issuer annotation (P15-T04b)"
    }
    if ($Content -notmatch 'nginx\.ingress\.kubernetes\.io/ssl-protocols:\s*"?TLSv1\.2 TLSv1\.3"?') {
        throw "render-$ProfileName.yaml missing TLS 1.2+ ssl-protocols annotation (P15-T04c / ADR-0030)"
    }
    if ($Content -notmatch "name:\s*$([regex]::Escape($backendSvc))") {
        throw "render-$ProfileName.yaml Ingress does not reference backend Service '$backendSvc' (P15-T04b)"
    }
    if ($Content -notmatch "name:\s*$([regex]::Escape($frontendSvc))") {
        throw "render-$ProfileName.yaml Ingress does not reference frontend Service '$frontendSvc' (P15-T04b)"
    }
    if ($Content -notmatch '(?ms)kind:\s*Ingress\r?\n.*?path:\s*/api\r?\n.*?pathType:\s*Prefix\r?\n.*?name:\s*' + [regex]::Escape($backendSvc)) {
        throw "render-$ProfileName.yaml Ingress missing /api Prefix path to backend Service (P15-T04b)"
    }
    if ($Content -notmatch '(?ms)kind:\s*Ingress\r?\n.*?path:\s*/\r?\n.*?pathType:\s*Prefix\r?\n.*?name:\s*' + [regex]::Escape($frontendSvc)) {
        throw "render-$ProfileName.yaml Ingress missing / Prefix path to frontend Service (P15-T04b)"
    }
    if ($Content -notmatch '(?ms)kind:\s*Ingress\r?\n.*?tls:\r?\n\s+- hosts:\r?\n\s+- "[^"]+"\r?\n\s+secretName:\s*\S+') {
        throw "render-$ProfileName.yaml Ingress missing TLS hosts + secretName (P15-T04b)"
    }

    if (-not $ExpectCertificate) {
        return
    }

    # T04c: cert-manager Certificate with issuerRef aligned to Ingress TLS secret
    if ($Content -notmatch '(?ms)kind:\s*Certificate\r?\n.*?issuerRef:\r?\n\s+name:\s*\S+\r?\n\s+kind:\s*(ClusterIssuer|Issuer)') {
        throw "render-$ProfileName.yaml missing cert-manager Certificate with issuerRef (P15-T04c)"
    }
    if ($Content -notmatch '(?ms)kind:\s*Certificate\r?\n.*?dnsNames:\r?\n\s+- "[^"]+"') {
        throw "render-$ProfileName.yaml Certificate missing dnsNames (P15-T04c)"
    }
}

function Get-HpaSection {
    param(
        [string]$Content,
        [string]$TemplateSource
    )

    $pattern = '(?ms)# Source: docgen/templates/' + [regex]::Escape($TemplateSource) + '\r?\n(.*?)(?=^---|\z)'
    if ($Content -notmatch $pattern) {
        return $null
    }
    return $Matches[1]
}

function Assert-T05Hpa {
    param(
        [string]$ProfileName,
        [string]$Content,
        [bool]$ExpectHpa = $false,
        [bool]$ExpectBlueGreen = $false,
        [bool]$ExpectBackendCustomMetric = $false
    )

    if (-not $ExpectHpa) {
        if ($Content -match 'kind:\s*HorizontalPodAutoscaler') {
            throw "render-$ProfileName.yaml renders HorizontalPodAutoscaler but autoscaling is disabled for this profile (P15-T05)"
        }
        return
    }

    foreach ($component in @(
            @{ Name = "backend"; Template = "backend-hpa.yaml"; ExpectCustomMetric = $ExpectBackendCustomMetric },
            @{ Name = "frontend"; Template = "frontend-hpa.yaml"; ExpectCustomMetric = $false }
        )) {
        $section = Get-HpaSection -Content $Content -TemplateSource $component.Template
        if ($null -eq $section) {
            throw "render-$ProfileName.yaml missing $($component.Name) HPA (templates/$($component.Template)) (P15-T05a)"
        }

        if ($section -notmatch 'apiVersion:\s*autoscaling/v2') {
            throw "render-$ProfileName.yaml $($component.Name) HPA must use autoscaling/v2 (P15-T05a)"
        }
        if ($section -notmatch 'kind:\s*HorizontalPodAutoscaler') {
            throw "render-$ProfileName.yaml $($component.Name) HPA missing kind HorizontalPodAutoscaler (P15-T05a)"
        }
        if ($section -notmatch 'scaleTargetRef:\r?\n\s+apiVersion:\s*apps/v1\r?\n\s+kind:\s*Deployment\r?\n\s+name:\s*(?<TargetName>[^\r\n]+)') {
            throw "render-$ProfileName.yaml $($component.Name) HPA scaleTargetRef must target apps/v1 Deployment (P15-T05a)"
        }
        $targetName = $Matches['TargetName'].Trim()

        if ($ExpectBlueGreen) {
            if ($targetName -notmatch '-blue$|-green$') {
                throw "render-$ProfileName.yaml $($component.Name) HPA scaleTargetRef '$targetName' must target blue-green active color Deployment (P15-T05a)"
            }
            if ($targetName -notmatch '-blue$') {
                throw "render-$ProfileName.yaml $($component.Name) HPA scaleTargetRef '$targetName' must match blueGreen.activeColor=blue (P15-T05a)"
            }
        }
        elseif ($targetName -match '-blue$|-green$') {
            throw "render-$ProfileName.yaml $($component.Name) HPA scaleTargetRef '$targetName' must not use blue-green suffix when blueGreen.enabled=false (P15-T05a)"
        }

        if ($section -notmatch 'minReplicas:\s*(?<Min>\d+)') {
            throw "render-$ProfileName.yaml $($component.Name) HPA missing minReplicas (P15-T05a)"
        }
        $minReplicas = [int]$Matches['Min']
        if ($section -notmatch 'maxReplicas:\s*(?<Max>\d+)') {
            throw "render-$ProfileName.yaml $($component.Name) HPA missing maxReplicas (P15-T05a)"
        }
        $maxReplicas = [int]$Matches['Max']
        if ($minReplicas -gt $maxReplicas) {
            throw "render-$ProfileName.yaml $($component.Name) HPA minReplicas ($minReplicas) exceeds maxReplicas ($maxReplicas) (P15-T05a)"
        }

        if ($section -notmatch '(?ms)- type: Resource\r?\n\s+resource:\r?\n\s+name: cpu\r?\n\s+target:\r?\n\s+type: Utilization\r?\n\s+averageUtilization:\s*\d+') {
            throw "render-$ProfileName.yaml $($component.Name) HPA missing CPU utilization Resource metric (P15-T05a)"
        }
        if ($section -notmatch '(?ms)- type: Resource\r?\n\s+resource:\r?\n\s+name: memory\r?\n\s+target:\r?\n\s+type: Utilization\r?\n\s+averageUtilization:\s*\d+') {
            throw "render-$ProfileName.yaml $($component.Name) HPA missing memory utilization Resource metric (P15-T05a)"
        }

        if ($component.ExpectCustomMetric) {
            if ($section -notmatch '(?ms)- type: Pods\r?\n\s+pods:\r?\n\s+metric:\r?\n\s+name:\s*docgen_http_requests_per_second\r?\n\s+target:\r?\n\s+type:\s*AverageValue\r?\n\s+averageValue:\s*"?\d+"?') {
                throw "render-$ProfileName.yaml $($component.Name) HPA missing Pods custom metric docgen_http_requests_per_second (P15-T05b)"
            }
        }
        elseif ($section -match 'docgen_http_requests_per_second') {
            throw "render-$ProfileName.yaml $($component.Name) HPA renders custom metric but autoscaling.$($component.Name).customMetric.enabled is false (P15-T05b)"
        }
    }
}

function Get-DeploymentSections {
    param([string]$Content)

    return [regex]::Matches(
        $Content,
        '(?ms)kind: Deployment\r?\nmetadata:\r?\n(?:[^\r\n]+\r?\n)*?\s+name: (?<Name>[^\r\n]+)(?<Body>.*?)(?=^---|\z)'
    )
}

function Assert-ContainerProbeBlock {
    param(
        [string]$ProfileName,
        [string]$DeploymentName,
        [string]$Component,
        [string]$ProbeKind,
        [string]$ExpectedPath,
        [string]$Section,
        [int]$MinInitialDelaySeconds,
        [int]$MinFailureThreshold
    )

    $pattern = '(?ms)' + [regex]::Escape($ProbeKind) + 'Probe:\r?\n\s+httpGet:\r?\n\s+path:\s*' +
        [regex]::Escape($ExpectedPath) + '\r?\n\s+port:\s*http\r?\n(?:\s+\w+:\s*[^\r\n]+\r?\n)*?\s+initialDelaySeconds:\s*(?<Delay>\d+)\r?\n(?:\s+\w+:\s*[^\r\n]+\r?\n)*?\s+failureThreshold:\s*(?<Threshold>\d+)'

    if ($Section -notmatch $pattern) {
        throw "render-$ProfileName.yaml Deployment '$DeploymentName' ($Component) missing $ProbeKind probe httpGet $ExpectedPath on port http with timing (P15-T07)"
    }

    $delay = [int]$Matches['Delay']
    $threshold = [int]$Matches['Threshold']
    if ($delay -lt $MinInitialDelaySeconds) {
        throw "render-$ProfileName.yaml Deployment '$DeploymentName' $ProbeKind initialDelaySeconds ($delay) below minimum $MinInitialDelaySeconds (P15-T07)"
    }
    if ($threshold -lt $MinFailureThreshold) {
        throw "render-$ProfileName.yaml Deployment '$DeploymentName' $ProbeKind failureThreshold ($threshold) below minimum $MinFailureThreshold (P15-T07)"
    }
}

function Assert-T07Probes {
    param(
        [string]$ProfileName,
        [string]$Content
    )

    $deployments = Get-DeploymentSections -Content $Content
    if ($deployments.Count -eq 0) {
        throw "render-$ProfileName.yaml missing Deployment resources (P15-T07)"
    }

    $backendCount = 0
    $frontendCount = 0

    foreach ($match in $deployments) {
        $name = $match.Groups['Name'].Value.Trim()
        $section = $match.Groups[0].Value

        $isBackend = $section -match 'app\.kubernetes\.io/component:\s*backend' -or $name -match '-backend'
        $isFrontend = $section -match 'app\.kubernetes\.io/component:\s*frontend' -or $name -match '-frontend'

        if (-not $isBackend -and -not $isFrontend) {
            continue
        }

        if ($section -notmatch '(?ms)ports:\r?\n\s+- name: http\r?\n\s+containerPort:\s*8080') {
            throw "render-$ProfileName.yaml Deployment '$name' missing named port http on containerPort 8080 (P15-T07)"
        }

        if ($isBackend) {
            $backendCount++
            Assert-ContainerProbeBlock -ProfileName $ProfileName -DeploymentName $name -Component "backend" `
                -ProbeKind "liveness" -ExpectedPath "/healthz" -Section $section `
                -MinInitialDelaySeconds 15 -MinFailureThreshold 3
            Assert-ContainerProbeBlock -ProfileName $ProfileName -DeploymentName $name -Component "backend" `
                -ProbeKind "readiness" -ExpectedPath "/readyz" -Section $section `
                -MinInitialDelaySeconds 5 -MinFailureThreshold 3
        }

        if ($isFrontend) {
            $frontendCount++
            Assert-ContainerProbeBlock -ProfileName $ProfileName -DeploymentName $name -Component "frontend" `
                -ProbeKind "liveness" -ExpectedPath "/healthz" -Section $section `
                -MinInitialDelaySeconds 3 -MinFailureThreshold 3
            Assert-ContainerProbeBlock -ProfileName $ProfileName -DeploymentName $name -Component "frontend" `
                -ProbeKind "readiness" -ExpectedPath "/readyz" -Section $section `
                -MinInitialDelaySeconds 3 -MinFailureThreshold 3
        }
    }

    if ($backendCount -eq 0) {
        throw "render-$ProfileName.yaml missing backend Deployment with health probes (P15-T07a)"
    }
    if ($frontendCount -eq 0) {
        throw "render-$ProfileName.yaml missing frontend Deployment with health probes (P15-T07b)"
    }

    if ($Content -notmatch '(?ms)kind: ConfigMap\r?\nmetadata:\r?\n(?:[^\r\n]+\r?\n)*?\s+name: [^\r\n]+-frontend-nginx[^\r\n]*\r?\n.*?location /healthz') {
        throw "render-$ProfileName.yaml frontend NGINX ConfigMap missing location /healthz (P15-T07b)"
    }
    if ($Content -notmatch '(?ms)kind: ConfigMap\r?\nmetadata:\r?\n(?:[^\r\n]+\r?\n)*?\s+name: [^\r\n]+-frontend-nginx[^\r\n]*\r?\n.*?location /readyz') {
        throw "render-$ProfileName.yaml frontend NGINX ConfigMap missing location /readyz (P15-T07b)"
    }
}

function Assert-T06NetworkPolicy {
    param(
        [string]$ProfileName,
        [string]$Content,
        [bool]$ExpectIngress = $false,
        [bool]$ExpectMetrics = $true
    )

    $npMatches = [regex]::Matches(
        $Content,
        '(?ms)apiVersion: networking\.k8s\.io/v1\r?\nkind: NetworkPolicy\r?\nmetadata:\r?\n\s+name: (?<Name>[^\r\n]+)(?<Body>.*?)(?=^---|\z)'
    )
    if ($npMatches.Count -eq 0) {
        throw "render-$ProfileName.yaml missing NetworkPolicy resources (P15-T06)"
    }

    $defaultDeny = $npMatches | Where-Object { $_.Groups['Name'].Value -match '-default-deny$' } | Select-Object -First 1
    if ($null -eq $defaultDeny) {
        throw "render-$ProfileName.yaml missing default-deny NetworkPolicy (P15-T06a)"
    }

    $defaultDenyText = $defaultDeny.Groups[0].Value
    if ($defaultDenyText -notmatch 'podSelector:\s*\{\}') {
        throw "render-$ProfileName.yaml default-deny NetworkPolicy must use podSelector: {} (P15-T06a)"
    }
    if ($defaultDenyText -notmatch '(?ms)policyTypes:\s*\r?\n\s+- Ingress\r?\n\s+- Egress') {
        throw "render-$ProfileName.yaml default-deny NetworkPolicy must deny both Ingress and Egress (P15-T06a)"
    }

    $allowPolicies = @($npMatches | Where-Object { $_.Groups['Name'].Value -notmatch '-default-deny$' })
    $requiredAllowSuffixes = @(
        '-allow-ingress-controller',
        '-allow-frontend-to-backend',
        '-allow-backend-external-egress',
        '-allow-dns'
    )
    if ($ExpectIngress) {
        $requiredAllowSuffixes += '-allow-ingress-to-backend'
    }
    if ($ExpectMetrics) {
        $requiredAllowSuffixes += '-allow-metrics'
    }

    $expectedAllowCount = $requiredAllowSuffixes.Count
    if ($allowPolicies.Count -ne $expectedAllowCount) {
        throw "render-$ProfileName.yaml expected $expectedAllowCount allow NetworkPolicies, found $($allowPolicies.Count) (P15-T06b)"
    }

    foreach ($suffix in $requiredAllowSuffixes) {
        $found = $allowPolicies | Where-Object { $_.Groups['Name'].Value -match ([regex]::Escape($suffix) + '$') }
        if (-not $found) {
            throw "render-$ProfileName.yaml missing allow NetworkPolicy *$suffix (P15-T06b)"
        }
    }

    if ($ExpectIngress) {
        $backendIngress = $allowPolicies | Where-Object { $_.Groups['Name'].Value -match '-allow-ingress-to-backend$' } | Select-Object -First 1
        if ($backendIngress.Groups['Body'].Value -notmatch 'port:\s*8080') {
            throw "render-$ProfileName.yaml allow-ingress-to-backend must restrict to backend port 8080 (P15-T06b)"
        }
    }

    $externalEgress = $allowPolicies | Where-Object { $_.Groups['Name'].Value -match '-allow-backend-external-egress$' } | Select-Object -First 1
    if ($null -eq $externalEgress) {
        throw "render-$ProfileName.yaml missing allow-backend-external-egress NetworkPolicy (P15-T06b)"
    }
    $egressText = $externalEgress.Groups['Body'].Value
    foreach ($requiredPort in @(5432, 6379, 9092, 443)) {
        if ($egressText -notmatch "port:\s*$requiredPort") {
            throw "render-$ProfileName.yaml backend external egress missing TCP port $requiredPort (P15-T06b)"
        }
    }

    $dnsPolicy = $allowPolicies | Where-Object { $_.Groups['Name'].Value -match '-allow-dns$' } | Select-Object -First 1
    if ($dnsPolicy.Groups['Body'].Value -notmatch 'kubernetes\.io/metadata\.name:\s*kube-system') {
        throw "render-$ProfileName.yaml allow-dns must target kube-system namespace (P15-T06b)"
    }
    if ($dnsPolicy.Groups['Body'].Value -notmatch '(?ms)port:\s*53') {
        throw "render-$ProfileName.yaml allow-dns must permit port 53 (P15-T06b)"
    }

    $ipBlockEgressRules = [regex]::Matches(
        $Content,
        '(?ms)- to:\r?\n\s+- ipBlock:\r?\n\s+cidr:\s*(?<Cidr>[^\r\n]+)(?<Tail>.*?)(?=\r?\n\s+- to:|\r?\n\s+{{-|\r?\n# |\z)'
    )
    foreach ($rule in $ipBlockEgressRules) {
        $cidr = $rule.Groups['Cidr'].Value.Trim()
        $tail = $rule.Groups['Tail'].Value
        if ($cidr -eq '0.0.0.0/0' -and $tail -notmatch '(?ms)ports:\s*\r?\n\s+-') {
            throw "render-$ProfileName.yaml overly permissive egress: 0.0.0.0/0 without port restrictions (P15-T06b)"
        }
    }
}

function Assert-T08BlueGreen {
    param(
        [string]$ProfileName,
        [string]$Content,
        [bool]$ExpectBlueGreen = $false,
        [string]$ExpectedActiveColor = "blue"
    )

    if (-not $ExpectBlueGreen) {
        if ($Content -match 'docgen\.io/deployment-color:') {
            throw "render-$ProfileName.yaml renders blue-green color labels but blueGreen.enabled is false (P15-T08a)"
        }
        if ($Content -match 'docgen\.io/traffic-role:\s*preview') {
            throw "render-$ProfileName.yaml renders preview Service but blueGreen.enabled is false (P15-T08a)"
        }
        if ($Content -match '# Source: docgen/templates/(backend|frontend)-color-deployments\.yaml') {
            throw "render-$ProfileName.yaml renders color Deployments but blueGreen.enabled is false (P15-T08a)"
        }
        return
    }

    $inactiveColor = if ($ExpectedActiveColor -eq "blue") { "green" } else { "blue" }

    if ($Content -match '# Source: docgen/templates/backend-deployment\.yaml\r?\n') {
        throw "render-$ProfileName.yaml renders single backend Deployment — use color Deployments when blueGreen.enabled (P15-T08a)"
    }
    if ($Content -match '# Source: docgen/templates/frontend-deployment\.yaml\r?\n') {
        throw "render-$ProfileName.yaml renders single frontend Deployment — use color Deployments when blueGreen.enabled (P15-T08a)"
    }
    if ($Content -notmatch '# Source: docgen/templates/backend-color-deployments\.yaml') {
        throw "render-$ProfileName.yaml missing backend color Deployments template (P15-T08a)"
    }
    if ($Content -notmatch '# Source: docgen/templates/frontend-color-deployments\.yaml') {
        throw "render-$ProfileName.yaml missing frontend color Deployments template (P15-T08a)"
    }

    $deployments = Get-DeploymentSections -Content $Content
    $backendColors = @()
    $frontendColors = @()

    foreach ($match in $deployments) {
        $name = $match.Groups['Name'].Value.Trim()
        $section = $match.Groups[0].Value

        if ($section -match 'app\.kubernetes\.io/component:\s*backend' -or $name -match '-backend') {
            if ($name -match '-blue$') { $backendColors += 'blue' }
            elseif ($name -match '-green$') { $backendColors += 'green' }
        }
        if ($section -match 'app\.kubernetes\.io/component:\s*frontend' -or $name -match '-frontend') {
            if ($name -match '-blue$') { $frontendColors += 'blue' }
            elseif ($name -match '-green$') { $frontendColors += 'green' }
        }
    }

    foreach ($component in @(
            @{ Name = "backend"; Colors = $backendColors },
            @{ Name = "frontend"; Colors = $frontendColors }
        )) {
        foreach ($requiredColor in @("blue", "green")) {
            if ($component.Colors -notcontains $requiredColor) {
                throw "render-$ProfileName.yaml missing $($component.Name)-$requiredColor Deployment (P15-T08a)"
            }
        }
    }

    foreach ($svcSuffix in @("backend", "frontend")) {
        $templateSource = "${svcSuffix}-service.yaml"
        $sectionPattern = '(?ms)# Source: docgen/templates/' + [regex]::Escape($templateSource) +
            '\r?\n.*?kind:\s*Service\r?\n.*?spec:\r?\n\s+type:\s*ClusterIP\r?\n\s+selector:(?<Selector>.*?)\r?\n\s+ports:'
        if ($Content -notmatch $sectionPattern) {
            throw "render-$ProfileName.yaml missing main $svcSuffix ClusterIP Service from templates/$templateSource (P15-T08a)"
        }
        $selector = $Matches['Selector']
        if ($selector -notmatch ('docgen\.io/deployment-color:\s*"' + [regex]::Escape($ExpectedActiveColor) + '"')) {
            throw "render-$ProfileName.yaml main $svcSuffix Service selector must route to activeColor=$ExpectedActiveColor (P15-T08a)"
        }
    }

    if ($Content -notmatch 'docgen\.io/traffic-role:\s*preview') {
        throw "render-$ProfileName.yaml missing preview Services (blueGreen.previewService.enabled) (P15-T08a)"
    }
    if ($Content -notmatch '# Source: docgen/templates/bluegreen-preview-services\.yaml') {
        throw "render-$ProfileName.yaml missing bluegreen-preview-services template (P15-T08a)"
    }

    $previewServices = [regex]::Matches(
        $Content,
        '(?ms)kind:\s*Service\r?\nmetadata:\r?\n(?:[^\r\n]+\r?\n)*?\s+name:\s*(?<Name>[^\r\n]+-preview)\r?\n.*?docgen\.io/traffic-role:\s*preview.*?spec:\r?\n\s+type:\s*ClusterIP\r?\n\s+selector:(?<Selector>.*?)\r?\n\s+ports:'
    )
    if ($previewServices.Count -lt 2) {
        throw "render-$ProfileName.yaml expected backend + frontend preview Services (P15-T08a)"
    }
    foreach ($preview in $previewServices) {
        $previewSelector = $preview.Groups['Selector'].Value
        if ($previewSelector -notmatch ('docgen\.io/deployment-color:\s*"' + [regex]::Escape($inactiveColor) + '"')) {
            $previewName = $preview.Groups['Name'].Value.Trim()
            throw "render-$ProfileName.yaml preview Service '$previewName' must target inactive color $inactiveColor (P15-T08a)"
        }
    }

    foreach ($component in @("backend", "frontend")) {
        $hpaSection = Get-HpaSection -Content $Content -TemplateSource "${component}-hpa.yaml"
        if ($null -eq $hpaSection) {
            throw "render-$ProfileName.yaml missing $component HPA for blue-green prod profile (P15-T08a)"
        }
        if ($hpaSection -notmatch ('scaleTargetRef:\r?\n\s+apiVersion:\s*apps/v1\r?\n\s+kind:\s*Deployment\r?\n\s+name:\s*[^\r\n]+-' + [regex]::Escape($ExpectedActiveColor) + '\b')) {
            throw "render-$ProfileName.yaml $component HPA scaleTargetRef must target active color $ExpectedActiveColor (P15-T08a)"
        }
    }
}

function Assert-RenderContent {
    param(
        [string]$ProfileName,
        [string]$Content,
        [string]$ExpectedSecretName,
        [bool]$ExpectIngress = $false,
        [bool]$ExpectHpa = $false,
        [bool]$ExpectBlueGreen = $false,
        [bool]$ExpectBackendCustomMetric = $false,
        [bool]$ExpectMetrics = $true
    )

    if ($Content -notmatch "readOnlyRootFilesystem:\s*true") {
        throw "render-$ProfileName.yaml missing readOnlyRootFilesystem: true"
    }
    if ($Content -notmatch "resources:") {
        throw "render-$ProfileName.yaml missing container resources"
    }
    if ($Content -notmatch "kind: Deployment") {
        throw "render-$ProfileName.yaml missing Deployment"
    }
    if ($Content -notmatch "secretRef:") {
        throw "render-$ProfileName.yaml missing secretRef for external credentials"
    }
    if ($Content -notmatch "configMapRef:") {
        throw "render-$ProfileName.yaml missing configMapRef for external service endpoints"
    }
    Assert-T07Probes -ProfileName $ProfileName -Content $Content

    Assert-T06NetworkPolicy -ProfileName $ProfileName -Content $Content -ExpectIngress $ExpectIngress -ExpectMetrics $ExpectMetrics

    if ($ExpectIngress) {
        if ($Content -notmatch "kind:\s*Ingress") {
            throw "render-$ProfileName.yaml missing Ingress"
        }
        if ($Content -notmatch "kind:\s*Certificate") {
            throw "render-$ProfileName.yaml missing cert-manager Certificate"
        }
    }

    Assert-T04IngressTls -ProfileName $ProfileName -Content $Content -ExpectIngress $ExpectIngress -ExpectCertificate $ExpectIngress

    Assert-T05Hpa -ProfileName $ProfileName -Content $Content -ExpectHpa $ExpectHpa -ExpectBlueGreen $ExpectBlueGreen -ExpectBackendCustomMetric $ExpectBackendCustomMetric

    Assert-T08BlueGreen -ProfileName $ProfileName -Content $Content -ExpectBlueGreen $ExpectBlueGreen -ExpectedActiveColor "blue"

    Assert-T03ConfigSecrets -ProfileName $ProfileName -Content $Content -ExpectedSecretName $ExpectedSecretName
}

$ChartPath = (Resolve-Path $ChartPath).Path
$Helm = Resolve-HelmCommand

Write-Host "==> helm lint ($($Helm.Mode))"
$lintExit = Invoke-Helm -Helm $Helm -Chart $ChartPath -HelmArgs @("lint")
if ($lintExit -ne 0) {
    exit $lintExit
}

$ValueFiles = @(
    @{ Name = "default"; Args = @(); ExpectedSecretName = "docgen-app-secrets"; ExpectIngress = $false; ExpectHpa = $false; ExpectBlueGreen = $false },
    @{ Name = "dev"; Args = @("-f", (Join-Path $ChartPath "values-dev.yaml")); ExpectedSecretName = "docgen-app-secrets-dev"; ExpectIngress = $false; ExpectHpa = $false; ExpectBlueGreen = $false },
    @{ Name = "staging"; Args = @("-f", (Join-Path $ChartPath "values-staging.yaml")); ExpectedSecretName = "docgen-app-secrets-staging"; ExpectIngress = $true; ExpectHpa = $true; ExpectBlueGreen = $false; ExpectBackendCustomMetric = $true },
    @{ Name = "prod"; Args = @("-f", (Join-Path $ChartPath "values-prod.yaml")); ExpectedSecretName = "docgen-app-secrets-prod"; ExpectIngress = $true; ExpectHpa = $true; ExpectBlueGreen = $true; ExpectBackendCustomMetric = $true }
)

if (Test-Path $OutputDir) {
    Remove-Item -Recurse -Force $OutputDir
}
New-Item -ItemType Directory -Path $OutputDir | Out-Null

foreach ($profile in $ValueFiles) {
    $outFile = Join-Path $OutputDir ("render-{0}.yaml" -f $profile.Name)
    Write-Host ("==> helm template {0}" -f $profile.Name)

    $templateExit = Invoke-HelmTemplate -Helm $Helm -Chart $ChartPath -ReleaseName ("docgen-{0}" -f $profile.Name) -ExtraArgs $profile.Args -OutFile $outFile
    if ($templateExit -ne 0) {
        exit $templateExit
    }

    $content = Get-Content $outFile -Raw
    Assert-RenderContent -ProfileName $profile.Name -Content $content -ExpectedSecretName $profile.ExpectedSecretName -ExpectIngress $profile.ExpectIngress -ExpectHpa $profile.ExpectHpa -ExpectBlueGreen $profile.ExpectBlueGreen -ExpectBackendCustomMetric $(if ($profile.ContainsKey('ExpectBackendCustomMetric')) { $profile.ExpectBackendCustomMetric } else { $false })
}

Write-Host "==> fail-closed: missing existingSecretName must error"
$failClosedFile = Join-Path $OutputDir "render-fail-closed.yaml"
$failClosedArgs = @("--set", "secrets.existingSecretName=")
$failClosedExit = 0
try {
    $failClosedExit = Invoke-HelmTemplate -Helm $Helm -Chart $ChartPath -ReleaseName "docgen-fail-closed" -ExtraArgs $failClosedArgs -OutFile $failClosedFile
}
catch {
    $failClosedExit = 1
}
if ($failClosedExit -eq 0) {
    Write-Error "helm template succeeded with empty secrets.existingSecretName — expected fail-closed error"
}
Write-Host "PASS: helm template failed as expected for missing secret reference"

function Resolve-KubeconformCommand {
    $native = Get-Command kubeconform -ErrorAction SilentlyContinue
    if ($native) {
        return @{ Mode = "native"; Command = $native.Source }
    }
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        return @{ Mode = "docker"; Command = "docker" }
    }
    Write-Error "Neither kubeconform nor Docker found. Install kubeconform or Docker, or pass -SkipKubeconform."
}

function Invoke-Kubeconform {
    param(
        [hashtable]$Kubeconform,
        [string]$ManifestFile,
        [string]$DockerImage,
        [string]$K8sVersion
    )

    $manifestMount = (Resolve-Path $ManifestFile).Path -replace '\\', '/'
    $manifestDir = Split-Path $manifestMount -Parent
    $manifestName = Split-Path $manifestMount -Leaf

    if ($Kubeconform.Mode -eq "native") {
        & $Kubeconform.Command `
            -summary `
            -kubernetes-version $K8sVersion `
            -ignore-missing-schemas `
            -skip Certificate,ExternalSecret `
            $ManifestFile
        return $LASTEXITCODE
    }

    & docker run --rm `
        -v "${manifestDir}:/manifests:ro" `
        $DockerImage `
        -summary `
        -kubernetes-version $K8sVersion `
        -ignore-missing-schemas `
        -skip Certificate,ExternalSecret `
        "/manifests/$manifestName"
    return $LASTEXITCODE
}

if (-not $SkipKubeconform) {
    $Kubeconform = Resolve-KubeconformCommand
    Write-Host "==> kubeconform ($($Kubeconform.Mode)) on rendered manifests"
    foreach ($profile in $ValueFiles) {
        $outFile = Join-Path $OutputDir ("render-{0}.yaml" -f $profile.Name)
        Write-Host ("    validating {0}" -f $profile.Name)
        $kcExit = Invoke-Kubeconform -Kubeconform $Kubeconform -ManifestFile $outFile -DockerImage $KubeconformDockerImage -K8sVersion $KubernetesVersion
        if ($kcExit -ne 0) {
            exit $kcExit
        }
    }
}

Write-Host ""
Write-Host "PASSED: helm lint + helm template for default, dev, staging, prod."
Write-Host "PASSED: P15-T03 ConfigMap/Secret wiring (no StatefulSet, no credentials in ConfigMap)."
Write-Host "PASSED: P15-T04 Service/Ingress/TLS (ClusterIP DNS, NGINX class, cert-manager, TLS 1.2+)."
Write-Host "PASSED: P15-T05 HPA (autoscaling/v2, CPU+memory, custom metric, scaleTargetRef)."
Write-Host "PASSED: P15-T06 NetworkPolicy (default-deny, explicit allow-list, no open 0.0.0.0/0)."
Write-Host "PASSED: P15-T07 health probes (/healthz liveness, /readyz readiness, port 8080)."
Write-Host "PASSED: P15-T08 blue-green (dual color Deployments, activeColor selector, preview Services, HPA target)."
Write-Host "PASSED: fail-closed secret reference validation."
if (-not $SkipKubeconform) {
    Write-Host "PASSED: kubeconform validation for all rendered profiles."
}
Write-Host "Rendered manifests: $OutputDir"
exit 0

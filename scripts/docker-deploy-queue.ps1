# Single-Docker-host deploy queue: mutex around scripts/docker-deploy.ps1.
# Usage (from repo root):
#   .\scripts\docker-deploy-queue.ps1
#   .\scripts\docker-deploy-queue.ps1 -SkipBuild
#   .\scripts\docker-deploy-queue.ps1 -ForceRebuild
#   .\scripts\docker-deploy-queue.ps1 -Status
#   .\scripts\docker-deploy-queue.ps1 -WaitSeconds 3600 -Reason "F5 acceptance"

param(
    [switch]$SkipBuild,
    [switch]$ForceRebuild,
    [switch]$Status,
    [switch]$EnqueueOnly,
    [int]$WaitSeconds = 7200,
    [int]$PollSeconds = 15,
    [string]$Reason = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $RepoRoot

$RuntimeDir = Join-Path $RepoRoot ".cursor\runtime"
$LockPath = Join-Path $RuntimeDir "docker-deploy.lock"
$QueueDir = Join-Path $RuntimeDir "docker-deploy-queue"

function Ensure-RuntimeDirs {
    if (-not (Test-Path $RuntimeDir)) { New-Item -ItemType Directory -Path $RuntimeDir | Out-Null }
    if (-not (Test-Path $QueueDir)) { New-Item -ItemType Directory -Path $QueueDir | Out-Null }
}

function Read-Lock {
    if (-not (Test-Path $LockPath)) { return $null }
    try {
        return Get-Content -Raw -Path $LockPath | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Test-PidAlive([int]$ProcessId) {
    if ($ProcessId -le 0) { return $false }
    try {
        $p = Get-Process -Id $ProcessId -ErrorAction Stop
        return $null -ne $p
    } catch {
        return $false
    }
}

function Clear-StaleLock {
    $lock = Read-Lock
    if ($null -eq $lock) {
        if (Test-Path $LockPath) { Remove-Item -Force $LockPath }
        return
    }
    $ownerPid = 0
    if ($lock.pid) { $ownerPid = [int]$lock.pid }
    if (-not (Test-PidAlive $ownerPid)) {
        Write-Host "WARN: stale deploy lock (pid=$ownerPid dead) — removing"
        Remove-Item -Force $LockPath -ErrorAction SilentlyContinue
    }
}

function Write-Lock {
    param([string]$Phase)
    $payload = [ordered]@{
        pid           = $PID
        phase         = $Phase
        reason        = $Reason
        worktree      = $RepoRoot
        started_at    = (Get-Date).ToUniversalTime().ToString("o")
        hostname      = $env:COMPUTERNAME
    }
    ($payload | ConvertTo-Json) | Set-Content -Path $LockPath -Encoding utf8
}

function Show-Status {
    Ensure-RuntimeDirs
    Clear-StaleLock
    $lock = Read-Lock
    if ($null -eq $lock) {
        Write-Host "DEPLOY_QUEUE: idle (no lock)"
    } else {
        Write-Host "DEPLOY_QUEUE: BUSY"
        Write-Host ($lock | ConvertTo-Json -Depth 5)
    }
    $pending = @(Get-ChildItem -Path $QueueDir -Filter "*.json" -ErrorAction SilentlyContinue | Sort-Object Name)
    Write-Host "Pending ticket files: $($pending.Count)"
    foreach ($f in $pending) {
        Write-Host "  - $($f.Name)"
    }
}

function New-QueueTicket {
    Ensure-RuntimeDirs
    $id = "{0:yyyyMMddHHmmss}-{1}" -f (Get-Date), $PID
    $ticketPath = Join-Path $QueueDir "$id.json"
    $ticket = [ordered]@{
        id         = $id
        pid        = $PID
        reason     = $Reason
        worktree   = $RepoRoot
        enqueued   = (Get-Date).ToUniversalTime().ToString("o")
        skipBuild  = [bool]$SkipBuild
        forceRebuild = [bool]$ForceRebuild
    }
    ($ticket | ConvertTo-Json) | Set-Content -Path $ticketPath -Encoding utf8
    return $ticketPath
}

Ensure-RuntimeDirs

if ($Status) {
    Show-Status
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Reason)) {
    $Reason = "deploy from $RepoRoot"
}

Clear-StaleLock
$lock = Read-Lock

if ($EnqueueOnly) {
    $ticket = New-QueueTicket
    Write-Host "DEPLOY_QUEUED: $ticket"
    if ($null -ne $lock) {
        Write-Host "Current holder:"; Write-Host ($lock | ConvertTo-Json)
    }
    exit 0
}

# Wait for lock
$deadline = (Get-Date).AddSeconds($WaitSeconds)
while ($true) {
    Clear-StaleLock
    $lock = Read-Lock
    if ($null -eq $lock) { break }
    if ((Get-Date) -ge $deadline) {
        Write-Error "DEPLOY_QUEUE_TIMEOUT: lock held after ${WaitSeconds}s. Holder: $($lock | ConvertTo-Json -Compress)"
        exit 2
    }
    Write-Host "DEPLOY_QUEUE: waiting (holder pid=$($lock.pid) reason=$($lock.reason)); sleep ${PollSeconds}s"
    Start-Sleep -Seconds $PollSeconds
}

# Acquire
Write-Lock -Phase "running"
Write-Host "DEPLOY_QUEUE: acquired lock (pid=$PID) — $Reason"

$deployScript = Join-Path $RepoRoot "scripts\docker-deploy.ps1"
$deployArgs = @()
if ($SkipBuild) { $deployArgs += "-SkipBuild" }
if ($ForceRebuild) { $deployArgs += "-ForceRebuild" }

try {
    & $deployScript @deployArgs
    $code = $LASTEXITCODE
    if ($null -eq $code) { $code = 0 }
} finally {
    if (Test-Path $LockPath) {
        $current = Read-Lock
        if ($null -ne $current -and [int]$current.pid -eq $PID) {
            Remove-Item -Force $LockPath -ErrorAction SilentlyContinue
            Write-Host "DEPLOY_QUEUE: released lock"
        }
    }
}

exit $code

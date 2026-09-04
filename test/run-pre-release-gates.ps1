#requires -Version 7.0
# NovaFlow AI — 上线前门禁一键执行
# 用法: pwsh test/run-pre-release-gates.ps1 [-SkipConcurrency] [-IncludeProdCompose]
#
# 依次执行:
#   1. pre-deploy-gate.ps1
#   2. cross-tenant-idor.ps1
#   3. publish-concurrency-gate.ps1 (可选)
#   4. prod-compose-smoke.ps1 (可选)

param(
    [switch]$SkipConcurrency,
    [switch]$IncludeProdCompose
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$summaryFile = Join-Path $PSScriptRoot 'pre-release-gates-summary.json'
$steps = [System.Collections.Generic.List[object]]::new()
$failed = 0

function Invoke-GateStep {
    param([string]$Name, [string]$ScriptPath, [string[]]$ExtraArgs = @())
    Write-Host "`n========== $Name ==========" -ForegroundColor Cyan
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        & pwsh -NoProfile -File $ScriptPath @ExtraArgs
        $exit = $LASTEXITCODE
        if ($null -eq $exit) { $exit = 0 }
        $passed = ($exit -eq 0)
    }
    catch {
        $passed = $false
        $exit = 1
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    $sw.Stop()
    $steps.Add([pscustomobject]@{
        name     = $Name
        script   = $ScriptPath
        passed   = $passed
        exitCode = $exit
        ms       = $sw.ElapsedMilliseconds
    }) | Out-Null
    if (-not $passed) { $script:failed++ }
}

Invoke-GateStep 'Pre-deploy gate' (Join-Path $PSScriptRoot 'pre-deploy-gate.ps1')
Invoke-GateStep 'Cross-tenant IDOR' (Join-Path $PSScriptRoot 'cross-tenant-idor.ps1')

if (-not $SkipConcurrency) {
    Invoke-GateStep 'Publish concurrency (CC-02)' (Join-Path $PSScriptRoot 'publish-concurrency-gate.ps1')
}

if ($IncludeProdCompose) {
    Invoke-GateStep 'Prod compose smoke' (Join-Path $PSScriptRoot 'prod-compose-smoke.ps1')
}

$allPass = ($failed -eq 0)
[ordered]@{
    timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    passed    = $allPass
    failed    = $failed
    steps     = @($steps)
} | ConvertTo-Json -Depth 6 | Set-Content $summaryFile -Encoding UTF8

Write-Host "`n========== SUMMARY ==========" -ForegroundColor $(if ($allPass) { 'Green' } else { 'Red' })
Write-Host "passed=$allPass failedSteps=$failed -> $summaryFile"
if (-not $allPass) { exit 1 }

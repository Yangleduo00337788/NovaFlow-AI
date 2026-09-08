#requires -Version 7.0
# NovaFlow AI — CI 核心冒烟（轻量子集）
# 用法: pwsh test/ci-core-smoke.ps1

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'ci-core-smoke.log'
$outFile = Join-Path $PSScriptRoot 'ci-core-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Invoke-CoreSmoke {
    param([string]$Name, [string]$Script)
    Write-NovaLog "=== CI core: $Name ===" $logFile
    $output = & pwsh -NoProfile -File $Script 2>&1
    if ($output) {
        foreach ($line in @($output)) {
            Write-NovaLog "  $line" $logFile
        }
    }
    $ok = ($LASTEXITCODE -eq 0)
    $detail = if ($ok) { "exit=0" } else { "exit=$LASTEXITCODE" }
    $null = Assert-NovaGate $Name $ok $detail $results
    if (-not $ok) { $script:allPass = $false }
}

Write-NovaLog '=== ci-core-smoke ===' $logFile

try {
    Prepare-NovaGateEnvironment
    Write-NovaLog 'Gate environment prepared.' $logFile
} catch {
    Write-NovaLog "Prepare-NovaGateEnvironment warning: $($_.Exception.Message)" $logFile
}

$coreScripts = @(
    @{ Name = 'Pre-deploy gate'; Script = Join-Path $PSScriptRoot 'pre-deploy-gate.ps1' }
    @{ Name = 'RBAC API acceptance'; Script = Join-Path $PSScriptRoot 'rbac-api-acceptance.ps1' }
    @{ Name = 'Global search smoke'; Script = Join-Path $PSScriptRoot 'global-search-smoke.ps1' }
    @{ Name = 'Open API acceptance'; Script = Join-Path $PSScriptRoot 'open-api-acceptance.ps1' }
)

foreach ($item in $coreScripts) {
    Invoke-CoreSmoke -Name $item.Name -Script $item.Script
}

Write-NovaGateResult -ScriptName 'ci-core-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

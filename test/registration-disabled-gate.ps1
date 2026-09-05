#requires -Version 7.0
# NovaFlow AI — prod 注册关闭验收（A-03）
# 用法: pwsh test/registration-disabled-gate.ps1
# 通过 Java 集成测试在 registration-enabled=false 下验证 /auth/register 被拒绝

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$logFile = Join-Path $PSScriptRoot 'registration-disabled-gate.log'
$outFile = Join-Path $PSScriptRoot 'registration-disabled-gate-results.json'
$results = [System.Collections.Generic.List[object]]::new()

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
    return $pass
}

. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')
$allPass = $true

Write-NovaLog '=== registration-disabled-gate A-03 ===' $logFile

Push-Location $repoRoot
try {
    $mvnOut = & mvn -pl novaflow-server test `
        "-Dtest=RegistrationDisabledLocalIntegrationTest" `
        "-Dtest.excludedGroups=none" 2>&1 | Out-String
    $exit = $LASTEXITCODE
    if ($null -eq $exit) { $exit = 1 }
    $ok = ($exit -eq 0)
    Check 'A-03 registration disabled integration test' $ok "exit=$exit" | Out-Null
    if (-not $ok) {
        Write-NovaLog $mvnOut $logFile
    }
} catch {
    Check 'A-03 registration disabled integration test' $false $_.Exception.Message
} finally {
    Pop-Location
}

Write-NovaGateResult -ScriptName 'registration-disabled-gate' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

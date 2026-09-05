#requires -Version 7.0
# NovaFlow AI — 审计日志权限验收（U-06）
# 用法: pwsh test/audit-access-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'audit-access-smoke.log'
$outFile = Join-Path $PSScriptRoot 'audit-access-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== audit-access-smoke ===' $logFile

try {
    $adminToken = Get-NovaLoginToken
    $devToken = Get-NovaLoginToken -Email 'developer@novaflow.ai' -Password 'Developer123!'
    $userToken = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'

    $adminLogs = Invoke-NovaApi -Path '/api/v1/audit-logs?page=1&pageSize=10' -Token $adminToken
    Check 'U-06 admin can list audit logs' ($adminLogs.code -eq 0) "code=$($adminLogs.code)"

    $devLogs = Invoke-NovaApi -Path '/api/v1/audit-logs?page=1&pageSize=10' -Token $devToken
    Check 'U-06 developer denied audit logs' (($devLogs.code -ne 0) -or ($devLogs.http -ge 400)) "http=$($devLogs.http) code=$($devLogs.code)"

    $userLogs = Invoke-NovaApi -Path '/api/v1/audit-logs?page=1&pageSize=10' -Token $userToken
    Check 'U-06 user denied audit logs' (($userLogs.code -ne 0) -or ($userLogs.http -ge 400)) "http=$($userLogs.http) code=$($userLogs.code)"
} catch {
    Check 'audit-access setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'audit-access-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# NovaFlow AI — Billing overview / quota 验收（B-01）
# 用法: pwsh test/billing-overview-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'billing-overview-smoke.log'
$outFile = Join-Path $PSScriptRoot 'billing-overview-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== billing-overview-smoke ===' $logFile

try {
    $admin = Get-NovaLoginToken
    $user = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'

    $overview = Invoke-NovaApi -Path '/api/v1/billing/overview' -Token $admin
    $overviewOk = ($overview.code -eq 0) -and ($overview.raw -match 'periodLabel|totalTokens|totalCalls')
    Check 'B-01 admin billing overview' $overviewOk "code=$($overview.code)"

    $quota = Invoke-NovaApi -Path '/api/v1/billing/quota' -Token $admin
    $quotaOk = ($quota.code -eq 0) -and ($quota.raw -match 'token')
    Check 'B-01 admin billing quota' $quotaOk "code=$($quota.code)"

    $allocation = Invoke-NovaApi -Path '/api/v1/billing/allocation?dimension=application' -Token $admin
    Check 'B-01 admin billing allocation' ($allocation.code -eq 0) "code=$($allocation.code)"

    $records = Invoke-NovaApi -Path '/api/v1/billing/records?page=1&pageSize=5' -Token $admin
    Check 'B-01 admin billing records page' ($records.code -eq 0) "code=$($records.code)"

    $allPass = (Test-NovaApiDenied 'B-01 user cannot billing overview' '/api/v1/billing/overview' GET $user $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'B-01 user cannot billing quota' '/api/v1/billing/quota' GET $user $results) -and $allPass
} catch {
    Check 'billing-overview setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'billing-overview-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# NovaFlow AI — 三角色平台隔离冒烟（取代平台子角色）
# 用法: pwsh test/platform-sub-roles-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'platform-sub-roles-smoke.log'
$outFile = Join-Path $PSScriptRoot 'platform-sub-roles-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== three-role platform isolation ===' $logFile

try {
    $platform = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'
    $admin = Get-NovaLoginToken
    $user = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'
    Wait-NovaMaintenanceOff -PlatformToken $platform

    $tenants = Invoke-NovaApi -Path '/api/v1/platform/tenants?page=1&pageSize=5' -Token $platform
    Check 'platform can list tenants' ($tenants.code -eq 0) "code=$($tenants.code)"

    $billing = Invoke-NovaApi -Path '/api/v1/platform/billing/overview' -Token $platform
    Check 'platform can view billing' ($billing.code -eq 0) "code=$($billing.code)"

    $settings = Invoke-NovaApi -Path '/api/v1/platform/settings' -Token $platform
    Check 'platform can view settings' ($settings.code -eq 0) "code=$($settings.code)"

    $allPass = (Test-NovaApiDenied 'tenant admin cannot platform tenants' '/api/v1/platform/tenants?page=1&pageSize=5' GET $admin $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'tenant admin cannot platform settings' '/api/v1/platform/settings' GET $admin $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'portal user cannot platform tenants' '/api/v1/platform/tenants?page=1&pageSize=5' GET $user $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'platform cannot studio agents' '/api/v1/agents?page=1&pageSize=5' GET $platform $results) -and $allPass
} catch {
    Check 'platform isolation setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'platform-sub-roles-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# NovaFlow AI — Phase 33 存储配额统计冒烟
# 用法: pwsh test/platform-storage-quota-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'platform-storage-quota-smoke.log'
$outFile = Join-Path $PSScriptRoot 'platform-storage-quota-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog "=== Phase 33 platform storage quota ===" $logFile

$platformToken = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'

$settingsPath = Join-Path $script:NovaFlowTmpDir 'storage-warn.json'
Write-NovaJson -Path $settingsPath -Data @{ storageWarnPercent = 80 }
$settings = Invoke-NovaApi -Method PUT -Path '/api/v1/platform/settings' -Token $platformToken -OutFile $settingsPath
Check 'P33-01 set storage warn percent' ($settings.code -eq 0) $settings.raw

$tenants = Invoke-NovaApi -Method GET -Path '/api/v1/platform/tenants?page=1&pageSize=5' -Token $platformToken
Check 'P33-02 tenant list has storage fields' (
    $tenants.code -eq 0 -and $tenants.raw -match 'usedStorageBytes' -and $tenants.raw -match 'storageUsedPercent'
) $tenants.raw

$tenantId = $null
if ($tenants.raw -match '"id"\s*:\s*(\d+)') { $tenantId = [int]$Matches[1] }
Check 'P33-03 parse tenant id' ($tenantId -gt 0) "tenantId=$tenantId"

if ($tenantId) {
    $detail = Invoke-NovaApi -Method GET -Path "/api/v1/platform/tenants/$tenantId/detail" -Token $platformToken
    Check 'P33-04 tenant detail storage percent' (
        $detail.code -eq 0 -and $detail.raw -match 'storageUsedPercent' -and $detail.raw -match 'usedStorageBytes'
    ) $detail.raw
} else {
    Check 'P33-04 tenant detail storage percent' $false 'no tenant id'
}

$overview = Invoke-NovaApi -Method GET -Path '/api/v1/platform/dashboard/overview' -Token $platformToken
Check 'P33-05 dashboard overview with tenant health' (
    $overview.code -eq 0 -and $overview.raw -match 'tenantHealth'
) $overview.raw

Write-NovaJson $outFile @{ passed = $allPass; results = $results }
Write-NovaLog "=== Result: $(if ($allPass) { 'PASS' } else { 'FAIL' }) ===" $logFile
if (-not $allPass) { exit 1 }

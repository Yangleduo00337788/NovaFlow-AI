#requires -Version 7.0
# NovaFlow AI — Phase 33/36 存储配额统计与硬拦截冒烟
# 用法: pwsh test/platform-storage-quota-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'platform-storage-quota-smoke.log'
$outFile = Join-Path $PSScriptRoot 'platform-storage-quota-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$email = "storage-quota-$suffix@novaflow.test"
$password = 'SmokeTest123!'

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog "=== Phase 33/36 platform storage quota ===" $logFile

try {
    $platformToken = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'
    Wait-NovaMaintenanceOff -PlatformToken $platformToken

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

    $registerPath = Join-Path $script:NovaFlowTmpDir "register-storage-$suffix.json"
    $company = "StorageQuota-$suffix"
    Write-NovaJson -Path $registerPath -Data @{
        companyName     = $company
        email           = $email
        nickname        = "Storage $suffix"
        password        = $password
        confirmPassword = $password
        planType        = 'personal'
    }
    $registered = Invoke-NovaApi -Method POST -Path '/api/v1/auth/register' -OutFile $registerPath
    Check 'P36-01 register tenant for storage test' ($registered.code -eq 0) $registered.raw
    $userToken = [regex]::Match($registered.raw, '"token":"([^"]+)"').Groups[1].Value

    $tenantList = Invoke-NovaApi -Method GET -Path "/api/v1/platform/tenants?page=1&pageSize=20&keyword=$company" -Token $platformToken
    $testTenantId = $null
    if ($tenantList.raw -match '"id"\s*:\s*(\d+)') { $testTenantId = [int]$Matches[1] }
    Check 'P36-02 locate test tenant' ($testTenantId -gt 0) "tenantId=$testTenantId"

    if ($testTenantId) {
        $updatePath = Join-Path $script:NovaFlowTmpDir 'tenant-storage-limit.json'
        Write-NovaJson -Path $updatePath -Data @{ tenantName = $company; maxStorageMb = 1 }
        $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/platform/tenants/$testTenantId" -Token $platformToken -OutFile $updatePath
        Check 'P36-03 set tenant storage limit 1MB' ($updated.code -eq 0) $updated.raw
    } else {
        Check 'P36-03 set tenant storage limit 1MB' $false 'missing tenant id'
    }

    $usage = Invoke-NovaApi -Method GET -Path '/api/v1/knowledge-bases/storage-usage' -Token $userToken
    Check 'P36-04 storage usage api' (
        $usage.code -eq 0 -and $usage.raw -match 'usedBytes' -and $usage.raw -match 'maxStorageMb'
    ) $usage.raw

    $kbPath = Join-Path $script:NovaFlowTmpDir 'kb-storage.json'
    Write-NovaJson -Path $kbPath -Data @{
        kbName         = "KB-Storage-$suffix"
        description    = 'storage quota enforcement'
        embeddingModel = 'text-embedding-3-small'
    }
    $kb = Invoke-NovaApi -Method POST -Path '/api/v1/knowledge-bases' -Token $userToken -OutFile $kbPath
    $kbId = [regex]::Match($kb.raw, '"id":(\d+)').Groups[1].Value
    Check 'P36-05 create knowledge base' (($kb.code -eq 0) -and $kbId) "kbId=$kbId code=$($kb.code)"

    $oversizeFile = Join-Path $script:NovaFlowTmpDir "oversize-$suffix.txt"
    $bytes = New-Object byte[] (1200 * 1024)
    [System.IO.File]::WriteAllBytes($oversizeFile, $bytes)
    if ($kbId) {
        $upload = Invoke-NovaFileUpload -Path "/api/v1/knowledge-bases/$kbId/documents/upload" -Token $userToken -FilePath $oversizeFile
        Check 'P36-06 oversize upload rejected' (
            $upload.code -eq 40036 -or $upload.raw -match '存储空间超出套餐上限'
        ) "code=$($upload.code)"
    } else {
        Check 'P36-06 oversize upload rejected' $false 'missing kb id'
    }
} catch {
    Check 'platform-storage-quota setup' $false $_.Exception.Message
}

Write-NovaJson $outFile @{ passed = $allPass; results = $results }
Write-NovaLog "=== Result: $(if ($allPass) { 'PASS' } else { 'FAIL' }) ===" $logFile
if (-not $allPass) { exit 1 }

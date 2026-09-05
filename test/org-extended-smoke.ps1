#requires -Version 7.0
# NovaFlow AI — 组织/通知扩展验收（U-01, U-05）
# 用法: pwsh test/org-extended-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'org-extended-smoke.log'
$outFile = Join-Path $PSScriptRoot 'org-extended-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== org-extended-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $tenant = Invoke-NovaApi -Path '/api/v1/org/tenant' -Token $token
    $originalName = $null
    if ($tenant.raw -match '"tenantName":"([^"]*)"') { $originalName = $Matches[1] }

    $updatedName = "QA-Tenant-$suffix"
    $updatePath = Join-Path $script:NovaFlowTmpDir 'tenant-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        tenantName   = $updatedName
        contactName  = 'QA Smoke'
        contactEmail = 'qa-smoke@novaflow.test'
    }
    $updated = Invoke-NovaApi -Method PUT -Path '/api/v1/org/tenant' -Token $token -OutFile $updatePath
    Check 'U-01 update tenant info' (($updated.code -eq 0) -and ($updated.raw -match $updatedName)) "code=$($updated.code)"

    if ($originalName) {
        Write-NovaJson -Path $updatePath -Data @{ tenantName = $originalName }
        $restored = Invoke-NovaApi -Method PUT -Path '/api/v1/org/tenant' -Token $token -OutFile $updatePath
        Check 'U-01 restore tenant name' ($restored.code -eq 0) "code=$($restored.code)"
    }

    $notifications = Invoke-NovaApi -Path '/api/v1/notifications?page=1&pageSize=10' -Token $token
    Check 'U-05 list notifications' ($notifications.code -eq 0) "code=$($notifications.code)"

    $unread = Invoke-NovaApi -Path '/api/v1/notifications/unread-count' -Token $token
    Check 'U-05 unread count' ($unread.code -eq 0) "code=$($unread.code)"

    $readAll = Invoke-NovaApi -Method POST -Path '/api/v1/notifications/read-all' -Token $token
    Check 'U-05 mark all read' ($readAll.code -eq 0) "code=$($readAll.code)"
} catch {
    Check 'org-extended setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'org-extended-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

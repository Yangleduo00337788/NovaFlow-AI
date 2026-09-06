#requires -Version 7.0
# Phase 39/40: 门户收藏、分类、导出与品牌化冒烟

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'portal-enhancements-smoke.log'
$outFile = Join-Path $PSScriptRoot 'portal-enhancements-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== portal-enhancements-smoke (P39/P40) ===' $logFile

try {
    $user = Get-NovaLoginToken 'user@novaflow.ai' 'User123!'
    $admin = Get-NovaLoginToken

    $branding = Invoke-NovaApi -Path '/api/v1/portal/branding' -Token $user
    Check 'P40 portal branding' ($branding.code -eq 0) "code=$($branding.code)"

    $categories = Invoke-NovaApi -Path '/api/v1/portal/categories' -Token $user
    Check 'P39 portal categories' ($categories.code -eq 0) "code=$($categories.code)"

    $apps = Invoke-NovaApi -Path '/api/v1/portal/apps' -Token $user
    Check 'P39 portal apps list' ($apps.code -eq 0) "code=$($apps.code)"

    if ($apps.code -eq 0 -and $apps.raw -match '"id":(\d+)') {
        $appId = [int]$Matches[1]
        $togglePath = Join-Path $script:NovaFlowTmpDir "portal-fav-$appId.json"
        @{ applicationId = $appId } | ConvertTo-Json | Set-Content -Path $togglePath -Encoding utf8
        $toggle = Invoke-NovaApi -Method POST -Path '/api/v1/portal/favorites/toggle' -Token $user -OutFile $togglePath
        Check 'P39 favorite toggle' ($toggle.code -eq 0) "code=$($toggle.code)"

        $favOnly = Invoke-NovaApi -Path '/api/v1/portal/apps?favoritesOnly=true' -Token $user
        Check 'P39 favorites filter' ($favOnly.code -eq 0) "code=$($favOnly.code)"

        $convs = Invoke-NovaApi -Path "/api/v1/portal/apps/$appId/conversations?page=1&pageSize=5" -Token $user
        if ($convs.code -eq 0 -and $convs.raw -match '"conversationKey":"([^"]+)"') {
            $key = $Matches[1]
            $encodedKey = [uri]::EscapeDataString($key)
            $export = Invoke-CurlExe @(
                '-s', '-o', 'NUL', '-w', '%{http_code}',
                "$script:NovaFlowBaseUrl/api/v1/portal/apps/$appId/conversations/export?conversationKey=$encodedKey&format=markdown",
                '-H', "Authorization: Bearer $user"
            )
            $exportCode = [int]$export
            Check 'P39 conversation export' ($exportCode -eq 200) "http=$exportCode"
        } else {
            Check 'P39 conversation export' $true 'skipped (no conversation)'
        }
    } else {
        Check 'P39 favorite toggle' $true 'skipped (no published portal app)'
        Check 'P39 favorites filter' $true 'skipped'
        Check 'P39 conversation export' $true 'skipped'
    }

    $tenantGet = Invoke-NovaApi -Path '/api/v1/org/tenant' -Token $admin
    Check 'P40 tenant branding read' ($tenantGet.code -eq 0) "code=$($tenantGet.code)"

    if ($tenantGet.code -eq 0 -and $tenantGet.raw -match '"tenantName"\s*:\s*"([^"]+)"') {
        $tenantName = $Matches[1]
        $updatePath = Join-Path $script:NovaFlowTmpDir 'portal-branding-update.json'
        @{
            tenantName = $tenantName
            logoUrl = 'https://example.com/logo.png'
            portalThemeColor = '#6366f1'
        } | ConvertTo-Json | Set-Content -Path $updatePath -Encoding utf8
        $updated = Invoke-NovaApi -Method PUT -Path '/api/v1/org/tenant' -Token $admin -OutFile $updatePath
        Check 'P40 tenant branding update' ($updated.code -eq 0) "code=$($updated.code)"
    } else {
        Check 'P40 tenant branding update' $true 'skipped (tenant read failed)'
    }

    $brandingAfter = Invoke-NovaApi -Path '/api/v1/portal/branding' -Token $user
    $themeOk = ($brandingAfter.code -eq 0) -and ($brandingAfter.raw -match '#6366f1')
    Check 'P40 portal branding theme applied' $themeOk "code=$($brandingAfter.code)"
} catch {
    Write-NovaLog "portal-enhancements-smoke failed: $($_.Exception.Message)" $logFile
    Check 'portal-enhancements setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'portal-enhancements-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

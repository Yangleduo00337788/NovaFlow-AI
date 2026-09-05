#requires -Version 7.0
# NovaFlow AI — Dashboard 扩展验收（D-02, D-03）
# 用法: pwsh test/dashboard-extended-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'dashboard-extended-smoke.log'
$outFile = Join-Path $PSScriptRoot 'dashboard-extended-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== dashboard-extended-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "Dash-App-$suffix"

    $recent = Invoke-NovaApi -Path '/api/v1/dashboard/recent-items?limit=10' -Token $token
    Check 'D-02 recent items' ($recent.code -eq 0) "code=$($recent.code)"

    $favPath = Join-Path $script:NovaFlowTmpDir 'dash-fav.json'
    Write-NovaJson -Path $favPath -Data @{
        resourceType = 'application'
        resourceId   = $appId
        resourceName = "Dash-App-$suffix"
    }
    $fav1 = Invoke-NovaApi -Method POST -Path '/api/v1/dashboard/favorites/toggle' -Token $token -OutFile $favPath
    $fav2 = Invoke-NovaApi -Method POST -Path '/api/v1/dashboard/favorites/toggle' -Token $token -OutFile $favPath
    Check 'D-03 favorite toggle idempotent' (($fav1.code -eq 0) -and ($fav2.code -eq 0)) "code1=$($fav1.code) code2=$($fav2.code)"

    $favorites = Invoke-NovaApi -Path '/api/v1/dashboard/favorites?limit=20' -Token $token
    Check 'D-02 favorites list' ($favorites.code -eq 0) "code=$($favorites.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'dashboard-extended setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'dashboard-extended-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# NovaFlow AI — 全局搜索验收（D-04）
# 用法: pwsh test/global-search-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'global-search-smoke.log'
$outFile = Join-Path $PSScriptRoot 'global-search-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== global-search-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $user = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'

    $appName = "Search-App-$suffix"
    $appId = New-NovaApplication -Token $token -Name $appName

    $search = Invoke-NovaApi -Path "/api/v1/search?keyword=$appName&limit=20" -Token $token
    $found = ($search.code -eq 0) -and ($search.raw -match $appName)
    Check 'D-04 global search finds created app' $found "code=$($search.code)"

    $empty = Invoke-NovaApi -Path '/api/v1/search?keyword=__no_match_xyz_999__&limit=5' -Token $token
    $emptyOk = ($empty.code -eq 0) -and ($empty.raw -match '\[\]|"list":\[\]|"data":\[\]')
    Check 'D-04 global search empty result' $emptyOk "code=$($empty.code)"

    $userSearch = Invoke-NovaApi -Path "/api/v1/search?keyword=$appName&limit=5" -Token $user
    $userCanSearch = ($userSearch.code -eq 0)
    Check 'D-04 portal user can global search' $userCanSearch "code=$($userSearch.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'global-search setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'global-search-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

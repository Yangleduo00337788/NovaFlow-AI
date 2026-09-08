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
$appId = 0
$token = $null

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Test-NovaSearchHasKeyword {
    param([string]$Raw, [string]$Keyword)
    if (-not $Raw) { return $false }
    if ($Raw -match [regex]::Escape($Keyword)) { return $true }
    return $false
}

function Test-NovaSearchEmpty {
    param([string]$Raw)
    if (-not $Raw) { return $false }
    if ($Raw -match '"data"\s*:\s*\[\]') { return $true }
    if ($Raw -match '"list"\s*:\s*\[\]') { return $true }
    return $false
}

Write-NovaLog '=== global-search-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $user = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'

    $appName = "Search-App-$suffix"
    $appId = New-NovaApplication -Token $token -Name $appName
    $encodedKeyword = [uri]::EscapeDataString($appName)

    $found = $false
    for ($attempt = 1; $attempt -le 5; $attempt++) {
        $search = Invoke-NovaApi -Path "/api/v1/search?keyword=$encodedKeyword&limit=20" -Token $token
        $found = ($search.code -eq 0) -and (Test-NovaSearchHasKeyword $search.raw $appName)
        if ($found) { break }
        Start-Sleep -Seconds 1
    }
    Check 'D-04 global search finds created app' $found "code=$($search.code) attempts=$attempt"

    $emptyKeyword = [uri]::EscapeDataString('__no_match_xyz_999__')
    $empty = Invoke-NovaApi -Path "/api/v1/search?keyword=$emptyKeyword&limit=5" -Token $token
    $emptyOk = ($empty.code -eq 0) -and (Test-NovaSearchEmpty $empty.raw)
    Check 'D-04 global search empty result' $emptyOk "code=$($empty.code)"

    $userSearch = Invoke-NovaApi -Path "/api/v1/search?keyword=$encodedKeyword&limit=5" -Token $user
    if (Test-NovaDenied -Resp $userSearch) {
        Check 'D-04 portal user global search' $true "denied http=$($userSearch.http) code=$($userSearch.code)"
    } else {
        Check 'D-04 portal user can global search' ($userSearch.code -eq 0) "code=$($userSearch.code)"
    }
} catch {
    Check 'global-search setup' $false $_.Exception.Message
} finally {
    if ($appId -gt 0 -and $token) {
        try {
            Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
        } catch {
            Write-NovaLog "cleanup application $appId failed: $($_.Exception.Message)" $logFile
        }
    }
}

Write-NovaGateResult -ScriptName 'global-search-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

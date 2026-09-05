#requires -Version 7.0
# NovaFlow AI — 认证生命周期验收（A-01, A-05）
# 用法: pwsh test/auth-lifecycle-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'auth-lifecycle-smoke.log'
$outFile = Join-Path $PSScriptRoot 'auth-lifecycle-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== auth-lifecycle-smoke ===' $logFile

try {
    $registered = Register-NovaTenant -Suffix $suffix
    Check 'A-01 register valid tenant' ($registered.token -and $registered.email) "email=$($registered.email)"

    $me = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $registered.token
    Check 'A-01 register token works' ($me.code -eq 0) "code=$($me.code)"

    $badPath = Join-Path $script:NovaFlowTmpDir 'bad-login.json'
    Write-NovaJson -Path $badPath -Data @{
        email    = $registered.email
        password = 'WrongPassword123!'
    }
    $bad = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $badPath
    Check 'A-05 wrong password rejected' ($bad.code -ne 0) "code=$($bad.code)"

    $goodPath = Join-Path $script:NovaFlowTmpDir 'good-login.json'
    Write-NovaJson -Path $goodPath -Data @{
        email    = $registered.email
        password = 'SmokeTest123!'
    }
    $good = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $goodPath
    $goodToken = [regex]::Match($good.raw, '"token":"([^"]+)"').Groups[1].Value
    Check 'A-01 login after register' (($good.code -eq 0) -and $goodToken) "code=$($good.code)"
} catch {
    Check 'auth-lifecycle setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'auth-lifecycle-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

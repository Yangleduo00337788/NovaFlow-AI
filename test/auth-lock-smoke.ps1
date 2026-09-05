#requires -Version 7.0
# NovaFlow AI — 登录失败锁定验收（A-06）
# 用法: pwsh test/auth-lock-smoke.ps1
# 前提: Redis 可用；默认阈值 5 次 / 15 分钟

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'auth-lock-smoke.log'
$outFile = Join-Path $PSScriptRoot 'auth-lock-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$lockEmail = "lock-test-$suffix@novaflow.test"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== auth-lock-smoke ===' $logFile

try {
    $badPath = Join-Path $script:NovaFlowTmpDir 'lock-login.json'
    Write-NovaJson -Path $badPath -Data @{
        email    = $lockEmail
        password = 'WrongPassword123!'
    }

    for ($i = 1; $i -le 5; $i++) {
        $attempt = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $badPath
        if ($attempt.code -eq 0) {
            throw "Unexpected successful login on attempt $i"
        }
    }

    $locked = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $badPath
    $isLocked = ($locked.code -eq 42902) -or ($locked.raw -match '42902|登录失败次数过多')
    Check 'A-06 locked after repeated failures' $isLocked "code=$($locked.code) http=$($locked.http)"
} catch {
    Check 'auth-lock setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'auth-lock-smoke' -Passed $allPass -Details @{
    email  = $lockEmail
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

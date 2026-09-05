#requires -Version 7.0
# NovaFlow AI — 登录限流验收（A-07）
# 用法: pwsh test/auth-rate-limit-smoke.ps1
# 说明: 默认限流 120/min（application.yml）；使用正确密码避免触发锁定（A-06）

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'auth-rate-limit-smoke.log'
$outFile = Join-Path $PSScriptRoot 'auth-rate-limit-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$email = "rate-limit-$suffix@novaflow.test"
$password = 'SmokeTest123!'

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== auth-rate-limit-smoke ===' $logFile

try {
    $registerPath = Join-Path $script:NovaFlowTmpDir "register-$suffix.json"
    Write-NovaJson -Path $registerPath -Data @{
        companyName     = "QA-Rate-$suffix"
        email           = $email
        nickname        = "Rate $suffix"
        password        = $password
        confirmPassword = $password
        planType        = 'enterprise'
    }
    $registered = Invoke-NovaApi -Method POST -Path '/api/v1/auth/register' -OutFile $registerPath
    if ($registered.code -ne 0) {
        throw "Register failed: $($registered.raw)"
    }

    $loginPath = Join-Path $script:NovaFlowTmpDir 'rate-login.json'
    Write-NovaJson -Path $loginPath -Data @{ email = $email; password = $password }

    $rateLimited = $false
    for ($i = 1; $i -le 125; $i++) {
        $attempt = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $loginPath
        if ($attempt.code -eq 42901) {
            $rateLimited = $true
            Check 'A-07 login rate limit triggers 42901' $true "attempt=$i code=$($attempt.code)"
            break
        }
        if ($attempt.code -ne 0) {
            throw "Unexpected login failure on attempt ${i}: $($attempt.raw)"
        }
    }

    if (-not $rateLimited) {
        Check 'A-07 login rate limit triggers 42901' $false 'no 42901 after 125 attempts (limit may be raised in env)'
    }
} catch {
    Check 'auth-rate-limit setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'auth-rate-limit-smoke' -Passed $allPass -Details @{
    email  = $email
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

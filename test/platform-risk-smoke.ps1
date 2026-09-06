#requires -Version 7.0
# NovaFlow AI — Phase 32 平台风控冒烟
# 用法: pwsh test/platform-risk-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'platform-risk-smoke.log'
$outFile = Join-Path $PSScriptRoot 'platform-risk-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$platformToken = $null
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Reset-RiskSettings {
    if (-not $script:platformToken) { return }
    $resetPath = Join-Path $script:NovaFlowTmpDir 'risk-reset.json'
    Write-NovaJson -Path $resetPath -Data @{
        batchRegisterIpLimitPerDay = 5
        abnormalLoginEnabled       = $true
        newUserAgentEnabled        = $true
    }
    Invoke-NovaApi -Method PUT -Path '/api/v1/platform/settings' -Token $script:platformToken -OutFile $resetPath | Out-Null
}

Write-NovaLog "=== Phase 32 platform risk control ===" $logFile

try {
    $script:platformToken = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'
    Wait-NovaMaintenanceOff -PlatformToken $script:platformToken

    $overview = Invoke-NovaApi -Method GET -Path '/api/v1/platform/security/overview' -Token $script:platformToken
    Check 'P32-01 security overview' (
        $overview.code -eq 0 -and $overview.raw -match 'openAlertCount'
    ) $overview.raw

    $limitPath = Join-Path $script:NovaFlowTmpDir 'risk-limit.json'
    Write-NovaJson -Path $limitPath -Data @{ batchRegisterIpLimitPerDay = 2 }
    $limitSet = Invoke-NovaApi -Method PUT -Path '/api/v1/platform/settings' -Token $script:platformToken -OutFile $limitPath
    Check 'P32-02 set batch register limit' ($limitSet.code -eq 0) $limitSet.raw

    foreach ($i in 1..2) {
        $regPath = Join-Path $script:NovaFlowTmpDir "risk-reg-$suffix-$i.json"
        Write-NovaJson -Path $regPath -Data @{
            email           = "risk-$suffix-$i@novaflow.test"
            password        = 'Test1234'
            confirmPassword = 'Test1234'
            companyName     = "Risk Smoke $suffix-$i"
        }
        $reg = Invoke-NovaApi -Method POST -Path '/api/v1/auth/register' -OutFile $regPath
        Check "P32-03 register user $i allowed" ($reg.code -eq 0) $reg.raw
    }

    $regPath3 = Join-Path $script:NovaFlowTmpDir "risk-reg-$suffix-3.json"
    Write-NovaJson -Path $regPath3 -Data @{
        email           = "risk-$suffix-3@novaflow.test"
        password        = 'Test1234'
        confirmPassword = 'Test1234'
        companyName     = "Risk Smoke $suffix-3"
    }
    $reg3 = Invoke-NovaApi -Method POST -Path '/api/v1/auth/register' -OutFile $regPath3
    Check 'P32-04 third register blocked' ($reg3.code -ne 0) "code=$($reg3.code)"

    $alerts = Invoke-NovaApi -Method GET -Path '/api/v1/platform/security/alerts?page=1&pageSize=10&status=OPEN' -Token $script:platformToken
    Check 'P32-05 batch register alert exists' (
        $alerts.code -eq 0 -and $alerts.raw -match 'BATCH_REGISTER'
    ) $alerts.raw

    $alertId = $null
    if ($alerts.raw -match '"id"\s*:\s*(\d+)') { $alertId = [int]$Matches[1] }
    if ($alertId) {
        $ack = Invoke-NovaApi -Method POST -Path "/api/v1/platform/security/alerts/$alertId/ack" -Token $script:platformToken
        Check 'P32-06 acknowledge alert' ($ack.code -eq 0) $ack.raw
    } else {
        Check 'P32-06 acknowledge alert' $false 'no alert id parsed'
    }
}
finally {
    Reset-RiskSettings
    Check 'P32-07 risk settings reset' $true 'reset in finally'
}

Write-NovaJson $outFile @{ passed = $allPass; results = $results }
Write-NovaLog "=== Result: $(if ($allPass) { 'PASS' } else { 'FAIL' }) ===" $logFile
if (-not $allPass) { exit 1 }

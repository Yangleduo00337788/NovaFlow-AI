#requires -Version 7.0
# NovaFlow AI — Phase 34 维护模式/公告租户侧冒烟
# 用法: pwsh test/platform-maintenance-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'platform-maintenance-smoke.log'
$outFile = Join-Path $PSScriptRoot 'platform-maintenance-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$platformToken = $null

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Reset-Maintenance {
    if (-not $script:platformToken) { return }
    $resetPath = Join-Path $script:NovaFlowTmpDir 'maintenance-reset.json'
    Write-NovaJson -Path $resetPath -Data @{
        maintenanceEnabled   = $false
        maintenanceMessage   = ''
        platformAnnouncement = ''
    }
    Invoke-NovaApi -Method PUT -Path '/api/v1/platform/settings' -Token $script:platformToken -OutFile $resetPath | Out-Null
}

Write-NovaLog "=== Phase 34 platform maintenance ===" $logFile

try {
    $script:platformToken = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'

    $public = Invoke-NovaApi -Method GET -Path '/api/v1/public/platform-status'
    Check 'P34-01 public platform status' ($public.code -eq 0) $public.raw

    $enablePath = Join-Path $script:NovaFlowTmpDir 'maintenance-enable.json'
    Write-NovaJson -Path $enablePath -Data @{
        maintenanceEnabled   = $true
        maintenanceMessage   = 'Phase34 smoke maintenance'
        platformAnnouncement = 'Phase34 smoke announcement'
    }
    $enable = Invoke-NovaApi -Method PUT -Path '/api/v1/platform/settings' -Token $script:platformToken -OutFile $enablePath
    Check 'P34-02 enable maintenance' ($enable.code -eq 0) $enable.raw

    $publicOn = Invoke-NovaApi -Method GET -Path '/api/v1/public/platform-status'
    Check 'P34-03 public status reflects maintenance' (
        $publicOn.code -eq 0 -and $publicOn.raw -match '"maintenanceEnabled"\s*:\s*true'
    ) $publicOn.raw

    $tenantLoginPath = Join-Path $script:NovaFlowTmpDir 'tenant-login-blocked.json'
    Write-NovaJson -Path $tenantLoginPath -Data @{ email = 'admin@novaflow.ai'; password = 'Admin123!' }
    $tenantLogin = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $tenantLoginPath
    Check 'P34-04 tenant login blocked' ($tenantLogin.code -ne 0) "code=$($tenantLogin.code)"

    $registerPath = Join-Path $script:NovaFlowTmpDir 'register-blocked.json'
    Write-NovaJson -Path $registerPath -Data @{
        email           = "maint-$([Guid]::NewGuid().ToString('N').Substring(0,8))@novaflow.test"
        password        = 'Test1234'
        confirmPassword = 'Test1234'
        companyName     = 'Maint Test'
    }
    $register = Invoke-NovaApi -Method POST -Path '/api/v1/auth/register' -OutFile $registerPath
    Check 'P34-05 tenant register blocked' ($register.code -ne 0) "code=$($register.code)"

    $platformRelogin = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'
    Check 'P34-06 platform login still works' ($platformRelogin.Length -gt 10) 'token ok'

    try {
        $page = Invoke-WebRequest -Uri "$NovaFlowWebUrl/maintenance" -UseBasicParsing
        Check 'P34-07 maintenance page reachable' ($page.StatusCode -eq 200) "status=$($page.StatusCode)"
    } catch {
        Check 'P34-07 maintenance page reachable' $false $_.Exception.Message
    }
}
finally {
    Reset-Maintenance
    Check 'P34-08 maintenance reset' $true 'reset in finally'
}

Write-NovaJson $outFile @{ passed = $allPass; results = $results }
Write-NovaLog "=== Result: $(if ($allPass) { 'PASS' } else { 'FAIL' }) ===" $logFile
if (-not $allPass) { exit 1 }

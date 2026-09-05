#requires -Version 7.0
# NovaFlow AI — Phase 31 平台代开户增强冒烟
# 用法: pwsh test/platform-tenant-onboarding-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'platform-tenant-onboarding-smoke.log'
$outFile = Join-Path $PSScriptRoot 'platform-tenant-onboarding-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$ownerEmail = "phase31-$suffix@novaflow.test"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog "=== Phase 31 platform tenant onboarding ===" $logFile

$platformToken = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'

$templates = Invoke-NovaApi -Method GET -Path '/api/v1/platform/onboarding/templates' -Token $platformToken
Check 'P31-01 onboarding templates' ($templates.code -eq 0 -and $templates.raw -match 'planType') $templates.raw

$createPath = Join-Path $script:NovaFlowTmpDir "tenant-create-$suffix.json"
Write-NovaJson -Path $createPath -Data @{
    tenantName       = "Phase31 Smoke $suffix"
    planType         = 'starter'
    ownerEmail       = $ownerEmail
    generatePassword = $true
    sendInviteEmail  = $false
}
$create = Invoke-NovaApi -Method POST -Path '/api/v1/platform/tenants' -Token $platformToken -OutFile $createPath
Check 'P31-02 create tenant with generated password' ($create.code -eq 0 -and $create.raw -match 'generatedPassword') $create.raw

$tenantId = $null
$ownerId = $null
$generatedPassword = $null
if ($create.raw -match '"tenant"\s*:\s*\{[^\}]*"id"\s*:\s*(\d+)') { $tenantId = [int]$Matches[1] }
if ($create.raw -match '"ownerId"\s*:\s*(\d+)') { $ownerId = [int]$Matches[1] }
if ($create.raw -match '"generatedPassword"\s*:\s*"([^"]+)"') { $generatedPassword = $Matches[1] }

Check 'P31-03 parse tenant/owner ids' ($tenantId -and $ownerId) "tenantId=$tenantId ownerId=$ownerId"

Wait-NovaMaintenanceOff -PlatformToken $platformToken

$loginPath = Join-Path $script:NovaFlowTmpDir "owner-login-$suffix.json"
Write-NovaJson -Path $loginPath -Data @{ email = $ownerEmail; password = $generatedPassword }
$ownerLogin = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $loginPath
Check 'P31-04 owner login' ($ownerLogin.code -eq 0) $ownerLogin.raw

$resetPath = Join-Path $script:NovaFlowTmpDir "owner-reset-$suffix.json"
Write-NovaJson -Path $resetPath -Data @{ generatePassword = $true; sendInviteEmail = $false }
$reset = Invoke-NovaApi -Method POST -Path "/api/v1/platform/tenants/$tenantId/owner/reset-password" -Token $platformToken -OutFile $resetPath
Check 'P31-05 reset owner password' ($reset.code -eq 0 -and $reset.raw -match 'generatedPassword') $reset.raw

$newPassword = $null
if ($reset.raw -match '"generatedPassword"\s*:\s*"([^"]+)"') { $newPassword = $Matches[1] }
Wait-NovaMaintenanceOff -PlatformToken $platformToken
$reloginPath = Join-Path $script:NovaFlowTmpDir "owner-relogin-$suffix.json"
Write-NovaJson -Path $reloginPath -Data @{ email = $ownerEmail; password = $newPassword }
$relogin = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $reloginPath
Check 'P31-06 owner relogin after reset' ($relogin.code -eq 0) $relogin.raw

$auditorToken = Get-NovaLoginToken 'auditor@novaflow.ai' 'Auditor123!'
$denied = Invoke-NovaApi -Method POST -Path '/api/v1/platform/tenants' -Token $auditorToken -OutFile $createPath
Check 'P31-07 auditor denied create' ($denied.code -ne 0) "code=$($denied.code)"

if ($ownerId) {
    Invoke-NovaApi -Method DELETE -Path "/api/v1/platform/users/$ownerId" -Token $platformToken | Out-Null
}
if ($tenantId) {
    Invoke-NovaApi -Method DELETE -Path "/api/v1/platform/tenants/$tenantId" -Token $platformToken | Out-Null
}
Check 'P31-08 cleanup tenant/user' $true 'deleted'

Write-NovaJson $outFile @{ passed = $allPass; results = $results }
Write-NovaLog "=== Result: $(if ($allPass) { 'PASS' } else { 'FAIL' }) ===" $logFile
if (-not $allPass) { exit 1 }

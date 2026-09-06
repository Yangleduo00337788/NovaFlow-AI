#requires -Version 7.0
# NovaFlow AI — Phase 37 平台子角色冒烟
# 用法: pwsh test/platform-sub-roles-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'platform-sub-roles-smoke.log'
$outFile = Join-Path $PSScriptRoot 'platform-sub-roles-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== Phase 37 platform sub-roles ===' $logFile

try {
    $platform = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'
    $support = Get-NovaLoginToken 'support@novaflow.ai' 'Support123!'
    $billing = Get-NovaLoginToken 'billing@novaflow.ai' 'Billing123!'
    $auditor = Get-NovaLoginToken 'auditor@novaflow.ai' 'Auditor123!'
    Wait-NovaMaintenanceOff -PlatformToken $platform

    $supportTenants = Invoke-NovaApi -Path '/api/v1/platform/tenants?page=1&pageSize=5' -Token $support
    Check 'P37-01 support can list tenants' ($supportTenants.code -eq 0) "code=$($supportTenants.code)"

    $supportBilling = Invoke-NovaApi -Path '/api/v1/platform/billing/overview' -Token $support
    Check 'P37-02 support denied billing' ($supportBilling.code -ne 0) "code=$($supportBilling.code)"

    $supportSettings = Invoke-NovaApi -Path '/api/v1/platform/settings' -Token $support
    Check 'P37-03 support denied settings' ($supportSettings.code -ne 0) "code=$($supportSettings.code)"

    $billingOverview = Invoke-NovaApi -Path '/api/v1/platform/billing/overview' -Token $billing
    Check 'P37-04 billing can view overview' ($billingOverview.code -eq 0) "code=$($billingOverview.code)"

    $billingTenants = Invoke-NovaApi -Path '/api/v1/platform/tenants?page=1&pageSize=5' -Token $billing
    Check 'P37-05 billing can list tenants' ($billingTenants.code -eq 0) "code=$($billingTenants.code)"

    $suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
    $ownerEmail = "p37-support-$suffix@novaflow.test"
    $createPath = Join-Path $script:NovaFlowTmpDir 'platform-sub-role-tenant.json'
    Write-NovaJson -Path $createPath -Data @{
        tenantName       = "P37 Smoke $suffix"
        planType         = 'starter'
        ownerEmail       = $ownerEmail
        generatePassword = $true
        sendInviteEmail  = $false
    }
    $billingCreate = Invoke-NovaApi -Method POST -Path '/api/v1/platform/tenants' -Token $billing -OutFile $createPath
    Check 'P37-06 billing denied create tenant' ($billingCreate.code -ne 0) "code=$($billingCreate.code)"

    $billingSettings = Invoke-NovaApi -Path '/api/v1/platform/settings' -Token $billing
    Check 'P37-07 billing denied settings' ($billingSettings.code -ne 0) "code=$($billingSettings.code)"

    $auditorTenants = Invoke-NovaApi -Path '/api/v1/platform/tenants?page=1&pageSize=5' -Token $auditor
    Check 'P37-08 auditor denied tenants' ($auditorTenants.code -ne 0) "code=$($auditorTenants.code)"

    $supportCreate = Invoke-NovaApi -Method POST -Path '/api/v1/platform/tenants' -Token $support -OutFile $createPath
    Check 'P37-09 support can create tenant' ($supportCreate.code -eq 0 -and $supportCreate.raw -match 'generatedPassword') "code=$($supportCreate.code) $($supportCreate.raw)"

    if ($supportCreate.code -eq 0 -and $supportCreate.raw -match '"tenant"\s*:\s*\{[^\}]*"id"\s*:\s*(\d+)') {
        $tenantId = [int]$Matches[1]
        Invoke-NovaApi -Method DELETE -Path "/api/v1/platform/tenants/$tenantId" -Token $platform | Out-Null
    }
} catch {
    Check 'platform-sub-roles setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'platform-sub-roles-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

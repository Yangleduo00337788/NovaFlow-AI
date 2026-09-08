#requires -Version 7.0
# NovaFlow AI — Phase 35 平台外部告警冒烟
# 用法: pwsh test/platform-alert-dispatch-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'platform-alert-dispatch-smoke.log'
$outFile = Join-Path $PSScriptRoot 'platform-alert-dispatch-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== Phase 35 platform alert dispatch ===' $logFile

try {
    $platform = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'
    $admin = Get-NovaLoginToken
    Wait-NovaMaintenanceOff -PlatformToken $platform

    $channels = Invoke-NovaApi -Path '/api/v1/platform/notify-channels' -Token $platform
    Check 'P35-01 get notify channels' ($channels.code -eq 0) "code=$($channels.code)"

    $savePath = Join-Path $script:NovaFlowTmpDir 'platform-notify-channel.json'
    Write-NovaJson -Path $savePath -Data @{
        emailEnabled     = $true
        emailRecipients  = 'platform@novaflow.ai'
        webhookEnabled   = $false
        webhookUrl       = ''
    }
    $saved = Invoke-NovaApi -Method PUT -Path '/api/v1/platform/notify-channels' -Token $platform -OutFile $savePath
    Check 'P35-02 save notify channels' ($saved.code -eq 0) $saved.raw

    $settingsPath = Join-Path $script:NovaFlowTmpDir 'platform-alert-settings.json'
    Write-NovaJson -Path $settingsPath -Data @{
        securityAlertChannels     = @('email')
        apiMonitorAlertChannels   = @('email')
        storageQuotaAlertChannels = @('email')
    }
    $settings = Invoke-NovaApi -Method PUT -Path '/api/v1/platform/settings' -Token $platform -OutFile $settingsPath
    Check 'P35-03 save alert channel policy' (
        $settings.code -eq 0 -and $settings.raw -match 'securityAlertChannels'
    ) $settings.raw

    $test = Invoke-NovaApi -Method POST -Path '/api/v1/platform/notify-channels/test' -Token $platform
    Check 'P35-04 test notify dispatch' ($test.code -eq 0) "code=$($test.code)"

    $denied = Invoke-NovaApi -Path '/api/v1/platform/notify-channels' -Token $admin
    Check 'P35-05 tenant admin denied notify channels' ($denied.code -ne 0) "code=$($denied.code)"
} catch {
    Check 'platform-alert-dispatch setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'platform-alert-dispatch-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

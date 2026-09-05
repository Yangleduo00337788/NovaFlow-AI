#requires -Version 7.0
# NovaFlow AI — 账单预警配置验收（B-03）
# 用法: pwsh test/billing-alert-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'billing-alert-smoke.log'
$outFile = Join-Path $PSScriptRoot 'billing-alert-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== billing-alert-smoke ===' $logFile

try {
    $admin = Get-NovaLoginToken
    $user = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'

    $alerts = Invoke-NovaApi -Path '/api/v1/billing/alerts' -Token $admin
    Check 'B-03 list billing alerts' ($alerts.code -eq 0) "code=$($alerts.code)"

    $alertId = [regex]::Match($alerts.raw, '"id":(\d+)').Groups[1].Value
    if (-not $alertId) { throw 'No billing alert found to update' }

    $savePath = Join-Path $script:NovaFlowTmpDir 'billing-alert.json'
    # 更新已有预警，避免 uk_tenant_type_threshold 冲突（80/100 为默认预置）
    Write-NovaJson -Path $savePath -Data @{
        id                = [long]$alertId
        alertName         = "QA-Alert-$suffix"
        thresholdPercent  = 80
        enabled           = $true
        notifyChannels    = @('site')
    }
    $saved = Invoke-NovaApi -Method PUT -Path '/api/v1/billing/alerts' -Token $admin -OutFile $savePath
    Check 'B-03 save billing alert' ($saved.code -eq 0) "code=$($saved.code) alertId=$alertId"

    $channels = Invoke-NovaApi -Path '/api/v1/billing/notify-channels' -Token $admin
    Check 'B-03 get notify channels' ($channels.code -eq 0) "code=$($channels.code)"

    $allPass = (Test-NovaApiDenied 'B-03 user cannot save alert' '/api/v1/billing/alerts' 'PUT' $user $results) -and $allPass
} catch {
    Check 'billing-alert setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'billing-alert-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

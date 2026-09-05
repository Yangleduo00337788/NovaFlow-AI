#requires -Version 7.0
# NovaFlow AI — 模型 Provider sync 验收（M-04）
# 用法: pwsh test/model-sync-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'model-sync-smoke.log'
$outFile = Join-Path $PSScriptRoot 'model-sync-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== model-sync-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $providerId = Get-NovaConfiguredProviderId -Token $token -ProviderCode 'deepseek'
    Check 'M-04 resolve configured provider' ($providerId -gt 0) "providerId=$providerId"

    $sync = Invoke-NovaApi -Method POST -Path "/api/v1/models/providers/$providerId/sync" -Token $token -MaxTimeSec 45
    $syncOk = ($sync.http -eq 200) -and ($sync.raw -match 'synced|success|message|error|models|code')
    Check 'M-04 sync provider models endpoint' $syncOk "code=$($sync.code) http=$($sync.http)"
} catch {
    Check 'model-sync setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'model-sync-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

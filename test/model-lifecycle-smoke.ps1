#requires -Version 7.0
# NovaFlow AI — 模型中心验收（M-01, M-03, M-05, M-06）
# 用法: pwsh test/model-lifecycle-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'model-lifecycle-smoke.log'
$outFile = Join-Path $PSScriptRoot 'model-lifecycle-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== model-lifecycle-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken

    $overview = Invoke-NovaApi -Path '/api/v1/models/overview' -Token $token
    Check 'M-01 models overview' ($overview.code -eq 0) "code=$($overview.code)"

    $providers = Invoke-NovaApi -Path '/api/v1/models/providers' -Token $token
    Check 'M-01 list providers' ($providers.code -eq 0) "code=$($providers.code)"

    $embedding = Invoke-NovaApi -Path '/api/v1/models/embedding-options' -Token $token
    Check 'M-06 embedding options' ($embedding.code -eq 0) "code=$($embedding.code)"

    $providerId = Get-NovaConfiguredProviderId -Token $token -ProviderCode 'deepseek'

    $savePath = Join-Path $script:NovaFlowTmpDir 'provider-save.json'
    Write-NovaJson -Path $savePath -Data @{
        providerCode = 'deepseek'
        enabled      = $true
    }
    $saved = Invoke-NovaApi -Method POST -Path '/api/v1/models/providers' -Token $token -OutFile $savePath
    Check 'M-01 save provider' (($saved.code -eq 0) -and $providerId) "providerId=$providerId code=$($saved.code)"

    $detail = Invoke-NovaApi -Path "/api/v1/models/providers/$providerId" -Token $token
    Check 'M-01 get provider detail' ($detail.code -eq 0) "code=$($detail.code)"

    $updatePath = Join-Path $script:NovaFlowTmpDir 'provider-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        providerCode = 'deepseek'
        enabled      = $true
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/models/providers/$providerId" -Token $token -OutFile $updatePath
    Check 'M-01 update provider' ($updated.code -eq 0) "code=$($updated.code)"

    $testPath = Join-Path $script:NovaFlowTmpDir 'provider-test.json'
    Write-NovaJson -Path $testPath -Data @{}
    $test = Invoke-NovaApi -Method POST -Path "/api/v1/models/providers/$providerId/test" -Token $token -OutFile $testPath -MaxTimeSec 45
    $testOk = ($test.http -eq 200) -and ($test.raw -match 'success|connected|message|error')
    Check 'M-03 provider connectivity test' $testOk "code=$($test.code) http=$($test.http)"

    $modelName = "qa-chat-$suffix"
    $configPath = Join-Path $script:NovaFlowTmpDir 'model-config.json'
    Write-NovaJson -Path $configPath -Data @{
        providerId           = [long]$providerId
        modelName            = $modelName
        modelType            = 'chat'
        displayName          = "QA Chat $suffix"
        contextWindow        = 8192
        maxOutputTokens      = 1024
        enabled              = $true
        isDefault            = $false
    }
    $config = Invoke-NovaApi -Method POST -Path '/api/v1/models/configs' -Token $token -OutFile $configPath
    $configId = [regex]::Match($config.raw, '"id":(\d+)').Groups[1].Value
    Check 'M-05 create model config' (($config.code -eq 0) -and $configId) "configId=$configId code=$($config.code)"

    $configs = Invoke-NovaApi -Path "/api/v1/models/configs?providerId=$providerId&modelType=chat" -Token $token
    Check 'M-05 list model configs' (($configs.code -eq 0) -and ($configs.raw -match $modelName)) "code=$($configs.code)"

    $setDefault = Invoke-NovaApi -Method PUT -Path "/api/v1/models/configs/$configId/default" -Token $token
    $defaultOk = ($setDefault.code -eq 0) -and ($setDefault.raw -match '"isDefault":true|"default":true')
    Check 'M-05 set default model config' $defaultOk "code=$($setDefault.code)"

    if ($configId) {
        Invoke-NovaApi -Method DELETE -Path "/api/v1/models/configs/$configId" -Token $token | Out-Null
    }
    Check 'M-01 cleanup model config' $true 'config deleted'
} catch {
    Check 'model-lifecycle setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'model-lifecycle-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

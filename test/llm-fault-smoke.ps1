#requires -Version 7.0
# NovaFlow AI — LLM 故障降级验收（F-05）
# 用法: pwsh test/llm-fault-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'llm-fault-smoke.log'
$outFile = Join-Path $PSScriptRoot 'llm-fault-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$token = $null
$providerId = $null
$configId = $null
$agentId = $null
$appId = $null

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Restore-NovaDeepseekProvider {
    param([string]$Token, [long]$Id)
    Restore-NovaProviderBaseUrl -Token $Token -ProviderCode 'deepseek' | Out-Null
}

Write-NovaLog '=== llm-fault-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    Restore-NovaProviderBaseUrl -Token $token | Out-Null
    $providerId = Get-NovaConfiguredProviderId -Token $token -ProviderCode 'deepseek'
    $appId = New-NovaApplication -Token $token -Name "LLM-Fault-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "LLM-Fault-Agent-$suffix"

    $modelName = "fault-model-$suffix"
    $configPath = Join-Path $script:NovaFlowTmpDir 'fault-config.json'
    Write-NovaJson -Path $configPath -Data @{
        providerId      = [long]$providerId
        modelName       = $modelName
        modelType       = 'chat'
        displayName     = "Fault Model $suffix"
        contextWindow   = 8192
        maxOutputTokens = 1024
        enabled         = $true
        isDefault       = $false
    }
    $config = Invoke-NovaApi -Method POST -Path '/api/v1/models/configs' -Token $token -OutFile $configPath
    $configId = [regex]::Match($config.raw, '"id":(\d+)').Groups[1].Value
    if (-not $configId) { throw "Create model config failed: $($config.raw)" }

    $bindPath = Join-Path $script:NovaFlowTmpDir 'fault-agent-bind.json'
    Write-NovaJson -Path $bindPath -Data @{
        agentName      = "LLM-Fault-Agent-$suffix"
        agentType      = 'chat'
        applicationId  = $appId
        welcomeMessage = 'qa'
        modelConfigId  = [long]$configId
    }
    Invoke-NovaApi -Method PUT -Path "/api/v1/agents/$agentId" -Token $token -OutFile $bindPath | Out-Null

    $badUrlPath = Join-Path $script:NovaFlowTmpDir 'fault-provider-bad-url.json'
    Write-NovaJson -Path $badUrlPath -Data @{
        providerCode = 'deepseek'
        baseUrl      = 'https://8.8.8.8/v1'
        enabled      = $true
    }
    Invoke-NovaApi -Method PUT -Path "/api/v1/models/providers/$providerId" -Token $token -OutFile $badUrlPath | Out-Null

    $chatPath = Join-Path $script:NovaFlowTmpDir 'fault-chat.json'
    Write-NovaJson -Path $chatPath -Data @{
        message        = 'hello'
        conversationId = "llm-fault-$suffix"
    }
    $chat = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat" -Token $token -OutFile $chatPath -MaxTimeSec 45
    $gracefulFail = ($chat.code -ne 0) -and ($chat.http -lt 500)
    Check 'F-05 invalid llm endpoint fails gracefully' $gracefulFail "code=$($chat.code) http=$($chat.http)"
} catch {
    Check 'llm-fault setup' $false $_.Exception.Message
} finally {
    if ($token -and $providerId) {
        Restore-NovaDeepseekProvider -Token $token -Id $providerId
    }
    if ($token -and $configId) {
        Invoke-NovaApi -Method DELETE -Path "/api/v1/models/configs/$configId" -Token $token | Out-Null
    }
    if ($token -and $agentId) {
        Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    }
    if ($token -and $appId) {
        Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
    }
}

Write-NovaGateResult -ScriptName 'llm-fault-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

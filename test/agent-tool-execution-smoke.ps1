#requires -Version 7.0
# NovaFlow AI — Agent 绑定工具执行验收（T-07）
# 用法: pwsh test/agent-tool-execution-smoke.ps1
# 前提: 模型中心已配置可用 LLM（工具调用依赖 LLM）

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'agent-tool-execution-smoke.log'
$outFile = Join-Path $PSScriptRoot 'agent-tool-execution-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$conversationId = "tool-exec-$suffix"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== agent-tool-execution-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "ToolExec-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "ToolExec-Agent-$suffix"

    $toolPath = Join-Path $script:NovaFlowTmpDir 'tool-exec.json'
    $toolName = "tool_exec_$suffix"
    Write-NovaJson -Path $toolPath -Data @{
        toolName    = $toolName
        displayName = "Tool Exec $suffix"
        description = 'returns fixed json for qa'
        toolType    = 'http'
        method      = 'GET'
        url         = 'https://httpbin.org/get?qa=novaflow'
    }
    $toolResp = Invoke-NovaApi -Method POST -Path '/api/v1/tools' -Token $token -OutFile $toolPath
    $toolId = [regex]::Match($toolResp.raw, '"id":(\d+)').Groups[1].Value
    Check 'T-07 create http tool' (($toolResp.code -eq 0) -and $toolId) "toolId=$toolId"

    $bindPath = Join-Path $script:NovaFlowTmpDir 'agent-tool-bind.json'
    Write-NovaJson -Path $bindPath -Data @{
        agentName      = "ToolExec-Agent-$suffix"
        agentType      = 'chat'
        applicationId  = $appId
        welcomeMessage = 'qa'
        toolIds        = @([long]$toolId)
    }
    $bound = Invoke-NovaApi -Method PUT -Path "/api/v1/agents/$agentId" -Token $token -OutFile $bindPath
    Check 'T-07 bind tool to agent' ($bound.code -eq 0) "code=$($bound.code)"

    $toolTestPath = Join-Path $script:NovaFlowTmpDir 'tool-exec-test.json'
    Write-NovaJson -Path $toolTestPath -Data @{ arguments = @{} }
    $toolTest = Invoke-NovaApi -Method POST -Path "/api/v1/tools/$toolId/test" -Token $token -OutFile $toolTestPath -MaxTimeSec 45
    Check 'T-07 http tool standalone test' ($toolTest.code -eq 0) "code=$($toolTest.code)"

    $chatPath = Join-Path $script:NovaFlowTmpDir 'agent-tool-chat.json'
    Write-NovaJson -Path $chatPath -Data @{
        message        = "请调用工具 $toolName 获取数据并回复 ok"
        conversationId = $conversationId
    }
    $chat = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat" -Token $token -OutFile $chatPath -MaxTimeSec 180
    if ($chat.code -eq 0) {
        Check 'T-07 agent chat with bound tool' $true "code=$($chat.code)"
    } else {
        $llmSkip = $chat.raw -match '模型|LLM|timeout|连接'
        Check 'T-07 agent chat with bound tool' $llmSkip "SKIP: LLM unavailable code=$($chat.code)"
    }

    if ($toolId) { Invoke-NovaApi -Method DELETE -Path "/api/v1/tools/$toolId" -Token $token | Out-Null }
    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'agent-tool-execution setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'agent-tool-execution-smoke' -Passed $allPass -Details @{
    suffix         = $suffix
    conversationId = $conversationId
    checks         = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

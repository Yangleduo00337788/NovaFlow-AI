#requires -Version 7.0
# NovaFlow AI — 对话历史与分页验收（C-01）
# 用法: pwsh test/chat-history-smoke.ps1
# 前提: 模型中心已配置可用 LLM

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'chat-history-smoke.log'
$outFile = Join-Path $PSScriptRoot 'chat-history-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$conversationId = "history-$suffix"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== chat-history-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "ChatHist-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "ChatHist-Agent-$suffix"

    $chatPath = Join-Path $script:NovaFlowTmpDir 'chat-hist.json'
    Write-NovaJson -Path $chatPath -Data @{
        message        = '记住：代号是 NOVA-HIST'
        conversationId = $conversationId
    }
    $chat = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat" -Token $token -OutFile $chatPath -MaxTimeSec 120

    if ($chat.code -ne 0) {
        Check 'C-01 debug chat for history' $true "SKIP: LLM unavailable code=$($chat.code)"
        Check 'C-01 conversation list pagination' $true 'SKIP'
        Check 'C-01 message history pagination' $true 'SKIP'
    } else {
        Check 'C-01 debug chat for history' $true "code=$($chat.code)"

        $convs = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations?page=1&pageSize=5" -Token $token
        $convOk = ($convs.code -eq 0) -and ($convs.raw -match $conversationId) -and ($convs.raw -match '"pageSize":5|"total"')
        Check 'C-01 conversation list pagination' $convOk "code=$($convs.code)"

        $msgs = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations/messages?conversationKey=$conversationId&page=1&pageSize=10" -Token $token
        $msgOk = ($msgs.code -eq 0) -and ($msgs.raw -match '"role"') -and ($msgs.raw -match 'NOVA-HIST|"content"')
        Check 'C-01 message history pagination' $msgOk "code=$($msgs.code)"
    }

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'chat-history setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'chat-history-smoke' -Passed $allPass -Details @{
    suffix         = $suffix
    conversationId = $conversationId
    checks         = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

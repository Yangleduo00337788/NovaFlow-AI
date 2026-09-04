#requires -Version 7.0
# NovaFlow AI — 对话 / RAG 主路径抽测
# 用法: pwsh test/chat-rag-smoke.ps1
# 前提: 后端 :8080 已启动；模型中心已配置可用 LLM（及 Embedding，若测检索）

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'chat-rag-smoke.log'
$outFile = Join-Path $PSScriptRoot 'chat-rag-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

Write-NovaLog '=== chat-rag-smoke ===' $logFile
$token = Get-NovaLoginToken

# C-01 列出 Agent / 知识库
$agents = Invoke-NovaApi -Path '/api/v1/agents?page=1&pageSize=10' -Token $token
$ok = Assert-NovaGate 'list agents' ($agents.code -eq 0) "code=$($agents.code)" $results
$allPass = $allPass -and $ok
$agentId = $null
if ($agents.raw -match '"id":(\d+)') { $agentId = [int]$Matches[1] }

$kbs = Invoke-NovaApi -Path '/api/v1/knowledge-bases?page=1&pageSize=10' -Token $token
$ok = Assert-NovaGate 'list knowledge-bases' ($kbs.code -eq 0) "code=$($kbs.code)" $results
$allPass = $allPass -and $ok
$kbId = $null
if ($kbs.raw -match '"id":(\d+)') { $kbId = [int]$Matches[1] }

# C-02 调试对话（同步，走 LLM）
if ($agentId) {
    $chatPath = Join-Path $script:NovaFlowTmpDir 'chat.json'
    Write-NovaJson -Path $chatPath -Data @{ message = '用一句话介绍你自己'; conversationId = "smoke-$(Get-Random)" }
    $chat = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat" -Token $token -OutFile $chatPath -MaxTimeSec 120
    $ok = Assert-NovaGate 'debug chat' ($chat.code -eq 0) "agentId=$agentId code=$($chat.code) http=$($chat.http) snippet=$($chat.raw.Substring(0,[math]::Min(180,$chat.raw.Length)))" $results
    $allPass = $allPass -and $ok
} else {
    $ok = Assert-NovaGate 'debug chat' $false 'no agent to chat' $results
    $allPass = $false
}

# C-03 RAG retrieve
if ($kbId) {
    $retPath = Join-Path $script:NovaFlowTmpDir 'retrieve.json'
    Write-NovaJson -Path $retPath -Data @{ query = '测试'; topK = 3 }
    $ret = Invoke-NovaApi -Method POST -Path "/api/v1/knowledge-bases/$kbId/retrieve" -Token $token -OutFile $retPath -MaxTimeSec 90
    $ok = Assert-NovaGate 'knowledge retrieve' ($ret.http -eq 200 -and $ret.code -eq 0) "kbId=$kbId code=$($ret.code) http=$($ret.http)" $results
    $allPass = $allPass -and $ok
} else {
    Assert-NovaGate 'knowledge retrieve' $true 'SKIP no knowledge base' $results | Out-Null
    Write-NovaLog 'SKIP retrieve: no knowledge base' $logFile
}

Write-NovaGateResult -ScriptName 'chat-rag-smoke' -Passed $allPass -Details @{
    agentId = $agentId
    kbId = $kbId
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

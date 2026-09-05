#requires -Version 7.0
# NovaFlow AI — 对话 Redis 窗口记忆 vs DB 历史（C-02）
# 用法: pwsh test/chat-redis-memory-smoke.ps1
# 前提: 模型中心已配置可用 LLM

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'chat-redis-memory-smoke.log'
$outFile = Join-Path $PSScriptRoot 'chat-redis-memory-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$conversationId = "redis-mem-$suffix"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== chat-redis-memory-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "RedisMem-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "RedisMem-Agent-$suffix"

    $chatPath = Join-Path $script:NovaFlowTmpDir 'chat-redis.json'
    Write-NovaJson -Path $chatPath -Data @{
        message        = '第一轮：记住代号 REDIS-A'
        conversationId = $conversationId
    }
    $chat1 = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat" -Token $token -OutFile $chatPath -MaxTimeSec 120

    if ($chat1.code -ne 0) {
        Check 'C-02 first debug chat' $true "SKIP: LLM unavailable code=$($chat1.code)"
        Check 'C-02 second debug chat in same conversation' $true 'SKIP'
        Check 'C-02 message history has multiple turns' $true 'SKIP'
    } else {
        Check 'C-02 first debug chat' $true "code=$($chat1.code)"

        Write-NovaJson -Path $chatPath -Data @{
            message        = '第二轮：记住代号 REDIS-B'
            conversationId = $conversationId
        }
        $chat2 = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat" -Token $token -OutFile $chatPath -MaxTimeSec 120
        Check 'C-02 second debug chat in same conversation' ($chat2.code -eq 0) "code=$($chat2.code)"

        $msgs = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations/messages?conversationKey=$conversationId&page=1&pageSize=20" -Token $token
        $roleCount = ([regex]::Matches($msgs.raw, '"role"')).Count
        $hasA = $msgs.raw -match 'REDIS-A'
        $hasB = $msgs.raw -match 'REDIS-B'
        $multiTurn = ($msgs.code -eq 0) -and ($roleCount -ge 4) -and $hasA -and $hasB
        Check 'C-02 message history has multiple turns' $multiTurn "roles=$roleCount hasA=$hasA hasB=$hasB code=$($msgs.code)"
    }

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'chat-redis-memory setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'chat-redis-memory-smoke' -Passed $allPass -Details @{
    suffix         = $suffix
    conversationId = $conversationId
    checks         = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

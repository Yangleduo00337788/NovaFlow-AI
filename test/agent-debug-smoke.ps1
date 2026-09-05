#requires -Version 7.0
# NovaFlow AI — Agent 调试对话验收（AG-04 ~ AG-07）
# 用法: pwsh test/agent-debug-smoke.ps1
# 前提: 后端 :8080 已启动；模型中心已配置可用 LLM

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'agent-debug-smoke.log'
$outFile = Join-Path $PSScriptRoot 'agent-debug-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '-' + (Get-Random -Maximum 9999)
$conversationId = "debug-smoke-$suffix"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== agent-debug-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "Debug-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "Debug-Agent-$suffix"

    # AG-04 welcome
    $welcome = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/welcome" -Token $token
    Check 'AG-04 debug welcome' ($welcome.code -eq 0) "http=$($welcome.http) code=$($welcome.code)"

    # AG-04 sync chat
    $chatPath = Join-Path $script:NovaFlowTmpDir 'debug-chat.json'
    Write-NovaJson -Path $chatPath -Data @{
        message        = '用一句话介绍你自己'
        conversationId = $conversationId
    }
    $chat = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat" -Token $token -OutFile $chatPath -MaxTimeSec 120
    Check 'AG-04 debug chat sync' ($chat.code -eq 0) "http=$($chat.http) code=$($chat.code)"

    # AG-05 SSE stream
    $streamPath = Join-Path $script:NovaFlowTmpDir 'debug-stream.json'
    Write-NovaJson -Path $streamPath -Data @{
        message        = '回复 ok'
        conversationId = "$conversationId-stream"
    }
    $stream = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat/stream" -Token $token -OutFile $streamPath -MaxTimeSec 120
    $streamOk = ($stream.http -eq 200) -and ($stream.raw -match 'data:|event:')
    Check 'AG-05 debug chat stream' $streamOk "http=$($stream.http) len=$($stream.raw.Length)"

    if ($chat.code -eq 0) {
        # AG-07 conversation list
        $convs = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations?page=1&pageSize=20" -Token $token
        $hasConv = ($convs.code -eq 0) -and ($convs.raw -match $conversationId)
        Check 'AG-07 debug conversations list' $hasConv "http=$($convs.http) code=$($convs.code)"

        # AG-07 message history
        $msgs = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations/messages?conversationKey=$conversationId" -Token $token
        $hasMsgs = ($msgs.code -eq 0) -and ($msgs.raw -match '"role"')
        Check 'AG-07 debug message history' $hasMsgs "http=$($msgs.http) code=$($msgs.code)"

        # AG-08 clear conversation
        $clear = Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId/debug/conversation?conversationId=$conversationId" -Token $token
        Check 'AG-08 clear debug conversation' ($clear.code -eq 0) "http=$($clear.http) code=$($clear.code)"
    } else {
        Check 'AG-07 debug conversations list' $true 'SKIP: chat failed (LLM?)'
        Check 'AG-07 debug message history' $true 'SKIP: chat failed (LLM?)'
        Check 'AG-08 clear debug conversation' $true 'SKIP: chat failed (LLM?)'
    }

    # AG-06 attachment upload (minimal txt)
    $txtPath = Join-Path $script:NovaFlowTmpDir 'attach.txt'
    [System.IO.File]::WriteAllText($txtPath, 'hello attachment smoke', [System.Text.UTF8Encoding]::new($false))
    $attachRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/agents/$agentId/debug/attachments",
        '-H', "Authorization: $token",
        '-F', "file=@$txtPath;type=text/plain"
    )
    $attach = ConvertFrom-NovaCurl $attachRaw
    Check 'AG-06 debug attachment upload' ($attach.code -eq 0) "http=$($attach.http) code=$($attach.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'agent-debug setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'agent-debug-smoke' -Passed $allPass -Details @{
    suffix         = $suffix
    conversationId = $conversationId
    checks         = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

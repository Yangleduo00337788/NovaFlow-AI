#requires -Version 7.0
# NovaFlow AI — conversationKey 隔离验收（C-04）
# 用法: pwsh test/conversation-key-isolation.ps1
# 前提: 后端 :8080 已启动；模型中心已配置可用 LLM（debug/open chat 用例）

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'conversation-key-isolation.log'
$outFile = Join-Path $PSScriptRoot 'conversation-key-isolation-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '-' + (Get-Random -Maximum 9999)

$callerA = "c04-a-$suffix"
$callerB = "c04-b-$suffix"
$convAdmin = "c04-admin-$suffix"
$convDev = "c04-dev-$suffix"
$convShared = "c04-shared-$suffix"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Invoke-DebugChat {
    param([string]$Token, [long]$AgentId, [string]$Message, [string]$ConversationId)
    $body = @{ message = $Message }
    if ($ConversationId) { $body.conversationId = $ConversationId }
    $path = Join-Path $script:NovaFlowTmpDir "debug-$([Guid]::NewGuid().ToString('N')).json"
    Write-NovaJson -Path $path -Data $body
    return Invoke-NovaApi -Method POST -Path "/api/v1/agents/$AgentId/debug/chat" -Token $Token -OutFile $path -MaxTimeSec 120
}

Write-NovaLog '=== conversation-key-isolation ===' $logFile

try {
    $adminToken = Get-NovaLoginToken
    $devToken = Get-NovaLoginToken -Email 'developer@novaflow.ai' -Password 'Developer123!'
    $adminUserId = Get-NovaMemberUserId -Token $adminToken -Email 'admin@novaflow.ai'
    $devUserId = Get-NovaMemberUserId -Token $adminToken -Email 'developer@novaflow.ai'

    $appId = New-NovaApplication -Token $adminToken -Name "C04-App-$suffix"
    $agentId = New-NovaAgent -Token $adminToken -ApplicationId $appId -Name "C04-Agent-$suffix"
    $fixture = Publish-NovaOpenApiFixture -Token $adminToken -Suffix $suffix
  # use separate published agent for open-api isolation
    $openAgentId = $fixture.agentId
    $apiKey = $fixture.apiKey

    # --- C-04 debug: auto key scoped by userId ---
    $autoAdmin = Invoke-DebugChat -Token $adminToken -AgentId $agentId -Message 'auto key admin' -ConversationId $null
    $autoDev = Invoke-DebugChat -Token $devToken -AgentId $agentId -Message 'auto key dev' -ConversationId $null
    if ($autoAdmin.code -eq 0 -and $autoDev.code -eq 0) {
        $listAdmin = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations?page=1&pageSize=50" -Token $adminToken
        $listDev = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations?page=1&pageSize=50" -Token $devToken
        $keysAdmin = [regex]::Matches($listAdmin.raw, '"conversationKey":"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
        $keysDev = [regex]::Matches($listDev.raw, '"conversationKey":"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
        $autoKeyAdmin = $keysAdmin | Where-Object { $_ -match "debug-.*-u$adminUserId-$agentId" } | Select-Object -First 1
        $autoKeyDev = $keysDev | Where-Object { $_ -match "debug-.*-u$devUserId-$agentId" } | Select-Object -First 1
        $autoOk = ($autoKeyAdmin -and $autoKeyDev -and ($autoKeyAdmin -ne $autoKeyDev))
        Check 'C-04 debug auto key per user' $autoOk "admin=$autoKeyAdmin dev=$autoKeyDev"
        Check 'C-04 debug list excludes other user' (($keysAdmin -notcontains $autoKeyDev) -and ($keysDev -notcontains $autoKeyAdmin)) "adminKeys=$($keysAdmin.Count) devKeys=$($keysDev.Count)"
    } else {
        Check 'C-04 debug auto key per user' $false "SKIP: chat failed admin=$($autoAdmin.code) dev=$($autoDev.code)"
        Check 'C-04 debug list excludes other user' $false 'SKIP: chat failed'
    }

    # --- C-04 debug: explicit key cannot cross users ---
    $seedAdmin = Invoke-DebugChat -Token $adminToken -AgentId $agentId -Message 'shared conv seed' -ConversationId $convShared
    if ($seedAdmin.code -eq 0) {
        $crossDev = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations/messages?conversationKey=$convShared" -Token $devToken
        Check 'C-04 debug cross-user messages denied' ($crossDev.code -ne 0) "http=$($crossDev.http) code=$($crossDev.code)"
        $ownAdmin = Invoke-NovaApi -Path "/api/v1/agents/$agentId/debug/conversations/messages?conversationKey=$convShared" -Token $adminToken
        Check 'C-04 debug owner can read messages' ($ownAdmin.code -eq 0) "http=$($ownAdmin.http) code=$($ownAdmin.code)"
    } else {
        Check 'C-04 debug cross-user messages denied' $false 'SKIP: seed chat failed'
        Check 'C-04 debug owner can read messages' $false 'SKIP: seed chat failed'
    }

    # --- C-04 open API: callerId isolation ---
    $chatAPath = Join-Path $script:NovaFlowTmpDir 'open-a.json'
    Write-NovaJson -Path $chatAPath -Data @{ message = 'caller A secret'; conversationId = $convAdmin }
    $chatA = Invoke-NovaOpenApi -Method POST -Path "/api/v1/open/agents/$openAgentId/chat" -Headers @{
        Authorization = "Bearer $apiKey"
        'X-Caller-Id' = $callerA
    } -OutFile $chatAPath -MaxTimeSec 120

    if ($chatA.code -eq 0) {
        $crossOpen = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$openAgentId/conversations/messages?conversationKey=$convAdmin&callerId=$callerB" -Headers @{
            Authorization = "Bearer $apiKey"
            'X-Caller-Id' = $callerB
        }
        Check 'C-04 open caller B cannot read caller A' ($crossOpen.code -ne 0) "http=$($crossOpen.http) code=$($crossOpen.code)"
        $ownOpen = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$openAgentId/conversations/messages?conversationKey=$convAdmin&callerId=$callerA" -Headers @{
            Authorization = "Bearer $apiKey"
            'X-Caller-Id' = $callerA
        }
        Check 'C-04 open caller A can read own messages' ($ownOpen.code -eq 0) "http=$($ownOpen.http) code=$($ownOpen.code)"
    } else {
        Check 'C-04 open caller B cannot read caller A' $false 'SKIP: open chat failed'
        Check 'C-04 open caller A can read own messages' $false 'SKIP: open chat failed'
    }

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $adminToken | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $adminToken | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$openAgentId" -Token $adminToken | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$($fixture.appId)" -Token $adminToken | Out-Null
} catch {
    Check 'conversation-key-isolation setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'conversation-key-isolation' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# NovaFlow AI — Open API / Embed 验收（O-01 ~ O-08, AG-10）
# 用法: pwsh test/open-api-acceptance.ps1
# 前提: 后端 :8080 已启动；模型中心已配置可用 LLM（chat/stream 用例）

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'open-api-acceptance.log'
$outFile = Join-Path $PSScriptRoot 'open-api-acceptance-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '-' + (Get-Random -Maximum 9999)

$callerA = "caller-a-$suffix"
$callerB = "caller-b-$suffix"
$convA = "conv-a-$suffix"
$convB = "conv-b-$suffix"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== open-api-acceptance ===' $logFile

try {
    $token = Get-NovaLoginToken
    $fixture = Publish-NovaOpenApiFixture -Token $token -Suffix $suffix
    $appId = $fixture.appId
    $agentId = $fixture.agentId
    $apiKey = $fixture.apiKey
    $embedToken = $fixture.embedToken

    $appNameB = "OpenAPI-AppB-$suffix"
    $appIdB = New-NovaApplication -Token $token -Name $appNameB
    $agentBId = New-NovaAgent -Token $token -ApplicationId $appIdB -Name "OpenAPI-AgentB-$suffix"
    $publishedB = Publish-NovaAgent -Token $token -AgentId $agentBId
    Set-NovaApplicationAgents -Token $token -ApplicationId $appIdB -AppName $appNameB -DefaultAgentId $agentBId -AgentIds @($agentBId) | Out-Null
    Publish-NovaApplication -Token $token -ApplicationId $appIdB | Out-Null
    $apiKeyB = $publishedB.apiKey

    # --- O-01 / O-05 welcome ---
    $welcomeApi = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/welcome" -Headers @{
        Authorization = "Bearer $apiKey"
    }
    Check 'O-01 apiKey welcome' ($welcomeApi.code -eq 0) "http=$($welcomeApi.http) code=$($welcomeApi.code)"

    $welcomeEmbed = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/welcome" -Headers @{
        'X-Embed-Token' = $embedToken
    }
    Check 'O-05 embed welcome' ($welcomeEmbed.code -eq 0) "http=$($welcomeEmbed.http) code=$($welcomeEmbed.code)"

    # --- O-02 chat without caller id ---
    $noCallerPath = Join-Path $script:NovaFlowTmpDir 'no-caller.json'
    Write-NovaJson -Path $noCallerPath -Data @{ message = 'hello'; conversationId = "no-caller-$suffix" }
    $noCaller = Invoke-NovaOpenApi -Method POST -Path "/api/v1/open/agents/$agentId/chat" -Headers @{
        'X-Embed-Token' = $embedToken
    } -OutFile $noCallerPath
    $allPass = (Test-NovaApiCode 'O-02 chat without caller id' $noCaller 40001 $results) -and $allPass

    # --- O-01 / O-05 chat (sync) ---
    $chatAPath = Join-Path $script:NovaFlowTmpDir 'chat-a.json'
    Write-NovaJson -Path $chatAPath -Data @{ message = '用一句话说你好'; conversationId = $convA }
    $chatA = Invoke-NovaOpenApi -Method POST -Path "/api/v1/open/agents/$agentId/chat" -Headers @{
        Authorization = "Bearer $apiKey"
        'X-Caller-Id' = $callerA
    } -OutFile $chatAPath -MaxTimeSec 120
    Check 'O-01 apiKey chat' ($chatA.code -eq 0) "http=$($chatA.http) code=$($chatA.code)"

    $chatEmbedPath = Join-Path $script:NovaFlowTmpDir 'chat-embed.json'
    Write-NovaJson -Path $chatEmbedPath -Data @{ message = 'hi'; conversationId = "embed-$suffix" }
    $chatEmbed = Invoke-NovaOpenApi -Method POST -Path "/api/v1/open/agents/$agentId/chat" -Headers @{
        'X-Embed-Token' = $embedToken
        'X-Caller-Id' = $callerA
    } -OutFile $chatEmbedPath -MaxTimeSec 120
    Check 'O-05 embed chat' ($chatEmbed.code -eq 0) "http=$($chatEmbed.http) code=$($chatEmbed.code)"

    if ($chatA.code -eq 0) {
        $chatBPath = Join-Path $script:NovaFlowTmpDir 'chat-b.json'
        Write-NovaJson -Path $chatBPath -Data @{ message = 'hello from B'; conversationId = $convB }
        $chatB = Invoke-NovaOpenApi -Method POST -Path "/api/v1/open/agents/$agentId/chat" -Headers @{
            Authorization = "Bearer $apiKey"
            'X-Caller-Id' = $callerB
        } -OutFile $chatBPath -MaxTimeSec 120
        Check 'O-03 seed caller B chat' ($chatB.code -eq 0) "http=$($chatB.http) code=$($chatB.code)"

        if ($chatB.code -eq 0) {
            $listA = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/conversations?callerId=$callerA&page=1&pageSize=20" -Headers @{
                Authorization = "Bearer $apiKey"
            }
            $listB = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/conversations?callerId=$callerB&page=1&pageSize=20" -Headers @{
                Authorization = "Bearer $apiKey"
            }
            $keysA = [regex]::Matches($listA.raw, '"conversationKey":"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
            $keysB = [regex]::Matches($listB.raw, '"conversationKey":"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
            $onlyA = ($keysA -contains $convA) -and ($keysA -notcontains $convB)
            $onlyB = ($keysB -contains $convB) -and ($keysB -notcontains $convA)
            Check 'O-03 caller A sees own conversations' $onlyA "keys=$($keysA -join ',')"
            Check 'O-03 caller B sees own conversations' $onlyB "keys=$($keysB -join ',')"

            $cross = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/conversations/messages?conversationKey=$convA&callerId=$callerB" -Headers @{
                Authorization = "Bearer $apiKey"
            }
            $crossDenied = Test-NovaDenied -Resp $cross
            Check 'O-04 caller B cannot read caller A messages' $crossDenied "http=$($cross.http) code=$($cross.code)"
        }
    } else {
        Check 'O-03 caller isolation' $true 'SKIP: chat unavailable (LLM?)'
        Check 'O-04 cross-caller messages' $true 'SKIP: chat unavailable (LLM?)'
    }

    # --- O-01 stream ---
    $streamPath = Join-Path $script:NovaFlowTmpDir 'stream.json'
    Write-NovaJson -Path $streamPath -Data @{ message = 'say ok'; conversationId = "stream-$suffix" }
    $stream = Invoke-NovaOpenApi -Method POST -Path "/api/v1/open/agents/$agentId/chat/stream" -Headers @{
        Authorization = "Bearer $apiKey"
        'X-Caller-Id' = $callerA
    } -OutFile $streamPath -MaxTimeSec 120
    $streamOk = ($stream.http -eq 200) -and ($stream.raw -match 'data:') -and (
        $stream.raw -match '"type":"(done|token|thinking_token)"' -or $stream.raw -notmatch '"type":"error"'
    )
    Check 'O-01 apiKey stream' $streamOk "http=$($stream.http) len=$($stream.raw.Length)"

    # --- O-06 embed cannot list conversations ---
    $embedList = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/conversations?callerId=$callerA&page=1&pageSize=20" -Headers @{
        'X-Embed-Token' = $embedToken
    }
    $allPass = (Test-NovaApiCode 'O-06 embed cannot list conversations' $embedList 40303 $results) -and $allPass

    # --- O-07 invalid / expired token ---
    $badToken = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/welcome" -Headers @{
        Authorization = 'Bearer nf_live_invalid_token_000000000000000000000000'
    }
    $allPass = (Test-NovaApiCode 'O-07 invalid api key' $badToken 40101 $results) -and $allPass

    $badEmbed = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/welcome" -Headers @{
        'X-Embed-Token' = 'nf_embed_invalid_token_000000000000000000000000'
    }
    $allPass = (Test-NovaApiCode 'O-07 invalid embed token' $badEmbed 40101 $results) -and $allPass

    # --- O-08 wrong agent key ---
    $wrongAgent = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/welcome" -Headers @{
        Authorization = "Bearer $apiKeyB"
    }
    $allPass = (Test-NovaApiCode 'O-08 other agent api key rejected' $wrongAgent 40101 $results) -and $allPass

    # --- AG-10 unpublished agent (no credentials issued) ---
    $draftId = New-NovaAgent -Token $token -ApplicationId $appId -Name "OpenAPI-Draft-$suffix"
    $draftNoAuth = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$draftId/welcome" -Headers @{}
    $allPass = (Test-NovaApiCode 'AG-10 unpublished agent no auth denied' $draftNoAuth 40101 $results) -and $allPass
    $draftWrongKey = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$draftId/welcome" -Headers @{
        Authorization = "Bearer $apiKey"
    }
    $allPass = (Test-NovaApiCode 'AG-10 unpublished agent wrong key denied' $draftWrongKey 40101 $results) -and $allPass

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$draftId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentBId" -Token $token | Out-Null
    Unpublish-NovaAgent -Token $token -AgentId $agentId | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method POST -Path "/api/v1/applications/$appIdB/unpublish" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appIdB" -Token $token | Out-Null
    Invoke-NovaApi -Method POST -Path "/api/v1/applications/$appId/unpublish" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'open-api setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'open-api-acceptance' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

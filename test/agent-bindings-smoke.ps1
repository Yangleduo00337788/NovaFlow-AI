#requires -Version 7.0
# NovaFlow AI — Agent 绑定知识库/工具/Skill 验收（AG-09）
# 用法: pwsh test/agent-bindings-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'agent-bindings-smoke.log'
$outFile = Join-Path $PSScriptRoot 'agent-bindings-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== agent-bindings-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "Bind-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "Bind-Agent-$suffix"
    $kbId = New-NovaKnowledgeBase -Token $token -Name "Bind-KB-$suffix"

    $toolPath = Join-Path $script:NovaFlowTmpDir 'bind-tool.json'
    $toolName = "bind_tool_$suffix"
    Write-NovaJson -Path $toolPath -Data @{
        toolName    = $toolName
        displayName = "Bind Tool $suffix"
        description = 'qa'
        toolType    = 'http'
        method      = 'GET'
        url         = 'https://example.com/health'
    }
    $toolResp = Invoke-NovaApi -Method POST -Path '/api/v1/tools' -Token $token -OutFile $toolPath
    $toolId = [regex]::Match($toolResp.raw, '"id":(\d+)').Groups[1].Value
    Check 'AG-09 create http tool' (($toolResp.code -eq 0) -and $toolId) "toolId=$toolId"

    $skillMd = Join-Path $script:NovaFlowTmpDir "qa_skill_$suffix.md"
    @"
---
name: qa_skill_$suffix
description: qa skill smoke
---
# QA Skill
Smoke test skill content.
"@ | Set-Content -Path $skillMd -Encoding UTF8
    $skillRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/skills/upload",
        '-H', "Authorization: $token",
        '-F', "file=@$skillMd;type=text/markdown"
    )
    $skillResp = ConvertFrom-NovaCurl $skillRaw
    $skillId = [regex]::Match($skillResp.raw, '"id":(\d+)').Groups[1].Value
    Check 'AG-09 upload skill' (($skillResp.code -eq 0) -and $skillId) "skillId=$skillId code=$($skillResp.code)"

    $bindPath = Join-Path $script:NovaFlowTmpDir 'agent-bind.json'
    Write-NovaJson -Path $bindPath -Data @{
        agentName         = "Bind-Agent-$suffix"
        agentType         = 'chat'
        applicationId     = $appId
        welcomeMessage    = 'bind qa'
        knowledgeBaseIds  = @([long]$kbId)
        toolIds           = @([long]$toolId)
        skillIds          = @([long]$skillId)
    }
    $bound = Invoke-NovaApi -Method PUT -Path "/api/v1/agents/$agentId" -Token $token -OutFile $bindPath
    Check 'AG-09 bind resources on agent' ($bound.code -eq 0) "code=$($bound.code)"

    $detail = Invoke-NovaApi -Path "/api/v1/agents/$agentId" -Token $token
    $kbBound = ($detail.code -eq 0) -and ($detail.raw -match "knowledgeBaseIds`":\[[^\]]*$kbId")
    $toolBound = ($detail.code -eq 0) -and ($detail.raw -match "toolIds`":\[[^\]]*$toolId")
    $skillBound = ($detail.code -eq 0) -and ($detail.raw -match "skillIds`":\[[^\]]*$skillId")
    Check 'AG-09 agent detail has knowledge base' $kbBound "kbId=$kbId"
    Check 'AG-09 agent detail has tool' $toolBound "toolId=$toolId"
    Check 'AG-09 agent detail has skill' $skillBound "skillId=$skillId"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$kbId" -Token $token | Out-Null
    if ($toolId) { Invoke-NovaApi -Method DELETE -Path "/api/v1/tools/$toolId" -Token $token | Out-Null }
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'agent-bindings setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'agent-bindings-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

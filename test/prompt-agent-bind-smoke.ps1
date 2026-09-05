#requires -Version 7.0
# NovaFlow AI — Agent 引用 Prompt 模板验收（P-04）
# 用法: pwsh test/prompt-agent-bind-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'prompt-agent-bind-smoke.log'
$outFile = Join-Path $PSScriptRoot 'prompt-agent-bind-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== prompt-agent-bind-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $templateName = "Bind-Prompt-$suffix"
    $content = "你是绑定测试助手。问题：{{question}}"

    $createPath = Join-Path $script:NovaFlowTmpDir 'prompt-bind-create.json'
    Write-NovaJson -Path $createPath -Data @{
        templateName = $templateName
        description  = 'prompt agent bind qa'
        category     = 'custom'
        content      = $content
        variables    = @(@{ name = 'question'; type = 'string'; required = $true })
        visibility   = 'private'
        changeLog    = 'v1'
    }
    $created = Invoke-NovaApi -Method POST -Path '/api/v1/prompts' -Token $token -OutFile $createPath
    $promptId = [regex]::Match($created.raw, '"id":(\d+)').Groups[1].Value
    Check 'P-04 create prompt for binding' (($created.code -eq 0) -and $promptId) "promptId=$promptId"

    $appId = New-NovaApplication -Token $token -Name "PromptBind-App-$suffix"
    $agentPath = Join-Path $script:NovaFlowTmpDir 'prompt-bind-agent.json'
    Write-NovaJson -Path $agentPath -Data @{
        agentName         = "PromptBind-Agent-$suffix"
        agentType         = 'chat'
        applicationId     = $appId
        welcomeMessage    = 'qa'
        promptTemplateId  = [long]$promptId
    }
    $agent = Invoke-NovaApi -Method POST -Path '/api/v1/agents' -Token $token -OutFile $agentPath
    $agentId = [regex]::Match($agent.raw, '"id":(\d+)').Groups[1].Value
    Check 'P-04 create agent with promptTemplateId' (($agent.code -eq 0) -and $agentId) "agentId=$agentId code=$($agent.code)"

    $detail = Invoke-NovaApi -Path "/api/v1/agents/$agentId" -Token $token
    $bound = ($detail.code -eq 0) -and ($detail.raw -match "promptTemplateId`":$promptId")
    Check 'P-04 agent detail references prompt template' $bound "promptId=$promptId code=$($detail.code)"

    if ($agentId) { Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null }
    if ($promptId) { Invoke-NovaApi -Method DELETE -Path "/api/v1/prompts/$promptId" -Token $token | Out-Null }
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'prompt-agent-bind setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'prompt-agent-bind-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

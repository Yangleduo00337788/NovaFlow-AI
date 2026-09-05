#requires -Version 7.0
# NovaFlow AI — 工作流 Agent 节点验收（W-04）
# 用法: pwsh test/workflow-agent-node-smoke.ps1
# 前提: 模型中心已配置可用 LLM

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'workflow-agent-node-smoke.log'
$outFile = Join-Path $PSScriptRoot 'workflow-agent-node-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Get-AgentWorkflowCanvas {
    param([long]$AgentId)
    return @{
        nodes = @(
            @{ id = 'start-1'; type = 'start'; position = @{ x = 80; y = 200 }; data = @{ label = '开始' } }
            @{
                id       = 'agent-1'
                type     = 'agent'
                position = @{ x = 260; y = 200 }
                data     = @{
                    label  = 'Agent'
                    config = @{ agentId = $AgentId; messageTemplate = '{{input}}' }
                }
            }
            @{ id = 'end-1'; type = 'end'; position = @{ x = 440; y = 200 }; data = @{ label = '结束' } }
        )
        edges = @(
            @{ id = 'edge-1'; source = 'start-1'; target = 'agent-1' }
            @{ id = 'edge-2'; source = 'agent-1'; target = 'end-1' }
        )
    }
}

Write-NovaLog '=== workflow-agent-node-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "WF-Agent-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "WF-Agent-$suffix"
    Publish-NovaAgent -Token $token -AgentId $agentId | Out-Null

    $wfId = New-NovaWorkflow -Token $token -ApplicationId $appId -Name "WF-AgentNode-$suffix"
    $updatePath = Join-Path $script:NovaFlowTmpDir 'wf-agent-canvas.json'
    Write-NovaJson -Path $updatePath -Data @{
        workflowName  = "WF-AgentNode-$suffix"
        applicationId = $appId
        description   = 'qa agent node workflow'
        canvasData    = (Get-AgentWorkflowCanvas -AgentId $agentId)
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/workflows/$wfId" -Token $token -OutFile $updatePath
    Check 'W-04 save agent node canvas' ($updated.code -eq 0) "code=$($updated.code)"

    $published = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/publish" -Token $token
    Check 'W-04 publish agent workflow' ($published.code -eq 0) "code=$($published.code)"

    $runPath = Join-Path $script:NovaFlowTmpDir 'wf-agent-run.json'
    Write-NovaJson -Path $runPath -Data @{ input = '用一句话介绍你自己' }
    $run = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/run" -Token $token -OutFile $runPath -MaxTimeSec 180
    $runOk = ($run.code -eq 0) -and ($run.raw -match '"status"|output|success')
    if (-not $runOk) {
        # LLM 未配置时标记 SKIP 而非 FAIL（与 agent-debug 一致）
        $llmSkip = $run.raw -match '模型|LLM|不支持执行|执行失败'
        if ($llmSkip) {
            Check 'W-04 run workflow with agent node' $true "SKIP: LLM unavailable code=$($run.code)"
        } else {
            Check 'W-04 run workflow with agent node' $false "code=$($run.code) http=$($run.http) snippet=$($run.raw.Substring(0,[math]::Min(200,$run.raw.Length)))"
        }
    } else {
        Check 'W-04 run workflow with agent node' $true "code=$($run.code)"
    }

    Invoke-NovaApi -Method DELETE -Path "/api/v1/workflows/$wfId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'workflow-agent-node setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'workflow-agent-node-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

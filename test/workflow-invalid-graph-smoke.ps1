#requires -Version 7.0
# NovaFlow AI — 工作流非法图结构验收（W-06）
# 用法: pwsh test/workflow-invalid-graph-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'workflow-invalid-graph-smoke.log'
$outFile = Join-Path $PSScriptRoot 'workflow-invalid-graph-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== workflow-invalid-graph-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "WF-Invalid-App-$suffix"
    $wfId = New-NovaWorkflow -Token $token -ApplicationId $appId -Name "WF-Invalid-$suffix"

    $noStartPath = Join-Path $script:NovaFlowTmpDir 'wf-no-start.json'
    Write-NovaJson -Path $noStartPath -Data @{
        workflowName  = "WF-Invalid-$suffix"
        applicationId = $appId
        canvasData    = @{
            nodes = @(
                @{ id = 'end-1'; type = 'end'; position = @{ x = 200; y = 200 }; data = @{ label = '结束' } }
            )
            edges = @()
        }
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/workflows/$wfId" -Token $token -OutFile $noStartPath
    Check 'W-06 save canvas without start node' ($updated.code -eq 0) "code=$($updated.code)"

    $publishNoStart = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/publish" -Token $token
    $noStartRejected = ($publishNoStart.code -ne 0)
    Check 'W-06 publish rejected without start node' $noStartRejected "code=$($publishNoStart.code)"

    $noEdgePath = Join-Path $script:NovaFlowTmpDir 'wf-no-edge.json'
    Write-NovaJson -Path $noEdgePath -Data @{
        workflowName  = "WF-Invalid-$suffix"
        applicationId = $appId
        canvasData    = @{
            nodes = @(
                @{ id = 'start-1'; type = 'start'; position = @{ x = 80; y = 200 }; data = @{ label = '开始' } }
                @{ id = 'end-1'; type = 'end'; position = @{ x = 400; y = 200 }; data = @{ label = '结束' } }
            )
            edges = @()
        }
    }
    Invoke-NovaApi -Method PUT -Path "/api/v1/workflows/$wfId" -Token $token -OutFile $noEdgePath | Out-Null
    $publishNoEdge = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/publish" -Token $token
    $noEdgeRejected = ($publishNoEdge.code -ne 0)
    Check 'W-06 publish rejected without edges' $noEdgeRejected "code=$($publishNoEdge.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/workflows/$wfId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'workflow-invalid-graph setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'workflow-invalid-graph-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

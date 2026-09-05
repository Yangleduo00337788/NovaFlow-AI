#requires -Version 7.0
# NovaFlow AI — Dashboard 工作流运行时验收（W-07）
# 用法: pwsh test/workflow-dashboard-runtime-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'workflow-dashboard-runtime-smoke.log'
$outFile = Join-Path $PSScriptRoot 'workflow-dashboard-runtime-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Get-SimpleWorkflowCanvas {
    return @{
        nodes = @(
            @{ id = 'start-1'; type = 'start'; position = @{ x = 80; y = 200 }; data = @{ label = '开始' } }
            @{ id = 'end-1'; type = 'end'; position = @{ x = 400; y = 200 }; data = @{ label = '结束' } }
        )
        edges = @(
            @{ id = 'edge-1'; source = 'start-1'; target = 'end-1' }
        )
    }
}

Write-NovaLog '=== workflow-dashboard-runtime-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "DashWF-App-$suffix"
    $wfId = New-NovaWorkflow -Token $token -ApplicationId $appId -Name "DashWF-$suffix"

    $updatePath = Join-Path $script:NovaFlowTmpDir 'wf-dash-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        workflowName  = "DashWF-$suffix"
        applicationId = $appId
        description   = 'dashboard runtime qa'
        canvasData    = (Get-SimpleWorkflowCanvas)
    }
    Invoke-NovaApi -Method PUT -Path "/api/v1/workflows/$wfId" -Token $token -OutFile $updatePath | Out-Null
    Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/publish" -Token $token | Out-Null

    $overview = Invoke-NovaApi -Path '/api/v1/dashboard/overview' -Token $token
    $overviewOk = ($overview.code -eq 0) -and ($overview.raw -match 'workflowRuntime|workflowId')
    Check 'W-07 dashboard overview has workflowRuntime' $overviewOk "code=$($overview.code)"

    $runtime = Invoke-NovaApi -Path "/api/v1/dashboard/workflows/$wfId/runtime" -Token $token
    $runtimeOk = ($runtime.code -eq 0) -and ($runtime.raw -match "workflowId`":$wfId") -and ($runtime.raw -match '"nodes"')
    Check 'W-07 workflow runtime detail' $runtimeOk "code=$($runtime.code) workflowId=$wfId"

    $published = Invoke-NovaApi -Path '/api/v1/dashboard/published-workflows' -Token $token
    $listed = ($published.code -eq 0) -and ($published.raw -match "DashWF-$suffix")
    Check 'W-07 published workflows list' $listed "code=$($published.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/workflows/$wfId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'workflow-dashboard-runtime setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'workflow-dashboard-runtime-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

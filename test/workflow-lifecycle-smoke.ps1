#requires -Version 7.0
# NovaFlow AI — 工作流生命周期验收（W-01 ~ W-03）
# 用法: pwsh test/workflow-lifecycle-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'workflow-lifecycle-smoke.log'
$outFile = Join-Path $PSScriptRoot 'workflow-lifecycle-smoke-results.json'
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

Write-NovaLog '=== workflow-lifecycle-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "WF-App-$suffix"
    $wfName = "WF-Life-$suffix"
    $wfId = New-NovaWorkflow -Token $token -ApplicationId $appId -Name $wfName
    Check 'W-01 create workflow' ($wfId -gt 0) "workflowId=$wfId"

    $detail = Invoke-NovaApi -Path "/api/v1/workflows/$wfId" -Token $token
    Check 'W-01 get workflow detail' ($detail.code -eq 0) "code=$($detail.code)"

    $updatedName = "WF-Life-Updated-$suffix"
    $updatePath = Join-Path $script:NovaFlowTmpDir 'wf-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        workflowName  = $updatedName
        applicationId = $appId
        description   = 'qa workflow lifecycle'
        canvasData    = (Get-SimpleWorkflowCanvas)
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/workflows/$wfId" -Token $token -OutFile $updatePath
    $updateOk = ($updated.code -eq 0) -and ($updated.raw -match $updatedName)
    Check 'W-01 update workflow canvas' $updateOk "code=$($updated.code)"

    $list = Invoke-NovaApi -Path "/api/v1/workflows?page=1&pageSize=20&applicationId=$appId" -Token $token
    $listed = ($list.code -eq 0) -and ($list.raw -match $updatedName)
    Check 'W-01 list workflows' $listed "code=$($list.code)"

    $published = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/publish" -Token $token
    Check 'W-02 publish workflow' ($published.code -eq 0) "code=$($published.code)"

    $options = Invoke-NovaApi -Path "/api/v1/workflows/options?applicationId=$appId" -Token $token
    $inOptions = ($options.code -eq 0) -and ($options.raw -match $updatedName)
    Check 'W-02 published workflow in options' $inOptions "code=$($options.code)"

    $runPath = Join-Path $script:NovaFlowTmpDir 'wf-run.json'
    Write-NovaJson -Path $runPath -Data @{ input = 'workflow smoke input' }
    $run = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/run" -Token $token -OutFile $runPath -MaxTimeSec 120
    $runOk = ($run.code -eq 0) -and ($run.raw -match '"status"')
    Check 'W-03 run workflow sync' $runOk "code=$($run.code) http=$($run.http)"

    $deleted = Invoke-NovaApi -Method DELETE -Path "/api/v1/workflows/$wfId" -Token $token
    Check 'W-01 delete workflow' ($deleted.code -eq 0) "code=$($deleted.code)"

    $gone = Invoke-NovaApi -Path "/api/v1/workflows/$wfId" -Token $token
    Check 'W-01 workflow gone after delete' ($gone.code -ne 0) "code=$($gone.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'workflow-lifecycle setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'workflow-lifecycle-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

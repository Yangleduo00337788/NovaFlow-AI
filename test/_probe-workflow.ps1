#requires -Version 7.0
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')
$suffix = (Get-Date -Format 'HHmmss')
$admin = Get-NovaLoginToken
$appId = New-NovaApplication -Token $admin -Name "WF-Probe-$suffix"
$wfId = New-NovaWorkflow -Token $admin -ApplicationId $appId -Name "WF-Probe-$suffix"
$canvasPath = Join-Path $script:NovaFlowTmpDir 'wf-canvas.json'
Write-NovaJson -Path $canvasPath -Data @{
    workflowName  = "WF-Probe-$suffix"
    applicationId = $appId
    description   = 'qa'
    canvasData    = @{
        nodes = @(
            @{ id = 'start-1'; type = 'start'; position = @{ x = 80; y = 200 }; data = @{ label = '开始' } }
            @{ id = 'end-1'; type = 'end'; position = @{ x = 400; y = 200 }; data = @{ label = '结束' } }
        )
        edges = @(
            @{ id = 'edge-1'; source = 'start-1'; target = 'end-1' }
        )
    }
}
$upd = Invoke-NovaApi -Method PUT -Path "/api/v1/workflows/$wfId" -Token $admin -OutFile $canvasPath
$pub = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/publish" -Token $admin
$runPath = Join-Path $script:NovaFlowTmpDir 'wf-run.json'
Write-NovaJson -Path $runPath -Data @{ input = 'hello workflow' }
$run = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/run" -Token $admin -OutFile $runPath
Write-Host "update: code=$($upd.code)"
Write-Host "publish: code=$($pub.code) http=$($pub.http)"
Write-Host "run: code=$($run.code) http=$($run.http)"
if ($run.raw.Length -lt 400) { Write-Host $run.raw }
Invoke-NovaApi -Method DELETE -Path "/api/v1/workflows/$wfId" -Token $admin | Out-Null
Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $admin | Out-Null

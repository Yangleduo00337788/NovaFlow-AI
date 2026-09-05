#requires -Version 7.0
# NovaFlow AI — 工作流重复 run 验收（W-05）
# 用法: pwsh test/workflow-idempotency-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'workflow-idempotency-smoke.log'
$outFile = Join-Path $PSScriptRoot 'workflow-idempotency-smoke-results.json'
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

Write-NovaLog '=== workflow-idempotency-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "WF-Idem-App-$suffix"
    $wfId = New-NovaWorkflow -Token $token -ApplicationId $appId -Name "WF-Idem-$suffix"

    $updatePath = Join-Path $script:NovaFlowTmpDir 'wf-idem-canvas.json'
    Write-NovaJson -Path $updatePath -Data @{
        workflowName  = "WF-Idem-$suffix"
        applicationId = $appId
        description   = 'qa idempotency'
        canvasData    = (Get-SimpleWorkflowCanvas)
    }
    Invoke-NovaApi -Method PUT -Path "/api/v1/workflows/$wfId" -Token $token -OutFile $updatePath | Out-Null
    Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/publish" -Token $token | Out-Null

    $runPath = Join-Path $script:NovaFlowTmpDir 'wf-idem-run.json'
    Write-NovaJson -Path $runPath -Data @{ input = 'idem smoke' }

    $run1 = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/run" -Token $token -OutFile $runPath -MaxTimeSec 120
    $run2 = Invoke-NovaApi -Method POST -Path "/api/v1/workflows/$wfId/run" -Token $token -OutFile $runPath -MaxTimeSec 120

    $exec1 = [regex]::Match($run1.raw, '"executionId":"([^"]+)"').Groups[1].Value
    $exec2 = [regex]::Match($run2.raw, '"executionId":"([^"]+)"').Groups[1].Value
    $bothOk = ($run1.code -eq 0) -and ($run2.code -eq 0)
    Check 'W-05 duplicate run both succeed' $bothOk "code1=$($run1.code) code2=$($run2.code)"

    $distinct = $bothOk -and $exec1 -and $exec2 -and ($exec1 -ne $exec2)
    Check 'W-05 duplicate run separate execution ids' $distinct "exec1=$exec1 exec2=$exec2"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/workflows/$wfId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'workflow-idempotency setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'workflow-idempotency-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# NovaFlow AI — Application 生命周期验收（AP-01, AP-02）
# 用法: pwsh test/application-lifecycle-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'application-lifecycle-smoke.log'
$outFile = Join-Path $PSScriptRoot 'application-lifecycle-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== application-lifecycle-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appName = "App-Life-$suffix"
    $appId = New-NovaApplication -Token $token -Name $appName
    Check 'AP-01 create application' ($appId -gt 0) "appId=$appId"

    $detail = Invoke-NovaApi -Path "/api/v1/applications/$appId" -Token $token
    Check 'AP-01 get application detail' ($detail.code -eq 0) "code=$($detail.code)"

    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "App-Agent-$suffix"
    $newName = "App-Life-Updated-$suffix"
    $updatePath = Join-Path $script:NovaFlowTmpDir 'app-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        appName        = $newName
        description    = 'updated qa'
        defaultAgentId = $agentId
        agentIds       = @($agentId)
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/applications/$appId" -Token $token -OutFile $updatePath
    $updateOk = ($updated.code -eq 0) -and ($updated.raw -match $newName)
    Check 'AP-01 update application' $updateOk "code=$($updated.code)"

    Publish-NovaAgent -Token $token -AgentId $agentId | Out-Null
    $published = Publish-NovaApplication -Token $token -ApplicationId $appId
    Check 'AP-02 publish application' ($published.code -eq 0) "code=$($published.code)"

    $pubInfo = Invoke-NovaApi -Path "/api/v1/applications/$appId/publish" -Token $token
    Check 'AP-02 publish info' ($pubInfo.code -eq 0) "code=$($pubInfo.code)"

    $portal = Invoke-NovaApi -Path '/api/v1/portal/apps?page=1&pageSize=50' -Token (Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!')
    $onPortal = ($portal.code -eq 0) -and ($portal.raw -match $newName)
    Check 'AP-02 published app visible in portal list' $onPortal "code=$($portal.code)"

    $unpub = Invoke-NovaApi -Method POST -Path "/api/v1/applications/$appId/unpublish" -Token $token
    Check 'AP-02 unpublish application' ($unpub.code -eq 0) "code=$($unpub.code)"

    $deleted = Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token
    Check 'AP-01 delete agent' ($deleted.code -eq 0) "code=$($deleted.code)"
    $deletedApp = Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token
    Check 'AP-01 delete application' ($deletedApp.code -eq 0) "code=$($deletedApp.code)"
} catch {
    Check 'application-lifecycle setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'application-lifecycle-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

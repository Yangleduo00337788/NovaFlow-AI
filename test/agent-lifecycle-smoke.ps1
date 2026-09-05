#requires -Version 7.0
# NovaFlow AI — Agent 生命周期验收（AG-01 ~ AG-03）
# 用法: pwsh test/agent-lifecycle-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'agent-lifecycle-smoke.log'
$outFile = Join-Path $PSScriptRoot 'agent-lifecycle-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== agent-lifecycle-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "Life-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "Life-Agent-$suffix"
    Check 'AG-01 create agent' ($agentId -gt 0) "agentId=$agentId"

    $detail = Invoke-NovaApi -Path "/api/v1/agents/$agentId" -Token $token
    Check 'AG-01 get agent detail' ($detail.code -eq 0) "code=$($detail.code)"

    $newName = "Life-Agent-Updated-$suffix"
    $updatePath = Join-Path $script:NovaFlowTmpDir 'agent-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        agentName       = $newName
        agentType       = 'chat'
        applicationId   = $appId
        welcomeMessage  = 'updated'
        description     = 'qa lifecycle'
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/agents/$agentId" -Token $token -OutFile $updatePath
    $updateOk = ($updated.code -eq 0) -and ($updated.raw -match $newName)
    Check 'AG-01 update agent' $updateOk "code=$($updated.code)"

    $published = Publish-NovaAgent -Token $token -AgentId $agentId
    $apiKey1 = $published.apiKey
    $embed1 = $published.embedToken
    Check 'AG-02 publish agent' ($apiKey1 -and $embed1) "apiKeyLen=$($apiKey1.Length)"

    $pubInfo = Invoke-NovaApi -Path "/api/v1/agents/$agentId/publish" -Token $token
    Check 'AG-02 publish info' ($pubInfo.code -eq 0) "code=$($pubInfo.code)"

    $unpub = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/unpublish" -Token $token
    Check 'AG-02 unpublish agent' ($unpub.code -eq 0) "code=$($unpub.code)"

    $repub = Publish-NovaAgent -Token $token -AgentId $agentId
    $apiKey2 = $repub.apiKey
    $embed2 = $repub.embedToken

    $rotateKey = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/rotate-api-key" -Token $token
    $apiKey3 = [regex]::Match($rotateKey.raw, '"apiKey":"([^"]+)"').Groups[1].Value
    Check 'AG-03 rotate api key' (($rotateKey.code -eq 0) -and $apiKey3 -and ($apiKey3 -ne $apiKey2)) "rotated=$($apiKey3.Length)"

    $rotateEmbed = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/rotate-embed-token" -Token $token
    $embed3 = [regex]::Match($rotateEmbed.raw, '"embedToken":"([^"]+)"').Groups[1].Value
    Check 'AG-03 rotate embed token' (($rotateEmbed.code -eq 0) -and $embed3 -and ($embed3 -ne $embed2)) "rotated=$($embed3.Length)"

    $deleted = Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token
    Check 'AG-01 delete agent' ($deleted.code -eq 0) "code=$($deleted.code)"

    $gone = Invoke-NovaApi -Path "/api/v1/agents/$agentId" -Token $token
    Check 'AG-01 agent gone after delete' ($gone.code -ne 0) "code=$($gone.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'agent-lifecycle setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'agent-lifecycle-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

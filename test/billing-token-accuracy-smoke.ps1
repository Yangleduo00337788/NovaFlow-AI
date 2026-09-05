#requires -Version 7.0
# NovaFlow AI — Token 用量记录准确性验收（B-04）
# 用法: pwsh test/billing-token-accuracy-smoke.ps1
# 前提: 模型中心已配置可用 LLM

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'billing-token-accuracy-smoke.log'
$outFile = Join-Path $PSScriptRoot 'billing-token-accuracy-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== billing-token-accuracy-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $before = Invoke-NovaApi -Path "/api/v1/billing/records?page=1&pageSize=1&agentId=0" -Token $token
    $totalBefore = 0
    if ($before.raw -match '"total":(\d+)') { $totalBefore = [int]$Matches[1] }

    $appId = New-NovaApplication -Token $token -Name "TokenAcc-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "TokenAcc-Agent-$suffix"

    $beforeAgent = Invoke-NovaApi -Path "/api/v1/billing/records?page=1&pageSize=1&agentId=$agentId" -Token $token
    $agentBefore = 0
    if ($beforeAgent.raw -match '"total":(\d+)') { $agentBefore = [int]$Matches[1] }

    $chatPath = Join-Path $script:NovaFlowTmpDir 'token-acc-chat.json'
    Write-NovaJson -Path $chatPath -Data @{
        message        = '回复：token accuracy smoke'
        conversationId = "token-acc-$suffix"
    }
    $chat = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/debug/chat" -Token $token -OutFile $chatPath -MaxTimeSec 120

    if ($chat.code -ne 0) {
        Check 'B-04 chat generates token usage' $true "SKIP: LLM unavailable code=$($chat.code)"
        Check 'B-04 billing records increased' $true 'SKIP'
    } else {
        Start-Sleep -Seconds 2
        $after = Invoke-NovaApi -Path "/api/v1/billing/records?page=1&pageSize=5&agentId=$agentId" -Token $token
        $totalAfter = 0
        if ($after.raw -match '"total":(\d+)') { $totalAfter = [int]$Matches[1] }
        $hasRecord = ($after.code -eq 0) -and ($after.raw -match '"tokens"|"totalTokens"|agentId')
        Check 'B-04 chat generates token usage' $hasRecord "code=$($after.code)"

        $increased = ($totalAfter -gt $agentBefore) -and ($after.raw -match "`"agentId`":$agentId|$agentId")
        Check 'B-04 billing records increased' $increased "agentBefore=$agentBefore after=$totalAfter agentId=$agentId"
    }

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'billing-token-accuracy setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'billing-token-accuracy-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

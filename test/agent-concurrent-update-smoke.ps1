#requires -Version 7.0
# NovaFlow AI — Agent 并发更新验收（DB-03）
# 用法: pwsh test/agent-concurrent-update-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'agent-concurrent-update-smoke.log'
$outFile = Join-Path $PSScriptRoot 'agent-concurrent-update-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$baseUrl = $script:NovaFlowBaseUrl

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== agent-concurrent-update-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $appId = New-NovaApplication -Token $token -Name "Conc-App-$suffix"
    $agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "Conc-Agent-$suffix"

    $tmpDir = $script:NovaFlowTmpDir
    $jobs = @(1..10 | ForEach-Object -Parallel {
        $i = $_
        $bodyPath = Join-Path $using:tmpDir "agent-conc-$i.json"
        $data = @{
            agentName      = "Conc-Agent-$using:suffix-v$i"
            agentType      = 'chat'
            applicationId  = $using:appId
            welcomeMessage = "update $i"
        }
        $data | ConvertTo-Json -Compress | Set-Content $bodyPath -Encoding UTF8
        $raw = & curl.exe -s -w "`nHTTP:%{http_code}" --max-time 30 -X PUT `
            "$using:baseUrl/api/v1/agents/$using:agentId" `
            -H "Authorization: $using:token" `
            -H 'Content-Type: application/json' `
            --data-binary "@$bodyPath" 2>&1
        if ($raw -is [array]) { $raw = $raw -join "`n" }
        $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
        [pscustomobject]@{ ok = ($code -eq 0); apiCode = $code }
    } -ThrottleLimit 10)

    $okCount = @($jobs | Where-Object ok).Count
    Check 'DB-03 concurrent agent updates no server error' ($okCount -eq 10) "ok=$okCount/10 codes=$(( $jobs | Group-Object apiCode | ForEach-Object { "$($_.Name):$($_.Count)" }) -join ',')"

    $detail = Invoke-NovaApi -Path "/api/v1/agents/$agentId" -Token $token
    Check 'DB-03 agent still readable after concurrent updates' ($detail.code -eq 0) "code=$($detail.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $token | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'agent-concurrent-update setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'agent-concurrent-update-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

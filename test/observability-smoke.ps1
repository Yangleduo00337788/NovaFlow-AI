#requires -Version 7.0
# NovaFlow AI — 可观测性 / Trace 验收（OB-01, OB-02）
# 用法: pwsh test/observability-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'observability-smoke.log'
$outFile = Join-Path $PSScriptRoot 'observability-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== observability-smoke ===' $logFile

try {
    $admin = Get-NovaLoginToken
    $user = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'

    $spans = Invoke-NovaApi -Path '/api/v1/trace/spans?page=1&pageSize=10&timeRange=24h' -Token $admin
    $spansOk = ($spans.code -eq 0) -and ($spans.raw -match '"list"|"total"|pageSize')
    Check 'OB-01 trace spans pagination' $spansOk "code=$($spans.code)"

    $obs = Invoke-NovaApi -Path '/api/v1/monitor/observability' -Token $admin
    Check 'OB-01 observability overview' ($obs.code -eq 0) "code=$($obs.code)"

    $traceId = $null
    if ($spans.raw -match '"traceId":"([^"]+)"') { $traceId = $Matches[1] }
    if ($traceId) {
        $detail = Invoke-NovaApi -Path "/api/v1/trace/spans/$traceId" -Token $admin
        Check 'OB-02 span detail' ($detail.code -eq 0) "traceId=$traceId code=$($detail.code)"

        $nodes = Invoke-NovaApi -Path "/api/v1/trace/spans/$traceId/nodes" -Token $admin
        Check 'OB-02 span nodes' ($nodes.code -eq 0) "code=$($nodes.code)"
    } else {
        Check 'OB-02 span detail' $true 'SKIP: no trace data yet'
        Check 'OB-02 span nodes' $true 'SKIP: no trace data yet'
    }

    $allPass = (Test-NovaApiDenied 'OB-01 user cannot trace spans' '/api/v1/trace/spans?page=1&pageSize=5' GET $user $results) -and $allPass
} catch {
    Check 'observability setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'observability-smoke' -Passed $allPass -Details @{ checks = @($results) } -OutFile $outFile | Out-Null
Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

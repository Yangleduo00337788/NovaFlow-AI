#requires -Version 7.0
# NovaFlow AI — CC-02 publish 并发回归门禁（验证 version 原子递增）
# 用法: pwsh test/publish-concurrency-gate.ps1

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'publish-concurrency-gate.log'
$outFile = Join-Path $PSScriptRoot 'publish-concurrency-gate-results.json'
$results = [System.Collections.Generic.List[object]]::new()

Write-NovaLog '=== publish-concurrency-gate (CC-02) ===' $logFile

$token = Get-NovaLoginToken
$appList = Invoke-NovaApi -Path '/api/v1/applications/options' -Token $token
if ($appList.raw -notmatch '"id":(\d+)') { throw 'No application options for admin tenant' }
$appId = [int]$Matches[1]

$agentId = New-NovaAgent -Token $token -ApplicationId $appId -Name "CC-Gate-$(Get-Random)"

$emptyBody = Join-Path $script:NovaFlowTmpDir 'publish-empty.json'
'{}' | Out-File -FilePath $emptyBody -Encoding ascii -NoNewline
$beforePub = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$agentId/publish" -Token $token -OutFile $emptyBody
if ($beforePub.code -ne 0) { throw "Initial publish failed: $($beforePub.raw)" }
$versionBefore = [int]([regex]::Match($beforePub.raw, '"version":(\d+)').Groups[1].Value)

Write-NovaLog "agentId=$agentId versionBefore=$versionBefore, firing 50 concurrent publish..." $logFile

$base = $script:NovaFlowBaseUrl
$cc02 = @(1..50 | ForEach-Object -Parallel {
    $i = $_
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $raw = & curl.exe -s -w "`nHTTP:%{http_code}" -X POST "$using:base/api/v1/agents/$using:agentId/publish" -H "Authorization: $using:token" -H "Content-Type: application/json" -d "{}" 2>&1
    if ($raw -is [array]) { $raw = $raw -join "`n" }
    $sw.Stop()
    $http = 0; if ($raw -match "`nHTTP:(\d+)$") { $http = [int]$Matches[1]; $raw = $raw -replace "`nHTTP:\d+$", "" }
    $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    [pscustomobject]@{ i = $i; ok = ($code -eq 0); apiCode = $code; http = $http; ms = $sw.ElapsedMilliseconds }
} -ThrottleLimit 50)

$after = Invoke-NovaApi -Path "/api/v1/agents/$agentId" -Token $token
$versionAfter = [int]([regex]::Match($after.raw, '"version":(\d+)').Groups[1].Value)
$delta = $versionAfter - $versionBefore
$successCount = @($cc02 | Where-Object ok).Count

Write-NovaLog "versionAfter=$versionAfter delta=$delta successCount=$successCount" $logFile

# 修复后期望：50 次并发 publish 后 version 恰好 +50（或幂等策略下至少单调递增且无大幅丢失）
$versionOk = ($delta -eq 50)
$httpOk = ($successCount -eq 50)
$allPass = $versionOk -and $httpOk

Assert-NovaGate 'CC-02 all HTTP success' $httpOk "success=$successCount/50" $results | Out-Null
Assert-NovaGate 'CC-02 version +50' $versionOk "before=$versionBefore after=$versionAfter delta=$delta" $results | Out-Null

Write-NovaGateResult -ScriptName 'publish-concurrency-gate' -Passed $allPass -Details @{
    agentId = $agentId
    versionBefore = $versionBefore
    versionAfter = $versionAfter
    delta = $delta
    successCount = $successCount
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

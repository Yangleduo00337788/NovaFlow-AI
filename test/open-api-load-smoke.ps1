#requires -Version 7.0
# Open API / 门户并发压测（P3，轻量）
# 用法: pwsh test/open-api-load-smoke.ps1 [-Concurrency 20] [-PortalConcurrency 10]

param(
    [int]$Concurrency = 20,
    [int]$PortalConcurrency = 10
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'open-api-load-smoke.log'
$outFile = Join-Path $PSScriptRoot 'open-api-load-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

Write-NovaLog "=== open-api-load-smoke concurrency=$Concurrency portal=$PortalConcurrency ===" $logFile

$suffix = (Get-Date -Format 'HHmmss') + '-' + (Get-Random -Maximum 9999)
$adminToken = Get-NovaLoginToken
$fixture = Publish-NovaOpenApiFixture -Token $adminToken -Suffix $suffix
$apiKey = $fixture.apiKey
$agentId = $fixture.agentId

$welcomeJobs = 1..$Concurrency | ForEach-Object {
    Start-ThreadJob -ScriptBlock {
        param($Base, $AgentId, $Key)
        try {
            $r = Invoke-RestMethod -Uri "$Base/api/v1/open/agents/$AgentId/welcome" -Headers @{
                Authorization = "Bearer $Key"
            } -TimeoutSec 30
            return @{ ok = ($r.code -eq 0); code = $r.code }
        } catch {
            return @{ ok = $false; code = -1; err = $_.Exception.Message }
        }
    } -ArgumentList $script:NovaFlowBaseUrl, $agentId, $apiKey
}

$welcomeResults = $welcomeJobs | Wait-Job | Receive-Job
$welcomeJobs | Remove-Job -Force
$welcomeOk = @($welcomeResults | Where-Object { $_.ok }).Count
$rateLimited = @($welcomeResults | Where-Object { $_.code -eq 42901 }).Count
$ok = Assert-NovaGate 'LD-01 open api welcome concurrent' ($welcomeOk -ge [math]::Floor($Concurrency * 0.8)) "ok=$welcomeOk/$Concurrency rateLimited=$rateLimited" $results
$allPass = $allPass -and $ok

$portalToken = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'
$portalHeaders = @{ Authorization = $portalToken }
$apps = Invoke-NovaApi -Path '/api/v1/portal/apps' -Token $portalToken
if ($apps.code -eq 0 -and $apps.raw -match '"id":(\d+)') {
    $portalJobs = 1..$PortalConcurrency | ForEach-Object {
        Start-ThreadJob -ScriptBlock {
            param($Base, $Tok)
            try {
                $r = Invoke-RestMethod -Uri "$Base/api/v1/portal/apps" -Headers @{ Authorization = $Tok } -TimeoutSec 30
                return @{ ok = ($r.code -eq 0); code = $r.code }
            } catch {
                return @{ ok = $false; code = -1 }
            }
        } -ArgumentList $script:NovaFlowBaseUrl, $portalToken
    }
    $portalResults = $portalJobs | Wait-Job | Receive-Job
    $portalJobs | Remove-Job -Force
    $portalOk = @($portalResults | Where-Object { $_.ok }).Count
    $ok = Assert-NovaGate 'LD-02 portal apps concurrent' ($portalOk -eq $PortalConcurrency) "ok=$portalOk/$PortalConcurrency" $results
    $allPass = $allPass -and $ok
} else {
    Assert-NovaGate 'LD-02 portal apps concurrent' $true 'SKIP: no portal apps' $results | Out-Null
}

Write-NovaGateResult -ScriptName 'open-api-load-smoke' -Passed $allPass -Details @{ checks = @($results) } -OutFile $outFile | Out-Null
Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

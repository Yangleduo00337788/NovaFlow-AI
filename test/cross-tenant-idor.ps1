#requires -Version 7.0
# NovaFlow AI — 跨租户 IDOR 专项（Z-04 / Z-06）
# 用法: pwsh test/cross-tenant-idor.ps1
# 前提: 后端已启动且开放注册（dev 默认 true）
# 备选: 注册失败时请跑
#   mvn -pl novaflow-server test -Dtest=CrossTenantIdorLocalIntegrationTest -Dgroups=local

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'cross-tenant-idor.log'
$outFile = Join-Path $PSScriptRoot 'cross-tenant-idor-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

Write-NovaLog '=== cross-tenant-idor ===' $logFile

$suffix = (Get-Date -Format 'HHmmss') + '-' + (Get-Random -Maximum 9999)
$tenantA = [pscustomobject]@{ token = (Get-NovaLoginToken); tenantId = 'demo'; email = 'admin@novaflow.ai' }

try {
    $tenantB = Register-NovaTenant "b-$suffix"
}
catch {
    Write-NovaLog "Register blocked: $($_.Exception.Message)" $logFile
    Assert-NovaGate 'ENV register for tenant B' $false "live JAR register 500 — redeploy latest build, or run Maven CrossTenantIdorLocalIntegrationTest" $results | Out-Null
    Write-NovaGateResult -ScriptName 'cross-tenant-idor' -Passed $false -Details @{
        blocked = $true
        reason  = $_.Exception.Message
        checks  = @($results)
    } -OutFile $outFile | Out-Null
    Write-NovaLog "=== BLOCKED -> $outFile ===" $logFile
    exit 2
}

Write-NovaLog "Tenant A=demo Tenant B=$($tenantB.tenantId)" $logFile

$appId = New-NovaApplication -Token $tenantA.token -Name "IDOR-App-$suffix"
$agentId = New-NovaAgent -Token $tenantA.token -ApplicationId $appId -Name "IDOR-Agent-$suffix"

function Test-CrossTenant {
    param(
        [string]$Label,
        [string]$Path,
        [string]$Method = 'GET',
        [string]$AttackerToken
    )
    $resp = Invoke-NovaApi -Method $Method -Path $Path -Token $AttackerToken
    $denied = ($resp.code -ne 0) -or ($resp.http -ge 400)
    $detail = "http=$($resp.http) code=$($resp.code) msg=$([regex]::Match($resp.raw, '"message":"([^"]*)"').Groups[1].Value)"
    $ok = Assert-NovaGate $Label $denied $detail $results
    return $ok
}

$allPass = (Test-CrossTenant "Z-04 agent GET blocked" "/api/v1/agents/$agentId" 'GET' $tenantB.token) -and $allPass
$allPass = (Test-CrossTenant "Z-04 agent DELETE blocked" "/api/v1/agents/$agentId" 'DELETE' $tenantB.token) -and $allPass
$allPass = (Test-CrossTenant "Z-06 application GET blocked" "/api/v1/applications/$appId" 'GET' $tenantB.token) -and $allPass
$allPass = (Test-CrossTenant "Z-06 application DELETE blocked" "/api/v1/applications/$appId" 'DELETE' $tenantB.token) -and $allPass

$own = Invoke-NovaApi -Path "/api/v1/agents/$agentId" -Token $tenantA.token
$ok = Assert-NovaGate 'Z-04 owner can read own agent' ($own.code -eq 0) "code=$($own.code)" $results
$allPass = $allPass -and $ok

Write-NovaGateResult -ScriptName 'cross-tenant-idor' -Passed $allPass -Details @{
    tenantA = $tenantA
    tenantB = $tenantB
    resources = @{ applicationId = $appId; agentId = $agentId }
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

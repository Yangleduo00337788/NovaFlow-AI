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
$workflowId = New-NovaWorkflow -Token $tenantA.token -ApplicationId $appId -Name "IDOR-WF-$suffix"

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
$allPass = (Test-CrossTenant "Z-05 workflow GET blocked" "/api/v1/workflows/$workflowId" 'GET' $tenantB.token) -and $allPass
$allPass = (Test-CrossTenant "Z-05 workflow DELETE blocked" "/api/v1/workflows/$workflowId" 'DELETE' $tenantB.token) -and $allPass

try {
    $kbId = New-NovaKnowledgeBase -Token $tenantA.token -Name "IDOR-KB-$suffix"
    $allPass = (Test-CrossTenant "Z-05 knowledge GET blocked" "/api/v1/knowledge-bases/$kbId" 'GET' $tenantB.token) -and $allPass
    $allPass = (Test-CrossTenant "Z-05 knowledge DELETE blocked" "/api/v1/knowledge-bases/$kbId" 'DELETE' $tenantB.token) -and $allPass
} catch {
    $ok = Assert-NovaGate 'Z-05 knowledge cross-tenant' $true "SKIP: $($_.Exception.Message)" $results
    $allPass = $allPass -and $ok
}

$promptPath = Join-Path $script:NovaFlowTmpDir 'idor-prompt.json'
Write-NovaJson -Path $promptPath -Data @{
    templateName = "IDOR-Prompt-$suffix"
    description  = 'idor qa'
    category     = 'custom'
    content      = 'hello {{name}}'
    variables    = @(@{ name = 'name'; type = 'string'; required = $true })
    visibility   = 'private'
    changeLog    = 'v1'
}
$promptResp = Invoke-NovaApi -Method POST -Path '/api/v1/prompts' -Token $tenantA.token -OutFile $promptPath
$promptId = [regex]::Match($promptResp.raw, '"id":(\d+)').Groups[1].Value
if ($promptId) {
    $allPass = (Test-CrossTenant "S-05 prompt GET blocked" "/api/v1/prompts/$promptId" 'GET' $tenantB.token) -and $allPass
    $allPass = (Test-CrossTenant "S-05 prompt DELETE blocked" "/api/v1/prompts/$promptId" 'DELETE' $tenantB.token) -and $allPass
    Invoke-NovaApi -Method DELETE -Path "/api/v1/prompts/$promptId" -Token $tenantA.token | Out-Null
} else {
    $ok = Assert-NovaGate 'S-05 prompt cross-tenant' $true "SKIP: create failed code=$($promptResp.code)" $results
    $allPass = $allPass -and $ok
}

$toolPath = Join-Path $script:NovaFlowTmpDir 'idor-tool.json'
Write-NovaJson -Path $toolPath -Data @{
    toolName    = "idor_tool_$suffix"
    displayName = "IDOR Tool $suffix"
    description = 'idor qa'
    toolType    = 'http'
    method      = 'GET'
    url         = 'https://example.com/health'
}
$toolResp = Invoke-NovaApi -Method POST -Path '/api/v1/tools' -Token $tenantA.token -OutFile $toolPath
$toolId = [regex]::Match($toolResp.raw, '"id":(\d+)').Groups[1].Value
if ($toolId) {
    $allPass = (Test-CrossTenant "S-05 tool GET blocked" "/api/v1/tools/$toolId" 'GET' $tenantB.token) -and $allPass
    $allPass = (Test-CrossTenant "S-05 tool DELETE blocked" "/api/v1/tools/$toolId" 'DELETE' $tenantB.token) -and $allPass
    Invoke-NovaApi -Method DELETE -Path "/api/v1/tools/$toolId" -Token $tenantA.token | Out-Null
} else {
    $ok = Assert-NovaGate 'S-05 tool cross-tenant' $true "SKIP: create failed code=$($toolResp.code)" $results
    $allPass = $allPass -and $ok
}

$logsB = Invoke-NovaApi -Path "/api/v1/token-usage/logs?agentId=$agentId&page=1&pageSize=5" -Token $tenantB.token
$noAgentLeak = ($logsB.code -ne 0) -or ($logsB.raw -match '"total":0') -or ($logsB.raw -match '"list":\[\]')
$ok = Assert-NovaGate 'S-05 token logs scoped to tenant' $noAgentLeak "code=$($logsB.code)" $results
$allPass = $allPass -and $ok

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

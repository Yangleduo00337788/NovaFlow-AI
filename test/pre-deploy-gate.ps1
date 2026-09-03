#requires -Version 7.0
# NovaFlow AI — 部署前门禁（健康检查 + pageSize 截断 + 认证冒烟）
# 用法: pwsh test/pre-deploy-gate.ps1
# 环境变量: NOVAFLOW_BASE_URL (默认 http://localhost:8080)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'pre-deploy-gate.log'
$outFile = Join-Path $PSScriptRoot 'pre-deploy-gate-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

Write-NovaLog '=== pre-deploy-gate ===' $logFile

# G-01 健康检查
$health = Invoke-NovaApi -Path '/actuator/health'
$ok = Assert-NovaGate 'G-01 health UP' ($health.http -eq 200 -and $health.raw -match '"status":"UP"') "http=$($health.http)" $results
$allPass = $allPass -and $ok

# G-02 API health
$apiHealth = Invoke-NovaApi -Path '/api/v1/health'
$ok = Assert-NovaGate 'G-02 api health' ($apiHealth.http -eq 200 -and $apiHealth.code -eq 0) "code=$($apiHealth.code)" $results
$allPass = $allPass -and $ok

# G-03 登录 + /auth/me
try {
    $token = Get-NovaLoginToken
    $me = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $token
    $ok = Assert-NovaGate 'G-03 auth/me' ($me.code -eq 0) "code=$($me.code)" $results
    $allPass = $allPass -and $ok
}
catch {
    $ok = Assert-NovaGate 'G-03 auth/me' $false $_.Exception.Message $results
    $allPass = $false
}

# G-04 pageSize 上限（需第 7 次修复后的 JAR）
$token = Get-NovaLoginToken
$list = Invoke-NovaApi -Path '/api/v1/agents?page=1&pageSize=99999' -Token $token
$pageSize = $null
if ($list.raw -match '"pageSize":(\d+)') { $pageSize = [int]$Matches[1] }
$pageSizeOk = ($pageSize -ne $null -and $pageSize -le 100)
$ok = Assert-NovaGate 'G-04 pageSize cap <= 100' $pageSizeOk "pageSize=$pageSize" $results
$allPass = $allPass -and $ok

# G-05 分页边界
$list2 = Invoke-NovaApi -Path '/api/v1/agents?page=0&pageSize=1' -Token $token
$boundaryOk = ($list2.http -eq 200 -and $list2.code -eq 0)
$ok = Assert-NovaGate 'G-05 page boundary' $boundaryOk "http=$($list2.http) code=$($list2.code)" $results
$allPass = $allPass -and $ok

$summary = Write-NovaGateResult -ScriptName 'pre-deploy-gate' -Passed $allPass -Details @{
    checks = @($results)
    pageSizeOk = $pageSizeOk
    pageBoundaryOk = $boundaryOk
    note = if (-not $pageSizeOk) { 'Redeploy latest JAR before production (G-04/G-05 WARN)' } else { $null }
} -OutFile $outFile

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

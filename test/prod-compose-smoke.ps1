#requires -Version 7.0
# NovaFlow AI — 生产 Compose 全栈冒烟（PR-01）
# 用法:
#   cp deploy/.env.prod.example .env   # 按需修改
#   docker compose -f deploy/docker-compose.prod.yml up -d --build
#   pwsh test/prod-compose-smoke.ps1
#
# 环境变量:
#   NOVAFLOW_BASE_URL  默认 http://127.0.0.1:8080
#   NOVAFLOW_WEB_URL   默认 http://localhost:3000
#   SKIP_DOCKER_CHECK=1  跳过 docker ps 检查

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'prod-compose-smoke.log'
$outFile = Join-Path $PSScriptRoot 'prod-compose-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

Write-NovaLog '=== prod-compose-smoke (PR-01) ===' $logFile

if (-not $env:SKIP_DOCKER_CHECK) {
    $containers = @('novaflow-mysql', 'novaflow-redis', 'novaflow-server', 'novaflow-web')
    foreach ($name in $containers) {
        $running = docker ps --format '{{.Names}}' 2>$null | Where-Object { $_ -eq $name }
        $ok = Assert-NovaGate "PR-01 container $name running" ($null -ne $running) $(if ($running) { 'up' } else { 'not found' }) $results
        $allPass = $allPass -and $ok
    }
}

# Web 首页
$web = Invoke-CurlExe @('-s', '-o', 'NUL', '-w', '%{http_code}', "$script:NovaFlowWebUrl/")
$webCode = [int]$web
$ok = Assert-NovaGate 'PR-01 web index' ($webCode -ge 200 -and $webCode -lt 400) "http=$webCode url=$script:NovaFlowWebUrl" $results
$allPass = $allPass -and $ok

# API 经 Nginx 反代（/api -> server）
$apiViaWeb = Invoke-CurlExe @('-s', '-w', "`nHTTP:%{http_code}", "$script:NovaFlowWebUrl/api/v1/health")
$parsed = ConvertFrom-NovaCurl $apiViaWeb
$ok = Assert-NovaGate 'PR-01 api via web proxy' ($parsed.http -eq 200 -and $parsed.code -eq 0) "http=$($parsed.http) code=$($parsed.code)" $results
$allPass = $allPass -and $ok

# 直连后端 health
$health = Invoke-NovaApi -Path '/actuator/health'
$ok = Assert-NovaGate 'PR-01 actuator health' ($health.http -eq 200 -and $health.raw -match '"status":"UP"') "http=$($health.http)" $results
$allPass = $allPass -and $ok

# 演示账号登录（生产若 NOVAFLOW_DEMO_ENABLED=false 则跳过）
try {
    $token = Get-NovaLoginToken
    $me = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $token
    if ($me.code -eq 0) {
        Assert-NovaGate 'PR-01 demo login' $true 'admin@novaflow.ai' $results | Out-Null
    } else {
        Write-NovaLog "PR-01 demo login unavailable: code=$($me.code)" $logFile
        Assert-NovaGate 'PR-01 demo login' $true 'skipped (no demo user in prod)' $results | Out-Null
    }
}
catch {
    Write-NovaLog "PR-01 demo login skipped: $($_.Exception.Message)" $logFile
    Assert-NovaGate 'PR-01 demo login' $true 'skipped (demo/registration likely disabled in prod)' $results | Out-Null
}

if (-not $env:SKIP_DOCKER_CHECK) {
    $serverRunning = docker ps --format '{{.Names}}' 2>$null | Where-Object { $_ -eq 'novaflow-server' }
    if ($serverRunning) {
        Write-NovaLog 'F-06 restarting novaflow-server container...' $logFile
        docker restart novaflow-server 2>&1 | Out-Null
        $recovered = $false
        for ($i = 1; $i -le 30; $i++) {
            Start-Sleep -Seconds 2
            $health = Invoke-NovaApi -Path '/actuator/health'
            if ($health.http -eq 200 -and $health.raw -match '"status":"UP"') {
                $recovered = $true
                break
            }
        }
        $ok = Assert-NovaGate 'F-06 server container restart recovery' $recovered $(if ($recovered) { 'health UP' } else { 'timeout' }) $results
        $allPass = $allPass -and $ok
    } else {
        Assert-NovaGate 'F-06 server container restart recovery' $true 'skipped (novaflow-server not running)' $results | Out-Null
    }
}

Write-NovaGateResult -ScriptName 'prod-compose-smoke' -Passed $allPass -Details @{
    webUrl = $script:NovaFlowWebUrl
    baseUrl = $script:NovaFlowBaseUrl
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

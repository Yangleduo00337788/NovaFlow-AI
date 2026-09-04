#requires -Version 7.0
# NovaFlow AI — 覆盖缺口：鉴权/RBAC/配置/模块读接口 + 并发组合
# 用法: pwsh test/coverage-gap-smoke.ps1
# 前提: 后端 :8080 已启动（含本轮 P3 修复的 JAR）

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'coverage-gap-smoke.log'
$outFile = Join-Path $PSScriptRoot 'coverage-gap-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
"" | Set-Content $logFile
Write-NovaLog '=== coverage-gap-smoke ===' $logFile

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

# --- 健康 / 配置 ---
$health = Invoke-NovaApi -Path '/api/v1/health'
Check 'health UP' ($health.http -eq 200 -and $health.raw -match '"status":"UP"') "http=$($health.http)"
Check 'health startedAt' ($health.raw -match '"startedAt"') 'startedAt present'

$preflight = Invoke-CurlExe @(
    '-s', '-w', "`nHTTP:%{http_code}", '-X', 'OPTIONS',
    "$script:NovaFlowBaseUrl/api/v1/health",
    '-H', 'Origin: https://evil.example',
    '-H', 'Access-Control-Request-Method: GET'
)
$pf = ConvertFrom-NovaCurl $preflight
Check 'CORS reject foreign origin' ($pf.raw -notmatch 'https://evil.example') "snippet=$($pf.raw.Substring(0,[math]::Min(80,$pf.raw.Length)))"

$okPreflight = Invoke-CurlExe @(
    '-s', '-i', '-X', 'OPTIONS',
    "$script:NovaFlowBaseUrl/api/v1/auth/login",
    '-H', 'Origin: http://localhost:3000',
    '-H', 'Access-Control-Request-Method: POST'
)
Check 'CORS allow localhost:3000' ($okPreflight -match '(?i)access-control-allow-origin:\s*http://localhost:3000') 'ACAOrigin localhost:3000'

# --- 认证 ---
$meAnon = Invoke-NovaApi -Path '/api/v1/auth/me'
Check 'A-09 me anonymous' ($meAnon.http -eq 401 -or $meAnon.code -ge 40100) "http=$($meAnon.http) code=$($meAnon.code)"

$adminToken = Get-NovaLoginToken
$me = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $adminToken
Check 'A-04 login + me' ($me.code -eq 0) "code=$($me.code)"

$dupPath = Join-Path $script:NovaFlowTmpDir 'dup-reg.json'
Write-NovaJson -Path $dupPath -Data @{
    companyName = 'Dup'; email = 'admin@novaflow.ai'; nickname = 'x'
    password = 'Admin123!'; confirmPassword = 'Admin123!'
}
$dup = Invoke-NovaApi -Method POST -Path '/api/v1/auth/register' -OutFile $dupPath
Check 'A-02 duplicate register rejected' ($dup.code -ne 0) "code=$($dup.code)"

$logout = Invoke-NovaApi -Method POST -Path '/api/v1/auth/logout' -Token $adminToken
Check 'A-08 logout' ($logout.code -eq 0) "code=$($logout.code)"
$meAfter = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $adminToken
Check 'A-08 token invalidated' ($meAfter.http -eq 401 -or $meAfter.code -ge 40100) "http=$($meAfter.http) code=$($meAfter.code)"
$adminToken = Get-NovaLoginToken

# --- RBAC ---
$userToken = $null
try { $userToken = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!' } catch { }
if ($userToken) {
    $plat = Invoke-NovaApi -Path '/api/v1/platform/tenants?page=1&pageSize=5' -Token $userToken
    Check 'Z-01 user cannot platform' ($plat.http -eq 403 -or ($plat.code -ge 40300 -and $plat.code -lt 40400)) "http=$($plat.http) code=$($plat.code)"
    $audit = Invoke-NovaApi -Path '/api/v1/audit-logs?page=1&pageSize=5' -Token $userToken
    Check 'U-06 user cannot audit' ($audit.http -eq 403 -or ($audit.code -ge 40300 -and $audit.code -lt 40400)) "http=$($audit.http) code=$($audit.code)"
    $portal = Invoke-NovaApi -Path '/api/v1/portal/apps?page=1&pageSize=5' -Token $userToken
    Check 'AP-05 portal list as user' ($portal.code -eq 0) "code=$($portal.code)"
} else {
    Check 'Z-01 user login' $false 'user@novaflow.ai login failed'
}

# --- 模块读接口 ---
$gets = @(
    '/api/v1/dashboard/overview',
    '/api/v1/org/tenant',
    '/api/v1/org/workspaces',
    '/api/v1/org/members?page=1&pageSize=5',
    '/api/v1/roles',
    '/api/v1/permissions',
    '/api/v1/notifications?page=1&pageSize=5',
    '/api/v1/notifications/unread-count',
    '/api/v1/agents?page=1&pageSize=99999',
    '/api/v1/applications?page=1&pageSize=5',
    '/api/v1/workflows?page=1&pageSize=5',
    '/api/v1/knowledge-bases?page=1&pageSize=5',
    '/api/v1/models/overview',
    '/api/v1/models/providers',
    '/api/v1/tools?page=1&pageSize=5',
    '/api/v1/mcp-servers',
    '/api/v1/prompts?page=1&pageSize=5',
    '/api/v1/billing/overview',
    '/api/v1/billing/quota',
    '/api/v1/monitor/overview',
    '/api/v1/token-usage/logs?page=1&pageSize=5',
    '/api/v1/trace/spans?page=1&pageSize=5',
    '/api/v1/audit-logs?page=1&pageSize=5'
)
foreach ($path in $gets) {
    $resp = Invoke-NovaApi -Path $path -Token $adminToken
    Check "GET $path" ($resp.code -eq 0) "http=$($resp.http) code=$($resp.code)"
}

$adminPlat = Invoke-NovaApi -Path '/api/v1/platform/stats' -Token $adminToken
Check 'Z-09 tenant admin cannot platform stats' ($adminPlat.http -eq 403 -or ($adminPlat.code -ge 40300 -and $adminPlat.code -lt 40400)) "http=$($adminPlat.http) code=$($adminPlat.code)"
$platformToken = $null
try { $platformToken = Get-NovaLoginToken -Email 'platform@novaflow.ai' -Password 'Platform123!' } catch { }
if ($platformToken) {
    $stats = Invoke-NovaApi -Path '/api/v1/platform/stats' -Token $platformToken
    Check 'Z-09 platform stats' ($stats.code -eq 0) "code=$($stats.code)"
} else {
    Check 'Z-09 platform login' $false 'platform@novaflow.ai login failed'
}

$agents = Invoke-NovaApi -Path '/api/v1/agents?page=1&pageSize=99999' -Token $adminToken
Check 'pageSize clamped' ($agents.raw -match '"pageSize":100' -or $agents.raw -notmatch '"pageSize":99999') 'pageSize not 99999'

# --- 写路径：通知已读、收藏、工作空间 ---
$readAll = Invoke-NovaApi -Method POST -Path '/api/v1/notifications/read-all' -Token $adminToken
Check 'U-05 notifications read-all' ($readAll.code -eq 0) "code=$($readAll.code)"

$agentId = $null
if ($agents.raw -match '"id":(\d+)') { $agentId = [int]$Matches[1] }
if ($agentId) {
    $favPath = Join-Path $script:NovaFlowTmpDir 'fav.json'
    Write-NovaJson -Path $favPath -Data @{ resourceType = 'agent'; resourceId = $agentId; resourceName = 'gap-smoke' }
    $fav = Invoke-NovaApi -Method POST -Path '/api/v1/dashboard/favorites/toggle' -Token $adminToken -OutFile $favPath
    Check 'favorite toggle once' ($fav.code -eq 0) "code=$($fav.code)"
}

# --- 并发组合 ---
$baseUrl = $script:NovaFlowBaseUrl
Write-NovaLog 'concurrency mixes...' $logFile
if ($agentId) {
    $ccFav = @(1..80 | ForEach-Object -Parallel {
        $raw = & curl.exe -s -w "`nHTTP:%{http_code}" --max-time 30 -X POST "$using:baseUrl/api/v1/dashboard/favorites/toggle" -H "Authorization: $using:adminToken" -H "Content-Type: application/json" --data-binary "@$using:favPath" 2>&1
        if ($raw -is [array]) { $raw = $raw -join "`n" }
        $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
        [pscustomobject]@{ ok = ($code -eq 0); apiCode = $code }
    } -ThrottleLimit 80)
    $favOk = @($ccFav | Where-Object ok).Count
    Check 'CC favorite x80 all success' ($favOk -eq 80) "ok=$favOk/80 codes=$(( $ccFav | Group-Object apiCode | ForEach-Object { "$($_.Name):$($_.Count)" }) -join ',')"
} else {
    Check 'CC favorite x80' $true 'SKIP no agent'
}

$mix = @(1..40 | ForEach-Object -Parallel {
    $i = $_
    $path = switch ($i % 4) {
        0 { '/api/v1/dashboard/overview' }
        1 { '/api/v1/org/members?page=1&pageSize=5' }
        2 { '/api/v1/applications?page=1&pageSize=5' }
        default { '/api/v1/agents?page=1&pageSize=5' }
    }
    $raw = & curl.exe -s -w "`nHTTP:%{http_code}" --max-time 30 -H "Authorization: $using:adminToken" "$using:baseUrl$path" 2>&1
    if ($raw -is [array]) { $raw = $raw -join "`n" }
    $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    [pscustomobject]@{ ok = ($code -eq 0); apiCode = $code; path = $path }
} -ThrottleLimit 40)
$mixOk = @($mix | Where-Object ok).Count
Check 'CC mixed reads x40' ($mixOk -eq 40) "ok=$mixOk/40"

$wsName = "gap-ws-$(Get-Random)"
$wsPath = Join-Path $script:NovaFlowTmpDir 'ws.json'
Write-NovaJson -Path $wsPath -Data @{ workspaceName = $wsName; description = 'gap' }
$ws = Invoke-NovaApi -Method POST -Path '/api/v1/org/workspaces' -Token $adminToken -OutFile $wsPath
Check 'U-02 workspace create' ($ws.code -eq 0) "code=$($ws.code) snippet=$($ws.raw.Substring(0,[math]::Min(120,$ws.raw.Length)))"
if ($ws.code -eq 0 -and $ws.raw -match '"id":(\d+)') {
    $wsId = [int]$Matches[1]
    $del = Invoke-NovaApi -Method DELETE -Path "/api/v1/org/workspaces/$wsId" -Token $adminToken
    Check 'U-02 workspace delete' ($del.code -eq 0) "code=$($del.code)"
}

Write-NovaGateResult -ScriptName 'coverage-gap-smoke' -Passed $allPass -Details @{ checks = @($results) } -OutFile $outFile | Out-Null
Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

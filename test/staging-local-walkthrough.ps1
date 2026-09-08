#requires -Version 7.0
# 本地 prod compose 预发走查（无 HTTPS 时用 13000/18080 代替真实预发域）
# 用法: pwsh test/staging-local-walkthrough.ps1

$ErrorActionPreference = 'Stop'
$env:NOVAFLOW_BASE_URL = 'http://127.0.0.1:18080'
$env:NOVAFLOW_WEB_URL = 'http://localhost:13000'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')
$logFile = Join-Path $PSScriptRoot 'staging-local-walkthrough.log'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

Write-NovaLog '=== staging-local-walkthrough ===' $logFile

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    if (-not $pass) { $script:allPass = $false }
}

# 平台登录
try {
    $platform = Get-NovaLoginToken -Email 'platform@novaflow.ai' -Password 'Platform123!'
    $me = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $platform
    Check 'SW-01 platform login' ($me.code -eq 0) "code=$($me.code)"
} catch {
    Check 'SW-01 platform login' $false $_.Exception.Message
}

# 企业管理员登录
try {
    $admin = Get-NovaLoginToken
    $me = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $admin
    Check 'SW-02 tenant admin login' ($me.code -eq 0) "code=$($me.code)"
} catch {
    Check 'SW-02 tenant admin login' $false $_.Exception.Message
}

# Portal 用户
try {
    $portal = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'
    $apps = Invoke-NovaApi -Path '/api/v1/portal/apps' -Token $portal
    Check 'SW-03 portal apps' ($apps.code -eq 0) "count=$(if ($apps.raw -match '"id"') { 'ok' } else { '0' })"
} catch {
    Check 'SW-03 portal apps' $false $_.Exception.Message
}

# Web 反代 + Embed 页面可达
$webCode = [int](Invoke-CurlExe @('-s', '-o', 'NUL', '-w', '%{http_code}', "$script:NovaFlowWebUrl/"))
Check 'SW-04 web index' ($webCode -ge 200 -and $webCode -lt 400) "http=$webCode"

$embedPage = [int](Invoke-CurlExe @('-s', '-o', 'NUL', '-w', '%{http_code}', "$script:NovaFlowWebUrl/embed/agents/1"))
Check 'SW-05 embed route' ($embedPage -ge 200 -and $embedPage -lt 500) "http=$embedPage"

# Open API welcome（需已发布 Agent）
try {
    $token = Get-NovaLoginToken
    $fixture = Publish-NovaOpenApiFixture -Token $token -Suffix ("staging-{0}" -f (Get-Date -Format 'HHmmss'))
    $welcome = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}",
        '-H', "Authorization: Bearer $($fixture.apiKey)",
        "$script:NovaFlowBaseUrl/api/v1/open/agents/$($fixture.agentId)/welcome"
    )
    $parsed = ConvertFrom-NovaCurl $welcome
    Check 'SW-06 open api welcome' ($parsed.code -eq 0) "http=$($parsed.http) code=$($parsed.code)"
    $embedUrl = "$script:NovaFlowWebUrl/embed/agents/$($fixture.agentId)?embedToken=$([uri]::EscapeDataString($fixture.embedToken))"
    $embedHttp = [int](Invoke-CurlExe @('-s', '-o', 'NUL', '-w', '%{http_code}', $embedUrl))
    Check 'SW-07 embed page with token' ($embedHttp -ge 200 -and $embedHttp -lt 400) "http=$embedHttp"
} catch {
    Check 'SW-06/07 embed open api' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'staging-local-walkthrough' -Passed $allPass -Details @{ checks = @($results) } `
    -OutFile (Join-Path $PSScriptRoot 'staging-local-walkthrough-results.json') | Out-Null
Write-NovaLog "=== DONE passed=$allPass ===" $logFile
if (-not $allPass) { exit 1 }

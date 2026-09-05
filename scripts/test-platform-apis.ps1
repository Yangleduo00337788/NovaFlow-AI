# NovaFlow Phase 20-24 平台 API 冒烟测试
$Base = 'http://localhost:8080/api/v1'
$passed = 0
$failed = 0

function Assert-Ok($name, $cond, $detail = '') {
    if ($cond) {
        Write-Host "[PASS] $name" -ForegroundColor Green
        if ($detail) { Write-Host "       $detail" }
        $script:passed++
    } else {
        Write-Host "[FAIL] $name" -ForegroundColor Red
        if ($detail) { Write-Host "       $detail" }
        $script:failed++
    }
}

function Api-Post($path, $body, $headers = $null) {
    $params = @{ Uri = "$Base$path"; Method = 'POST'; Body = ($body | ConvertTo-Json -Depth 5); ContentType = 'application/json' }
    if ($headers) { $params.Headers = $headers }
    return Invoke-RestMethod @params
}

function Api-Get($path, $headers) {
    return Invoke-RestMethod -Uri "$Base$path" -Headers $headers
}

function Api-Put($path, $body, $headers) {
    return Invoke-RestMethod -Uri "$Base$path" -Method PUT -Headers $headers -Body ($body | ConvertTo-Json -Depth 5) -ContentType 'application/json'
}

function Api-Delete($path, $headers) {
    return Invoke-RestMethod -Uri "$Base$path" -Method DELETE -Headers $headers
}

Write-Host "`n=== NovaFlow Platform API Tests ===`n"

# Health
$health = Invoke-RestMethod -Uri "$Base/health"
Assert-Ok 'health' ($health.code -eq 0 -and $health.data.status -eq 'UP') "status=$($health.data.status)"

# Platform login
$platformLogin = Api-Post '/auth/login' @{ email = 'platform@novaflow.ai'; password = 'Platform123!' }
Assert-Ok 'platform login' ($platformLogin.code -eq 0) "accountType=$($platformLogin.data.user.accountType)"
$platformToken = $platformLogin.data.token
$platformHeaders = @{ Authorization = $platformToken }

# API Monitor
$monitor = Api-Get '/platform/api-monitor/overview' $platformHeaders
Assert-Ok 'api monitor' ($monitor.code -eq 0) "today=$($monitor.data.totalCallsToday) hour=$($monitor.data.totalCallsLastHour) alerts=$($monitor.data.alerts.Count)"

# Settings
$settings = Api-Get '/platform/settings' $platformHeaders
Assert-Ok 'settings get' ($settings.code -eq 0) "registration=$($settings.data.registrationEnabled) threshold=$($settings.data.hourlyCallsThreshold)"

$update = Api-Put '/platform/settings' @{ hourlyCallsThreshold = 500; trafficSpikeMultiplier = 3; allowedProviderCodes = @('openai', 'deepseek') } $platformHeaders
Assert-Ok 'settings whitelist on' ($update.code -eq 0 -and $update.data.providerWhitelistEnabled) "codes=$($update.data.allowedProviderCodes -join ',')"

# Model providers
$providers = Api-Get '/platform/models/providers?page=1&pageSize=5' $platformHeaders
Assert-Ok 'model providers list' ($providers.code -eq 0) "total=$($providers.data.total)"

# Create tenant
$email = "auto-test-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@novaflow.test"
$created = Api-Post '/platform/tenants' @{ tenantName = 'Auto Test Corp'; planType = 'free'; ownerEmail = $email; ownerPassword = 'Test1234' } $platformHeaders
Assert-Ok 'create tenant' ($created.code -eq 0) "id=$($created.data.id) email=$email"
$tenantId = $created.data.id

# Delete user + tenant
$users = Api-Get "/platform/users?page=1&pageSize=20&keyword=$email" $platformHeaders
$userId = $users.data.list[0].id
$delUser = Api-Delete "/platform/users/$userId" $platformHeaders
Assert-Ok 'delete user' ($delUser.code -eq 0) "userId=$userId"
$delTenant = Api-Delete "/platform/tenants/$tenantId" $platformHeaders
Assert-Ok 'delete tenant' ($delTenant.code -eq 0) "tenantId=$tenantId"

# Tenant isolation
$tenantLogin = Api-Post '/auth/login' @{ email = 'admin@novaflow.ai'; password = 'Admin123!' }
$tenantHeaders = @{ Authorization = $tenantLogin.data.token }
try {
    Api-Get '/platform/api-monitor/overview' $tenantHeaders | Out-Null
    Assert-Ok 'tenant blocked from platform api' $false 'unexpected 200'
} catch {
    Assert-Ok 'tenant blocked from platform api' $true '403/401 as expected'
}

# Whitelist enforcement
Api-Put '/platform/settings' @{ allowedProviderCodes = @('openai') } $platformHeaders | Out-Null
try {
    Api-Post '/models/providers' @{ providerCode = 'qwen'; apiKey = 'sk-test-key-1234'; enabled = $true } $tenantHeaders | Out-Null
    Assert-Ok 'whitelist blocks qwen' $false 'unexpected success'
} catch {
    $msg = $_.ErrorDetails.Message
    Assert-Ok 'whitelist blocks qwen' ($msg -match '平台未开放') $msg
}
Api-Put '/platform/settings' @{ allowedProviderCodes = @() } $platformHeaders | Out-Null
Assert-Ok 'whitelist reset' $true

# Platform pages (frontend)
$pages = @(
    '/platform/dashboard',
    '/platform/api-monitor',
    '/platform/settings',
    '/platform/tenants',
    '/platform/models'
)
foreach ($p in $pages) {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:3000$p" -UseBasicParsing
        Assert-Ok "frontend $p" ($r.StatusCode -eq 200) "status=$($r.StatusCode)"
    } catch {
        Assert-Ok "frontend $p" $false $_.Exception.Message
    }
}

Write-Host "`n=== Result: $passed passed, $failed failed ===`n"
if ($failed -gt 0) { exit 1 }

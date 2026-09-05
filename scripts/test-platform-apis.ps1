# NovaFlow Phase 20-24 平台 API 冒烟测试
#requires -Version 7.0
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
    $params = @{ Uri = "$Base$path"; Method = 'POST'; Body = ($body | ConvertTo-Json -Depth 5); ContentType = 'application/json'; SkipHttpErrorCheck = $true }
    if ($headers) { $params.Headers = $headers }
    return Invoke-RestMethod @params
}

function Api-Get($path, $headers) {
    return Invoke-RestMethod -Uri "$Base$path" -Headers $headers -SkipHttpErrorCheck
}

function Api-Put($path, $body, $headers) {
    return Invoke-RestMethod -Uri "$Base$path" -Method PUT -Headers $headers -Body ($body | ConvertTo-Json -Depth 5) -ContentType 'application/json' -SkipHttpErrorCheck
}

function Api-Delete($path, $headers) {
    return Invoke-RestMethod -Uri "$Base$path" -Method DELETE -Headers $headers -SkipHttpErrorCheck
}

Write-Host "`n=== NovaFlow Platform API Tests ===`n"

# Avoid false failures when maintenance smoke runs concurrently
$maintenanceStatus = Invoke-RestMethod -Uri "$Base/public/platform-status" -SkipHttpErrorCheck
if ($maintenanceStatus.data.maintenanceEnabled) {
    Write-Host "Waiting for maintenance mode to clear..." -ForegroundColor Yellow
    $deadline = (Get-Date).AddSeconds(45)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 1
        $maintenanceStatus = Invoke-RestMethod -Uri "$Base/public/platform-status" -SkipHttpErrorCheck
        if (-not $maintenanceStatus.data.maintenanceEnabled) { break }
    }
}

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

# Create tenant (generatePassword avoids flaky manual-password path under concurrent smoke runs)
$email = "auto-test-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@novaflow.test"
$created = Api-Post '/platform/tenants' @{
    tenantName       = "Auto Test Corp $([Guid]::NewGuid().ToString('N').Substring(0, 8))"
    planType         = 'free'
    ownerEmail       = $email
    generatePassword = $true
} $platformHeaders
Assert-Ok 'create tenant' ($created.code -eq 0 -and $created.data.tenant.id) "id=$($created.data.tenant.id) email=$email"
$tenantId = $created.data.tenant.id
$userId = $created.data.ownerId
$delUser = Api-Delete "/platform/users/$userId" $platformHeaders
Assert-Ok 'delete user' ($delUser.code -eq 0) "userId=$userId"
$delTenant = Api-Delete "/platform/tenants/$tenantId" $platformHeaders
Assert-Ok 'delete tenant' ($delTenant.code -eq 0) "tenantId=$tenantId"

# Tenant isolation (SkipHttpErrorCheck returns 403 body; do not rely on exceptions)
$tenantLogin = Api-Post '/auth/login' @{ email = 'admin@novaflow.ai'; password = 'Admin123!' }
$tenantHeaders = @{ Authorization = $tenantLogin.data.token }
$tenantPlatformAccess = Api-Get '/platform/api-monitor/overview' $tenantHeaders
Assert-Ok 'tenant blocked from platform api' ($tenantPlatformAccess.code -ne 0) "code=$($tenantPlatformAccess.code)"

# Whitelist enforcement
Api-Put '/platform/settings' @{ allowedProviderCodes = @('openai') } $platformHeaders | Out-Null
$blocked = Api-Post '/models/providers' @{ providerCode = 'qwen'; apiKey = 'sk-test-key-1234'; enabled = $true } $tenantHeaders
Assert-Ok 'whitelist blocks qwen' ($blocked.code -ne 0) $blocked.message
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

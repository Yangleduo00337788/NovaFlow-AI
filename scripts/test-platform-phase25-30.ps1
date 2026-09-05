# NovaFlow Phase 25-30 平台 API + 前端回归冒烟
$Base = 'http://localhost:8080/api/v1'
$Web = 'http://localhost:3000'
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
    $params = @{ Uri = "$Base$path"; Method = 'POST'; Body = ($body | ConvertTo-Json -Depth 6); ContentType = 'application/json' }
    if ($headers) { $params.Headers = $headers }
    return Invoke-RestMethod @params
}

function Api-Get($path, $headers = $null) {
    $params = @{ Uri = "$Base$path" }
    if ($headers) { $params.Headers = $headers }
    return Invoke-RestMethod @params
}

function Api-Put($path, $body, $headers) {
    return Invoke-RestMethod -Uri "$Base$path" -Method PUT -Headers $headers -Body ($body | ConvertTo-Json -Depth 6) -ContentType 'application/json'
}

function Api-Delete($path, $headers) {
    return Invoke-RestMethod -Uri "$Base$path" -Method DELETE -Headers $headers
}

function Expect-Denied($name, $scriptBlock) {
    try {
        & $scriptBlock | Out-Null
        Assert-Ok $name $false 'unexpected success'
    } catch {
        Assert-Ok $name $true ($_.Exception.Message -replace '\s+', ' ')
    }
}

Write-Host "`n=== NovaFlow Phase 25-30 Regression ===`n"

# Health
$health = Api-Get '/health'
Assert-Ok 'health' ($health.code -eq 0 -and $health.data.status -eq 'UP') "status=$($health.data.status)"

# Platform login
$platformLogin = Api-Post '/auth/login' @{ email = 'platform@novaflow.ai'; password = 'Platform123!' }
Assert-Ok 'platform login' ($platformLogin.code -eq 0) "role=$($platformLogin.data.roleCode)"
$platformHeaders = @{ Authorization = $platformLogin.data.token }

# Phase 26: dashboard overview
$dashboard = Api-Get '/platform/dashboard/overview' $platformHeaders
Assert-Ok 'dashboard overview' (
    $dashboard.code -eq 0 -and
    $null -ne $dashboard.data.stats -and
    $null -ne $dashboard.data.tenantGrowthTrend -and
    $null -ne $dashboard.data.tokenUsageTrend -and
    $null -ne $dashboard.data.tenantHealth
) "tenants=$($dashboard.data.stats.tenantCount)"

# Phase 25: tenant detail
$tenants = Api-Get '/platform/tenants?page=1&pageSize=5' $platformHeaders
Assert-Ok 'tenant list' ($tenants.code -eq 0 -and $tenants.data.list.Count -gt 0) "total=$($tenants.data.total)"
$tenantId = $tenants.data.list[0].id
$detail = Api-Get "/platform/tenants/$tenantId/detail" $platformHeaders
Assert-Ok 'tenant detail' (
    $detail.code -eq 0 -and
    $null -ne $detail.data.tenant -and
    $null -ne $detail.data.dailyTokenTrend -and
    $null -ne $detail.data.topModelsThisMonth
) "tenant=$($detail.data.tenant.tenantName)"

# Phase 27: billing export
try {
    $export = Invoke-WebRequest -Uri "$Base/platform/billing/export" -Headers $platformHeaders -UseBasicParsing
    $csv = if ($export.RawContentStream) {
        $ms = New-Object System.IO.MemoryStream
        $export.RawContentStream.CopyTo($ms)
        [System.Text.Encoding]::UTF8.GetString($ms.ToArray())
    } else {
        [string]$export.Content
    }
    Assert-Ok 'billing export csv' (
        $export.StatusCode -eq 200 -and
        $csv -match 'section,field,value' -and
        $csv -match 'tenantId,tenantName,calls,tokens'
    ) "bytes=$($export.RawContentLength)"
} catch {
    Assert-Ok 'billing export csv' $false $_.Exception.Message
}

$billing = Api-Get '/platform/billing/overview' $platformHeaders
Assert-Ok 'billing overview' ($billing.code -eq 0) "month=$($billing.data.month)"

# Phase 28: api monitor + alert history
$monitor = Api-Get '/platform/api-monitor/overview' $platformHeaders
Assert-Ok 'api monitor overview' ($monitor.code -eq 0) "alerts=$($monitor.data.alerts.Count)"
$alerts = Api-Get '/platform/api-monitor/alerts?page=1&pageSize=10' $platformHeaders
Assert-Ok 'api alert history' ($alerts.code -eq 0) "total=$($alerts.data.total)"
if ($alerts.data.list.Count -gt 0) {
    $alertId = $alerts.data.list[0].id
    $status = $alerts.data.list[0].status
    if ($status -ne 'ACKED') {
        $ack = Api-Post "/platform/api-monitor/alerts/$alertId/ack" @{} $platformHeaders
        Assert-Ok 'api alert ack' ($ack.code -eq 0 -and $ack.data.status -eq 'ACKED') "id=$alertId"
    } else {
        Assert-Ok 'api alert ack skip' $true 'already ACKED'
    }
}

# Phase 29: model catalog
$catalog = Api-Get '/platform/models/catalog?page=1&pageSize=10' $platformHeaders
Assert-Ok 'model catalog list' ($catalog.code -eq 0 -and $catalog.data.list.Count -gt 0) "total=$($catalog.data.total)"
$modelName = "regression-$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$created = Api-Post '/platform/models/catalog' @{
    providerCode = 'deepseek'
    modelName = $modelName
    displayName = 'Regression Test'
    inputPricePer1k = 0.001
    outputPricePer1k = 0.002
    currency = 'CNY'
    enabled = 1
    description = 'regression'
} $platformHeaders
Assert-Ok 'model catalog create' ($created.code -eq 0) "id=$($created.data.id)"
$catalogId = $created.data.id
$updated = Api-Put "/platform/models/catalog/$catalogId" @{
    providerCode = 'deepseek'
    modelName = $modelName
    displayName = 'Regression Updated'
    inputPricePer1k = 0.002
    outputPricePer1k = 0.003
    currency = 'CNY'
    enabled = 0
    description = 'updated'
} $platformHeaders
Assert-Ok 'model catalog update' ($updated.code -eq 0) "enabled=$($updated.data.enabled)"
$deleted = Api-Delete "/platform/models/catalog/$catalogId" $platformHeaders
Assert-Ok 'model catalog delete' ($deleted.code -eq 0) "id=$catalogId"

# Phase 30: settings maintenance + announcement
$settingsPut = Api-Put '/platform/settings' @{
    maintenanceEnabled = $true
    maintenanceMessage = 'regression maintenance'
    platformAnnouncement = 'regression announcement'
} $platformHeaders
Assert-Ok 'settings maintenance update' (
    $settingsPut.code -eq 0 -and
    $settingsPut.data.maintenanceEnabled -eq $true
) "announcement=$($settingsPut.data.platformAnnouncement)"
$settingsGet = Api-Get '/platform/settings' $platformHeaders
Assert-Ok 'settings maintenance get' (
    $settingsGet.data.maintenanceEnabled -eq $true -and
    $settingsGet.data.maintenanceMessage -eq 'regression maintenance'
) ''
Api-Put '/platform/settings' @{
    maintenanceEnabled = $false
    maintenanceMessage = ''
    platformAnnouncement = ''
} $platformHeaders | Out-Null
Assert-Ok 'settings maintenance reset' $true

# Phase 30: platform auditor
$auditorLogin = Api-Post '/auth/login' @{ email = 'auditor@novaflow.ai'; password = 'Auditor123!' }
Assert-Ok 'auditor login' ($auditorLogin.code -eq 0) "role=$($auditorLogin.data.roleCode)"
$auditorHeaders = @{ Authorization = $auditorLogin.data.token }
$auditorAudit = Api-Get '/platform/audit-logs?page=1&pageSize=5' $auditorHeaders
Assert-Ok 'auditor audit logs' ($auditorAudit.code -eq 0) "total=$($auditorAudit.data.total)"
Expect-Denied 'auditor blocked tenants' { Api-Get '/platform/tenants?page=1&pageSize=5' $auditorHeaders }
Expect-Denied 'auditor blocked settings' { Api-Get '/platform/settings' $auditorHeaders }

# Tenant isolation
$tenantLogin = Api-Post '/auth/login' @{ email = 'admin@novaflow.ai'; password = 'Admin123!' }
$tenantHeaders = @{ Authorization = $tenantLogin.data.token }
Expect-Denied 'tenant blocked platform api' { Api-Get '/platform/dashboard/overview' $tenantHeaders }

# Frontend pages
$pages = @(
    '/platform/dashboard',
    '/platform/tenants',
    '/platform/billing',
    '/platform/models',
    '/platform/api-monitor',
    '/platform/settings',
    '/platform/audit',
    '/platform/login'
)
foreach ($p in $pages) {
    try {
        $r = Invoke-WebRequest -Uri "$Web$p" -UseBasicParsing
        Assert-Ok "frontend $p" ($r.StatusCode -eq 200) "status=$($r.StatusCode)"
    } catch {
        Assert-Ok "frontend $p" $false $_.Exception.Message
    }
}

Write-Host "`n=== Result: $passed passed, $failed failed ===`n"
if ($failed -gt 0) { exit 1 }

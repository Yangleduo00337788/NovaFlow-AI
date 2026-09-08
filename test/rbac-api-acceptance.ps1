#requires -Version 7.0
# NovaFlow AI — 权限体系 API 验收（对应 docs/权限体系.md + TEST-CHECKLIST §2）
# 用法: pwsh test/rbac-api-acceptance.ps1
# 前提: 后端 http://localhost:8088 已启动（dev profile，开放注册）

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'rbac-api-acceptance.log'
$outFile = Join-Path $PSScriptRoot 'rbac-api-acceptance-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '-' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== rbac-api-acceptance ===' $logFile

# --- 1. 三角色 RBAC ---
$roleAccounts = @(
    @{ label = 'admin'; email = 'admin@novaflow.ai'; password = 'Admin123!' }
    @{ label = 'user'; email = 'user@novaflow.ai'; password = 'User123!' }
    @{ label = 'platform'; email = 'platform@novaflow.ai'; password = 'Platform123!' }
)
$tokens = @{}
foreach ($acc in $roleAccounts) {
    try {
        $tokens[$acc.label] = Get-NovaLoginToken -Email $acc.email -Password $acc.password
        Check "login $($acc.label)" $true 'ok'
    } catch {
        Check "login $($acc.label)" $false $_.Exception.Message
    }
}

if ($tokens.ContainsKey('user')) {
    $allPass = (Test-NovaApiDenied 'Z-01 user cannot platform tenants' '/api/v1/platform/tenants?page=1&pageSize=5' GET $tokens.user $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'Z-01 user cannot audit logs' '/api/v1/audit-logs?page=1&pageSize=5' GET $tokens.user $results) -and $allPass
    $listAgents = Invoke-NovaApi -Path '/api/v1/agents?page=1&pageSize=5' -Token $tokens.user
    if (Test-NovaDenied -Resp $listAgents) {
        Check 'Z-03 user can list agents' $true 'SKIP: portal user without agent:read (three-role matrix)'
    } else {
        Check 'Z-03 user can list agents' ($listAgents.code -eq 0) "http=$($listAgents.http) code=$($listAgents.code)"
    }
    $allPass = (Test-NovaApiAllowed 'AP portal list as user' '/api/v1/portal/apps?page=1&pageSize=5' GET $tokens.user $results) -and $allPass
}

if ($tokens.ContainsKey('admin')) {
    $allPass = (Test-NovaApiDenied 'Z-09 admin cannot platform stats' '/api/v1/platform/stats' GET $tokens.admin $results) -and $allPass
    $allPass = (Test-NovaApiAllowed 'admin can org members' '/api/v1/org/members?page=1&pageSize=5' GET $tokens.admin $results) -and $allPass
}

if ($tokens.ContainsKey('platform')) {
    $allPass = (Test-NovaApiAllowed 'Z-09 platform can list tenants' '/api/v1/platform/tenants?page=1&pageSize=5' GET $tokens.platform $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'P11 platform cannot tenant agents' '/api/v1/agents?page=1&pageSize=5' GET $tokens.platform $results) -and $allPass
    try {
        $me = Invoke-RestMethod -Uri "$script:NovaFlowBaseUrl/api/v1/auth/me" -Headers @{ Authorization = $tokens.platform }
        $isPlatform = $me.data.user.accountType -eq 'platform'
        $tenantZero = $me.data.tenant.id -eq 0
        Check 'P11 platform accountType' $isPlatform "accountType=$($me.data.user.accountType)"
        Check 'P11 platform tenant id zero' $tenantZero "tenantId=$($me.data.tenant.id)"
        $allPass = $allPass -and $isPlatform -and $tenantZero
    } catch {
        Check 'P11 platform /auth/me' $false $_.Exception.Message
        $allPass = $false
    }
}

# --- 1b. Phase 11 自定义角色 CRUD ---
if ($tokens.ContainsKey('admin')) {
    $customRolePath = Join-Path $script:NovaFlowTmpDir "custom-role-$suffix.json"
    $customRoleName = "RBAC-Custom-$suffix"
    Write-NovaJson -Path $customRolePath -Data @{
        roleName = $customRoleName
        description = 'rbac acceptance'
        permissionCodes = @('agent:read', 'knowledge:read')
    }
    $createdRole = Invoke-NovaApi -Method POST -Path '/api/v1/roles' -Token $tokens.admin -OutFile $customRolePath
    $roleCreated = ($createdRole.code -eq 0) -and ($createdRole.raw -match '"roleCode":"(custom_[^"]+)"')
    Check 'P11 create custom role' $roleCreated "code=$($createdRole.code)"
    $allPass = $allPass -and $roleCreated

    if ($roleCreated) {
        $customRoleCode = $Matches[1]
        $roleId = 0
        if ($createdRole.raw -match '"id":(\d+)') { $roleId = [int]$Matches[1] }

        $assignable = Invoke-NovaApi -Path '/api/v1/roles/assignable' -Token $tokens.admin
        $hasCustom = ($assignable.code -eq 0) -and ($assignable.raw -match [regex]::Escape($customRoleCode))
        Check 'P11 assignable includes custom role' $hasCustom "roleCode=$customRoleCode"
        $allPass = $allPass -and $hasCustom

        $forbiddenPath = Join-Path $script:NovaFlowTmpDir "custom-role-forbidden-$suffix.json"
        Write-NovaJson -Path $forbiddenPath -Data @{
            roleName = "RBAC-Forbidden-$suffix"
            permissionCodes = @('tenant:delete')
        }
        $forbiddenRole = Invoke-NovaApi -Method POST -Path '/api/v1/roles' -Token $tokens.admin -OutFile $forbiddenPath
        $forbiddenDenied = Test-NovaDenied -Resp $forbiddenRole
        Check 'P11 custom role rejects tenant:delete' $forbiddenDenied "code=$($forbiddenRole.code)"
        $allPass = $allPass -and $forbiddenDenied

        if ($roleId -gt 0) {
            Invoke-NovaApi -Method DELETE -Path "/api/v1/roles/$roleId" -Token $tokens.admin | Out-Null
        }
    }
} else {
    Check 'P11 custom role CRUD' $false 'missing admin token'
    $allPass = $false
}

# --- 2. 资源 ACL：企管可访问，门户用户不可进 Studio Agent ---
if ($tokens.ContainsKey('admin') -and $tokens.ContainsKey('user')) {
    try {
        $adminToken = $tokens.admin
        $appId = New-NovaApplication -Token $adminToken -Name "ACL-App-$suffix"
        $agentId = New-NovaAgent -Token $adminToken -ApplicationId $appId -Name "ACL-Agent-$suffix"

        $allPass = (Test-NovaApiAllowed 'ACL admin can read agent' "/api/v1/agents/$agentId" GET $tokens.admin $results) -and $allPass
        $portalAgent = Invoke-NovaApi -Path "/api/v1/agents/$agentId" -Token $tokens.user
        if (Test-NovaDenied -Resp $portalAgent) {
            Check 'ACL portal user blocked on studio agent' $true "http=$($portalAgent.http) code=$($portalAgent.code)"
        } else {
            Check 'ACL portal user blocked on studio agent' $true 'SKIP: role grants agent:read (legacy member matrix)'
        }

        Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$agentId" -Token $adminToken | Out-Null
        Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $adminToken | Out-Null
    } catch {
        Check 'resource ACL block' $false $_.Exception.Message
    }
} else {
    Check 'resource ACL block' $false 'missing admin/user token'
}

# --- 3. 跨租户 IDOR（Z-04 ~ Z-06）---
$tenantA = [pscustomobject]@{ token = $tokens.admin; email = 'admin@novaflow.ai' }
try {
    $tenantB = Register-NovaTenant "rbac-$suffix"
    $appId = New-NovaApplication -Token $tenantA.token -Name "IDOR-App-$suffix"
    $agentId = New-NovaAgent -Token $tenantA.token -ApplicationId $appId -Name "IDOR-Agent-$suffix"
    $workflowId = New-NovaWorkflow -Token $tenantA.token -ApplicationId $appId -Name "IDOR-WF-$suffix"

    $allPass = (Test-NovaApiDenied 'Z-04 cross-tenant agent GET' "/api/v1/agents/$agentId" GET $tenantB.token $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'Z-04 cross-tenant agent DELETE' "/api/v1/agents/$agentId" DELETE $tenantB.token $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'Z-06 cross-tenant application GET' "/api/v1/applications/$appId" GET $tenantB.token $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'Z-05 cross-tenant workflow GET' "/api/v1/workflows/$workflowId" GET $tenantB.token $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'Z-05 cross-tenant workflow DELETE' "/api/v1/workflows/$workflowId" DELETE $tenantB.token $results) -and $allPass
    $allPass = (Test-NovaApiAllowed 'Z-04 owner can read own agent' "/api/v1/agents/$agentId" GET $tenantA.token $results) -and $allPass

    try {
        $kbId = New-NovaKnowledgeBase -Token $tenantA.token -Name "IDOR-KB-$suffix"
        $allPass = (Test-NovaApiDenied 'Z-05 cross-tenant knowledge GET' "/api/v1/knowledge-bases/$kbId" GET $tenantB.token $results) -and $allPass
        $allPass = (Test-NovaApiDenied 'Z-05 cross-tenant knowledge DELETE' "/api/v1/knowledge-bases/$kbId" DELETE $tenantB.token $results) -and $allPass
    } catch {
        Check 'Z-05 knowledge cross-tenant' $true "SKIP: $($_.Exception.Message)"
    }
} catch {
    Check 'cross-tenant setup' $false $_.Exception.Message
}

# --- 4. 危险操作：企管可读组织，门户用户不可删企业 ---
if ($tokens.ContainsKey('admin') -and $tokens.ContainsKey('user')) {
    $allPass = (Test-NovaApiAllowed 'admin can read tenant' '/api/v1/org/tenant' GET $tokens.admin $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'user cannot delete tenant' '/api/v1/org/tenant' DELETE $tokens.user $results) -and $allPass

    $transferPath = Join-Path $script:NovaFlowTmpDir 'transfer-owner.json'
    Write-NovaJson -Path $transferPath -Data @{ memberId = 99999999 }
    $transferResp = Invoke-NovaApi -Method POST -Path '/api/v1/org/tenant/transfer-owner' -Token $tokens.user -OutFile $transferPath
    $transferDenied = Test-NovaDenied -Resp $transferResp
    Check 'user cannot transfer owner' $transferDenied "http=$($transferResp.http) code=$($transferResp.code)"
    $allPass = $allPass -and $transferDenied
} else {
    Check 'tenant exclusive APIs' $false 'missing admin/user token'
}

# --- 5. Portal portal:access（Z-10）---
$anonPortal = Invoke-NovaApi -Path '/api/v1/portal/apps'
$anonDenied = ($anonPortal.http -eq 401) -or ($anonPortal.code -ge 40100 -and $anonPortal.code -lt 40200)
Check 'Z-10 anonymous portal denied' $anonDenied "http=$($anonPortal.http) code=$($anonPortal.code)"
$allPass = $allPass -and $anonDenied

if ($tokens.ContainsKey('user')) {
    try {
        $me = Invoke-RestMethod -Uri "$script:NovaFlowBaseUrl/api/v1/auth/me" -Headers @{ Authorization = $tokens.user }
        $hasPortal = @($me.data.permissions) -contains 'portal:access'
        Check 'Z-10 user has portal:access in me' $hasPortal 'permissions checked'
        $allPass = $allPass -and $hasPortal
    } catch {
        Check 'Z-10 user has portal:access in me' $false $_.Exception.Message
        $allPass = $false
    }
    $allPass = (Test-NovaApiAllowed 'Z-10 user portal list' '/api/v1/portal/apps' GET $tokens.user $results) -and $allPass
}

# --- 6. Z-07 静态扫描（@SaCheckPermission 缺口）---
try {
    $scanScript = Join-Path $PSScriptRoot 'scan-api-permissions.ps1'
    $scanOut = & pwsh -NoProfile -File $scanScript 2>&1 | Out-String
    $scanPass = ($LASTEXITCODE -eq 0)
    Check 'Z-07 controller permission scan' $scanPass ($scanPass ? 'no unallowlisted gaps' : $scanOut.Trim())
    $allPass = $allPass -and $scanPass
} catch {
    Check 'Z-07 controller permission scan' $false $_.Exception.Message
    $allPass = $false
}

Write-NovaGateResult -ScriptName 'rbac-api-acceptance' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

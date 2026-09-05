#requires -Version 7.0
# NovaFlow AI — 成员管理验收（U-03, U-04）
# 用法: pwsh test/member-management-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'member-management-smoke.log'
$outFile = Join-Path $PSScriptRoot 'member-management-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$inviteEmail = "qa-member-$suffix@novaflow.test"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Get-NovaDbConfig {
    $envFile = Join-Path (Split-Path $PSScriptRoot -Parent) '.env'
    $cfg = @{ host = 'localhost'; port = '3306'; user = 'root'; pass = 'root'; db = 'novaflow' }
    if (Test-Path $envFile) {
        foreach ($line in Get-Content $envFile) {
            if ($line -match '^SPRING_DATASOURCE_URL=(.+)$') {
                $url = $Matches[1].Trim()
                if ($url -match 'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)') {
                    $cfg.host = $Matches[1]
                    if ($Matches[2]) { $cfg.port = $Matches[2] }
                    $cfg.db = $Matches[3]
                }
            }
            if ($line -match '^SPRING_DATASOURCE_USERNAME=(.+)$') { $cfg.user = $Matches[1].Trim() }
            if ($line -match '^SPRING_DATASOURCE_PASSWORD=(.+)$') { $cfg.pass = $Matches[1].Trim() }
        }
    }
    return $cfg
}

function Invoke-NovaDbExec {
    param([string]$Sql)
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if (-not $mysql) { return $false }
    $cfg = Get-NovaDbConfig
    $args = @('-h', $cfg.host, '-P', $cfg.port, '-u', $cfg.user, "-p$($cfg.pass)", $cfg.db, '-e', $Sql)
    & mysql @args 2>$null | Out-Null
    return ($LASTEXITCODE -eq 0)
}

function Invoke-NovaDbScalar {
    param([string]$Sql)
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if (-not $mysql) { return $null }
    $cfg = Get-NovaDbConfig
    $args = @('-h', $cfg.host, '-P', $cfg.port, '-u', $cfg.user, "-p$($cfg.pass)", $cfg.db, '-N', '-B', '-e', $Sql)
    $out = & mysql @args 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return [string]$out
}

Write-NovaLog '=== member-management-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken

    $invitePath = Join-Path $script:NovaFlowTmpDir 'member-invite.json'
    Write-NovaJson -Path $invitePath -Data @{
        email    = $inviteEmail
        nickname = "QA $suffix"
        roleCode = 'viewer'
        password = 'SmokeTest123!'
    }
    $invite = Invoke-NovaApi -Method POST -Path '/api/v1/org/members/invite' -Token $token -OutFile $invitePath
    $memberId = [regex]::Match($invite.raw, '"id":(\d+)').Groups[1].Value
    Check 'U-03 invite member' (($invite.code -eq 0) -and $memberId) "memberId=$memberId code=$($invite.code)"

    $list = Invoke-NovaApi -Path "/api/v1/org/members?page=1&pageSize=50&keyword=$inviteEmail" -Token $token
    $found = ($list.code -eq 0) -and ($list.raw -match $inviteEmail)
    Check 'U-03 member listed' $found "code=$($list.code)"

    $updatePath = Join-Path $script:NovaFlowTmpDir 'member-update.json'
    Write-NovaJson -Path $updatePath -Data @{ roleCode = 'operator' }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/org/members/$memberId" -Token $token -OutFile $updatePath
    $roleOk = ($updated.code -eq 0) -and ($updated.raw -match '"roleCode":"operator"')
    Check 'U-03 update member role' $roleOk "code=$($updated.code)"

    $removed = Invoke-NovaApi -Method DELETE -Path "/api/v1/org/members/$memberId" -Token $token
    Check 'U-03 remove member' ($removed.code -eq 0) "code=$($removed.code)"

    $listAfter = Invoke-NovaApi -Path "/api/v1/org/members?page=1&pageSize=50&keyword=$inviteEmail" -Token $token
    $gone = ($listAfter.code -eq 0) -and ($listAfter.raw -notmatch $inviteEmail)
    Check 'U-03 member removed from list' $gone "code=$($listAfter.code)"

    $tenantId = Invoke-NovaDbScalar -Sql 'SELECT id FROM tenant WHERE tenant_code = ''demo'' LIMIT 1'
    $memberCount = Invoke-NovaDbScalar -Sql "SELECT COUNT(*) FROM tenant_member WHERE tenant_id=$tenantId AND is_deleted=0"
    $origMax = Invoke-NovaDbScalar -Sql "SELECT max_members FROM tenant WHERE id=$tenantId"
    if ($tenantId -and $memberCount -and $origMax) {
        Invoke-NovaDbExec -Sql "UPDATE tenant SET max_members=$memberCount WHERE id=$tenantId" | Out-Null
        $quotaPath = Join-Path $script:NovaFlowTmpDir 'quota-invite.json'
        Write-NovaJson -Path $quotaPath -Data @{
            email    = "qa-quota-$suffix@novaflow.test"
            nickname = 'Quota QA'
            roleCode = 'viewer'
            password = 'SmokeTest123!'
        }
        $quotaInvite = Invoke-NovaApi -Method POST -Path '/api/v1/org/members/invite' -Token $token -OutFile $quotaPath
        Check 'U-04 max_members blocks invite' ($quotaInvite.code -ne 0) "code=$($quotaInvite.code)"
        Invoke-NovaDbExec -Sql "UPDATE tenant SET max_members=$origMax WHERE id=$tenantId" | Out-Null
    } else {
        Check 'U-04 max_members blocks invite' $false 'SKIP: mysql unavailable'
    }
} catch {
    Check 'member-management setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'member-management-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# NovaFlow AI — Portal 对话与 Studio 权限隔离验收（AP-04）
# 用法: pwsh test/portal-studio-isolation-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'portal-studio-isolation-smoke.log'
$outFile = Join-Path $PSScriptRoot 'portal-studio-isolation-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== portal-studio-isolation-smoke ===' $logFile

try {
    $admin = Get-NovaLoginToken
    $user = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'
    $developer = Get-NovaLoginToken -Email 'developer@novaflow.ai' -Password 'Developer123!'

    $pubAppId = New-NovaApplication -Token $admin -Name "Portal-App-$suffix"
    $pubAgentId = New-NovaAgent -Token $admin -ApplicationId $pubAppId -Name "Portal-Agent-$suffix"
    Publish-NovaAgent -Token $admin -AgentId $pubAgentId | Out-Null
    Set-NovaApplicationAgents -Token $admin -ApplicationId $pubAppId -AppName "Portal-App-$suffix" -DefaultAgentId $pubAgentId -AgentIds @($pubAgentId) | Out-Null
    Publish-NovaApplication -Token $admin -ApplicationId $pubAppId | Out-Null

    $unpubAppId = New-NovaApplication -Token $admin -Name "Portal-Unpub-$suffix"
    $unpubAgentId = New-NovaAgent -Token $admin -ApplicationId $unpubAppId -Name "Portal-Unpub-Agent-$suffix"

    $portalList = Invoke-NovaApi -Path '/api/v1/portal/apps' -Token $user
    $portalListed = ($portalList.code -eq 0) -and ($portalList.raw -match "Portal-App-$suffix")
    Check 'AP-04 user can list published portal apps' $portalListed "code=$($portalList.code)"

    $portalDetail = Invoke-NovaApi -Path "/api/v1/portal/apps/$pubAppId" -Token $user
    Check 'AP-04 user can view published portal app detail' ($portalDetail.code -eq 0) "code=$($portalDetail.code)"

    $pubWelcome = Invoke-NovaApi -Path "/api/v1/agents/$pubAgentId/debug/welcome" -Token $user
    Check 'AP-04 user can welcome on published app agent' ($pubWelcome.code -eq 0) "http=$($pubWelcome.http) code=$($pubWelcome.code)"

    $unpubDetail = Invoke-NovaApi -Path "/api/v1/agents/$unpubAgentId" -Token $user
    $unpubDenied = ($unpubDetail.code -ne 0) -or ($unpubDetail.raw -match '无权访问')
    Check 'AP-04 user denied unpublished app agent detail' $unpubDenied "http=$($unpubDetail.http) code=$($unpubDetail.code)"

    $unpubWelcome = Invoke-NovaApi -Path "/api/v1/agents/$unpubAgentId/debug/welcome" -Token $user
    $unpubWelcomeDenied = ($unpubWelcome.code -ne 0) -or ($unpubWelcome.raw -match '无权访问')
    Check 'AP-04 user denied unpublished app agent welcome' $unpubWelcomeDenied "http=$($unpubWelcome.http) code=$($unpubWelcome.code)"

    $allPass = (Test-NovaApiDenied 'AP-04 user cannot create workflow' '/api/v1/workflows' POST $user $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'AP-04 user cannot create agent' '/api/v1/agents' POST $user $results) -and $allPass
    $allPass = (Test-NovaApiDenied 'AP-04 user cannot list debug conversations' "/api/v1/agents/$pubAgentId/debug/conversations?page=1&pageSize=5" GET $user $results) -and $allPass

    $devWelcome = Invoke-NovaApi -Path "/api/v1/agents/$pubAgentId/debug/welcome" -Token $developer
    Check 'AP-04 developer can studio welcome' ($devWelcome.code -eq 0) "code=$($devWelcome.code)"

    $devConvs = Invoke-NovaApi -Path "/api/v1/agents/$pubAgentId/debug/conversations?page=1&pageSize=5" -Token $developer
    Check 'AP-04 developer can list debug conversations' ($devConvs.code -eq 0) "code=$($devConvs.code)"

    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$pubAgentId" -Token $admin | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$unpubAgentId" -Token $admin | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$pubAppId" -Token $admin | Out-Null
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$unpubAppId" -Token $admin | Out-Null
} catch {
    Check 'portal-studio-isolation setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'portal-studio-isolation-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

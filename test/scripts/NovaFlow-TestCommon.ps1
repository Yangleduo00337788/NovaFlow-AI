#requires -Version 7.0
# NovaFlow AI — 测试脚本公共模块
# 用法: . "$PSScriptRoot/scripts/NovaFlow-TestCommon.ps1"

$script:NovaFlowBaseUrl = if ($env:NOVAFLOW_BASE_URL) { $env:NOVAFLOW_BASE_URL.TrimEnd('/') } else { 'http://localhost:8080' }
$script:NovaFlowWebUrl = if ($env:NOVAFLOW_WEB_URL) { $env:NOVAFLOW_WEB_URL.TrimEnd('/') } else { 'http://localhost:3000' }
$script:NovaFlowTmpDir = Join-Path $env:TEMP "novaflow-test-$(Get-Random)"
New-Item -ItemType Directory -Force -Path $script:NovaFlowTmpDir | Out-Null

function Write-NovaLog {
    param([string]$Message, [string]$LogFile)
    $line = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $Message"
    if ($LogFile) { Add-Content -Path $LogFile -Value $line }
    Write-Host $line
}

function Invoke-CurlExe {
    param([string[]]$CurlArgs)
    $out = & curl.exe @CurlArgs 2>&1
    if ($out -is [array]) { return ($out -join "`n") }
    return [string]$out
}

function ConvertFrom-NovaCurl {
    param([string]$Raw)
    $http = 0
    $body = $Raw
    if ($body -match "`nHTTP:(\d+)$") {
        $http = [int]$Matches[1]
        $body = $body -replace "`nHTTP:\d+$", ""
    }
    $code = $null
    if ($body -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    return [pscustomobject]@{
        raw  = $body
        http = $http
        code = $code
    }
}

function Write-NovaJson {
    param([string]$Path, [object]$Data)
    $json = if ($Data -is [string]) { $Data } else { $Data | ConvertTo-Json -Depth 8 -Compress }
    [System.IO.File]::WriteAllText($Path, $json, [System.Text.UTF8Encoding]::new($false))
}

function Invoke-NovaApi {
    param(
        [string]$Method = 'GET',
        [string]$Path,
        [string]$Token,
        [hashtable]$Body,
        [string]$OutFile,
        [int]$MaxTimeSec = 60
    )

    $url = "$script:NovaFlowBaseUrl$Path"
    $args = @('-s', '-w', "`nHTTP:%{http_code}", '--max-time', "$MaxTimeSec", '-X', $Method, $url)
    if ($Token) { $args += @('-H', "Authorization: $Token") }
    if ($OutFile -or $Body) {
        $jsonPath = if ($OutFile) { $OutFile } else { Join-Path $script:NovaFlowTmpDir "body-$(Get-Random).json" }
        if ($Body) { Write-NovaJson -Path $jsonPath -Data $Body }
        $args += @('-H', 'Content-Type: application/json', '--data-binary', "@$jsonPath")
    }
    return ConvertFrom-NovaCurl (Invoke-CurlExe $args)
}

function Get-NovaLoginToken {
    param(
        [string]$Email = 'admin@novaflow.ai',
        [string]$Password = 'Admin123!'
    )
    $loginPath = Join-Path $script:NovaFlowTmpDir 'login.json'
    Write-NovaJson -Path $loginPath -Data @{ email = $Email; password = $Password }
    $lastError = $null
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $resp = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $loginPath
        if ($resp.code -eq 0) {
            $token = [regex]::Match($resp.raw, '"token":"([^"]+)"').Groups[1].Value
            if ($token) { return $token }
        }
        $lastError = $resp.raw
        Start-Sleep -Seconds 2
    }
    throw "Login failed ($Email) after 3 attempts: $lastError"
}

function Wait-NovaMaintenanceOff {
    param(
        [int]$TimeoutSec = 45,
        [string]$PlatformToken
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $status = Invoke-NovaApi -Method GET -Path '/api/v1/public/platform-status'
        if ($status.code -eq 0 -and $status.raw -notmatch '"maintenanceEnabled"\s*:\s*true') {
            return
        }
        if ($PlatformToken) {
            $resetPath = Join-Path $script:NovaFlowTmpDir 'maintenance-wait-reset.json'
            Write-NovaJson -Path $resetPath -Data @{
                maintenanceEnabled   = $false
                maintenanceMessage   = ''
                platformAnnouncement = ''
            }
            Invoke-NovaApi -Method PUT -Path '/api/v1/platform/settings' -Token $PlatformToken -OutFile $resetPath | Out-Null
        }
        Start-Sleep -Seconds 1
    }
    throw "Platform maintenance still enabled after ${TimeoutSec}s"
}

function Prepare-NovaGateEnvironment {
    param([string]$PlatformToken)
    if (-not $PlatformToken) {
        $PlatformToken = Get-NovaLoginToken 'platform@novaflow.ai' 'Platform123!'
    }
    Wait-NovaMaintenanceOff -PlatformToken $PlatformToken
    $resetPath = Join-Path $script:NovaFlowTmpDir 'gate-prep-reset.json'
    Write-NovaJson -Path $resetPath -Data @{
        maintenanceEnabled          = $false
        maintenanceMessage          = ''
        platformAnnouncement        = ''
        batchRegisterIpLimitPerDay  = 0
        abnormalLoginEnabled        = $true
        newUserAgentEnabled         = $true
    }
    Invoke-NovaApi -Method PUT -Path '/api/v1/platform/settings' -Token $PlatformToken -OutFile $resetPath | Out-Null
    Invoke-NovaApi -Method POST -Path '/api/v1/platform/security/register-counters/reset' -Token $PlatformToken | Out-Null
}

function Get-NovaConfiguredProviderId {
    param(
        [string]$Token,
        [string]$ProviderCode = 'deepseek'
    )
    $resp = Invoke-NovaApi -Path '/api/v1/models/providers' -Token $Token
    if ($resp.code -ne 0) { throw "List providers failed: $($resp.raw)" }
    $preferred = [regex]::Match($resp.raw, "`"id`":(\d+),`"providerCode`":`"$ProviderCode`"").Groups[1].Value
    if ($preferred) { return [long]$preferred }
    foreach ($m in [regex]::Matches($resp.raw, '"id":(\d+),"providerCode":"([^"]+)"')) {
        if ($m.Groups[2].Value -and $m.Groups[1].Value) {
            return [long]$m.Groups[1].Value
        }
    }
    throw 'No configured model provider found'
}

function Restore-NovaProviderBaseUrl {
    param(
        [string]$Token,
        [string]$ProviderCode = 'deepseek',
        [string]$BaseUrl = 'https://api.deepseek.com/v1'
    )
    $providerId = Get-NovaConfiguredProviderId -Token $Token -ProviderCode $ProviderCode
    $path = Join-Path $script:NovaFlowTmpDir "restore-$ProviderCode.json"
    Write-NovaJson -Path $path -Data @{
        providerCode = $ProviderCode
        baseUrl      = $BaseUrl
        enabled      = $true
    }
    return Invoke-NovaApi -Method PUT -Path "/api/v1/models/providers/$providerId" -Token $Token -OutFile $path
}

function Register-NovaTenant {
    param([string]$Suffix)
    $registerPath = Join-Path $script:NovaFlowTmpDir "register-$Suffix.json"
    Write-NovaJson -Path $registerPath -Data @{
        companyName     = "QA-$Suffix"
        email           = "qa-$Suffix@novaflow.test"
        nickname        = "QA $Suffix"
        password        = 'SmokeTest123!'
        confirmPassword = 'SmokeTest123!'
        planType        = 'enterprise'
    }
    $lastError = $null
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $resp = Invoke-NovaApi -Method POST -Path '/api/v1/auth/register' -OutFile $registerPath
        if ($resp.code -eq 0) {
            $token = [regex]::Match($resp.raw, '"token":"([^"]+)"').Groups[1].Value
            $tenantId = [regex]::Match($resp.raw, '"id":(\d+)').Groups[1].Value
            return [pscustomobject]@{ suffix = $Suffix; token = $token; tenantId = $tenantId; email = "qa-$Suffix@novaflow.test" }
        }
        $lastError = $resp.raw
        Start-Sleep -Seconds 2
    }
    throw "Register failed ($Suffix) after 3 attempts: $lastError"
}

function New-NovaApplication {
    param([string]$Token, [string]$Name)
    $path = Join-Path $script:NovaFlowTmpDir "app-$(Get-Random).json"
    Write-NovaJson -Path $path -Data @{ appName = $Name; description = 'qa script' }
    $resp = Invoke-NovaApi -Method POST -Path '/api/v1/applications' -Token $Token -OutFile $path
    if ($resp.code -ne 0) { throw "Create application failed: $($resp.raw)" }
    return [int]([regex]::Match($resp.raw, '"id":(\d+)').Groups[1].Value)
}

function New-NovaAgent {
    param([string]$Token, [long]$ApplicationId, [string]$Name)
    $path = Join-Path $script:NovaFlowTmpDir "agent-$(Get-Random).json"
    Write-NovaJson -Path $path -Data @{
        agentName       = $Name
        agentType       = 'chat'
        applicationId   = $ApplicationId
        welcomeMessage  = 'qa'
    }
    $resp = Invoke-NovaApi -Method POST -Path '/api/v1/agents' -Token $Token -OutFile $path
    if ($resp.code -ne 0) { throw "Create agent failed: $($resp.raw)" }
    return [int]([regex]::Match($resp.raw, '"id":(\d+)').Groups[1].Value)
}

function New-NovaKnowledgeBase {
    param([string]$Token, [string]$Name, [string]$EmbeddingModel = 'text-embedding-3-small')
    $path = Join-Path $script:NovaFlowTmpDir "kb-$(Get-Random).json"
    Write-NovaJson -Path $path -Data @{
        kbName          = $Name
        description     = 'qa'
        embeddingModel  = $EmbeddingModel
    }
    $resp = Invoke-NovaApi -Method POST -Path '/api/v1/knowledge-bases' -Token $Token -OutFile $path
    if ($resp.code -ne 0) { throw "Create knowledge base failed: $($resp.raw)" }
    return [int]([regex]::Match($resp.raw, '"id":(\d+)').Groups[1].Value)
}

function New-NovaWorkflow {
    param([string]$Token, [long]$ApplicationId, [string]$Name)
    $path = Join-Path $script:NovaFlowTmpDir "wf-$(Get-Random).json"
    Write-NovaJson -Path $path -Data @{
        workflowName  = $Name
        description   = 'qa'
        applicationId = $ApplicationId
    }
    $resp = Invoke-NovaApi -Method POST -Path '/api/v1/workflows' -Token $Token -OutFile $path
    if ($resp.code -ne 0) { throw "Create workflow failed: $($resp.raw)" }
    return [int]([regex]::Match($resp.raw, '"id":(\d+)').Groups[1].Value)
}

function Get-NovaMemberUserId {
    param([string]$Token, [string]$Email)
    $payload = Invoke-RestMethod -Uri "$script:NovaFlowBaseUrl/api/v1/org/members?page=1&pageSize=50" -Headers @{ Authorization = $Token }
    if ($payload.code -ne 0) { throw "List members failed: $($payload.message)" }
    $member = @($payload.data.list | Where-Object { $_.email -eq $Email } | Select-Object -First 1)
    if ($member.Count -eq 0 -or -not $member[0].userId) {
        throw "Member userId not found for $Email"
    }
    return [long]$member[0].userId
}

function Set-NovaResourcePermissions {
    param(
        [string]$Token,
        [string]$ResourceType,
        [long]$ResourceId,
        [array]$Grants
    )
    $path = Join-Path $script:NovaFlowTmpDir "acl-$(Get-Random).json"
    Write-NovaJson -Path $path -Data @{ grants = $Grants }
    $resp = Invoke-NovaApi -Method PUT -Path "/api/v1/resources/$ResourceType/$ResourceId/permissions" -Token $Token -OutFile $path
    if ($resp.code -ne 0) { throw "Save resource permissions failed: $($resp.raw)" }
    return $resp
}

function Test-NovaDenied {
    param(
        $Resp,
        [switch]$AllowAuthFailure
    )
    if ($Resp.code -eq 0 -and $Resp.http -lt 400) { return $false }
    if (-not $AllowAuthFailure -and $Resp.code -ge 40100 -and $Resp.code -lt 40200) { return $false }
    return $true
}

function Test-NovaApiDenied {
    param(
        [string]$Label,
        [string]$Path,
        [string]$Method = 'GET',
        [string]$Token,
        [System.Collections.Generic.List[object]]$Results,
        [switch]$AllowAuthFailure
    )
    $resp = Invoke-NovaApi -Method $Method -Path $Path -Token $Token
    $denied = Test-NovaDenied -Resp $resp -AllowAuthFailure:$AllowAuthFailure
    $detail = "http=$($resp.http) code=$($resp.code)"
    $ok = Assert-NovaGate $Label $denied $detail $Results
    return $ok
}

function Test-NovaApiAllowed {
    param(
        [string]$Label,
        [string]$Path,
        [string]$Method = 'GET',
        [string]$Token,
        [System.Collections.Generic.List[object]]$Results
    )
    $resp = Invoke-NovaApi -Method $Method -Path $Path -Token $Token
    $allowed = ($resp.code -eq 0 -and $resp.http -lt 400)
    $detail = "http=$($resp.http) code=$($resp.code)"
    $ok = Assert-NovaGate $Label $allowed $detail $Results
    return $ok
}

function Write-NovaGateResult {
    param(
        [string]$ScriptName,
        [bool]$Passed,
        [hashtable]$Details,
        [string]$OutFile
    )
    $result = [ordered]@{
        script    = $ScriptName
        timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
        passed    = $Passed
        baseUrl   = $script:NovaFlowBaseUrl
        details   = $Details
    }
    $result | ConvertTo-Json -Depth 8 | Set-Content $OutFile -Encoding UTF8
    return $result
}

function Assert-NovaGate {
    param(
        [string]$Name,
        [bool]$Condition,
        [string]$Detail,
        [System.Collections.Generic.List[object]]$Results
    )
    $row = [pscustomobject]@{ name = $Name; passed = $Condition; detail = $Detail }
    $Results.Add($row) | Out-Null
    $icon = if ($Condition) { 'PASS' } else { 'FAIL' }
    Write-Host "[$icon] $Name — $Detail"
    return $Condition
}

function Invoke-NovaOpenApi {
    param(
        [string]$Method = 'GET',
        [string]$Path,
        [hashtable]$Headers = @{},
        [hashtable]$Body,
        [string]$OutFile,
        [int]$MaxTimeSec = 120
    )

    $url = "$script:NovaFlowBaseUrl$Path"
    $args = @('-s', '-w', "`nHTTP:%{http_code}", '--max-time', "$MaxTimeSec", '-X', $Method, $url)
    foreach ($key in $Headers.Keys) {
        $args += @('-H', "$key`: $($Headers[$key])")
    }
    if ($OutFile -or $Body) {
        $jsonPath = if ($OutFile) { $OutFile } else { Join-Path $script:NovaFlowTmpDir "open-body-$(Get-Random).json" }
        if ($Body) { Write-NovaJson -Path $jsonPath -Data $Body }
        $args += @('-H', 'Content-Type: application/json', '--data-binary', "@$jsonPath")
    }
    return ConvertFrom-NovaCurl (Invoke-CurlExe $args)
}

function Publish-NovaAgent {
    param([string]$Token, [long]$AgentId)
    $resp = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$AgentId/publish" -Token $Token
    if ($resp.code -ne 0) { throw "Publish agent failed: $($resp.raw)" }
    $apiKey = [regex]::Match($resp.raw, '"apiKey":"([^"]+)"').Groups[1].Value
    $embedToken = [regex]::Match($resp.raw, '"embedToken":"([^"]+)"').Groups[1].Value
    if (-not $apiKey -or -not $embedToken) { throw "Publish response missing apiKey/embedToken" }
    return [pscustomobject]@{
        apiKey     = $apiKey
        embedToken = $embedToken
        raw        = $resp.raw
    }
}

function Set-NovaApplicationAgents {
    param(
        [string]$Token,
        [long]$ApplicationId,
        [string]$AppName,
        [long]$DefaultAgentId,
        [long[]]$AgentIds
    )
    $path = Join-Path $script:NovaFlowTmpDir "app-agents-$(Get-Random).json"
    Write-NovaJson -Path $path -Data @{
        appName        = $AppName
        description    = 'qa open api'
        defaultAgentId = $DefaultAgentId
        agentIds       = @($AgentIds)
    }
    $resp = Invoke-NovaApi -Method PUT -Path "/api/v1/applications/$ApplicationId" -Token $Token -OutFile $path
    if ($resp.code -ne 0) { throw "Update application agents failed: $($resp.raw)" }
    return $resp
}

function Publish-NovaApplication {
    param([string]$Token, [long]$ApplicationId)
    $resp = Invoke-NovaApi -Method POST -Path "/api/v1/applications/$ApplicationId/publish" -Token $Token
    if ($resp.code -ne 0) { throw "Publish application failed: $($resp.raw)" }
    return $resp
}

function Publish-NovaOpenApiFixture {
    param(
        [string]$Token,
        [string]$Suffix
    )
    $appName = "OpenAPI-App-$Suffix"
    $appId = New-NovaApplication -Token $Token -Name $appName
    $agentId = New-NovaAgent -Token $Token -ApplicationId $appId -Name "OpenAPI-Agent-$Suffix"
    $published = Publish-NovaAgent -Token $Token -AgentId $agentId
    Set-NovaApplicationAgents -Token $Token -ApplicationId $appId -AppName $appName -DefaultAgentId $agentId -AgentIds @($agentId) | Out-Null
    Publish-NovaApplication -Token $Token -ApplicationId $appId | Out-Null
    return [pscustomobject]@{
        appId      = $appId
        agentId    = $agentId
        apiKey     = $published.apiKey
        embedToken = $published.embedToken
    }
}

function Unpublish-NovaAgent {
    param([string]$Token, [long]$AgentId)
    $resp = Invoke-NovaApi -Method POST -Path "/api/v1/agents/$AgentId/unpublish" -Token $Token
    if ($resp.code -ne 0) { throw "Unpublish agent failed: $($resp.raw)" }
    return $resp
}

function Test-NovaApiCode {
    param(
        [string]$Label,
        $Resp,
        [int]$ExpectedCode,
        [System.Collections.Generic.List[object]]$Results
    )
    $ok = ($Resp.code -eq $ExpectedCode)
    $detail = "expected=$ExpectedCode http=$($Resp.http) code=$($Resp.code)"
    return (Assert-NovaGate $Label $ok $detail $Results)
}

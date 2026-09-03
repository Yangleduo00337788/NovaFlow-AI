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
    $resp = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -OutFile $loginPath
    if ($resp.code -ne 0) { throw "Login failed ($Email): $($resp.raw)" }
    $token = [regex]::Match($resp.raw, '"token":"([^"]+)"').Groups[1].Value
    if (-not $token) { throw "Login response missing token" }
    return $token
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

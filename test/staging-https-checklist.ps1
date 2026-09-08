#requires -Version 7.0
# 预发 / 生产 HTTPS 手工走查清单（P2）
# 用法:
#   $env:STAGING_BASE_URL='https://staging.example.com'
#   pwsh test/staging-https-checklist.ps1

param(
    [string]$BaseUrl = $env:STAGING_BASE_URL
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

if (-not $BaseUrl) {
    Write-Host 'Set STAGING_BASE_URL or pass -BaseUrl https://your-domain' -ForegroundColor Yellow
    exit 2
}

$logFile = Join-Path $PSScriptRoot 'staging-https-checklist.log'
$outFile = Join-Path $PSScriptRoot 'staging-https-checklist-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$origin = $BaseUrl.TrimEnd('/')

Write-NovaLog "=== staging-https-checklist $origin ===" $logFile

function Check-Url {
    param([string]$Name, [string]$Path, [int[]]$Allowed = @(200))
    $url = "$origin$Path"
    $code = [int](Invoke-CurlExe @('-s', '-o', 'NUL', '-w', '%{http_code}', $url))
    $ok = $Allowed -contains $code
    $null = Assert-NovaGate $Name $ok "url=$url http=$code" $results
    if (-not $ok) { $script:allPass = $false }
}

function Check-Tls {
    param([string]$Name)
    try {
        $uri = [Uri]$origin
        if ($uri.Scheme -ne 'https') {
            $null = Assert-NovaGate $Name $false 'expected https:// origin' $results
            $script:allPass = $false
            return
        }
        $req = [System.Net.HttpWebRequest]::Create($origin)
        $req.AllowAutoRedirect = $true
        $req.Timeout = 15000
        $resp = $req.GetResponse()
        $resp.Close()
        $null = Assert-NovaGate $Name $true 'TLS handshake ok' $results
    } catch {
        $null = Assert-NovaGate $Name $false $_.Exception.Message $results
        $script:allPass = $false
    }
}

Check-Tls 'SH-01 HTTPS TLS handshake'
Check-Url 'SH-02 web index' '/'
Check-Url 'SH-03 api health via proxy' '/api/v1/health'

$cors = Invoke-CurlExe @(
    '-s', '-D', '-', '-o', 'NUL',
    '-H', "Origin: $origin",
    '-H', 'Access-Control-Request-Method: GET',
    '-X', 'OPTIONS',
    "$origin/api/v1/health"
)
$corsOk = ($cors -match 'HTTP/[0-9.]+ 204' -or $cors -match 'HTTP/[0-9.]+ 200') -and ($cors -match 'access-control-allow-origin')
$null = Assert-NovaGate 'SH-04 CORS preflight' $corsOk $(if ($corsOk) { 'allow-origin present' } else { 'missing CORS headers' }) $results
if (-not $corsOk) { $allPass = $false }

Write-NovaLog 'Manual follow-ups: login, portal chat, embed iframe on partner domain, platform admin.' $logFile
Write-NovaGateResult -ScriptName 'staging-https-checklist' -Passed $allPass -Details @{ checks = @($results) } -OutFile $outFile | Out-Null
Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

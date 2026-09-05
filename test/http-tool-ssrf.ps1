#requires -Version 7.0
# NovaFlow AI — HTTP 工具 SSRF 防护验收（T-02）
# 用法: pwsh test/http-tool-ssrf.ps1
# 前提: 后端 :8080 已启动

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'http-tool-ssrf.log'
$outFile = Join-Path $PSScriptRoot 'http-tool-ssrf-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function New-ToolAttempt {
    param([string]$Token, [string]$ToolName, [string]$Url)
    $path = Join-Path $script:NovaFlowTmpDir "tool-$([Guid]::NewGuid().ToString('N')).json"
    Write-NovaJson -Path $path -Data @{
        toolName    = $ToolName
        displayName = "SSRF QA $ToolName"
        description = 'qa'
        toolType    = 'http'
        method      = 'GET'
        url         = $Url
    }
    return Invoke-NovaApi -Method POST -Path '/api/v1/tools' -Token $Token -OutFile $path
}

Write-NovaLog '=== http-tool-ssrf ===' $logFile

try {
    $token = Get-NovaLoginToken

    $blockedCases = @(
        @{ name = 'T-02 localhost blocked'; label = "ssrf_local_$suffix"; url = 'http://localhost:8080/admin' }
        @{ name = 'T-02 loopback IP blocked'; label = "ssrf_loop_$suffix"; url = 'http://127.0.0.1:8080/admin' }
        @{ name = 'T-02 private IP blocked'; label = "ssrf_priv_$suffix"; url = 'http://192.168.1.10/internal' }
        @{ name = 'T-02 metadata IP blocked'; label = "ssrf_meta_$suffix"; url = 'http://169.254.169.254/latest/meta-data' }
        @{ name = 'T-02 non-http scheme blocked'; label = "ssrf_file_$suffix"; url = 'file:///etc/passwd' }
    )

    foreach ($case in $blockedCases) {
        $resp = New-ToolAttempt -Token $token -ToolName $case.label -Url $case.url
        $denied = ($resp.code -ne 0) -and ($resp.http -ge 400)
        Check $case.name $denied "http=$($resp.http) code=$($resp.code)"
    }

    $safeName = "ssrf_safe_$suffix"
    $safe = New-ToolAttempt -Token $token -ToolName $safeName -Url 'https://example.com/health'
    $safeId = $null
    if ($safe.code -eq 0) {
        $safeId = [regex]::Match($safe.raw, '"id":(\d+)').Groups[1].Value
    }
    Check 'T-02 public URL allowed on create' ($safe.code -eq 0 -and $safeId) "http=$($safe.http) code=$($safe.code) id=$safeId"

    if ($safeId) {
        $updatePath = Join-Path $script:NovaFlowTmpDir 'tool-update.json'
        Write-NovaJson -Path $updatePath -Data @{
            toolName    = $safeName
            displayName = 'SSRF QA update'
            description = 'qa'
            toolType    = 'http'
            method      = 'GET'
            url         = 'http://10.0.0.5/internal'
        }
        $update = Invoke-NovaApi -Method PUT -Path "/api/v1/tools/$safeId" -Token $token -OutFile $updatePath
        $updateBlocked = ($update.code -ne 0) -and ($update.http -ge 400)
        Check 'T-02 private URL blocked on update' $updateBlocked "http=$($update.http) code=$($update.code)"

        $testPath = Join-Path $script:NovaFlowTmpDir 'tool-test.json'
        Write-NovaJson -Path $testPath -Data @{ arguments = @{} }
        $test = Invoke-NovaApi -Method POST -Path "/api/v1/tools/$safeId/test" -Token $token -OutFile $testPath -MaxTimeSec 30
        Check 'T-02 test endpoint reachable for safe tool' ($test.code -eq 0) "http=$($test.http) code=$($test.code)"

        Invoke-NovaApi -Method DELETE -Path "/api/v1/tools/$safeId" -Token $token | Out-Null
    } else {
        Check 'T-02 private URL blocked on update' $false 'SKIP: safe tool not created'
        Check 'T-02 test endpoint reachable for safe tool' $false 'SKIP: safe tool not created'
    }
} catch {
    Check 'http-tool-ssrf setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'http-tool-ssrf' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

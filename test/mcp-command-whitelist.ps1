#requires -Version 7.0
# NovaFlow AI — MCP stdio 命令白名单验收（T-04）
# 用法: pwsh test/mcp-command-whitelist.ps1
# 前提: 后端 :8080 已启动

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'mcp-command-whitelist.log'
$outFile = Join-Path $PSScriptRoot 'mcp-command-whitelist-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function New-McpAttempt {
    param([string]$Token, [string]$ServerName, [object]$ServerConfig)
    $path = Join-Path $script:NovaFlowTmpDir "mcp-$([Guid]::NewGuid().ToString('N')).json"
    $configJson = if ($ServerConfig -is [string]) { $ServerConfig } else { $ServerConfig | ConvertTo-Json -Depth 6 -Compress }
    Write-NovaJson -Path $path -Data @{
        serverName   = $ServerName
        description  = 'qa mcp whitelist'
        serverConfig = $configJson
    }
    return Invoke-NovaApi -Method POST -Path '/api/v1/mcp-servers' -Token $Token -OutFile $path
}

Write-NovaLog '=== mcp-command-whitelist ===' $logFile

try {
    $token = Get-NovaLoginToken

    $blockedCases = @(
        @{ name = 'T-04 bash blocked'; server = "mcp_bash_$suffix"; config = @{ transportType = 'stdio'; command = 'bash'; args = @('-c', 'echo hi') } }
        @{ name = 'T-04 cmd blocked'; server = "mcp_cmd_$suffix"; config = @{ transportType = 'stdio'; command = 'cmd.exe'; args = @('/c', 'echo hi') } }
        @{ name = 'T-04 powershell blocked'; server = "mcp_ps_$suffix"; config = @{ transportType = 'stdio'; command = 'powershell'; args = @('-Command', 'Write-Output hi') } }
        @{ name = 'T-04 path cmd blocked'; server = "mcp_path_$suffix"; config = @{ transportType = 'stdio'; command = 'C:\Windows\System32\cmd.exe'; args = @() } }
    )

    foreach ($case in $blockedCases) {
        $resp = New-McpAttempt -Token $token -ServerName $case.server -ServerConfig $case.config
        $denied = ($resp.code -ne 0) -and ($resp.http -ge 400)
        Check $case.name $denied "http=$($resp.http) code=$($resp.code)"
    }

    $argsBlocked = New-McpAttempt -Token $token -ServerName "mcp_args_$suffix" -ServerConfig @{
        transportType = 'stdio'
        command       = 'npx'
        args          = @('-y', 'pkg;rm -rf /')
    }
    Check 'T-04 illegal args blocked' (($argsBlocked.code -ne 0) -and ($argsBlocked.http -ge 400)) "http=$($argsBlocked.http) code=$($argsBlocked.code)"

    $envBlocked = New-McpAttempt -Token $token -ServerName "mcp_env_$suffix" -ServerConfig @{
        transportType = 'stdio'
        command       = 'node'
        args          = @('-v')
        env           = @{ LD_PRELOAD = '/tmp/evil.so' }
    }
    Check 'T-04 dangerous env blocked' (($envBlocked.code -ne 0) -and ($envBlocked.http -ge 400)) "http=$($envBlocked.http) code=$($envBlocked.code)"

    $safeName = "mcp_npx_$suffix"
    $safe = New-McpAttempt -Token $token -ServerName $safeName -ServerConfig @{
        transportType = 'stdio'
        command       = 'npx'
        args          = @('-y', '@modelcontextprotocol/server-filesystem', '/tmp')
    }
    $safeId = $null
    if ($safe.code -eq 0) {
        $safeId = [regex]::Match($safe.raw, '"id":(\d+)').Groups[1].Value
    }
    Check 'T-04 whitelisted npx allowed' ($safe.code -eq 0 -and $safeId) "http=$($safe.http) code=$($safe.code) id=$safeId"

    $remote = New-McpAttempt -Token $token -ServerName "mcp_sse_$suffix" -ServerConfig @{
        transportType = 'sse'
        url           = 'https://example.com/mcp'
    }
    $remoteId = $null
    if ($remote.code -eq 0) {
        $remoteId = [regex]::Match($remote.raw, '"id":(\d+)').Groups[1].Value
    }
    Check 'T-04 remote MCP without command allowed' ($remote.code -eq 0 -and $remoteId) "http=$($remote.http) code=$($remote.code) id=$remoteId"

    foreach ($id in @($safeId, $remoteId)) {
        if ($id) {
            Invoke-NovaApi -Method DELETE -Path "/api/v1/mcp-servers/$id" -Token $token | Out-Null
        }
    }
} catch {
    Check 'mcp-command-whitelist setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'mcp-command-whitelist' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

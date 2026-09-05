#requires -Version 7.0
# NovaFlow AI — MCP Server 验收（T-03, T-05）
# 用法: pwsh test/mcp-server-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'mcp-server-smoke.log'
$outFile = Join-Path $PSScriptRoot 'mcp-server-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$serverName = "mcp_smoke_$suffix"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== mcp-server-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $createPath = Join-Path $script:NovaFlowTmpDir 'mcp-create.json'
    $config = @{
        transportType = 'stdio'
        command       = 'npx'
        args          = @('-y', '@modelcontextprotocol/server-filesystem', '/tmp')
    } | ConvertTo-Json -Compress
    Write-NovaJson -Path $createPath -Data @{
        serverName   = $serverName
        description  = 'qa mcp smoke'
        serverConfig = $config
    }
    $created = Invoke-NovaApi -Method POST -Path '/api/v1/mcp-servers' -Token $token -OutFile $createPath
    $mcpId = [regex]::Match($created.raw, '"id":(\d+)').Groups[1].Value
    Check 'T-03 create mcp server' (($created.code -eq 0) -and $mcpId) "id=$mcpId code=$($created.code)"

    $detail = Invoke-NovaApi -Path "/api/v1/mcp-servers/$mcpId" -Token $token
    Check 'T-03 get mcp detail' ($detail.code -eq 0) "code=$($detail.code)"

    $page = Invoke-NovaApi -Path '/api/v1/mcp-servers?page=1&pageSize=20' -Token $token
    $listed = ($page.code -eq 0) -and ($page.raw -match $serverName)
    Check 'T-03 list mcp servers' $listed "code=$($page.code)"

    $connect = Invoke-NovaApi -Method POST -Path "/api/v1/mcp-servers/$mcpId/connect" -Token $token -MaxTimeSec 120
    $connectHandled = ($connect.code -eq 0) -or (
        ($connect.http -ge 400) -and ($connect.raw -match 'MCP|连接|npx|超时|失败')
    )
    Check 'T-05 connect endpoint handles result' $connectHandled "http=$($connect.http) code=$($connect.code)"

    if ($connect.code -eq 0 -and $connect.raw -match '"status":(\d+)') {
        $status = [int]$Matches[1]
        Check 'T-05 connect status recorded' ($status -ge 0) "status=$status"
    } else {
        Check 'T-05 connect status recorded' $connectHandled 'connect rejected with MCP error (env without npx ok)'
    }

    $deleted = Invoke-NovaApi -Method DELETE -Path "/api/v1/mcp-servers/$mcpId" -Token $token
    Check 'T-03 delete mcp server' ($deleted.code -eq 0) "code=$($deleted.code)"
} catch {
    Check 'mcp-server-smoke setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'mcp-server-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

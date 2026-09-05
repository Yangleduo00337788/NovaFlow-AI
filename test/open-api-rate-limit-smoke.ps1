#requires -Version 7.0
# NovaFlow AI — Open API 限流验收（O-09）
# 用法: pwsh test/open-api-rate-limit-smoke.ps1
# 说明: IP 限流默认 30/min；对 welcome 端点连续请求触发 42901

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'open-api-rate-limit-smoke.log'
$outFile = Join-Path $PSScriptRoot 'open-api-rate-limit-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '-' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== open-api-rate-limit-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $fixture = Publish-NovaOpenApiFixture -Token $token -Suffix $suffix
    $agentId = $fixture.agentId
    $apiKey = $fixture.apiKey

    $rateLimited = $false
    for ($i = 1; $i -le 35; $i++) {
        $resp = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/welcome" -Headers @{
            Authorization = "Bearer $apiKey"
        }
        if ($resp.code -eq 42901) {
            $rateLimited = $true
            Check 'O-09 open api ip rate limit 42901' $true "attempt=$i code=$($resp.code)"
            break
        }
    }

    if (-not $rateLimited) {
        Check 'O-09 open api ip rate limit 42901' $false 'no 42901 after 35 welcome calls'
    }
} catch {
    Check 'open-api-rate-limit setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'open-api-rate-limit-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

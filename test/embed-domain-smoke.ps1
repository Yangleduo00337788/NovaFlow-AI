#requires -Version 7.0
# NovaFlow AI — Embed 域名白名单 + 主题色 / welcome 字段
# 用法: pwsh test/embed-domain-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'embed-domain-smoke.log'
$outFile = Join-Path $PSScriptRoot 'embed-domain-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$themeColor = '#6366f1'

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== embed-domain-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $fixture = Publish-NovaOpenApiFixture -Token $token -Suffix "embd-$suffix"
    $agentId = $fixture.agentId
    $embedToken = $fixture.embedToken

    $cfgPath = Join-Path $script:NovaFlowTmpDir 'embed-config.json'
    Write-NovaJson -Path $cfgPath -Data @{
        themeColor              = $themeColor
        allowedDomains          = @('partner.example')
        postMessageTargetOrigin = 'https://partner.example'
    }
    $saved = Invoke-NovaApi -Method PUT -Path "/api/v1/agents/$agentId/embed-config" -Token $token -OutFile $cfgPath
    Check 'EM-01 save embed config' ($saved.code -eq 0) "code=$($saved.code)"

    $loaded = Invoke-NovaApi -Path "/api/v1/agents/$agentId/embed-config" -Token $token
    $configOk = ($loaded.code -eq 0) `
        -and ($loaded.raw -match '"themeColor"\s*:\s*"#6366f1"') `
        -and ($loaded.raw -match 'partner\.example') `
        -and ($loaded.raw -match '"postMessageTargetOrigin"\s*:\s*"https://partner\.example"')
    Check 'EM-02 get embed config' $configOk "code=$($loaded.code)"

    $blocked = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/welcome" -Headers @{
        'X-Embed-Token' = $embedToken
        'Referer'       = 'https://evil.example/embed'
    }
    Check 'EM-03 foreign origin rejected' ($blocked.code -eq 40304) "code=$($blocked.code) http=$($blocked.http)"

    $allowed = Invoke-NovaOpenApi -Path "/api/v1/open/agents/$agentId/welcome" -Headers @{
        'X-Embed-Token' = $embedToken
        'Referer'       = 'https://partner.example/app'
    }
    $welcomeOk = ($allowed.code -eq 0) `
        -and ($allowed.raw -match '"embedThemeColor"\s*:\s*"#6366f1"') `
        -and ($allowed.raw -match '"postMessageTargetOrigin"\s*:\s*"https://partner\.example"')
    Check 'EM-04 welcome returns theme and postMessage origin' $welcomeOk "code=$($allowed.code) http=$($allowed.http)"
} catch {
    Check 'embed-domain setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'embed-domain-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

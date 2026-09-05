#requires -Version 7.0
# NovaFlow AI — API 写接口边界验收（API-01 ~ API-06）
# 用法: pwsh test/api-boundary-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'api-boundary-smoke.log'
$outFile = Join-Path $PSScriptRoot 'api-boundary-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Test-Rejected {
    param([string]$Name, $Resp)
    $rejected = ($Resp.code -ne 0) -or ($Resp.http -ge 400)
    Check $Name $rejected "http=$($Resp.http) code=$($Resp.code)"
}

Write-NovaLog '=== api-boundary-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $emptyPath = Join-Path $script:NovaFlowTmpDir 'empty.json'
    '{}' | Set-Content $emptyPath -Encoding UTF8

    $nullAgent = Invoke-NovaApi -Method POST -Path '/api/v1/agents' -Token $token -OutFile $emptyPath
    Test-Rejected 'API-01 null agent body rejected' $nullAgent

    $longName = 'x' * 300
    $longPath = Join-Path $script:NovaFlowTmpDir 'long-agent.json'
    Write-NovaJson -Path $longPath -Data @{
        agentName       = $longName
        agentType       = 'chat'
        applicationId   = 1
        welcomeMessage  = 'qa'
    }
    $longAgent = Invoke-NovaApi -Method POST -Path '/api/v1/agents' -Token $token -OutFile $longPath
    Test-Rejected 'API-02 oversized agent name rejected' $longAgent

    $badId = Invoke-NovaApi -Path '/api/v1/agents/-1' -Token $token
    Test-Rejected 'API-03 negative agent id rejected' $badId

    $zeroId = Invoke-NovaApi -Path '/api/v1/agents/0' -Token $token
    Test-Rejected 'API-03 zero agent id rejected' $zeroId

    $missingPath = Join-Path $script:NovaFlowTmpDir 'missing-kb.json'
    Write-NovaJson -Path $missingPath -Data @{ description = 'no name' }
    $missingKb = Invoke-NovaApi -Method POST -Path '/api/v1/knowledge-bases' -Token $token -OutFile $missingPath
    Test-Rejected 'API-04 missing required kb field rejected' $missingKb

    $missingPromptPath = Join-Path $script:NovaFlowTmpDir 'missing-prompt.json'
    Write-NovaJson -Path $missingPromptPath -Data @{ templateName = 'only name' }
    $missingPrompt = Invoke-NovaApi -Method POST -Path '/api/v1/prompts' -Token $token -OutFile $missingPromptPath
    Test-Rejected 'API-04 missing prompt content rejected' $missingPrompt

    $appId = New-NovaApplication -Token $token -Name "API-Unicode-$suffix"
    $unicodeName = "测试Agent🚀-$suffix"
    $unicodePath = Join-Path $script:NovaFlowTmpDir 'unicode-agent.json'
    Write-NovaJson -Path $unicodePath -Data @{
        agentName       = $unicodeName
        agentType       = 'chat'
        applicationId   = $appId
        welcomeMessage  = 'unicode qa'
    }
    $unicodeAgent = Invoke-NovaApi -Method POST -Path '/api/v1/agents' -Token $token -OutFile $unicodePath
    $unicodeId = [regex]::Match($unicodeAgent.raw, '"id":(\d+)').Groups[1].Value
    Check 'API-05 unicode agent name accepted' (($unicodeAgent.code -eq 0) -and $unicodeId) "agentId=$unicodeId code=$($unicodeAgent.code)"

    $badContentTypeRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/agents",
        '-H', "Authorization: $token",
        '-H', 'Content-Type: text/plain',
        '--data-binary', 'not-json'
    )
    $badContentType = ConvertFrom-NovaCurl $badContentTypeRaw
    $contentTypeRejected = ($badContentType.code -ne 0) -or ($badContentType.http -ge 400)
    Check 'API-06 wrong content-type rejected' $contentTypeRejected "http=$($badContentType.http) code=$($badContentType.code)"

    if ($unicodeId) { Invoke-NovaApi -Method DELETE -Path "/api/v1/agents/$unicodeId" -Token $token | Out-Null }
    Invoke-NovaApi -Method DELETE -Path "/api/v1/applications/$appId" -Token $token | Out-Null
} catch {
    Check 'api-boundary setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'api-boundary-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

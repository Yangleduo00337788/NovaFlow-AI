#requires -Version 7.0
# NovaFlow AI — HTTP 工具 CRUD + test 验收（T-01）
# 用法: pwsh test/http-tool-crud-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'http-tool-crud-smoke.log'
$outFile = Join-Path $PSScriptRoot 'http-tool-crud-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== http-tool-crud-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $toolName = "http_crud_$suffix"
    $createPath = Join-Path $script:NovaFlowTmpDir 'tool-create.json'
    Write-NovaJson -Path $createPath -Data @{
        toolName    = $toolName
        displayName = "HTTP CRUD $suffix"
        description = 'qa http tool crud'
        toolType    = 'http'
        method      = 'GET'
        url         = 'https://example.com/health'
    }
    $created = Invoke-NovaApi -Method POST -Path '/api/v1/tools' -Token $token -OutFile $createPath
    $toolId = [regex]::Match($created.raw, '"id":(\d+)').Groups[1].Value
    Check 'T-01 create http tool' (($created.code -eq 0) -and $toolId) "toolId=$toolId code=$($created.code)"

    $detail = Invoke-NovaApi -Path "/api/v1/tools/$toolId" -Token $token
    Check 'T-01 get tool detail' (($detail.code -eq 0) -and ($detail.raw -match $toolName)) "code=$($detail.code)"

    $updatedName = "HTTP CRUD Updated $suffix"
    $updatePath = Join-Path $script:NovaFlowTmpDir 'tool-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        toolName    = $toolName
        displayName = $updatedName
        description = 'qa updated'
        toolType    = 'http'
        method      = 'GET'
        url         = 'https://httpbin.org/get'
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/tools/$toolId" -Token $token -OutFile $updatePath
    Check 'T-01 update http tool' (($updated.code -eq 0) -and ($updated.raw -match $updatedName)) "code=$($updated.code)"

    $list = Invoke-NovaApi -Path "/api/v1/tools?page=1&pageSize=20&keyword=$toolName" -Token $token
    Check 'T-01 list tools' (($list.code -eq 0) -and ($list.raw -match $toolName)) "code=$($list.code)"

    $options = Invoke-NovaApi -Path '/api/v1/tools/options' -Token $token
    Check 'T-01 tool options' (($options.code -eq 0) -and ($options.raw -match $toolName)) "code=$($options.code)"

    $testPath = Join-Path $script:NovaFlowTmpDir 'tool-test.json'
    Write-NovaJson -Path $testPath -Data @{ arguments = @{} }
    $test = Invoke-NovaApi -Method POST -Path "/api/v1/tools/$toolId/test" -Token $token -OutFile $testPath -MaxTimeSec 45
    Check 'T-01 test http tool' ($test.code -eq 0) "http=$($test.http) code=$($test.code)"

    $deleted = Invoke-NovaApi -Method DELETE -Path "/api/v1/tools/$toolId" -Token $token
    Check 'T-01 delete http tool' ($deleted.code -eq 0) "code=$($deleted.code)"

    $gone = Invoke-NovaApi -Path "/api/v1/tools/$toolId" -Token $token
    Check 'T-01 tool gone after delete' ($gone.code -ne 0) "code=$($gone.code)"
} catch {
    Check 'http-tool-crud setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'http-tool-crud-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

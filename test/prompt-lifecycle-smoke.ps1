#requires -Version 7.0
# NovaFlow AI — Prompt 模板验收（P-01 ~ P-03）
# 用法: pwsh test/prompt-lifecycle-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'prompt-lifecycle-smoke.log'
$outFile = Join-Path $PSScriptRoot 'prompt-lifecycle-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== prompt-lifecycle-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $templateName = "Prompt-Smoke-$suffix"
    $contentV1 = "你是 QA 助手。用户问题：{{question}}"
    $contentV2 = "你是升级版 QA 助手。用户问题：{{question}}"

    $createPath = Join-Path $script:NovaFlowTmpDir 'prompt-create.json'
    Write-NovaJson -Path $createPath -Data @{
        templateName = $templateName
        description  = 'qa prompt smoke'
        category     = 'custom'
        content      = $contentV1
        variables    = @(@{ name = 'question'; type = 'string'; required = $true })
        visibility   = 'private'
        changeLog    = 'v1'
    }
    $created = Invoke-NovaApi -Method POST -Path '/api/v1/prompts' -Token $token -OutFile $createPath
    $promptId = [regex]::Match($created.raw, '"id":(\d+)').Groups[1].Value
    Check 'P-01 create prompt template' (($created.code -eq 0) -and $promptId) "promptId=$promptId code=$($created.code)"

    $detail = Invoke-NovaApi -Path "/api/v1/prompts/$promptId" -Token $token
    Check 'P-01 get prompt detail' (($detail.code -eq 0) -and ($detail.raw -match $templateName)) "code=$($detail.code)"

    $list = Invoke-NovaApi -Path "/api/v1/prompts?page=1&pageSize=20&keyword=$templateName" -Token $token
    Check 'P-01 list prompts' (($list.code -eq 0) -and ($list.raw -match $templateName)) "code=$($list.code)"

    $options = Invoke-NovaApi -Path "/api/v1/prompts/options?keyword=$templateName" -Token $token
    Check 'P-01 prompt options' (($options.code -eq 0) -and ($options.raw -match $templateName)) "code=$($options.code)"

    $updatePath = Join-Path $script:NovaFlowTmpDir 'prompt-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        templateName = $templateName
        description  = 'qa prompt smoke updated'
        category     = 'custom'
        content      = $contentV2
        variables    = @(@{ name = 'question'; type = 'string'; required = $true })
        visibility   = 'private'
        changeLog    = 'v2'
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/prompts/$promptId" -Token $token -OutFile $updatePath
    $afterUpdate = Invoke-NovaApi -Path "/api/v1/prompts/$promptId" -Token $token
    $updateOk = ($updated.code -eq 0) -and ($afterUpdate.code -eq 0) -and ($afterUpdate.raw -match 'QA')
    Check 'P-01 update prompt template' $updateOk "code=$($updated.code)"

    $versions = Invoke-NovaApi -Path "/api/v1/prompts/$promptId/versions" -Token $token
    $hasV1 = ($versions.code -eq 0) -and ($versions.raw -match '"version":1')
    $hasV2 = ($versions.code -eq 0) -and ($versions.raw -match '"version":2')
    Check 'P-02 list prompt versions' ($hasV1 -and $hasV2) "code=$($versions.code)"

    $rollback = Invoke-NovaApi -Method POST -Path "/api/v1/prompts/$promptId/rollback?version=1" -Token $token
    $afterRollback = Invoke-NovaApi -Path "/api/v1/prompts/$promptId" -Token $token
    $rollbackOk = ($rollback.code -eq 0) -and ($afterRollback.code -eq 0)
    Check 'P-02 rollback prompt version' $rollbackOk "code=$($rollback.code)"

    $testPath = Join-Path $script:NovaFlowTmpDir 'prompt-test.json'
    Write-NovaJson -Path $testPath -Data @{
        variables = @{ question = '什么是 NovaFlow？' }
    }
    $test = Invoke-NovaApi -Method POST -Path "/api/v1/prompts/$promptId/test" -Token $token -OutFile $testPath
    $testOk = ($test.code -eq 0) -and ($test.raw -match 'renderedPrompt|什么是 NovaFlow')
    Check 'P-03 prompt online test (render only)' $testOk "code=$($test.code)"

    $deleted = Invoke-NovaApi -Method DELETE -Path "/api/v1/prompts/$promptId" -Token $token
    Check 'P-01 delete prompt template' ($deleted.code -eq 0) "code=$($deleted.code)"

    $gone = Invoke-NovaApi -Path "/api/v1/prompts/$promptId" -Token $token
    Check 'P-01 prompt gone after delete' ($gone.code -ne 0) "code=$($gone.code)"
} catch {
    Check 'prompt-lifecycle setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'prompt-lifecycle-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

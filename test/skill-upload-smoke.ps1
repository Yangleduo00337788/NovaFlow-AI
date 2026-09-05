#requires -Version 7.0
# NovaFlow AI — Skill 上传验收（T-06）
# 用法: pwsh test/skill-upload-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'skill-upload-smoke.log'
$outFile = Join-Path $PSScriptRoot 'skill-upload-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$skillName = "qa_skill_$suffix"

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== skill-upload-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $skillMd = Join-Path $script:NovaFlowTmpDir "$skillName.md"
    @"
---
name: $skillName
description: qa skill upload smoke
---
# QA Skill
Smoke test skill content for T-06.
"@ | Set-Content -Path $skillMd -Encoding UTF8

    $uploadRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/skills/upload",
        '-H', "Authorization: $token",
        '-F', "file=@$skillMd;type=text/markdown"
    )
    $upload = ConvertFrom-NovaCurl $uploadRaw
    $skillId = [regex]::Match($upload.raw, '"id":(\d+)').Groups[1].Value
    Check 'T-06 upload skill markdown' (($upload.code -eq 0) -and $skillId) "skillId=$skillId code=$($upload.code)"

    $options = Invoke-NovaApi -Path '/api/v1/skills/options' -Token $token
    $listed = ($options.code -eq 0) -and ($options.raw -match $skillName)
    Check 'T-06 skill appears in options' $listed "code=$($options.code)"

    if ($skillId) {
        $reuploadRaw = Invoke-CurlExe @(
            '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
            '-X', 'POST',
            "$script:NovaFlowBaseUrl/api/v1/skills/$skillId/upload",
            '-H', "Authorization: $token",
            '-F', "file=@$skillMd;type=text/markdown"
        )
        $reupload = ConvertFrom-NovaCurl $reuploadRaw
        Check 'T-06 re-upload skill by id' ($reupload.code -eq 0) "code=$($reupload.code) http=$($reupload.http)"
    } else {
        Check 'T-06 re-upload skill by id' $false 'SKIP: no skill id'
    }
} catch {
    Check 'skill-upload setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'skill-upload-smoke' -Passed $allPass -Details @{
    suffix    = $suffix
    skillName = $skillName
    checks    = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

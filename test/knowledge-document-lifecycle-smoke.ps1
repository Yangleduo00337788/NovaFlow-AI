#requires -Version 7.0
# NovaFlow AI — 知识库文档生命周期（K-05）
# 用法: pwsh test/knowledge-document-lifecycle-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'knowledge-document-lifecycle-smoke.log'
$outFile = Join-Path $PSScriptRoot 'knowledge-document-lifecycle-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== knowledge-document-lifecycle-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $kbId = New-NovaKnowledgeBase -Token $token -Name "KB-DocLife-$suffix"

    $docPath = Join-Path $script:NovaFlowTmpDir 'doc-life.txt'
    [System.IO.File]::WriteAllText($docPath, "NovaFlow doc lifecycle $suffix`nLine two.", [System.Text.UTF8Encoding]::new($false))
    $uploadRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '120',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/knowledge-bases/$kbId/documents/upload",
        '-H', "Authorization: $token",
        '-F', "file=@$docPath;type=text/plain"
    )
    $upload = ConvertFrom-NovaCurl $uploadRaw
    $docId = [regex]::Match($upload.raw, '"id":(\d+)').Groups[1].Value
    Check 'K-05 upload document' (($upload.code -eq 0) -and $docId) "docId=$docId code=$($upload.code)"

    if ($docId) {
        $reprocess = Invoke-NovaApi -Method POST -Path "/api/v1/knowledge-bases/$kbId/documents/$docId/reprocess" -Token $token -MaxTimeSec 90
        Check 'K-05 reprocess document' ($reprocess.code -eq 0) "code=$($reprocess.code)"

        $deleted = Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$kbId/documents/$docId" -Token $token
        Check 'K-05 delete document' ($deleted.code -eq 0) "code=$($deleted.code)"

        $docs = Invoke-NovaApi -Path "/api/v1/knowledge-bases/$kbId/documents?page=1&pageSize=10" -Token $token
        $gone = ($docs.code -eq 0) -and ($docs.raw -notmatch "`"id`":$docId")
        Check 'K-05 document removed from list' $gone "code=$($docs.code)"
    } else {
        Check 'K-05 reprocess document' $false 'SKIP: upload failed'
        Check 'K-05 delete document' $false 'SKIP: upload failed'
        Check 'K-05 document removed from list' $false 'SKIP: upload failed'
    }

    Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$kbId" -Token $token | Out-Null
} catch {
    Check 'knowledge-document-lifecycle setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'knowledge-document-lifecycle-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

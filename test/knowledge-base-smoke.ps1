#requires -Version 7.0
# NovaFlow AI — 知识库验收（K-01, K-02）
# 用法: pwsh test/knowledge-base-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'knowledge-base-smoke.log'
$outFile = Join-Path $PSScriptRoot 'knowledge-base-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== knowledge-base-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $kbName = "KB-Smoke-$suffix"
    $kbId = New-NovaKnowledgeBase -Token $token -Name $kbName
    Check 'K-01 create knowledge base' ($kbId -gt 0) "kbId=$kbId"

    $detail = Invoke-NovaApi -Path "/api/v1/knowledge-bases/$kbId" -Token $token
    Check 'K-01 get knowledge base detail' ($detail.code -eq 0) "code=$($detail.code)"

    $updatedName = "KB-Smoke-Updated-$suffix"
    $updatePath = Join-Path $script:NovaFlowTmpDir 'kb-update.json'
    Write-NovaJson -Path $updatePath -Data @{
        kbName         = $updatedName
        description    = 'updated qa'
        embeddingModel = 'text-embedding-3-small'
    }
    $updated = Invoke-NovaApi -Method PUT -Path "/api/v1/knowledge-bases/$kbId" -Token $token -OutFile $updatePath
    Check 'K-01 update knowledge base' (($updated.code -eq 0) -and ($updated.raw -match $updatedName)) "code=$($updated.code)"

    $list = Invoke-NovaApi -Path '/api/v1/knowledge-bases?page=1&pageSize=20' -Token $token
    $listed = ($list.code -eq 0) -and ($list.raw -match $updatedName)
    Check 'K-01 list knowledge bases' $listed "code=$($list.code)"

    $docPath = Join-Path $script:NovaFlowTmpDir 'kb-doc.txt'
    [System.IO.File]::WriteAllText($docPath, "NovaFlow knowledge smoke $suffix`nSecond line for chunking.", [System.Text.UTF8Encoding]::new($false))
    $uploadRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '120',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/knowledge-bases/$kbId/documents/upload",
        '-H', "Authorization: $token",
        '-F', "file=@$docPath;type=text/plain"
    )
    $upload = ConvertFrom-NovaCurl $uploadRaw
    $docId = [regex]::Match($upload.raw, '"id":(\d+)').Groups[1].Value
    Check 'K-02 upload document' (($upload.code -eq 0) -and $docId) "docId=$docId code=$($upload.code)"

    if ($docId) {
        $docs = Invoke-NovaApi -Path "/api/v1/knowledge-bases/$kbId/documents?page=1&pageSize=10" -Token $token
        $hasDoc = ($docs.code -eq 0) -and ($docs.raw -match $docId)
        Check 'K-02 document listed' $hasDoc "code=$($docs.code)"
    } else {
        Check 'K-02 document listed' $false 'SKIP: upload failed'
    }

    $deleted = Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$kbId" -Token $token
    Check 'K-01 delete knowledge base' ($deleted.code -eq 0) "code=$($deleted.code)"
} catch {
    Check 'knowledge-base setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'knowledge-base-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

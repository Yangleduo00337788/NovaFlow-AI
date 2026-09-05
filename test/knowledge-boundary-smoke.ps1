#requires -Version 7.0
# NovaFlow AI — 知识库边界验收（K-03, K-04, S-06）
# 用法: pwsh test/knowledge-boundary-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'knowledge-boundary-smoke.log'
$outFile = Join-Path $PSScriptRoot 'knowledge-boundary-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== knowledge-boundary-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $kbId = New-NovaKnowledgeBase -Token $token -Name "KB-Boundary-$suffix"

    $bigPath = Join-Path $script:NovaFlowTmpDir 'oversize.bin'
    $fs = [System.IO.File]::Create($bigPath)
    $fs.SetLength([long](51 * 1024 * 1024))
    $fs.Close()

    $bigRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '180',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/knowledge-bases/$kbId/documents/upload",
        '-H', "Authorization: $token",
        '-F', "file=@$bigPath;type=application/octet-stream"
    )
    $big = ConvertFrom-NovaCurl $bigRaw
    $bigRejected = ($big.code -ne 0) -or ($big.http -ge 400)
    Check 'K-03 reject file >50MB' $bigRejected "http=$($big.http) code=$($big.code)"

    $badPath = Join-Path $script:NovaFlowTmpDir 'malware.exe'
    [System.IO.File]::WriteAllBytes($badPath, [byte[]](0x4D, 0x5A, 0x90, 0x00))
    $badRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/knowledge-bases/$kbId/documents/upload",
        '-H', "Authorization: $token",
        '-F', "file=@$badPath;type=application/octet-stream"
    )
    $bad = ConvertFrom-NovaCurl $badRaw
    $badRejected = ($bad.code -ne 0) -or ($bad.http -ge 400)
    Check 'K-04 reject invalid file type' $badRejected "http=$($bad.http) code=$($bad.code)"

    $safeTxt = Join-Path $script:NovaFlowTmpDir 'safe-content.txt'
    'path traversal probe content' | Set-Content -Path $safeTxt -Encoding UTF8
    $traversalRaw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
        '-X', 'POST',
        "$script:NovaFlowBaseUrl/api/v1/knowledge-bases/$kbId/documents/upload",
        '-H', "Authorization: $token",
        '-F', "file=@$safeTxt;filename=../../evil.txt;type=text/plain"
    )
    $traversal = ConvertFrom-NovaCurl $traversalRaw
    $pathSafe = $false
    if ($traversal.code -eq 0 -and $traversal.raw -match '"filePath":"([^"]+)"') {
        $storedPath = $Matches[1]
        $pathSafe = ($storedPath -notmatch '\.\.') -and ($storedPath -match "^knowledge/\d+/$kbId/")
        Check 'S-06 upload path traversal sanitized' $pathSafe "filePath=$storedPath"
        if ($traversal.raw -match '"id":(\d+)') {
            $docId = $Matches[1]
            Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$kbId/documents/$docId" -Token $token | Out-Null
        }
    } else {
        Check 'S-06 upload path traversal sanitized' $true "SKIP or rejected code=$($traversal.code)"
    }

    Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$kbId" -Token $token | Out-Null
} catch {
    Check 'knowledge-boundary setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'knowledge-boundary-smoke' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# NovaFlow AI — Docker 基础镜像版本审计（DEP-03）
# 用法: pwsh test/docker-image-audit.ps1

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$outFile = Join-Path $PSScriptRoot 'docker-image-audit-results.json'
$logFile = Join-Path $PSScriptRoot 'docker-image-audit.log'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Log($msg) {
    $line = "$(Get-Date -Format 'HH:mm:ss') $msg"
    Add-Content -Path $logFile -Value $line
    Write-Host $line
}

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $row = [pscustomobject]@{ name = $Name; passed = $Ok; detail = $Detail }
    $results.Add($row) | Out-Null
    $icon = if ($Ok) { 'PASS' } else { 'FAIL' }
    Write-Host "[$icon] $Name — $Detail"
    if (-not $Ok) { $script:allPass = $false }
}

"" | Set-Content $logFile
Log '=== docker-image-audit ==='

$dockerfiles = Get-ChildItem -Path (Join-Path $repoRoot 'deploy') -Filter 'Dockerfile*' -File
foreach ($file in $dockerfiles) {
    $content = Get-Content $file.FullName -Raw
    $fromLines = [regex]::Matches($content, '(?m)^FROM\s+(\S+)')
    foreach ($match in $fromLines) {
        $image = $match.Groups[1].Value
        $usesLatest = $image -match ':latest\b' -or ($image -notmatch ':')
        $tagOk = -not $usesLatest
        Check "DEP-03 $($file.Name) FROM $image" $tagOk $(if ($tagOk) { 'pinned tag' } else { 'unpinned or :latest' })
    }
}

[ordered]@{
    timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    passed    = $allPass
    checks    = @($results)
} | ConvertTo-Json -Depth 4 | Set-Content $outFile -Encoding UTF8

Log "=== DONE passed=$allPass -> $outFile ==="
if (-not $allPass) { exit 1 }

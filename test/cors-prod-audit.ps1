#requires -Version 7.0
# NovaFlow AI — 生产 CORS 配置审计（PR-06）
# 用法: pwsh test/cors-prod-audit.ps1

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$outFile = Join-Path $PSScriptRoot 'cors-prod-audit-results.json'
$logFile = Join-Path $PSScriptRoot 'cors-prod-audit.log'
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
Log '=== cors-prod-audit ==='

$prodYml = Get-Content (Join-Path $repoRoot 'novaflow-server/src/main/resources/application-prod.yml') -Raw
Check 'PR-06 prod cors uses CORS_ALLOWED_ORIGIN' ($prodYml -match 'CORS_ALLOWED_ORIGIN') 'env var referenced'
Check 'PR-06 prod cors localhost off by default' ($prodYml -match 'allow-localhost:\s*\$\{NOVAFLOW_CORS_ALLOW_LOCALHOST:false\}') 'default false'

$validator = Get-Content (Join-Path $repoRoot 'novaflow-server/src/main/java/ai/novaflow/server/config/ProdSecurityValidator.java') -Raw
Check 'PR-06 validator rejects wildcard cors' ($validator -match '禁止空值或 \*') 'wildcard blocked'
Check 'PR-06 validator rejects localhost cors' ($validator -match '禁止 localhost') 'localhost blocked'

$compose = Get-Content (Join-Path $repoRoot 'deploy/docker-compose.prod.yml') -Raw
Check 'PR-06 compose requires CORS_ALLOWED_ORIGIN' ($compose -match 'CORS_ALLOWED_ORIGIN:\s*\$\{CORS_ALLOWED_ORIGIN:\?') 'required in compose'

[ordered]@{
    timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    passed    = $allPass
    checks    = @($results)
} | ConvertTo-Json -Depth 4 | Set-Content $outFile -Encoding UTF8

Log "=== DONE passed=$allPass -> $outFile ==="
if (-not $allPass) { exit 1 }

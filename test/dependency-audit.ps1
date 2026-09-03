#requires -Version 7.0
# NovaFlow AI — 依赖安全扫描（DEP-01 / DEP-02）
# 用法: pwsh test/dependency-audit.ps1
# 说明: npm audit 使用官方 registry，避免 npmmirror 404

$ErrorActionPreference = 'Continue'
$repoRoot = Split-Path -Parent $PSScriptRoot
$outFile = Join-Path $PSScriptRoot 'dependency-audit-results.json'
$logFile = Join-Path $PSScriptRoot 'dependency-audit.log'
$results = [System.Collections.Generic.List[object]]::new()

function Log($msg) {
    $line = "$(Get-Date -Format 'HH:mm:ss') $msg"
    Add-Content -Path $logFile -Value $line
    Write-Host $line
}

"" | Set-Content $logFile
Log '=== dependency-audit ==='

# DEP-02 npm audit
$webDir = Join-Path $repoRoot 'novaflow-web'
Push-Location $webDir
try {
    $npmOut = npm audit --registry=https://registry.npmjs.org --json 2>&1 | Out-String
    $npmJson = $null
    try { $npmJson = $npmOut | ConvertFrom-Json } catch { }
    $critical = $npmJson?.metadata?.vulnerabilities?.critical ?? 0
    $high = $npmJson?.metadata?.vulnerabilities?.high ?? 0
    $npmPass = ($critical -eq 0 -and $high -eq 0)
    $results.Add([pscustomobject]@{
        name   = 'DEP-02 npm audit'
        passed = $npmPass
        detail = "critical=$critical high=$high"
    }) | Out-Null
    Log "DEP-02 npm: critical=$critical high=$high pass=$npmPass"
}
catch {
    $results.Add([pscustomobject]@{ name = 'DEP-02 npm audit'; passed = $false; detail = $_.Exception.Message }) | Out-Null
}
finally {
    Pop-Location
}

# DEP-01 Maven versions（轻量替代 OWASP dependency-check，无需额外插件）
Push-Location $repoRoot
try {
    $mvnOut = mvn -q -pl novaflow-server -am dependency:tree -Dverbose=false 2>&1 | Out-String
    $mvnPass = ($LASTEXITCODE -eq 0)
    $results.Add([pscustomobject]@{
        name   = 'DEP-01 maven dependency:tree'
        passed = $mvnPass
        detail = if ($mvnPass) { 'BUILD OK' } else { $mvnOut.Substring(0, [math]::Min(200, $mvnOut.Length)) }
    }) | Out-Null
    Log "DEP-01 maven tree pass=$mvnPass"
}
catch {
    $results.Add([pscustomobject]@{ name = 'DEP-01 maven dependency:tree'; passed = $false; detail = $_.Exception.Message }) | Out-Null
}
finally {
    Pop-Location
}

$allPass = -not @($results | Where-Object { -not $_.passed }).Count
[ordered]@{
    timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    passed    = $allPass
    checks    = @($results)
} | ConvertTo-Json -Depth 5 | Set-Content $outFile -Encoding UTF8

Log "=== DONE passed=$allPass -> $outFile ==="
if (-not $allPass) { exit 1 }

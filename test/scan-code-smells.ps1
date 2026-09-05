#requires -Version 7.0
# NovaFlow AI — ST-01 代码异味关键字扫描
# 用法: pwsh test/scan-code-smells.ps1

param(
    [string]$RepoRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = 'Stop'
$outFile = Join-Path $PSScriptRoot 'scan-code-smells-results.json'
$logFile = Join-Path $PSScriptRoot 'scan-code-smells.log'
$results = [System.Collections.Generic.List[object]]::new()

function Log($msg) {
    $line = "$(Get-Date -Format 'HH:mm:ss') $msg"
    Add-Content -Path $logFile -Value $line
    Write-Host $line
}

"" | Set-Content $logFile
Log '=== scan-code-smells ==='

$extensions = @('*.java', '*.ts', '*.tsx', '*.vue', '*.yml', '*.yaml', '*.ps1')
$excludeDirs = '(\\|/)(target|node_modules|\\.git|dist|build|\\.idea|test-results)(\\|/)'
$patterns = @(
    @{ name = 'TODO/FIXME in prod code'; regex = '\b(TODO|FIXME|HACK)\b'; allow = @('test/', 'docs/', 'TEST-', 'QA-', 'scan-code-smells.ps1') }
    @{ name = 'console.log in frontend'; regex = '\bconsole\.log\('; allow = @('e2e/', 'test/', '.spec.') }
    @{ name = 'System.out.println'; regex = 'System\.out\.println\('; allow = @('test/') }
    @{ name = 'debug logging in prod profile'; regex = 'ai\.novaflow:\s*debug'; allow = @('application.yml', 'application-dev', 'application-test', 'ProdLoggingConfigTest') }
)

$findings = [System.Collections.Generic.List[string]]::new()
$scanned = 0

foreach ($ext in $extensions) {
    Get-ChildItem -Path $RepoRoot -Recurse -File -Filter $ext -ErrorAction SilentlyContinue | ForEach-Object {
        $rel = $_.FullName.Substring($RepoRoot.Length).TrimStart('\', '/')
        if ($rel -match $excludeDirs) { return }
        $scanned++
        $lines = Get-Content -LiteralPath $_.FullName -ErrorAction SilentlyContinue
        if (-not $lines) { return }
        for ($i = 0; $i -lt $lines.Count; $i++) {
            $line = $lines[$i]
            foreach ($p in $patterns) {
                if ($line -notmatch $p.regex) { continue }
                $allowed = $false
                foreach ($a in $p.allow) {
                    if ($rel -like "*$a*") { $allowed = $true; break }
                }
                if (-not $allowed) {
                    $findings.Add("$($p.name) | $rel`:$($i + 1) | $($line.Trim())")
                }
            }
        }
    }
}

$passed = ($findings.Count -eq 0)
$results.Add([pscustomobject]@{ name = 'ST-01 code smell scan'; passed = $passed; detail = "scanned=$scanned findings=$($findings.Count)" }) | Out-Null
if (-not $passed) {
    $findings | Select-Object -First 30 | ForEach-Object { Log "FINDING: $_" }
}

[ordered]@{
    script    = 'scan-code-smells'
    timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    passed    = $passed
    scanned   = $scanned
    findings  = @($findings)
} | ConvertTo-Json -Depth 6 | Set-Content $outFile -Encoding UTF8

Log "=== DONE passed=$passed findings=$($findings.Count) -> $outFile ==="
if (-not $passed) { exit 1 }

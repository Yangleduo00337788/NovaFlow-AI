#requires -Version 7.0
# NovaFlow AI — ST-02 硬编码凭证扫描
# 用法: pwsh test/scan-hardcoded-secrets.ps1
# 返回码 0=通过，1=发现未豁免的疑似硬编码凭证

param(
    [string]$RepoRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = 'Stop'

$extensions = @('*.java', '*.ts', '*.tsx', '*.vue', '*.yml', '*.yaml', '*.properties', '*.ps1', '*.md', '*.json')
$excludeDirs = '(\\|/)(target|node_modules|\\.git|dist|build|\\.idea|test-results)(\\|/)'
$allowPathPatterns = @(
    'test[\\/].*\.(md|json|ps1)$',
    'TEST-CHECKLIST\.md$',
    'SCRIPTS\.md$',
    'SECURITY-AUDIT-REPORT\.md$',
    '\.env\.example$',
    'application-test\.yml$',
    'global\.setup\.ts$',
    'auth\.ts$',
    'roles\.spec\.ts$',
    'rbac-api-acceptance\.ps1$',
    'NovaFlow-TestCommon\.ps1$',
    'IntegrationTestSupport\.java$',
    'AbstractLocalIntegrationTest\.java$',
    'PasswordEncoder',
    'password_hash',
    'passwordHash',
    'confirmPassword',
    'User123!',
    'Admin123!',
    'Developer123!',
    'Platform123!',
    'Auditor123!',
    'Operator123!',
    'Viewer123!',
    'SmokeTest123!',
    'WrongPassword123!',
    'scan-hardcoded-secrets-results\.json$',
    '-results\.json$',
    'test-access-key',
    'test-secret-key',
    'sk-test-key',
    'scripts[\\/]test-platform',
    'NovaFlowAI-DevKey',
    'changeme',
    'your-',
    'example\.com',
    'placeholder',
    'masked',
    '\*\*\*\*'
)

$secretPatterns = @(
    @{ name = 'OpenAI-style sk- key'; regex = 'sk-[A-Za-z0-9]{20,}' }
    @{ name = 'Live API key'; regex = 'nf_live_[A-Za-z0-9]{16,}' }
    @{ name = 'AWS access key'; regex = 'AKIA[0-9A-Z]{16}' }
    @{ name = 'Bearer hardcoded JWT'; regex = 'Bearer eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+' }
    @{ name = 'password assignment'; regex = '(?i)(password|passwd|secret|api[_-]?key)\s*[:=]\s*["''][^{][^"'']{8,}["'']' }
)

$findings = [System.Collections.Generic.List[string]]::new()
$scanned = 0

function Test-AllowedLine {
    param([string]$Line)
    foreach ($pat in $allowPathPatterns) {
        if ($Line -match $pat) { return $true }
    }
    return $false
}

foreach ($ext in $extensions) {
    Get-ChildItem -Path $RepoRoot -Recurse -Filter $ext -File -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch $excludeDirs } |
        ForEach-Object {
            $scanned++
            $rel = $_.FullName.Substring($RepoRoot.Length).TrimStart('\', '/')
            $lineNo = 0
            foreach ($line in Get-Content -Path $_.FullName -ErrorAction SilentlyContinue) {
                $lineNo++
                if (Test-AllowedLine $line) { continue }
                foreach ($pat in $secretPatterns) {
                    if ($line -match $pat.regex) {
                        $findings.Add("$rel`:$lineNo [$($pat.name)] $($line.Trim())")
                    }
                }
            }
        }
}

$outFile = Join-Path $PSScriptRoot 'scan-hardcoded-secrets-results.json'
$result = [ordered]@{
    timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    passed    = ($findings.Count -eq 0)
    scanned   = $scanned
    findings  = @($findings)
}
$result | ConvertTo-Json -Depth 6 | Set-Content $outFile -Encoding UTF8

Write-Host "Files scanned: $scanned"
if ($findings.Count -eq 0) {
    Write-Host 'ST-02 scan PASS — no unallowlisted hardcoded secrets'
    exit 0
}

Write-Host "ST-02 scan FAIL — $($findings.Count) finding(s):"
foreach ($f in $findings) { Write-Host "  $f" }
exit 1

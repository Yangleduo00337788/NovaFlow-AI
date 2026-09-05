#requires -Version 7.0
# NovaFlow AI — 账单/日志导出验收（B-05）
# 用法: pwsh test/billing-export-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'billing-export-smoke.log'
$outFile = Join-Path $PSScriptRoot 'billing-export-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Invoke-NovaBinaryExport {
    param(
        [string]$Path,
        [string]$Token
    )
    $url = "$script:NovaFlowBaseUrl$Path"
    $raw = Invoke-CurlExe @(
        '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
        '-H', "Authorization: $Token",
        $url
    )
    $http = 0
    if ($raw -match "`nHTTP:(\d+)$") {
        $http = [int]$Matches[1]
        $raw = $raw -replace "`nHTTP:\d+$", ''
    }
    return [pscustomobject]@{ http = $http; size = $raw.Length; raw = $raw }
}

Write-NovaLog '=== billing-export-smoke ===' $logFile

try {
    $token = Get-NovaLoginToken
    $month = (Get-Date -Format 'yyyy-MM')

    $excel = Invoke-NovaBinaryExport -Path "/api/v1/billing/export?month=$month&format=excel" -Token $token
    $excelOk = ($excel.http -eq 200) -and ($excel.size -gt 100)
    Check 'B-05 billing excel export' $excelOk "http=$($excel.http) bytes=$($excel.size)"

    $pdf = Invoke-NovaBinaryExport -Path "/api/v1/billing/export?month=$month&format=pdf" -Token $token
    $pdfOk = ($pdf.http -eq 200) -and ($pdf.size -gt 100) -and ($pdf.raw.StartsWith('%PDF'))
    Check 'B-05 billing pdf export' $pdfOk "http=$($pdf.http) bytes=$($pdf.size)"

    $csv = Invoke-NovaBinaryExport -Path '/api/v1/token-usage/logs/export' -Token $token
    $csvOk = ($csv.http -eq 200) -and ($csv.size -ge 0)
    Check 'B-05 token usage logs export' $csvOk "http=$($csv.http) bytes=$($csv.size)"
} catch {
    Check 'billing-export setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'billing-export-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

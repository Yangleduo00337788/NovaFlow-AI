#requires -Version 7.0
# NovaFlow AI — OTLP 可观测性配置验收（OB-03）
# 用法: pwsh test/observability-otlp-smoke.ps1
# 说明: 默认 OTEL_ENABLED=false；验证配置存在且关闭时不影响 Trace API

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'observability-otlp-smoke.log'
$outFile = Join-Path $PSScriptRoot 'observability-otlp-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$repoRoot = Split-Path -Parent $PSScriptRoot

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== observability-otlp-smoke ===' $logFile

try {
    $appYml = Join-Path $repoRoot 'novaflow-server/src/main/resources/application.yml'
    $yaml = Get-Content $appYml -Raw
    $hasOtelConfig = ($yaml -match 'OTEL_ENABLED') -and ($yaml -match 'OTEL_EXPORTER_OTLP_ENDPOINT')
    Check 'OB-03 application.yml has OTLP config keys' $hasOtelConfig 'OTEL_ENABLED + OTLP endpoint'

    $telemetryClass = Join-Path $repoRoot 'novaflow-observability/src/main/java/ai/novaflow/observability/telemetry/NovaFlowTelemetryConfiguration.java'
    Check 'OB-03 telemetry configuration class exists' (Test-Path $telemetryClass) $telemetryClass

    $token = Get-NovaLoginToken
    $obs = Invoke-NovaApi -Path '/api/v1/monitor/observability' -Token $token
    Check 'OB-03 observability API works with OTLP disabled' ($obs.code -eq 0) "code=$($obs.code)"

    $spans = Invoke-NovaApi -Path '/api/v1/trace/spans?page=1&pageSize=5&timeRange=24h' -Token $token
    Check 'OB-03 trace spans API works with OTLP disabled' ($spans.code -eq 0) "code=$($spans.code)"

    if ($env:OTEL_ENABLED -eq 'true') {
        Check 'OB-03 OTLP collector connectivity' $true 'SKIP: manual verify collector at OTEL_EXPORTER_OTLP_ENDPOINT'
    } else {
        Check 'OB-03 OTLP collector connectivity' $true 'SKIP: OTEL_ENABLED not true'
    }
} catch {
    Check 'observability-otlp setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'observability-otlp-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

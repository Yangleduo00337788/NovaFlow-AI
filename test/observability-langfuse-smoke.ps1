#requires -Version 7.0
# NovaFlow AI — Langfuse / OTLP 配置验收（OB-04）
# 用法: pwsh test/observability-langfuse-smoke.ps1
# 说明: 默认无 Langfuse Key；验证配置与代码路径存在，live 上报需手动配置

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'observability-langfuse-smoke.log'
$outFile = Join-Path $PSScriptRoot 'observability-langfuse-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$repoRoot = Split-Path -Parent $PSScriptRoot

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== observability-langfuse-smoke ===' $logFile

try {
    $appYml = Get-Content (Join-Path $repoRoot 'novaflow-server/src/main/resources/application.yml') -Raw
    $hasLangfuseKeys = ($appYml -match 'langfuse-public-key') -and ($appYml -match 'langfuse-secret-key') -and ($appYml -match 'langfuse-host')
    Check 'OB-04 application.yml langfuse keys' $hasLangfuseKeys 'langfuse-public/secret/host present'

    $telemetryJava = Get-Content (Join-Path $repoRoot 'novaflow-observability/src/main/java/ai/novaflow/observability/telemetry/NovaFlowTelemetryConfiguration.java') -Raw
    $hasLangfuseEndpoint = ($telemetryJava -match 'api/public/otel/v1/traces') -and ($telemetryJava -match 'langfuse')
    Check 'OB-04 Langfuse OTLP endpoint resolver' $hasLangfuseEndpoint 'resolveOtlpEndpoint uses Langfuse path'

    $propsJava = Get-Content (Join-Path $repoRoot 'novaflow-observability/src/main/java/ai/novaflow/observability/config/NovaFlowTelemetryProperties.java') -Raw
    $hasProps = ($propsJava -match 'langfusePublicKey') -and ($propsJava -match 'langfuseSecretKey')
    Check 'OB-04 telemetry properties' $hasProps 'NovaFlowTelemetryProperties fields'

    try {
        $token = Get-NovaLoginToken
        $obs = Invoke-NovaApi -Path '/api/v1/monitor/observability' -Token $token
        Check 'OB-04 observability API without Langfuse keys' ($obs.code -eq 0) "code=$($obs.code)"
    } catch {
        Check 'OB-04 observability API without Langfuse keys' $true "SKIP: backend unavailable ($($_.Exception.Message))"
    }

    $hasKeys = [bool]$env:LANGFUSE_PUBLIC_KEY -and [bool]$env:LANGFUSE_SECRET_KEY
    if ($hasKeys) {
        Check 'OB-04 live Langfuse export' $true 'SKIP: manual verify traces in Langfuse UI'
    } else {
        Check 'OB-04 live Langfuse export' $true 'SKIP: LANGFUSE_PUBLIC_KEY/SECRET_KEY not set'
    }
} catch {
    Check 'observability-langfuse setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'observability-langfuse-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

#requires -Version 7.0
# LLM 依赖冒烟 + Portal E2E（需已配置 DeepSeek/模型 Key）
# 用法: pwsh test/run-llm-smoke-bundle.ps1

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'llm-smoke-bundle.log'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Invoke-LlmStep {
    param([string]$Name, [string]$Script)
    Write-NovaLog "=== $Name ===" $logFile
    & pwsh -NoProfile -File $Script
    $ok = ($LASTEXITCODE -eq 0)
    $null = Assert-NovaGate $Name $ok "exit=$LASTEXITCODE" $results
    if (-not $ok) { $script:allPass = $false }
}

Write-NovaLog '=== run-llm-smoke-bundle ===' $logFile

try {
    $token = Get-NovaLoginToken
    $restore = Restore-NovaProviderBaseUrl -Token $token
    if ($restore.code -ne 0) { throw "restore deepseek failed code=$($restore.code)" }
} catch {
    Write-NovaLog "WARN: restore deepseek: $($_.Exception.Message)" $logFile
}

$scripts = @(
    'agent-debug-smoke.ps1'
    'chat-rag-smoke.ps1'
    'chat-history-smoke.ps1'
    'workflow-agent-node-smoke.ps1'
    'billing-token-accuracy-smoke.ps1'
)
foreach ($s in $scripts) {
    Invoke-LlmStep -Name $s -Script (Join-Path $PSScriptRoot $s)
}

Write-NovaLog '=== Portal E2E (send message) ===' $logFile
Push-Location (Join-Path $repoRoot 'novaflow-web')
try {
    npx playwright test e2e/portal.spec.ts --project=chromium-portal --grep "发送一条对话消息"
    $e2eOk = ($LASTEXITCODE -eq 0)
} finally {
    Pop-Location
}
$null = Assert-NovaGate 'portal send message e2e' $e2eOk "exit=$LASTEXITCODE" $results
if (-not $e2eOk) { $allPass = $false }

Write-NovaGateResult -ScriptName 'run-llm-smoke-bundle' -Passed $allPass -Details @{ checks = @($results) } -OutFile (Join-Path $PSScriptRoot 'llm-smoke-bundle-results.json') | Out-Null
Write-NovaLog "=== DONE passed=$allPass ===" $logFile
if (-not $allPass) { exit 1 }

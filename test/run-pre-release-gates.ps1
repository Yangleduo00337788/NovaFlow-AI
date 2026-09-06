#requires -Version 7.0
# NovaFlow AI — 上线前门禁一键执行
# 用法: pwsh test/run-pre-release-gates.ps1 [-SkipConcurrency] [-SkipFaultInjection] [-IncludeProdCompose]
#
# 依次执行:
#   1. pre-deploy-gate.ps1
#   2. cross-tenant-idor.ps1 + rbac-api-acceptance.ps1
#   3. coverage-gap-smoke.ps1 + chat-rag-smoke.ps1
#   4. 各模块 smoke / 安全扫描
#   5. dependency-audit.ps1
#   6. publish-concurrency-gate.ps1 (可选)
#   7. fault-injection.ps1 (可选，需 Docker)
#   8. prod-compose-smoke.ps1 (可选)

param(
    [switch]$SkipConcurrency,
    [switch]$SkipFaultInjection,
    [switch]$IncludeProdCompose
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$summaryFile = Join-Path $PSScriptRoot 'pre-release-gates-summary.json'
$steps = [System.Collections.Generic.List[object]]::new()
$failed = 0

function Invoke-GateStep {
    param([string]$Name, [string]$ScriptPath, [string[]]$ExtraArgs = @())
    Write-Host "`n========== $Name ==========" -ForegroundColor Cyan
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        & pwsh -NoProfile -File $ScriptPath @ExtraArgs
        $exit = $LASTEXITCODE
        if ($null -eq $exit) { $exit = 0 }
        $passed = ($exit -eq 0)
    }
    catch {
        $passed = $false
        $exit = 1
        Write-Host $_.Exception.Message -ForegroundColor Red
    }
    $sw.Stop()
    $steps.Add([pscustomobject]@{
        name     = $Name
        script   = $ScriptPath
        passed   = $passed
        exitCode = $exit
        ms       = $sw.ElapsedMilliseconds
    }) | Out-Null
    if (-not $passed) { $script:failed++ }
}

Invoke-GateStep 'Pre-deploy gate' (Join-Path $PSScriptRoot 'pre-deploy-gate.ps1')
Invoke-GateStep 'Cross-tenant IDOR' (Join-Path $PSScriptRoot 'cross-tenant-idor.ps1')
Invoke-GateStep 'RBAC API acceptance' (Join-Path $PSScriptRoot 'rbac-api-acceptance.ps1')
Invoke-GateStep 'API permissions scan' (Join-Path $PSScriptRoot 'scan-api-permissions.ps1')
Invoke-GateStep 'Platform tenant onboarding' (Join-Path $PSScriptRoot 'platform-tenant-onboarding-smoke.ps1')
Invoke-GateStep 'Platform maintenance smoke' (Join-Path $PSScriptRoot 'platform-maintenance-smoke.ps1')
Invoke-GateStep 'Platform risk smoke' (Join-Path $PSScriptRoot 'platform-risk-smoke.ps1')
Invoke-GateStep 'Platform storage quota smoke' (Join-Path $PSScriptRoot 'platform-storage-quota-smoke.ps1')
Invoke-GateStep 'Coverage gap smoke' (Join-Path $PSScriptRoot 'coverage-gap-smoke.ps1')
Invoke-GateStep 'Auth lifecycle smoke' (Join-Path $PSScriptRoot 'auth-lifecycle-smoke.ps1')
Invoke-GateStep 'Auth lock smoke' (Join-Path $PSScriptRoot 'auth-lock-smoke.ps1')
Invoke-GateStep 'API boundary smoke' (Join-Path $PSScriptRoot 'api-boundary-smoke.ps1')
Invoke-GateStep 'Open API acceptance' (Join-Path $PSScriptRoot 'open-api-acceptance.ps1')
Invoke-GateStep 'Agent debug smoke' (Join-Path $PSScriptRoot 'agent-debug-smoke.ps1')
Invoke-GateStep 'Conversation key isolation' (Join-Path $PSScriptRoot 'conversation-key-isolation.ps1')
Invoke-GateStep 'HTTP tool SSRF' (Join-Path $PSScriptRoot 'http-tool-ssrf.ps1')
Invoke-GateStep 'MCP command whitelist' (Join-Path $PSScriptRoot 'mcp-command-whitelist.ps1')
Invoke-GateStep 'Model API key encryption' (Join-Path $PSScriptRoot 'model-api-key-encryption.ps1')
Invoke-GateStep 'Model lifecycle smoke' (Join-Path $PSScriptRoot 'model-lifecycle-smoke.ps1')
Invoke-GateStep 'Registration disabled gate' (Join-Path $PSScriptRoot 'registration-disabled-gate.ps1')
Invoke-GateStep 'Hardcoded secrets scan' (Join-Path $PSScriptRoot 'scan-hardcoded-secrets.ps1')
Invoke-GateStep 'Code smell scan' (Join-Path $PSScriptRoot 'scan-code-smells.ps1')
Invoke-GateStep 'Org extended smoke' (Join-Path $PSScriptRoot 'org-extended-smoke.ps1')
Invoke-GateStep 'Dashboard extended smoke' (Join-Path $PSScriptRoot 'dashboard-extended-smoke.ps1')
Invoke-GateStep 'Agent lifecycle smoke' (Join-Path $PSScriptRoot 'agent-lifecycle-smoke.ps1')
Invoke-GateStep 'Member management smoke' (Join-Path $PSScriptRoot 'member-management-smoke.ps1')
Invoke-GateStep 'MCP server smoke' (Join-Path $PSScriptRoot 'mcp-server-smoke.ps1')
Invoke-GateStep 'Agent bindings smoke' (Join-Path $PSScriptRoot 'agent-bindings-smoke.ps1')
Invoke-GateStep 'Agent tool execution smoke' (Join-Path $PSScriptRoot 'agent-tool-execution-smoke.ps1')
Invoke-GateStep 'Chat history smoke' (Join-Path $PSScriptRoot 'chat-history-smoke.ps1')
Invoke-GateStep 'Prompt lifecycle smoke' (Join-Path $PSScriptRoot 'prompt-lifecycle-smoke.ps1')
Invoke-GateStep 'Application lifecycle smoke' (Join-Path $PSScriptRoot 'application-lifecycle-smoke.ps1')
Invoke-GateStep 'Audit access smoke' (Join-Path $PSScriptRoot 'audit-access-smoke.ps1')
Invoke-GateStep 'Knowledge base smoke' (Join-Path $PSScriptRoot 'knowledge-base-smoke.ps1')
Invoke-GateStep 'Knowledge boundary smoke' (Join-Path $PSScriptRoot 'knowledge-boundary-smoke.ps1')
Invoke-GateStep 'Knowledge document lifecycle smoke' (Join-Path $PSScriptRoot 'knowledge-document-lifecycle-smoke.ps1')
Invoke-GateStep 'Chat RAG smoke' (Join-Path $PSScriptRoot 'chat-rag-smoke.ps1')
Invoke-GateStep 'Workflow lifecycle smoke' (Join-Path $PSScriptRoot 'workflow-lifecycle-smoke.ps1')
Invoke-GateStep 'Workflow agent node smoke' (Join-Path $PSScriptRoot 'workflow-agent-node-smoke.ps1')
Invoke-GateStep 'Workflow idempotency smoke' (Join-Path $PSScriptRoot 'workflow-idempotency-smoke.ps1')
Invoke-GateStep 'HTTP tool CRUD smoke' (Join-Path $PSScriptRoot 'http-tool-crud-smoke.ps1')
Invoke-GateStep 'Skill upload smoke' (Join-Path $PSScriptRoot 'skill-upload-smoke.ps1')
Invoke-GateStep 'Portal studio isolation' (Join-Path $PSScriptRoot 'portal-studio-isolation-smoke.ps1')
Invoke-GateStep 'Billing overview smoke' (Join-Path $PSScriptRoot 'billing-overview-smoke.ps1')
Invoke-GateStep 'Billing manage smoke' (Join-Path $PSScriptRoot 'billing-manage-smoke.ps1')
Invoke-GateStep 'Billing token accuracy smoke' (Join-Path $PSScriptRoot 'billing-token-accuracy-smoke.ps1')
Invoke-GateStep 'Billing alert smoke' (Join-Path $PSScriptRoot 'billing-alert-smoke.ps1')
Invoke-GateStep 'Observability smoke' (Join-Path $PSScriptRoot 'observability-smoke.ps1')
Invoke-GateStep 'Global search smoke' (Join-Path $PSScriptRoot 'global-search-smoke.ps1')
Invoke-GateStep 'Restore deepseek provider' (Join-Path $PSScriptRoot 'restore-deepseek-provider.ps1')
Invoke-GateStep 'LLM fault smoke' (Join-Path $PSScriptRoot 'llm-fault-smoke.ps1')
Invoke-GateStep 'Agent concurrent update smoke' (Join-Path $PSScriptRoot 'agent-concurrent-update-smoke.ps1')
Invoke-GateStep 'Auth rate limit smoke' (Join-Path $PSScriptRoot 'auth-rate-limit-smoke.ps1')
Invoke-GateStep 'Open API rate limit smoke' (Join-Path $PSScriptRoot 'open-api-rate-limit-smoke.ps1')
Invoke-GateStep 'Knowledge quota smoke' (Join-Path $PSScriptRoot 'knowledge-quota-smoke.ps1')
Invoke-GateStep 'Billing export smoke' (Join-Path $PSScriptRoot 'billing-export-smoke.ps1')
Invoke-GateStep 'Workflow invalid graph smoke' (Join-Path $PSScriptRoot 'workflow-invalid-graph-smoke.ps1')
Invoke-GateStep 'Workflow dashboard runtime smoke' (Join-Path $PSScriptRoot 'workflow-dashboard-runtime-smoke.ps1')
Invoke-GateStep 'Chat redis memory smoke' (Join-Path $PSScriptRoot 'chat-redis-memory-smoke.ps1')
Invoke-GateStep 'Prompt agent bind smoke' (Join-Path $PSScriptRoot 'prompt-agent-bind-smoke.ps1')
Invoke-GateStep 'Model sync smoke' (Join-Path $PSScriptRoot 'model-sync-smoke.ps1')
Invoke-GateStep 'Observability OTLP smoke' (Join-Path $PSScriptRoot 'observability-otlp-smoke.ps1')
Invoke-GateStep 'Observability Langfuse smoke' (Join-Path $PSScriptRoot 'observability-langfuse-smoke.ps1')
Invoke-GateStep 'CORS prod audit' (Join-Path $PSScriptRoot 'cors-prod-audit.ps1')
Invoke-GateStep 'Docker image audit' (Join-Path $PSScriptRoot 'docker-image-audit.ps1')
Invoke-GateStep 'Dependency audit' (Join-Path $PSScriptRoot 'dependency-audit.ps1')

if (-not $SkipConcurrency) {
    Invoke-GateStep 'Publish concurrency (CC-02)' (Join-Path $PSScriptRoot 'publish-concurrency-gate.ps1')
}

if (-not $SkipFaultInjection) {
    Invoke-GateStep 'Fault injection (F-01~F-04, K-07/K-08, R/A-12)' (Join-Path $PSScriptRoot 'fault-injection.ps1')
}

if ($IncludeProdCompose) {
    Invoke-GateStep 'Prod compose smoke' (Join-Path $PSScriptRoot 'prod-compose-smoke.ps1')
}

$allPass = ($failed -eq 0)
[ordered]@{
    timestamp = (Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
    passed    = $allPass
    failed    = $failed
    steps     = @($steps)
} | ConvertTo-Json -Depth 6 | Set-Content $summaryFile -Encoding UTF8

Write-Host "`n========== SUMMARY ==========" -ForegroundColor $(if ($allPass) { 'Green' } else { 'Red' })
Write-Host "passed=$allPass failedSteps=$failed -> $summaryFile"
if (-not $allPass) { exit 1 }

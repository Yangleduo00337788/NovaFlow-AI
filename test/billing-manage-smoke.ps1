#requires -Version 7.0
# NovaFlow AI — Billing 配额管理验收（B-02）
# 用法: pwsh test/billing-manage-smoke.ps1

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'billing-manage-smoke.log'
$outFile = Join-Path $PSScriptRoot 'billing-manage-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== billing-manage-smoke ===' $logFile

try {
    $admin = Get-NovaLoginToken
    $user = Get-NovaLoginToken -Email 'user@novaflow.ai' -Password 'User123!'

    $quotaBefore = Invoke-NovaApi -Path '/api/v1/billing/quota' -Token $admin
    $original = $null
    if ($quotaBefore.raw -match '"monthlyTokenQuota":(\d+)') {
        $original = [long]$Matches[1]
    }
    Check 'B-02 read quota before update' ($quotaBefore.code -eq 0) "code=$($quotaBefore.code) quota=$original"

    $newQuota = if ($original -and $original -gt 1000000) { $original - 1 } else { 5000000 }
    $updatePath = Join-Path $script:NovaFlowTmpDir 'billing-quota.json'
    Write-NovaJson -Path $updatePath -Data @{ monthlyTokenQuota = $newQuota }
    $updated = Invoke-NovaApi -Method PUT -Path '/api/v1/billing/quota' -Token $admin -OutFile $updatePath
    $updateOk = ($updated.code -eq 0) -and ($updated.raw -match "`"monthlyTokenQuota`":$newQuota")
    Check 'B-02 admin update quota' $updateOk "code=$($updated.code) new=$newQuota"

    if ($null -ne $original) {
        Write-NovaJson -Path $updatePath -Data @{ monthlyTokenQuota = $original }
        $restored = Invoke-NovaApi -Method PUT -Path '/api/v1/billing/quota' -Token $admin -OutFile $updatePath
        Check 'B-02 restore quota' ($restored.code -eq 0) "code=$($restored.code)"
    }

    $allPass = (Test-NovaApiDenied 'B-02 user cannot update quota' '/api/v1/billing/quota' 'PUT' $user $results) -and $allPass
} catch {
    Check 'billing-manage setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'billing-manage-smoke' -Passed $allPass -Details @{
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

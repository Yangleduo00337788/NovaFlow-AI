#requires -Version 7.0
# NovaFlow AI — 知识库套餐配额验收（K-09）
# 用法: pwsh test/knowledge-quota-smoke.ps1
# 说明: personal 套餐 maxKnowledge=1，第二个知识库应被拒绝

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'knowledge-quota-smoke.log'
$outFile = Join-Path $PSScriptRoot 'knowledge-quota-smoke-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)
$email = "kb-quota-$suffix@novaflow.test"
$password = 'SmokeTest123!'

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

Write-NovaLog '=== knowledge-quota-smoke ===' $logFile

try {
    $registerPath = Join-Path $script:NovaFlowTmpDir "register-personal-$suffix.json"
    Write-NovaJson -Path $registerPath -Data @{
        companyName     = "QA-Personal-$suffix"
        email           = $email
        nickname        = "Personal $suffix"
        password        = $password
        confirmPassword = $password
        planType        = 'personal'
    }
    $registered = Invoke-NovaApi -Method POST -Path '/api/v1/auth/register' -OutFile $registerPath
    if ($registered.code -ne 0) {
        throw "Register personal tenant failed: $($registered.raw)"
    }
    $token = [regex]::Match($registered.raw, '"token":"([^"]+)"').Groups[1].Value

    $kbPath = Join-Path $script:NovaFlowTmpDir 'kb-quota.json'
    Write-NovaJson -Path $kbPath -Data @{
        kbName         = "KB-1-$suffix"
        description    = 'quota test'
        embeddingModel = 'text-embedding-3-small'
    }
    $kb1 = Invoke-NovaApi -Method POST -Path '/api/v1/knowledge-bases' -Token $token -OutFile $kbPath
    $kb1Id = [regex]::Match($kb1.raw, '"id":(\d+)').Groups[1].Value
    Check 'K-09 first knowledge base allowed' (($kb1.code -eq 0) -and $kb1Id) "kbId=$kb1Id code=$($kb1.code)"

    Write-NovaJson -Path $kbPath -Data @{
        kbName         = "KB-2-$suffix"
        description    = 'quota test overflow'
        embeddingModel = 'text-embedding-3-small'
    }
    $kb2 = Invoke-NovaApi -Method POST -Path '/api/v1/knowledge-bases' -Token $token -OutFile $kbPath
    $quotaRejected = ($kb2.code -ne 0) -and (
        ($kb2.raw -match '1/1') -or ($kb2.raw -match 'maxKnowledge') -or ($kb2.code -eq 40000)
    )
    Check 'K-09 second knowledge base rejected' $quotaRejected "code=$($kb2.code)"

    if ($kb1Id) {
        Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$kb1Id" -Token $token | Out-Null
    }
} catch {
    Check 'knowledge-quota setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'knowledge-quota-smoke' -Passed $allPass -Details @{
    email  = $email
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

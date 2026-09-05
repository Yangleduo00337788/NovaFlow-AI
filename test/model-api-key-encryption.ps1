#requires -Version 7.0
# NovaFlow AI — Model Provider API Key 加密存储验收（M-02）
# 用法: pwsh test/model-api-key-encryption.ps1
# 前提: 后端 :8080 已启动；本机 MySQL 可访问（用于校验 api_key_encrypted 列）

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'model-api-key-encryption.log'
$outFile = Join-Path $PSScriptRoot 'model-api-key-encryption-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$suffix = (Get-Date -Format 'HHmmss') + '_' + (Get-Random -Maximum 9999)

function Check {
    param([string]$Name, [bool]$Ok, [string]$Detail)
    $pass = Assert-NovaGate $Name $Ok $Detail $results
    $script:allPass = $script:allPass -and $pass
}

function Get-NovaDbConfig {
    $envFile = Join-Path (Split-Path $PSScriptRoot -Parent) '.env'
    $cfg = @{
        host = 'localhost'
        port = '3306'
        user = 'root'
        pass = 'root'
        db   = 'novaflow'
    }
    if (Test-Path $envFile) {
        foreach ($line in Get-Content $envFile) {
            if ($line -match '^SPRING_DATASOURCE_URL=(.+)$') {
                $url = $Matches[1].Trim()
                if ($url -match 'jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)') {
                    $cfg.host = $Matches[1]
                    if ($Matches[2]) { $cfg.port = $Matches[2] }
                    $cfg.db = $Matches[3]
                }
            }
            if ($line -match '^SPRING_DATASOURCE_USERNAME=(.+)$') { $cfg.user = $Matches[1].Trim() }
            if ($line -match '^SPRING_DATASOURCE_PASSWORD=(.+)$') { $cfg.pass = $Matches[1].Trim() }
        }
    }
    return $cfg
}

function Invoke-NovaDbScalar {
    param([string]$Sql)
    $mysql = Get-Command mysql -ErrorAction SilentlyContinue
    if (-not $mysql) { return $null }
    $cfg = Get-NovaDbConfig
    $args = @(
        '-h', $cfg.host, '-P', $cfg.port, '-u', $cfg.user,
        "-p$($cfg.pass)", $cfg.db, '-N', '-B', '-e', $Sql
    )
    $out = & mysql @args 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return [string]$out
}

Write-NovaLog '=== model-api-key-encryption ===' $logFile

try {
    $token = Get-NovaLoginToken
    $providerId = Get-NovaConfiguredProviderId -Token $token -ProviderCode 'deepseek'

    $detail = Invoke-NovaApi -Path "/api/v1/models/providers/$providerId" -Token $token
    $masked = [regex]::Match($detail.raw, '"apiKeyMasked":"([^"]*)"').Groups[1].Value
    $maskedOk = ($detail.code -eq 0) -and ($masked -match '\*\*\*\*')
    Check 'M-02 API returns masked key only' $maskedOk "masked=$masked"

    $leakOk = ($detail.code -eq 0) -and ($detail.raw -notmatch 'sk-[a-zA-Z0-9]{20,}')
    Check 'M-02 response has no plaintext key' $leakOk "providerId=$providerId"

    $cipher = Invoke-NovaDbScalar -Sql "SELECT api_key_encrypted FROM model_provider WHERE id=$providerId AND is_deleted=0 LIMIT 1"
    if ($null -eq $cipher) {
        Check 'M-02 DB stores encrypted ciphertext' $false 'mysql unavailable or row missing'
        Check 'M-02 DB column not plaintext' $false 'SKIP: mysql unavailable'
    } else {
        $cipher = $cipher.Trim()
        $dbEncrypted = ($cipher.Length -gt 16) -and ($cipher -notmatch '^sk-')
        Check 'M-02 DB stores encrypted ciphertext' $dbEncrypted "len=$($cipher.Length)"
        Check 'M-02 DB column not plaintext' ($cipher -notmatch '^sk-[a-zA-Z0-9]{20,}$') 'plaintext absent'
    }
} catch {
    Check 'model-api-key-encryption setup' $false $_.Exception.Message
}

Write-NovaGateResult -ScriptName 'model-api-key-encryption' -Passed $allPass -Details @{
    suffix = $suffix
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

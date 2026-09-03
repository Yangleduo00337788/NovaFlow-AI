#requires -Version 7.0
# NovaFlow AI — Redis / MySQL 故障注入（F-01 / F-02）
# 用法: pwsh test/fault-injection.ps1
# 结束后会尽力恢复 Redis 容器与 MySQL80 服务。

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'fault-injection.log'
$outFile = Join-Path $PSScriptRoot 'fault-injection-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$redisName = $null
$mysqlStopped = $false

function Find-RedisContainer {
    $names = docker ps --format '{{.Names}}' 2>$null
    foreach ($n in @('redis', 'novaflow-redis')) {
        if ($names -contains $n) { return $n }
    }
    return ($names | Select-Object -First 1)
}

Write-NovaLog '=== fault-injection F-01/F-02 ===' $logFile

$token = Get-NovaLoginToken
$meOk = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $token
$ok = Assert-NovaGate 'baseline auth/me' ($meOk.code -eq 0) "code=$($meOk.code)" $results
$allPass = $allPass -and $ok

# --- Redis ---
$redisName = Find-RedisContainer
if (-not $redisName) {
    $ok = Assert-NovaGate 'F-02 redis container found' $false 'no redis container' $results
    $allPass = $false
} else {
    Write-NovaLog "Stopping Redis container $redisName" $logFile
    docker stop $redisName | Out-Null
    Start-Sleep -Seconds 2

    $healthDown = Invoke-NovaApi -Path '/api/v1/health'
    $loginDown = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -Body @{
        email = 'admin@novaflow.ai'
        password = 'Admin123!'
    }
    $meDown = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $token

    $redisImpact = ($healthDown.http -ge 200) -and (
        $healthDown.raw -match '"healthy":false' -or
        $healthDown.raw -match 'redis' -or
        $loginDown.code -ne 0 -or
        $meDown.code -ne 0
    )
    $ok = Assert-NovaGate 'F-02 redis down observed' $redisImpact "healthHttp=$($healthDown.http) loginCode=$($loginDown.code) meCode=$($meDown.code)" $results
    $allPass = $allPass -and $ok

    Write-NovaLog "Starting Redis container $redisName" $logFile
    docker start $redisName | Out-Null
    $ready = $false
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 2
        $h = Invoke-NovaApi -Path '/api/v1/health'
        if ($h.code -eq 0 -and $h.raw -match '"status":"UP"') { $ready = $true; break }
    }
    $ok = Assert-NovaGate 'F-02 redis recovered' $ready 'health UP after start' $results
    $allPass = $allPass -and $ok

    try {
        $token2 = Get-NovaLoginToken
        $me2 = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $token2
        $ok = Assert-NovaGate 'F-02 login after redis' ($me2.code -eq 0) "code=$($me2.code)" $results
        $allPass = $allPass -and $ok
    } catch {
        $ok = Assert-NovaGate 'F-02 login after redis' $false $_.Exception.Message $results
        $allPass = $false
    }
}

# --- MySQL ---
$mysql = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
if (-not $mysql) {
    Assert-NovaGate 'F-01 MySQL80 service' $false 'service not found — skip' $results | Out-Null
    Write-NovaLog 'SKIP F-01: MySQL80 service missing' $logFile
} else {
    try {
        Write-NovaLog 'Stopping MySQL80' $logFile
        Stop-Service -Name 'MySQL80' -Force -ErrorAction Stop
        $mysqlStopped = $true
        Start-Sleep -Seconds 3

        $healthDb = Invoke-NovaApi -Path '/api/v1/health'
        $loginDb = Invoke-NovaApi -Method POST -Path '/api/v1/auth/login' -Body @{
            email = 'admin@novaflow.ai'
            password = 'Admin123!'
        }
        $dbImpact = ($healthDb.http -ge 500) -or ($healthDb.code -ne 0) -or ($loginDb.code -ne 0) -or (
            $healthDb.raw -match '"healthy":false'
        )
        $ok = Assert-NovaGate 'F-01 mysql down observed' $dbImpact "healthHttp=$($healthDb.http) healthCode=$($healthDb.code) loginCode=$($loginDb.code)" $results
        $allPass = $allPass -and $ok
    } catch {
        $ok = Assert-NovaGate 'F-01 stop MySQL80' $false $_.Exception.Message $results
        $allPass = $false
    } finally {
        if ($mysqlStopped) {
            Write-NovaLog 'Starting MySQL80' $logFile
            Start-Service -Name 'MySQL80' -ErrorAction Continue
            $dbReady = $false
            for ($i = 0; $i -lt 30; $i++) {
                Start-Sleep -Seconds 2
                $h = Invoke-NovaApi -Path '/api/v1/health'
                if ($h.http -eq 200 -and ($h.code -eq 0 -or $h.raw -match '"status":"UP"')) {
                    $dbReady = $true
                    break
                }
            }
            $ok = Assert-NovaGate 'F-01 mysql recovered' $dbReady 'health UP after start' $results
            $allPass = $allPass -and $ok
        }
    }
}

Write-NovaGateResult -ScriptName 'fault-injection' -Passed $allPass -Details @{
    redisContainer = $redisName
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

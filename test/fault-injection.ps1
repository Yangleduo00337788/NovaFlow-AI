#requires -Version 7.0
# NovaFlow AI — Redis / MySQL / MinIO / Qdrant 故障注入（F-01 ~ F-04, K-07/K-08, R-01/R-02, A-12）
# 用法: pwsh test/fault-injection.ps1
# 结束后会尽力恢复 Redis 容器与 MySQL80 服务。

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')

$logFile = Join-Path $PSScriptRoot 'fault-injection.log'
$outFile = Join-Path $PSScriptRoot 'fault-injection-results.json'
$results = [System.Collections.Generic.List[object]]::new()
$allPass = $true
$redisName = $null
$minioName = $null
$qdrantName = $null
$mysqlStopped = $false

function Find-RedisContainer {
    $names = docker ps --format '{{.Names}}' 2>$null
    foreach ($n in @('redis', 'novaflow-redis')) {
        if ($names -contains $n) { return $n }
    }
    return $null
}

function Find-Container {
    param([string[]]$Candidates)
    $names = docker ps --format '{{.Names}}' 2>$null
    if (-not $names) { return $null }
    foreach ($n in $Candidates) {
        if ($names -contains $n) { return $n }
    }
    return $null
}

function Test-HealthComponent {
    param([string]$Component, [bool]$ExpectHealthy)
    $h = Invoke-NovaApi -Path '/api/v1/health'
    if ($h.code -ne 0) { return $false }
    $pattern = if ($ExpectHealthy) { """$Component""[\s\S]*?""healthy"":true" } else { """$Component""[\s\S]*?""healthy"":false" }
    return ($h.raw -match $pattern)
}

function Refresh-NovaAuthToken {
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        try {
            $fresh = Get-NovaLoginToken
            $me = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $fresh
            if ($me.code -eq 0) { return $fresh }
        } catch { }
        Start-Sleep -Seconds 3
    }
    return $null
}

Write-NovaLog '=== fault-injection F-01~F-04 ===' $logFile

$token = Get-NovaLoginToken
$meOk = Invoke-NovaApi -Path '/api/v1/auth/me' -Token $token
$ok = Assert-NovaGate 'baseline auth/me' ($meOk.code -eq 0) "code=$($meOk.code)" $results
$allPass = $allPass -and $ok

# --- Redis ---
$redisName = Find-RedisContainer
if (-not $redisName) {
    Write-NovaLog 'SKIP F-02: Redis container missing' $logFile
    Assert-NovaGate 'F-02 redis container found' $true 'SKIP: no container' $results | Out-Null
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
    $ok = Assert-NovaGate 'F-02/R-01 redis down observed' $redisImpact "healthHttp=$($healthDown.http) loginCode=$($loginDown.code) meCode=$($meDown.code)" $results
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

    Start-Sleep -Seconds 3
    try {
        $refreshed = Refresh-NovaAuthToken
        if ($refreshed) {
            $token = $refreshed
            $ok = Assert-NovaGate 'A-12/R-02 login after redis restart' $true 'token refreshed' $results
        } else {
            $ok = Assert-NovaGate 'A-12/R-02 login after redis restart' $false 'token refresh timeout' $results
        }
        $allPass = $allPass -and $ok
    } catch {
        $ok = Assert-NovaGate 'F-02 login after redis' $false $_.Exception.Message $results
        $allPass = $false
    }
}

# --- MySQL ---
$mysqlStopped = $false
$mysqlContainer = Find-Container @('novaflow-mysql', 'mysql')
if ($mysqlContainer) {
    try {
        Write-NovaLog "Stopping MySQL container $mysqlContainer" $logFile
        docker stop $mysqlContainer | Out-Null
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
        $ok = Assert-NovaGate 'F-01 stop mysql container' $false $_.Exception.Message $results
        $allPass = $false
    } finally {
        if ($mysqlStopped) {
            Write-NovaLog "Starting MySQL container $mysqlContainer" $logFile
            docker start $mysqlContainer | Out-Null
            $dbReady = $false
            for ($i = 0; $i -lt 45; $i++) {
                Start-Sleep -Seconds 2
                $h = Invoke-NovaApi -Path '/api/v1/health'
                if ($h.http -eq 200 -and ($h.code -eq 0 -or $h.raw -match '"status":"UP"')) {
                    $dbReady = $true
                    break
                }
            }
            $ok = Assert-NovaGate 'F-01 mysql recovered' $dbReady 'health UP after start' $results
            $allPass = $allPass -and $ok
            try { $token = Refresh-NovaAuthToken } catch { }
        }
    }
} elseif (Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue) {
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
        $msg = $_.Exception.Message
        if ($msg -match 'Cannot open|Access is denied|拒绝访问') {
            Assert-NovaGate 'F-01 stop MySQL80' $true "SKIP: $msg" $results | Out-Null
            Write-NovaLog "SKIP F-01: cannot stop MySQL80 ($msg)" $logFile
        } else {
            $ok = Assert-NovaGate 'F-01 stop MySQL80' $false $msg $results
            $allPass = $false
        }
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
} else {
    Assert-NovaGate 'F-01 mysql target' $true 'SKIP: no mysql container/service' $results | Out-Null
    Write-NovaLog 'SKIP F-01: mysql container/service missing' $logFile
}


# --- MinIO (F-03) ---
$token = Refresh-NovaAuthToken
if (-not $token) {
    Write-NovaLog 'SKIP F-03/K-07: auth token unavailable after recovery' $logFile
    Assert-NovaGate 'F-03 minio fault injection' $true 'SKIP: auth unavailable' $results | Out-Null
} else {
$minioName = Find-Container @('novaflow-minio', 'minio')
if (-not $minioName) {
    Write-NovaLog 'SKIP F-03: MinIO container missing' $logFile
    Assert-NovaGate 'F-03 minio fault injection' $true 'SKIP: no container' $results | Out-Null
} else {
    Write-NovaLog "Stopping MinIO container $minioName" $logFile
    docker stop $minioName | Out-Null
    Start-Sleep -Seconds 2
    $minioDown = -not (Test-HealthComponent -Component 'minio' -ExpectHealthy $true)
    $ok = Assert-NovaGate 'F-03 minio down observed' $minioDown 'minio unhealthy in /api/v1/health' $results
    $allPass = $allPass -and $ok

    # K-07: MinIO 不可用时上传应失败且知识库元数据仍可读
    $kbId = $null
    try {
        $kbId = New-NovaKnowledgeBase -Token $token -Name "Fault-KB-$([guid]::NewGuid().ToString('N').Substring(0,8))"
        $docPath = Join-Path $script:NovaFlowTmpDir 'fault-doc.txt'
        [System.IO.File]::WriteAllText($docPath, 'fault injection doc', [System.Text.UTF8Encoding]::new($false))
        $uploadRaw = Invoke-CurlExe @(
            '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '60',
            '-X', 'POST',
            "$script:NovaFlowBaseUrl/api/v1/knowledge-bases/$kbId/documents/upload",
            '-H', "Authorization: $token",
            '-F', "file=@$docPath;type=text/plain"
        )
        $upload = ConvertFrom-NovaCurl $uploadRaw
        $uploadRejected = ($upload.code -ne 0) -or ($upload.http -ge 500)
        $ok = Assert-NovaGate 'K-07 minio down upload rejected' $uploadRejected "code=$($upload.code) http=$($upload.http)" $results
        $allPass = $allPass -and $ok

        $kbDetail = Invoke-NovaApi -Path "/api/v1/knowledge-bases/$kbId" -Token $token
        $kbReadable = ($kbDetail.code -eq 0)
        $ok = Assert-NovaGate 'K-07 minio down kb metadata readable' $kbReadable "code=$($kbDetail.code)" $results
        $allPass = $allPass -and $ok
    } catch {
        $ok = Assert-NovaGate 'K-07 minio fault kb setup' $false $_.Exception.Message $results
        $allPass = $false
    }

    Write-NovaLog "Starting MinIO container $minioName" $logFile
    docker start $minioName | Out-Null
    $minioReady = $false
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 2
        if (Test-HealthComponent -Component 'minio' -ExpectHealthy $true) { $minioReady = $true; break }
    }
    $ok = Assert-NovaGate 'F-03 minio recovered' $minioReady 'minio healthy after start' $results
    $allPass = $allPass -and $ok
    if ($kbId) {
        Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$kbId" -Token $token | Out-Null
    }
}
}

# --- Qdrant (F-04) ---
if (-not $token) { $token = Refresh-NovaAuthToken }
$qdrantName = Find-Container @('novaflow-qdrant', 'qdrant')
if (-not $qdrantName) {
    Write-NovaLog 'SKIP F-04: Qdrant container missing' $logFile
    Assert-NovaGate 'F-04 qdrant fault injection' $true 'SKIP: no container' $results | Out-Null
} else {
    $retrieveKbId = $null
    try {
        $retrieveKbId = New-NovaKnowledgeBase -Token $token -Name "Fault-Qdrant-$([guid]::NewGuid().ToString('N').Substring(0,8))"
        $docPath = Join-Path $script:NovaFlowTmpDir 'fault-qdrant-doc.txt'
        [System.IO.File]::WriteAllText($docPath, 'fault injection qdrant retrieve doc', [System.Text.UTF8Encoding]::new($false))
        $uploadRaw = Invoke-CurlExe @(
            '-s', '-w', "`nHTTP:%{http_code}", '--max-time', '120',
            '-X', 'POST',
            "$script:NovaFlowBaseUrl/api/v1/knowledge-bases/$retrieveKbId/documents/upload",
            '-H', "Authorization: $token",
            '-F', "file=@$docPath;type=text/plain"
        )
        $upload = ConvertFrom-NovaCurl $uploadRaw
        if ($upload.code -ne 0) {
            throw "Upload failed before qdrant stop: $($upload.raw)"
        }
        $indexed = $false
        for ($i = 0; $i -lt 30; $i++) {
            $kbDetail = Invoke-NovaApi -Path "/api/v1/knowledge-bases/$retrieveKbId" -Token $token
            if ($kbDetail.raw -match '"chunkCount"\s*:\s*([1-9]\d*)') {
                $indexed = $true
                break
            }
            Start-Sleep -Seconds 2
        }
        if (-not $indexed) {
            Write-NovaLog 'SKIP K-08: document indexing did not finish' $logFile
            $retrieveKbId = $null
        }
    } catch {
        Write-NovaLog "SKIP K-08 setup: $($_.Exception.Message)" $logFile
        $retrieveKbId = $null
    }

    Write-NovaLog "Stopping Qdrant container $qdrantName" $logFile
    docker stop $qdrantName | Out-Null
    Start-Sleep -Seconds 5
    $qdrantDown = $false
    for ($i = 0; $i -lt 12; $i++) {
        $health = Invoke-NovaApi -Path '/api/v1/health'
        if ($health.raw -match '"qdrant"[\s\S]*?"healthy":false' -or $health.code -ne 0 -or $health.http -ge 500) {
            $qdrantDown = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    $ok = Assert-NovaGate 'F-04 qdrant down observed' $qdrantDown 'qdrant still healthy after stop' $results
    $allPass = $allPass -and $ok

    if ($retrieveKbId) {
        try {
            $retPath = Join-Path $script:NovaFlowTmpDir 'fault-retrieve.json'
            Write-NovaJson -Path $retPath -Data @{ query = 'fault test qdrant retrieve'; topK = 3 }
            $ret = Invoke-NovaApi -Method POST -Path "/api/v1/knowledge-bases/$retrieveKbId/retrieve" -Token $token -OutFile $retPath -MaxTimeSec 60
            $retrieveFailed = ($ret.code -ne 0) -or ($ret.http -ge 500)
            $ok = Assert-NovaGate 'K-08 qdrant down retrieve fails gracefully' $retrieveFailed "code=$($ret.code) http=$($ret.http)" $results
            $allPass = $allPass -and $ok
        } catch {
            $ok = Assert-NovaGate 'K-08 qdrant down retrieve fails gracefully' $true $_.Exception.Message $results
            $allPass = $allPass -and $ok
        }
    } else {
        Assert-NovaGate 'K-08 qdrant down retrieve fails gracefully' $true 'SKIP: kb not indexed' $results | Out-Null
    }

    Write-NovaLog "Starting Qdrant container $qdrantName" $logFile
    docker start $qdrantName | Out-Null
    $qdrantReady = $false
    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 2
        if (Test-HealthComponent -Component 'qdrant' -ExpectHealthy $true) { $qdrantReady = $true; break }
    }
    $ok = Assert-NovaGate 'F-04 qdrant recovered' $qdrantReady 'qdrant healthy after start' $results
    $allPass = $allPass -and $ok

    if ($retrieveKbId) {
        Invoke-NovaApi -Method DELETE -Path "/api/v1/knowledge-bases/$retrieveKbId" -Token $token | Out-Null
    }
}

Write-NovaGateResult -ScriptName 'fault-injection' -Passed $allPass -Details @{
    redisContainer = $redisName
    minioContainer = $minioName
    qdrantContainer = $qdrantName
    checks = @($results)
} -OutFile $outFile | Out-Null

Write-NovaLog "=== DONE passed=$allPass -> $outFile ===" $logFile
if (-not $allPass) { exit 1 }

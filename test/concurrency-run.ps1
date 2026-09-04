#requires -Version 7.0
# NovaFlow AI — Phase 6 concurrency stress test
$ErrorActionPreference = "Stop"
$base = "http://localhost:8080"
$repo = Split-Path -Parent $PSScriptRoot
$loginFile = Join-Path $repo "login.json"
$outFile = Join-Path $PSScriptRoot "concurrency-results.json"
$logFile = Join-Path $PSScriptRoot "concurrency-run.log"
$tmpDir = Join-Path $env:TEMP "novaflow-cc"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

function Log($msg) {
    $line = "$(Get-Date -Format 'HH:mm:ss') $msg"
    Add-Content -Path $logFile -Value $line
    Write-Host $line
}

function Invoke-CurlExe([string[]]$CurlArgs) {
    $out = & curl.exe @CurlArgs 2>&1
    if ($out -is [array]) { return ($out -join "`n") }
    return [string]$out
}

function Parse([string]$Raw) {
    $http = 0; $body = $Raw
    if ($body -match "`nHTTP:(\d+)$") { $http = [int]$Matches[1]; $body = $body -replace "`nHTTP:\d+$", "" }
    $code = $null
    if ($body -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    return @{ raw = $body; http = $http; code = $code }
}

function Summarize($label, $rows) {
    $ok = @($rows | Where-Object ok).Count
    $fail = $rows.Count - $ok
    $codes = ($rows | Group-Object apiCode | ForEach-Object { "$($_.Name):$($_.Count)" }) -join ", "
    $ms = @($rows.ms)
    $avg = if ($ms.Count) { [math]::Round(($ms | Measure-Object -Average).Average, 0) } else { 0 }
    $sorted = $ms | Sort-Object
    $p95 = if ($sorted.Count) { $sorted[[math]::Min($sorted.Count-1, [math]::Ceiling($sorted.Count*0.95)-1)] } else { 0 }
    Log "$label => ok=$ok fail=$fail avg=${avg}ms p95=${p95}ms codes=$codes"
    return @{ label=$label; count=$rows.Count; ok=$ok; fail=$fail; avgMs=$avg; p95Ms=$p95; apiCodes=$codes; sampleErrors=@($rows|?{-not $_.ok}|select -First 3) }
}

"" | Set-Content $logFile
Log "=== NovaFlow concurrency test (pwsh) ==="

$login = Parse (Invoke-CurlExe @("-s","-w","`nHTTP:%{http_code}","--data-binary","@$loginFile","-X","POST","$base/api/v1/auth/login","-H","Content-Type: application/json"))
if ($login.code -ne 0) { throw "Login failed" }
$token = [regex]::Match($login.raw,'"token":"([^"]+)"').Groups[1].Value

$createPath = Join-Path $tmpDir "create.json"
@{agentName="QA-CC-$(Get-Random)";agentType="chat";applicationId=1;welcomeMessage="cc"}|ConvertTo-Json|Set-Content $createPath -Encoding UTF8
$created = Parse (Invoke-CurlExe @("-s","-w","`nHTTP:%{http_code}","-X","POST","$base/api/v1/agents","-H","Authorization: $token","-H","Content-Type: application/json","--data-binary","@$createPath"))
if ($created.code -ne 0) { throw "Create agent: $($created.raw)" }
$agentId = [int]([regex]::Match($created.raw,'"id":(\d+)').Groups[1].Value)
$pub = Parse (Invoke-CurlExe @("-s","-w","`nHTTP:%{http_code}","-X","POST","$base/api/v1/agents/$agentId/publish","-H","Authorization: $token","-H","Content-Type: application/json","-d","{}"))
if ($pub.code -ne 0) { throw "Publish: $($pub.raw)" }
$apiKey = [regex]::Match($pub.raw,'"apiKey":"([^"]+)"').Groups[1].Value
Log "agentId=$agentId"

$all = @{ timestamp=(Get-Date -Format "yyyy-MM-dd HH:mm:ss"); agentId=$agentId }

# CC-01: 10 concurrent Open API chat
Log "CC-01 starting..."
$cc01 = @(1..10 | ForEach-Object -Parallel {
    $i = $_
  $cid = "caller-$('{0:D4}' -f $i)"
  $sw = [System.Diagnostics.Stopwatch]::StartNew()
  $j = Join-Path $using:tmpDir "c$i.json"
  @{message="hi-$i";conversationKey="k$i"}|ConvertTo-Json|Set-Content $j -Encoding UTF8
  $raw = & curl.exe -s -w "`nHTTP:%{http_code}" --max-time 90 -X POST "$using:base/api/v1/open/agents/$using:agentId/chat" -H "Authorization: Bearer $using:apiKey" -H "X-Caller-Id: $cid" -H "Content-Type: application/json" --data-binary "@$j" 2>&1
    if ($raw -is [array]) { $raw = $raw -join "`n" }
    $sw.Stop()
    $http = 0; if ($raw -match "`nHTTP:(\d+)$") { $http = [int]$Matches[1]; $raw = $raw -replace "`nHTTP:\d+$","" }
    $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    [pscustomobject]@{ i=$i; ok=($code -eq 0); apiCode=$code; http=$http; ms=$sw.ElapsedMilliseconds; snippet=$raw.Substring(0,[math]::Min(80,$raw.Length)) }
} -ThrottleLimit 10)
$all.CC01 = Summarize "CC-01 Open API chat x10" $cc01

# CC-02: 50 concurrent publish
Log "CC-02 starting..."
$cc02 = @(1..50 | ForEach-Object -Parallel {
    $i = $_
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $raw = & curl.exe -s -w "`nHTTP:%{http_code}" -X POST "$using:base/api/v1/agents/$using:agentId/publish" -H "Authorization: $using:token" -H "Content-Type: application/json" -d "{}" 2>&1
    if ($raw -is [array]) { $raw = $raw -join "`n" }
    $sw.Stop()
    $http = 0; if ($raw -match "`nHTTP:(\d+)$") { $http = [int]$Matches[1]; $raw = $raw -replace "`nHTTP:\d+$","" }
    $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    [pscustomobject]@{ i=$i; ok=($code -eq 0); apiCode=$code; http=$http; ms=$sw.ElapsedMilliseconds; snippet=$raw.Substring(0,[math]::Min(80,$raw.Length)) }
} -ThrottleLimit 50)
$pubAfter = Parse (Invoke-CurlExe @("-s","-H","Authorization: $token","$base/api/v1/agents/$agentId/publish"))
$all.CC02 = Summarize "CC-02 duplicate publish x50" $cc02
$all.CC02_post = @{
    version = [regex]::Match($pubAfter.raw,'"version":(\d+)').Groups[1].Value
    prefix = [regex]::Match($pubAfter.raw,'"apiKeyPrefix":"([^"]+)"').Groups[1].Value
    successCount = @($cc02 | Where-Object ok).Count
}

# CC-03: 100 favorite toggle
Log "CC-03 starting..."
$favPath = Join-Path $tmpDir "fav.json"
@{resourceType="agent";resourceId=$agentId;resourceName="QA-CC"}|ConvertTo-Json|Set-Content $favPath -Encoding UTF8
Invoke-CurlExe @("-s","-X","POST","$base/api/v1/dashboard/favorites/toggle","-H","Authorization: $token","-H","Content-Type: application/json","--data-binary","@$favPath") | Out-Null
Invoke-CurlExe @("-s","-X","POST","$base/api/v1/dashboard/favorites/toggle","-H","Authorization: $token","-H","Content-Type: application/json","--data-binary","@$favPath") | Out-Null
$cc03 = @(1..100 | ForEach-Object -Parallel {
    $i = $_
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $raw = & curl.exe -s -w "`nHTTP:%{http_code}" -X POST "$using:base/api/v1/dashboard/favorites/toggle" -H "Authorization: $using:token" -H "Content-Type: application/json" --data-binary "@$using:favPath" 2>&1
    if ($raw -is [array]) { $raw = $raw -join "`n" }
    $sw.Stop()
    $http = 0; if ($raw -match "`nHTTP:(\d+)$") { $http = [int]$Matches[1]; $raw = $raw -replace "`nHTTP:\d+$","" }
    $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    [pscustomobject]@{ i=$i; ok=($code -eq 0); apiCode=$code; http=$http; ms=$sw.ElapsedMilliseconds }
} -ThrottleLimit 100)
$favList = Parse (Invoke-CurlExe @("-s","-H","Authorization: $token","$base/api/v1/dashboard/favorites?limit=200"))
$all.CC03 = Summarize "CC-03 favorite toggle x100" $cc03
$all.CC03_post = @{ favoriteRowsForAgent = ([regex]::Matches($favList.raw,"`"resourceId`":$agentId")).Count }

# CC-04: 10 concurrent workflow run
Log "CC-04 starting (LLM, ~30-120s)..."
$cc04 = @(1..10 | ForEach-Object -Parallel {
    $i = $_
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $j = Join-Path $using:tmpDir "w$i.json"
    @{input="cc$i"}|ConvertTo-Json|Set-Content $j -Encoding UTF8
    $raw = & curl.exe -s -w "`nHTTP:%{http_code}" --max-time 180 -X POST "$using:base/api/v1/workflows/1/run" -H "Authorization: $using:token" -H "Content-Type: application/json" --data-binary "@$j" 2>&1
    if ($raw -is [array]) { $raw = $raw -join "`n" }
    $sw.Stop()
    $http = 0; if ($raw -match "`nHTTP:(\d+)$") { $http = [int]$Matches[1]; $raw = $raw -replace "`nHTTP:\d+$","" }
    $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    [pscustomobject]@{ i=$i; ok=($code -eq 0); apiCode=$code; http=$http; ms=$sw.ElapsedMilliseconds; snippet=$raw.Substring(0,[math]::Min(80,$raw.Length)) }
} -ThrottleLimit 10)
$all.CC04 = Summarize "CC-04 workflow run x10" $cc04

# CC-05: 10 concurrent upload
Log "CC-05 starting..."
$cc05 = @(1..10 | ForEach-Object -Parallel {
    $i = $_
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $f = Join-Path $using:tmpDir "up$i-$(Get-Random).txt"
    "upload $i" | Set-Content $f -Encoding UTF8
    $raw = & curl.exe -s -w "`nHTTP:%{http_code}" -X POST "$using:base/api/v1/knowledge-bases/2/documents/upload" -H "Authorization: $using:token" -F "file=@$f" 2>&1
    if ($raw -is [array]) { $raw = $raw -join "`n" }
    $sw.Stop()
    $http = 0; if ($raw -match "`nHTTP:(\d+)$") { $http = [int]$Matches[1]; $raw = $raw -replace "`nHTTP:\d+$","" }
    $code = $null; if ($raw -match '"code":(-?\d+)') { $code = [int]$Matches[1] }
    [pscustomobject]@{ i=$i; ok=($code -eq 0); apiCode=$code; http=$http; ms=$sw.ElapsedMilliseconds; snippet=$raw.Substring(0,[math]::Min(80,$raw.Length)) }
} -ThrottleLimit 10)
$all.CC05 = Summarize "CC-05 document upload x10" $cc05

$all | ConvertTo-Json -Depth 8 | Set-Content $outFile -Encoding UTF8
Log "=== DONE -> $outFile ==="

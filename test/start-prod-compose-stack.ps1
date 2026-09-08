#requires -Version 7.0
# 构建并启动 prod compose 冒烟栈（prebuilt，端口 18080/13000）
# 用法: pwsh test/start-prod-compose-stack.ps1 [-Down]

param([switch]$Down)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $PSScriptRoot '.env.prod.smoke'
if (-not (Test-Path $envFile)) {
    $example = Join-Path $PSScriptRoot 'env-prod-smoke.example'
    if (Test-Path $example) {
        Copy-Item $example $envFile
    } else {
        throw "Missing $envFile (copy from test/env-prod-smoke.example)"
    }
}
$composeArgs = @(
    '-p', 'novaflow-prod-qa',
    '--env-file', $envFile,
    '-f', (Join-Path $repoRoot 'deploy/docker-compose.prod.yml'),
    '-f', (Join-Path $repoRoot 'deploy/docker-compose.prod.prebuilt.yml')
)

if ($Down) {
    Push-Location $repoRoot
    try {
        docker compose @composeArgs down
    } finally {
        Pop-Location
    }
    exit 0
}

Write-Host 'Building backend jar...' -ForegroundColor Cyan
Push-Location $repoRoot
try {
    mvn -B -q -pl novaflow-server -am package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "mvn package failed exit=$LASTEXITCODE" }

    Write-Host 'Building frontend dist...' -ForegroundColor Cyan
    Push-Location (Join-Path $repoRoot 'novaflow-web')
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "npm run build failed exit=$LASTEXITCODE" }
    Pop-Location

    Write-Host 'Recreating prod compose stack (fresh volumes)...' -ForegroundColor Cyan
    docker compose @composeArgs down -v 2>$null | Out-Null
    docker compose @composeArgs up -d --build
    if ($LASTEXITCODE -ne 0) { throw "docker compose up failed exit=$LASTEXITCODE" }
} finally {
    Pop-Location
}

Write-Host 'Waiting for health on http://127.0.0.1:18080 ...' -ForegroundColor Cyan
$ready = $false
for ($i = 1; $i -le 90; $i++) {
    try {
        $r = Invoke-RestMethod -Uri 'http://127.0.0.1:18080/api/v1/health' -TimeoutSec 5
        if ($r.code -eq 0) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 3
}
if (-not $ready) {
    Write-Host 'Health check timeout. Logs:' -ForegroundColor Red
    docker logs novaflow-server --tail 80 2>&1
    exit 1
}
Write-Host 'Prod compose stack is ready.' -ForegroundColor Green

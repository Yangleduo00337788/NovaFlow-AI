#requires -Version 7.0
# 针对 prod compose 栈运行完整 fault injection（Redis/MinIO/Qdrant 容器）
# 用法:
#   pwsh test/start-prod-compose-stack.ps1
#   pwsh test/run-docker-fault-injection.ps1

$ErrorActionPreference = 'Stop'
$env:NOVAFLOW_BASE_URL = 'http://127.0.0.1:18080'
$env:NOVAFLOW_WEB_URL = 'http://localhost:13000'

& pwsh -NoProfile -File (Join-Path $PSScriptRoot 'fault-injection.ps1')
exit $LASTEXITCODE

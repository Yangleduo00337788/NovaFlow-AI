#requires -Version 7.0
# NovaFlow AI — Z-07：清点缺少 @SaCheckPermission 的 Controller 端点
# 用法: pwsh test/scan-api-permissions.ps1
# 返回码 0=通过（仅允许白名单缺口），1=发现未登记缺口

param(
    [string]$RepoRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = 'Stop'

$allowlistControllers = @(
    'AuthController'          # 登录/注册走 PUBLIC_API_PATHS；me/logout 仅需登录
    'HealthController'        # 健康检查公开
    'PublicPlatformController' # 维护模式/公告公开状态（/api/v1/public/**）
    'AgentOpenController'     # Open API 独立鉴权（API Key / Embed Token）
    'NotificationController'  # 按 tenantId + userId 隔离，仅需登录
)

$guardAnnotations = @(
    '@SaCheckPermission',
    '@SaCheckRole',
    '@SaIgnore'
)

$mappingPattern = '^\s*@(Get|Post|Put|Delete|Patch)Mapping\b|^\s*@RequestMapping\b'

function Test-NovaControllerPermissionScan {
    param([string]$Root)

    $controllers = Get-ChildItem -Path $Root -Recurse -Filter '*Controller.java' |
        Where-Object { $_.FullName -notmatch '[\\/]target[\\/]' }

    $violations = [System.Collections.Generic.List[string]]::new()
    $allowlisted = [System.Collections.Generic.List[string]]::new()
    $covered = 0

    foreach ($file in $controllers) {
        $className = $file.BaseName
        $lines = Get-Content -Path $file.FullName
        $classLineIndex = -1
        for ($idx = 0; $idx -lt $lines.Count; $idx++) {
            if ($lines[$idx] -match '^\s*public\s+(final\s+)?class\s+\w+') {
                $classLineIndex = $idx
                break
            }
        }

        $fileHasClassGuard = $false
        if ($classLineIndex -ge 0) {
            for ($idx = 0; $idx -lt $classLineIndex; $idx++) {
                foreach ($guard in $guardAnnotations) {
                    if ($lines[$idx] -match [regex]::Escape($guard)) {
                        $fileHasClassGuard = $true
                        break
                    }
                }
                if ($fileHasClassGuard) { break }
            }
        }

        $classHasGuard = $fileHasClassGuard
        $inClass = $false
        $braceDepth = 0

        for ($i = 0; $i -lt $lines.Count; $i++) {
            $line = $lines[$i]

            if ($line -match '^\s*(public\s+)?(final\s+)?class\s+\w+') {
                $inClass = $true
                $braceDepth = 0
            }
            if (-not $inClass) { continue }

            $braceDepth += ([regex]::Matches($line, '\{')).Count
            $braceDepth -= ([regex]::Matches($line, '\}')).Count
            if ($braceDepth -lt 0) { $inClass = $false; continue }

            if ($fileHasClassGuard) {
                if ($line -match $mappingPattern) {
                    if ($line -match '@RequestMapping\b' -and $line -notmatch 'public\s') { continue }
                    $covered++
                }
                continue
            }

            foreach ($guard in $guardAnnotations) {
                if ($line -match [regex]::Escape($guard)) {
                    $classHasGuard = $true
                    break
                }
            }

            if ($line -notmatch $mappingPattern) { continue }
            if ($line -match '@RequestMapping\b' -and $line -notmatch 'public\s') { continue }

            $methodGuard = $false
            $start = [Math]::Max(0, $i - 12)
            for ($j = $start; $j -le $i; $j++) {
                foreach ($guard in $guardAnnotations) {
                    if ($lines[$j] -match [regex]::Escape($guard)) {
                        $methodGuard = $true
                        break
                    }
                }
                if ($methodGuard) { break }
            }

            if ($classHasGuard -or $methodGuard) {
                $covered++
                continue
            }

            $location = "$className`:$($i + 1)"
            if ($allowlistControllers -contains $className) {
                $allowlisted.Add($location) | Out-Null
            } else {
                $violations.Add($location) | Out-Null
            }
        }
    }

    return [pscustomobject]@{
        passed      = ($violations.Count -eq 0)
        covered     = $covered
        allowlisted = @($allowlisted)
        violations  = @($violations)
        controllers = $controllers.Count
    }
}

$result = Test-NovaControllerPermissionScan -Root $RepoRoot

Write-Host "Controllers scanned: $($result.controllers)"
Write-Host "Endpoints with permission guard: $($result.covered)"
Write-Host "Allowlisted gaps: $($result.allowlisted.Count)"
foreach ($item in $result.allowlisted) {
    Write-Host "  [ALLOW] $item"
}

if ($result.violations.Count -gt 0) {
    Write-Host "Unallowlisted gaps: $($result.violations.Count)"
    foreach ($item in $result.violations) {
        Write-Host "  [FAIL] $item"
    }
    exit 1
}

Write-Host 'Z-07 scan PASS — no unallowlisted permission gaps'
exit 0

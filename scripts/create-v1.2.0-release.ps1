#requires -Version 7.0
# 创建 Gitee / GitHub v1.2.0 Release（需环境变量 GITEE_TOKEN、GH_TOKEN）
# 用法:
#   $env:GITEE_TOKEN = '...'
#   $env:GH_TOKEN = '...'   # 或已 gh auth login
#   pwsh scripts/create-v1.2.0-release.ps1

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$notesFile = Join-Path $repoRoot 'docs\releases\v1.2.0.md'
$giteeNotesFile = Join-Path $repoRoot 'docs\releases\v1.2.0-gitee.txt'
$tag = 'v1.2.0'
$name = 'NovaFlow AI v1.2.0'

if (-not (Test-Path $notesFile)) {
    throw "Release notes not found: $notesFile"
}

$body = Get-Content $notesFile -Raw -Encoding UTF8
$giteeBody = if (Test-Path $giteeNotesFile) { Get-Content $giteeNotesFile -Raw -Encoding UTF8 } else { $body }

function New-GiteeRelease {
    param([string]$Token, [string]$Body)
    $uri = 'https://gitee.com/api/v5/repos/yangleduo7788/nova-flow-ai/releases'
    $form = @{
        access_token     = $Token
        tag_name         = $tag
        name             = $name
        body             = $Body
        target_commitish = 'master'
        prerelease       = 'false'
    }
    $resp = Invoke-RestMethod -Method Post -Uri $uri -Body $form
    return "https://gitee.com/yangleduo7788/nova-flow-ai/releases/tag/$tag (id=$($resp.id))"
}

function New-GithubRelease {
    param([string]$Body)
    $gh = Get-Command gh -ErrorAction SilentlyContinue
    if ($gh) {
        if ($env:GH_TOKEN) {
            $env:GH_TOKEN | & gh auth login --with-token 2>$null
        }
        & gh release view $tag 2>$null
        if ($LASTEXITCODE -eq 0) {
            & gh release edit $tag --title $name --notes-file $notesFile
            if ($LASTEXITCODE -ne 0) { throw "gh release edit failed" }
            return "https://github.com/Yangleduo00337788/NovaFlow-AI/releases/tag/$tag (updated)"
        }
        & gh release create $tag --title $name --notes-file $notesFile
        if ($LASTEXITCODE -ne 0) { throw "gh release create failed" }
        return "https://github.com/Yangleduo00337788/NovaFlow-AI/releases/tag/$tag (created)"
    }

    if (-not $env:GH_TOKEN) { throw 'GH_TOKEN not set and gh CLI unavailable' }
    $uri = 'https://api.github.com/repos/Yangleduo00337788/NovaFlow-AI/releases'
    $payload = @{
        tag_name         = $tag
        name             = $name
        body             = $Body
        draft            = $false
        prerelease       = $false
        generate_release_notes = $false
    } | ConvertTo-Json
    $headers = @{
        Authorization = "Bearer $($env:GH_TOKEN)"
        Accept        = 'application/vnd.github+json'
        'X-GitHub-Api-Version' = '2022-11-28'
    }
    Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -Body $payload -ContentType 'application/json; charset=utf-8' | Out-Null
    return "https://github.com/Yangleduo00337788/NovaFlow-AI/releases/tag/$tag (created via API)"
}

$results = @()

if ($env:GITEE_TOKEN) {
    Write-Host "Creating Gitee release $tag ..." -ForegroundColor Cyan
    $results += "Gitee: $(New-GiteeRelease -Token $env:GITEE_TOKEN -Body $giteeBody)"
} else {
    Write-Host "Skip Gitee: GITEE_TOKEN not set" -ForegroundColor Yellow
}

try {
    Write-Host "Creating GitHub release $tag ..." -ForegroundColor Cyan
    $results += "GitHub: $(New-GithubRelease -Body $body)"
} catch {
    if (-not $env:GH_TOKEN) {
        Write-Host "Skip GitHub: $($_.Exception.Message)" -ForegroundColor Yellow
    } else {
        throw
    }
}

if ($results.Count -eq 0) {
    Write-Host "`nNo release created. Set GITEE_TOKEN and/or GH_TOKEN, then re-run." -ForegroundColor Red
    exit 1
}

Write-Host "`nDone:" -ForegroundColor Green
$results | ForEach-Object { Write-Host "  $_" }

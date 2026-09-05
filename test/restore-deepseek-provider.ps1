#requires -Version 7.0
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts/NovaFlow-TestCommon.ps1')
$token = Get-NovaLoginToken
$r = Restore-NovaProviderBaseUrl -Token $token
Write-Host "RESTORE code=$($r.code) http=$($r.http)"

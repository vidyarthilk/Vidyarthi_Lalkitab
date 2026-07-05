# Builds signed release AAB for Play Store upload
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

if (-not (Test-Path "admob.properties")) {
    Write-Host "Run first: .\scripts\sync-publish-config.ps1"
    exit 1
}
if (-not (Test-Path "keystore.properties")) {
    Write-Host "Missing keystore.properties - see publish/KEYSTORE_ANYTIME.md"
    exit 1
}

Write-Host "Building release app bundle..."
.\gradlew bundleRelease
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$aab = "app\build\outputs\bundle\release\app-release.aab"
if (Test-Path $aab) {
    Write-Host ""
    Write-Host "SUCCESS. Upload this file to Play Console:"
    Write-Host "  $((Resolve-Path $aab).Path)"
} else {
    Write-Host "Build finished but AAB not found at expected path."
    exit 1
}

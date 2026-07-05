# Fix Gradle "Tag mismatch" when downloading Guava (corrupted .gradle cache / antivirus / VPN).
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

Write-Host "=== Step 1: Stop Gradle ==="
.\gradlew --stop 2>$null

Write-Host "=== Step 2: Delete corrupted Gradle src cache (IDE sync error) ==="
$gradleSrcRoot = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\gradle\gradle\8.10.2"
$gradleSrcMeta = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\metadata-2.106\descriptors\gradle\gradle\8.10.2"
$gradleTransforms = Join-Path $env:USERPROFILE ".gradle\caches\8.10.2\transforms"
foreach ($p in @($gradleSrcRoot, $gradleSrcMeta, $gradleTransforms)) {
    if (Test-Path $p) {
        Remove-Item -Recurse -Force $p
        Write-Host "Removed $p"
    }
}

Write-Host "=== Step 3: Delete corrupted Guava cache ==="
$guavaRoot = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\com.google.guava"
if (Test-Path $guavaRoot) {
    Remove-Item -Recurse -Force $guavaRoot
    Write-Host "Removed $guavaRoot"
}

$jarsDirs = Get-ChildItem (Join-Path $env:USERPROFILE ".gradle\caches") -Directory -Filter "jars-*" -ErrorAction SilentlyContinue
foreach ($d in $jarsDirs) {
    Get-ChildItem $d.FullName -Recurse -Filter "guava*.jar" -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "Removing $($_.FullName)"
        Remove-Item -Force $_.FullName -ErrorAction SilentlyContinue
    }
}

Write-Host "=== Step 4: Local Maven copy of Guava (bypass bad download) ==="
& "$root\scripts\setup-local-guava.ps1"
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "=== Step 5: Pre-install Gradle 8.10.2 sources (fix src transform) ==="
& "$root\scripts\fix-gradle-src-cache.ps1" -GradleVersion "8.10.2" -SkipBuild

Write-Host "=== Step 6: Build debug APK ==="
.\gradlew assembleDebug --refresh-dependencies --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "SUCCESS. Open Android Studio and Run the app."
} else {
    Write-Host ""
    Write-Host "Still failing? Try:"
    Write-Host "  1. Windows Security -> exclude folder: $env:USERPROFILE\.gradle"
    Write-Host "  2. Turn OFF VPN, use phone hotspot"
    Write-Host "  3. Run this script again as Administrator"
    exit 1
}

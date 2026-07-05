# Fix "Failed to transform gradle-8.10.2-src.zip ... artifactType=src-directory"
# Usually caused by corrupted/incomplete download (SSL Tag mismatch, antivirus, VPN).
param(
    [string]$GradleVersion = "8.10.2",
    [switch]$SkipBuild,
    [string]$LocalZipPath = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$srcUrl = "https://github.com/gradle/gradle-distributions/releases/download/v$GradleVersion/gradle-$GradleVersion-src.zip"
$gradleHome = Join-Path $env:USERPROFILE ".gradle"
$filesRoot = Join-Path $gradleHome "caches\modules-2\files-2.1\gradle\gradle\$GradleVersion"
$metaRoot = Join-Path $gradleHome "caches\modules-2\metadata-2.106\descriptors\gradle\gradle\$GradleVersion"
$transformsRoot = Join-Path $gradleHome "caches\$GradleVersion\transforms"
$tmpZip = Join-Path $env:TEMP "gradle-$GradleVersion-src.zip"

Write-Host "=== Step 1: Stop Gradle daemons ==="
& "$root\gradlew.bat" --stop 2>$null

Write-Host "=== Step 2: Remove corrupted Gradle src cache ($GradleVersion) ==="
foreach ($path in @($filesRoot, $metaRoot)) {
    if (Test-Path $path) {
        Remove-Item -Recurse -Force $path
        Write-Host "Removed $path"
    }
}
if (Test-Path $transformsRoot) {
    Remove-Item -Recurse -Force $transformsRoot
    Write-Host "Removed $transformsRoot"
}

Write-Host "=== Step 3: Download gradle-$GradleVersion-src.zip (chunked, SSL-safe) ==="
if ($LocalZipPath -and (Test-Path $LocalZipPath)) {
    Copy-Item -Force $LocalZipPath $tmpZip
    Write-Host "Using local zip: $LocalZipPath"
} else {
    try {
        & "$root\scripts\download-maven-jar.ps1" `
            -Url $srcUrl `
            -OutPath $tmpZip `
            -ChunkSize 65536 `
            -MaxRetries 20
    } catch {
        Write-Host ""
        Write-Host "Automatic download failed (network/SSL). Manual fix:"
        Write-Host "  1. Browser thi download karo:"
        Write-Host "     $srcUrl"
        Write-Host "  2. Pachhi run karo:"
        Write-Host "     powershell -ExecutionPolicy Bypass -File scripts\fix-gradle-src-cache.ps1 -LocalZipPath `"C:\path\gradle-$GradleVersion-src.zip`""
        Write-Host ""
        Write-Host "Build mate terminal thi chale che (src zip IDE sync mate j che):"
        Write-Host "     .\gradlew.bat assembleDebug"
        exit 1
    }
}

$sha1 = (Get-FileHash -Algorithm SHA1 $tmpZip).Hash.ToLowerInvariant()
$destDir = Join-Path $filesRoot $sha1
New-Item -ItemType Directory -Force -Path $destDir | Out-Null
$destZip = Join-Path $destDir "gradle-$GradleVersion-src.zip"
Copy-Item -Force $tmpZip $destZip
Remove-Item $tmpZip -ErrorAction SilentlyContinue
Write-Host "Installed $destZip (sha1=$sha1)"

Write-Host "=== Step 4: Verify Gradle (optional build) ==="
if (-not $SkipBuild) {
    & "$root\gradlew.bat" assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "Build failed after src cache fix."
        exit 1
    }
}

Write-Host ""
Write-Host "Gradle src cache installed. Re-open Android Studio / Cursor and Sync Project."

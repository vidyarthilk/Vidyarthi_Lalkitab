# Creates release keystore + keystore.properties (Point A)
# Run from project root:  .\scripts\setup-keystore.ps1
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$releaseDir = Join-Path $root "release"
$storeFile = Join-Path $releaseDir "vidyarthi-lalkitab.jks"
$propsFile = Join-Path $root "keystore.properties"
$alias = "vidyarthi_lalkitab"

if (-not (Test-Path $releaseDir)) {
    New-Item -ItemType Directory -Path $releaseDir | Out-Null
}

if ((Test-Path $storeFile) -and (Test-Path $propsFile)) {
    Write-Host "Already done:"
    Write-Host "  $storeFile"
    Write-Host "  $propsFile"
    exit 0
}

if (Test-Path $storeFile) {
    Write-Host "Keystore exists but keystore.properties missing."
    Write-Host "Copy keystore.properties.example to keystore.properties and fill passwords."
    exit 1
}

Write-Host ""
Write-Host "=== Point A: Release Keystore ==="
Write-Host "IMPORTANT: Write passwords on paper + backup the .jks file."
Write-Host "If you lose them, you cannot update the app on Play Store."
Write-Host ""

$storePass = Read-Host "Keystore password (min 6 chars)"
if ($storePass.Length -lt 6) {
    Write-Host "Password too short."
    exit 1
}
$keyPass = Read-Host "Key password (Enter = same as keystore)"
if ([string]::IsNullOrWhiteSpace($keyPass)) { $keyPass = $storePass }

$dname = "CN=Vidyarthi Lalkitab, OU=Mobile, O=Vidyarthi, L=India, ST=Gujarat, C=IN"

$keytool = "keytool"
$candidates = @()
if ($env:JAVA_HOME) {
    $candidates += Join-Path $env:JAVA_HOME "bin\keytool.exe"
}
$candidates += "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
foreach ($candidate in $candidates) {
    if (Test-Path $candidate) {
        $keytool = $candidate
        break
    }
}

& $keytool -genkeypair -v `
    -storetype JKS `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -alias $alias `
    -keystore $storeFile `
    -storepass $storePass `
    -keypass $keyPass `
    -dname $dname

if ($LASTEXITCODE -ne 0) {
    Write-Host "keytool failed. Install JDK or open Android Studio terminal."
    exit $LASTEXITCODE
}

@"
storeFile=release/vidyarthi-lalkitab.jks
storePassword=$storePass
keyAlias=$alias
keyPassword=$keyPass
"@ | Set-Content -Path $propsFile -Encoding UTF8

Write-Host ""
Write-Host "SUCCESS:"
Write-Host "  $storeFile"
Write-Host "  $propsFile"
Write-Host ""
Write-Host "BACKUP NOW:"
Write-Host "  1. Copy release\vidyarthi-lalkitab.jks to USB / Google Drive"
Write-Host "  2. Write passwords on paper (do not share publicly)"
Write-Host ""
Write-Host "Next: Point B - screenshots (publish/SCREENSHOTS_KARO_GUJ.txt)"

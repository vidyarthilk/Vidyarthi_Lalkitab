# Updates interstitial ad unit ID in publisher.config + admob.properties
param(
    [Parameter(Mandatory = $true)]
    [string]$InterstitialId
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$configPath = Join-Path $root "publisher.config"
$admobPath = Join-Path $root "admob.properties"

$id = $InterstitialId.Trim()
if ($id -notmatch '^ca-app-pub-\d+/\d+$') {
    Write-Host "ERROR: Invalid format. Expected: ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"
    exit 1
}

function Get-ConfigValue($path, $key) {
    Get-Content $path | ForEach-Object {
        if ($_ -match "^\s*$key\s*=\s*(.+)\s*$") { return $Matches[1].Trim() }
    }
    return $null
}

$banner = Get-ConfigValue $configPath "admob.banner.id"
if ($id -eq $banner) {
    Write-Host "ERROR: Interstitial ID must be different from banner ID ($banner)"
    exit 1
}

if (-not (Test-Path $configPath)) {
    Write-Host "ERROR: publisher.config not found"
    exit 1
}

$lines = Get-Content $configPath
$updated = $false
$newLines = $lines | ForEach-Object {
    if ($_ -match '^\s*admob\.interstitial\.id\s*=') {
        $updated = $true
        "admob.interstitial.id=$id"
    } else {
        $_
    }
}
if (-not $updated) {
    $newLines += "admob.interstitial.id=$id"
}
Set-Content -Path $configPath -Value ($newLines -join "`n") -Encoding UTF8
Write-Host "OK: publisher.config"

$appId = Get-ConfigValue $configPath "admob.app.id"
$bannerId = Get-ConfigValue $configPath "admob.banner.id"
@"
ADMOB_APP_ID=$appId
ADMOB_BANNER_ID=$bannerId
ADMOB_INTERSTITIAL_ID=$id
"@ | Set-Content -Path $admobPath -Encoding ascii
Write-Host "OK: admob.properties"

Write-Host ""
Write-Host "Done. Interstitial ID set."
Write-Host "Next: .\gradlew.bat bundleRelease"

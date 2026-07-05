# Syncs publisher.config → admob.properties, strings.xml, privacy HTML
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$configPath = Join-Path $root "publisher.config"

if (-not (Test-Path $configPath)) {
    Write-Host "ERROR: publisher.config not found."
    Write-Host "Copy publisher.config.example to publisher.config and fill all fields."
    exit 1
}

function Get-ConfigValue($key) {
    Get-Content $configPath | ForEach-Object {
        if ($_ -match "^\s*$key\s*=\s*(.+)\s*$") { return $Matches[1].Trim() }
    }
    return $null
}

$email = Get-ConfigValue "developer.email"
$privacyUrl = Get-ConfigValue "privacy.policy.url"
$admobApp = Get-ConfigValue "admob.app.id"
$admobBanner = Get-ConfigValue "admob.banner.id"
$admobInterstitial = Get-ConfigValue "admob.interstitial.id"

$missing = @()
if ([string]::IsNullOrWhiteSpace($email) -or $email -like "*your.email*") { $missing += "developer.email" }
if ([string]::IsNullOrWhiteSpace($privacyUrl) -or $privacyUrl -like "*YOUR_USERNAME*") { $missing += "privacy.policy.url" }
if ([string]::IsNullOrWhiteSpace($admobApp) -or $admobApp -like "*XXXX*") { $missing += "admob.app.id" }
if ([string]::IsNullOrWhiteSpace($admobBanner) -or $admobBanner -like "*XXXX*") { $missing += "admob.banner.id" }

if ($missing.Count -gt 0) {
    Write-Host "ERROR: Fill these in publisher.config:"
    $missing | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

if ([string]::IsNullOrWhiteSpace($admobInterstitial)) {
    Write-Host "WARNING: admob.interstitial.id is empty in publisher.config"
    Write-Host "         Run: .\scripts\set-interstitial-id.ps1 -InterstitialId ca-app-pub-.../..."
    Write-Host "         Using banner ID as fallback (not recommended for release)."
    $admobInterstitial = $admobBanner
}
if ($admobInterstitial -eq $admobBanner) {
    Write-Host "WARNING: Interstitial ID equals banner ID — create a separate Interstitial unit in AdMob."
}

# admob.properties
$admobPath = Join-Path $root "admob.properties"
@"
ADMOB_APP_ID=$admobApp
ADMOB_BANNER_ID=$admobBanner
ADMOB_INTERSTITIAL_ID=$admobInterstitial
"@ | Set-Content -Path $admobPath -Encoding ascii
Write-Host "OK: admob.properties"

# strings.xml privacy URL
$stringsPath = Join-Path $root "app\src\main\res\values\strings.xml"
$strings = Get-Content $stringsPath -Raw
$strings = $strings -replace '(<string name="privacy_policy_url" translatable="false">)[^<]*(</string>)', "`${1}$privacyUrl`${2}"
Set-Content -Path $stringsPath -Value $strings.TrimEnd() -NoNewline -Encoding UTF8
Write-Host "OK: privacy_policy_url in strings.xml"

# Privacy HTML files
$privacyFiles = @(
    (Join-Path $root "docs\privacy_policy.html"),
    (Join-Path $root "publish\index.html")
)
foreach ($pf in $privacyFiles) {
    if (Test-Path $pf) {
        $html = Get-Content $pf -Raw
        $html = $html -replace 'YOUR_EMAIL@example.com', $email
        Set-Content -Path $pf -Value $html.TrimEnd() -NoNewline -Encoding UTF8
        Write-Host "OK: $pf (email updated)"
    }
}

Write-Host ""
Write-Host "Done. Next:"
Write-Host "  1. Upload publish\index.html to GitHub Pages (see publish/GITHUB_PRIVACY_5MIN.txt)"
Write-Host "  2. Create keystore if not done (publish/KEYSTORE_ANYTIME.md)"
Write-Host "  3. On publish day: publish/PLAY_STORE_DAY_GUJ.md"

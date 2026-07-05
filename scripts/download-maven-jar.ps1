param(
    [Parameter(Mandatory = $true)][string]$Url,
    [Parameter(Mandatory = $true)][string]$OutPath,
    [string]$ExpectedSha256 = "",
    [string]$ExpectedSha1 = "",
    [int]$ChunkSize = 262144,
    [int]$MaxRetries = 12
)

$ErrorActionPreference = "Stop"
$dir = Split-Path $OutPath -Parent
if ($dir) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

function Get-RemoteTotalSize([string]$targetUrl) {
    $headers = curl.exe -s -I -L -H "Range: bytes=0-0" $targetUrl 2>$null
    foreach ($line in $headers) {
        if ($line -match '^Content-Range:\s*bytes\s+\d+-\d+/(\d+)') {
            return [int64]$Matches[1]
        }
    }
    throw "Unable to determine total size for $targetUrl"
}

$totalSize = Get-RemoteTotalSize $Url
$existingLen = 0
if (Test-Path $OutPath) {
    $existingLen = (Get-Item $OutPath).Length
    if ($existingLen -gt $totalSize) {
        Remove-Item $OutPath -Force
        $existingLen = 0
    }
}
$fs = [System.IO.File]::Open($OutPath, [System.IO.FileMode]::OpenOrCreate, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
try {
    if ($fs.Length -lt $totalSize) {
        $fs.SetLength($totalSize)
    }
    $offset = $existingLen
    while ($offset -lt $totalSize) {
        $end = [Math]::Min($offset + $ChunkSize - 1, $totalSize - 1)
        $range = "bytes=$offset-$end"
        $tmp = [System.IO.Path]::GetTempFileName()
        $expectedLen = $end - $offset + 1
        $success = $false
        for ($try = 1; $try -le $MaxRetries; $try++) {
            curl.exe -s -L -H "Range: $range" -o $tmp $Url 2>$null
            if (-not (Test-Path $tmp)) {
                Start-Sleep -Milliseconds 300
                continue
            }
            $len = (Get-Item $tmp).Length
            if ($len -ne $expectedLen) {
                Remove-Item $tmp -Force -ErrorAction SilentlyContinue
                Start-Sleep -Milliseconds 300
                continue
            }
            $bytes = [System.IO.File]::ReadAllBytes($tmp)
            $fs.Seek($offset, [System.IO.SeekOrigin]::Begin) | Out-Null
            $fs.Write($bytes, 0, $bytes.Length)
            $success = $true
            Remove-Item $tmp -Force
            break
        }
        if (-not $success) {
            throw "Download failed at offset $offset for $Url (partial file kept at $OutPath; re-run to resume)"
        }
        $offset += $expectedLen
        Start-Sleep -Milliseconds 150
    }
} finally {
    $fs.Close()
}

if ($ExpectedSha256) {
    $hash = (Get-FileHash $OutPath -Algorithm SHA256).Hash.ToLower()
    if ($hash -ne $ExpectedSha256.ToLower()) {
        Remove-Item $OutPath -Force
        throw "SHA256 mismatch for $Url"
    }
}
if ($ExpectedSha1) {
    $hash = (Get-FileHash $OutPath -Algorithm SHA1).Hash.ToLower()
    if ($hash -ne $ExpectedSha1.ToLower()) {
        Remove-Item $OutPath -Force
        throw "SHA1 mismatch for $Url"
    }
}

Write-Host "Downloaded $OutPath ($totalSize bytes)"

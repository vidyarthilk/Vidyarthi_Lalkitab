# Downloads Guava + related JARs into project/local-maven (fixes Tag mismatch SSL errors).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Download-File($url, $out, $minBytes) {
    $dir = Split-Path $out -Parent
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    if ((Test-Path $out) -and (Get-Item $out).Length -ge $minBytes) { return }
    for ($i = 1; $i -le 5; $i++) {
        if (Test-Path $out) { Remove-Item -Force $out }
        Write-Host "  try $i : $(Split-Path $out -Leaf)"
        try {
            Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing
        } catch {
            & curl.exe -fSL --ssl-no-revoke $url -o $out | Out-Null
        }
        if ((Test-Path $out) -and (Get-Item $out).Length -ge $minBytes) {
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Download failed or too small: $out (need >= $minBytes bytes)"
}

function Download-MavenArtifact($groupId, $artifact, $version, $minJarBytes) {
    $groupPath = $groupId.Replace('.', '/')
    $base = "https://repo.maven.apache.org/maven2/$groupPath/$artifact/$version"
    $dir = Join-Path $root "local-maven\$groupPath\$artifact\$version"
    $jar = Join-Path $dir "$artifact-$version.jar"
    $pom = Join-Path $dir "$artifact-$version.pom"
    Write-Host "Downloading ${groupId}:${artifact}:${version}"
    Download-File "$base/$artifact-$version.jar" $jar $minJarBytes
    Download-File "$base/$artifact-$version.pom" $pom 500
    Write-Host "OK ($((Get-Item $jar).Length) bytes)"
}

Remove-Item -Recurse -Force (Join-Path $root "local-maven\com\google") -ErrorAction SilentlyContinue

Download-MavenArtifact "com.google.guava" "guava" "32.0.1-jre" 2500000
Download-MavenArtifact "com.google.guava" "guava" "31.1-jre" 2500000
Download-MavenArtifact "com.google.guava" "guava" "31.1-android" 2000000
Download-MavenArtifact "com.google.guava" "failureaccess" "1.0.1" 4000
Download-MavenArtifact "com.google.guava" "listenablefuture" "9999.0-empty-to-avoid-conflict-with-guava" 2000

Write-Host "Local Maven repo ready: $root\local-maven"

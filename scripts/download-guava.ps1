$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$base = "https://repo.maven.apache.org/maven2/com/google/guava/guava"

function Install-Guava($version) {
    $dir = Join-Path $projectRoot "local-maven-repo\com\google\guava\guava\$version"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $jar = Join-Path $dir "guava-$version.jar"
    Write-Host "Downloading guava-$version..."
    Invoke-WebRequest -Uri "$base/$version/guava-$version.jar" -OutFile $jar -UseBasicParsing
    @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>$version</version>
  <packaging>jar</packaging>
</project>
"@ | Set-Content (Join-Path $dir "guava-$version.pom") -Encoding UTF8
    Copy-Item $jar (Join-Path $projectRoot "app\libs\guava-$version.jar") -Force
    Write-Host "OK: $jar"
}

New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot "app\libs") | Out-Null
Install-Guava "31.1-android"
Install-Guava "31.1-jre"
Write-Host "Done. Rebuild: .\gradlew assembleDebug"

param([string[]]$Tasks = @('assembleDebug', 'testDebugUnitTest'))
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskJdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-17*' | Select-Object -First 1
if ($taskJdk) { $env:JAVA_HOME = $taskJdk.FullName }
if (-not $env:JAVA_HOME) { throw 'Configure JAVA_HOME with JDK 17.' }
Push-Location $taskRoot
try {
    & .\gradlew.bat @Tasks --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }
} finally { Pop-Location }

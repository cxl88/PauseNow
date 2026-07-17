param(
    [string]$FlutterPath = "",
    [string]$AdbPath = ""
)

$ErrorActionPreference = "Stop"

function Resolve-Tool {
    param(
        [string]$Requested,
        [string]$CommandName,
        [string]$Fallback
    )

    if (-not [string]::IsNullOrWhiteSpace($Requested)) { return $Requested }
    $command = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($null -ne $command) { return $command.Source }
    if (Test-Path -LiteralPath $Fallback) { return $Fallback }
    throw "$CommandName was not found. Expected PATH or $Fallback"
}

$flutter = Resolve-Tool $FlutterPath "flutter" "C:\dev\flutter\bin\flutter.bat"
$adb = Resolve-Tool $AdbPath "adb" "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "== Java =="
java -version

Write-Host "`n== Flutter =="
& $flutter --version

Write-Host "`n== Flutter doctor =="
& $flutter doctor -v

Write-Host "`n== ADB devices =="
& $adb devices -l

Write-Host "`n== Project checks =="
& $flutter pub get
& $flutter analyze
& $flutter test

Push-Location "android"
try {
    & ".\gradlew.bat" testDebugUnitTest
    if ($LASTEXITCODE -ne 0) { throw "Kotlin unit tests failed." }
} finally {
    Pop-Location
}

& $flutter build apk --debug

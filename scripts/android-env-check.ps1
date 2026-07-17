param(
    [string]$AdbPath = "",
    [string]$JavaPath = ""
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

$java = Resolve-Tool $JavaPath "java" "$env:JAVA_HOME\bin\java.exe"
$adb = Resolve-Tool $AdbPath "adb" "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

Write-Host "== Java =="
& $java -version

Write-Host "`n== ADB devices =="
& $adb devices -l

Write-Host "`n== Kotlin unit tests =="
Push-Location "android"
try {
    & ".\gradlew.bat" testDebugUnitTest
    if ($LASTEXITCODE -ne 0) { throw "Kotlin unit tests failed." }
} finally {
    Pop-Location
}

Write-Host "`n== Android lint (debug) =="
Push-Location "android"
try {
    & ".\gradlew.bat" lintDebug
    if ($LASTEXITCODE -ne 0) { throw "Android lint failed." }
} finally {
    Pop-Location
}

Write-Host "`n== Debug APK build =="
Push-Location "android"
try {
    & ".\gradlew.bat" assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Debug APK build failed." }
} finally {
    Pop-Location
}

Write-Host "`n== Done. APK: android\app\build\outputs\apk\debug\app-debug.apk =="

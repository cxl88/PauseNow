param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$TargetPackage,

    [ValidateRange(1, 100)]
    [int]$Iterations = 20,

    [string]$DeviceSerial,

    [string]$AdbPath = "adb"
)

$ErrorActionPreference = "Stop"
$appPackage = "com.pausenow.app"
$serviceComponent = "$appPackage/com.pausenow.app.accessibility.PauseAccessibilityService"
$logTag = "PauseNow.ForegroundEvent"

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $serialArguments = if ([string]::IsNullOrWhiteSpace($DeviceSerial)) { @() } else { @("-s", $DeviceSerial) }
    $output = & $AdbPath @serialArguments @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $($Arguments -join ' ')`n$output"
    }
    return $output
}

try {
    $null = Get-Command $AdbPath -ErrorAction Stop
} catch {
    throw "adb not found. Add Android SDK platform-tools to PATH or pass -AdbPath."
}

$deviceState = (Invoke-Adb get-state | Out-String).Trim()
if ($deviceState -ne "device") {
    throw "The selected device is not ready. Current state: $deviceState"
}

$targetPath = Invoke-Adb shell pm path $TargetPackage | Out-String
if (-not $targetPath.Contains("package:")) {
    throw "Target package $TargetPackage is not installed on the selected device."
}

$enabledServices = Invoke-Adb shell settings get secure enabled_accessibility_services | Out-String
if (-not $enabledServices.Contains($serviceComponent)) {
    throw "PauseNow accessibility service is disabled. Read the in-app disclosure and enable it manually."
}


$usageAccess = Invoke-Adb shell appops get $appPackage GET_USAGE_STATS | Out-String
if ($usageAccess -notmatch "allow") {
    throw "PauseNow usage access is disabled. Enable it manually before collecting evidence."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$safeDevice = if ([string]::IsNullOrWhiteSpace($DeviceSerial)) { "default" } else { $DeviceSerial -replace '[^a-zA-Z0-9_.-]', '_' }
$evidenceDir = Join-Path $PSScriptRoot "..\docs\evidence\stage1-$safeDevice-$timestamp"
$resolvedEvidenceDir = [System.IO.Path]::GetFullPath($evidenceDir)
New-Item -ItemType Directory -Force -Path $resolvedEvidenceDir | Out-Null

$deviceInfo = [ordered]@{
    capturedAt = (Get-Date).ToString("o")
    serial = (Invoke-Adb get-serialno | Out-String).Trim()
    manufacturer = (Invoke-Adb shell getprop ro.product.manufacturer | Out-String).Trim()
    model = (Invoke-Adb shell getprop ro.product.model | Out-String).Trim()
    androidVersion = (Invoke-Adb shell getprop ro.build.version.release | Out-String).Trim()
    sdk = (Invoke-Adb shell getprop ro.build.version.sdk | Out-String).Trim()
    buildFingerprint = (Invoke-Adb shell getprop ro.build.fingerprint | Out-String).Trim()
    usageAccess = $usageAccess.Trim()
    targetPackage = $TargetPackage
    iterations = $Iterations
}

Invoke-Adb logcat -c | Out-Null

for ($index = 1; $index -le $Iterations; $index++) {
    Write-Host "[$index/$Iterations] Launching $TargetPackage"
    Invoke-Adb shell monkey -p $TargetPackage -c android.intent.category.LAUNCHER 1 | Out-Null
    Start-Sleep -Milliseconds 1200
    Invoke-Adb shell input keyevent 3 | Out-Null
    Start-Sleep -Milliseconds 900
}

$logs = Invoke-Adb logcat -d -v time -s "$logTag`:I" "*:S" | Out-String
$logPath = Join-Path $resolvedEvidenceDir "foreground-events.log"
[System.IO.File]::WriteAllText($logPath, $logs, [System.Text.UTF8Encoding]::new($false))

$escapedPackage = [regex]::Escape($TargetPackage)
$detections = [regex]::Matches($logs, "packageName=$escapedPackage(?:\s|$)").Count
$successRate = if ($Iterations -eq 0) { 0 } else { [math]::Round(($detections / $Iterations) * 100, 2) }
$passed = $detections -ge [math]::Ceiling($Iterations * 0.95)

$result = [ordered]@{
    passed = $passed
    detections = $detections
    iterations = $Iterations
    successRatePercent = $successRate
    thresholdPercent = 95
    logPath = $logPath
    device = $deviceInfo
}

$resultPath = Join-Path $resolvedEvidenceDir "result.json"
[System.IO.File]::WriteAllText(
    $resultPath,
    ($result | ConvertTo-Json -Depth 5),
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Detections: $detections / $Iterations ($successRate%)"
Write-Host "Evidence: $resolvedEvidenceDir"

if (-not $passed) {
    throw "Detection success rate is below 95%. Stage 1 device verification failed."
}

Write-Host "Stage 1 event detection passed on this device."

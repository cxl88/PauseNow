$ErrorActionPreference = "Stop"

$manifest = Get-Content -Raw -LiteralPath "android/app/src/main/AndroidManifest.xml"
$serviceConfig = Get-Content -Raw -LiteralPath "android/app/src/main/res/xml/pause_accessibility_service.xml"

$checks = [ordered]@{
    accessibilityServiceDeclared = $manifest -match "BIND_ACCESSIBILITY_SERVICE"
    usageAccessDeclared = $manifest -match "PACKAGE_USAGE_STATS"
    noQueryAllPackages = $manifest -notmatch "QUERY_ALL_PACKAGES"
    contentRetrievalDisabled = $serviceConfig -match 'canRetrieveWindowContent="false"'
    windowStateEventOnly =
        $serviceConfig -match 'accessibilityEventTypes="typeWindowStateChanged"' -and
        $serviceConfig -notmatch "typeWindowsChanged"
    declaredAsNonAccessibilityTool = $serviceConfig -match 'isAccessibilityTool="false"'
    noOverlayPermission = $manifest -notmatch "SYSTEM_ALERT_WINDOW"
    noInternetInMainManifest = $manifest -notmatch "android.permission.INTERNET"
}

$checks.GetEnumerator() | ForEach-Object {
    "{0}={1}" -f $_.Key, $_.Value
}

if ($checks.Values -contains $false) {
    exit 1
}

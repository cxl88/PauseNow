package com.pausenow.app.accessibility

class PackageExclusionPolicy(private val ownPackageName: String) {
    private val exactExcludedPackages = setOf(
        ownPackageName,
        "android",
        "com.android.settings",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
    )

    private val excludedPrefixes = listOf(
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.android.inputmethod",
        "com.google.android.inputmethod",
        "com.sohu.inputmethod",
        "com.baidu.input",
    )

    fun shouldExclude(packageName: String): Boolean =
        packageName.isBlank() ||
            packageName in exactExcludedPackages ||
            excludedPrefixes.any(packageName::startsWith) ||
            !PACKAGE_NAME.matches(packageName)

    private companion object {
        val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
    }
}

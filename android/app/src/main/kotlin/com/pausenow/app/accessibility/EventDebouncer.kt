package com.pausenow.app.accessibility

class EventDebouncer {
    private var lastPackageName: String? = null

    @Synchronized
    fun shouldAccept(packageName: String): Boolean {
        if (packageName == lastPackageName) return false
        lastPackageName = packageName
        return true
    }
}

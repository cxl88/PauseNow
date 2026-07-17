package com.pausenow.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.pausenow.app.events.ForegroundEventBus

class PauseAccessibilityService : AccessibilityService() {
    private val debouncer = EventDebouncer()
    private lateinit var exclusionPolicy: PackageExclusionPolicy
    private lateinit var eventStore: ForegroundEventStore

    override fun onServiceConnected() {
        super.onServiceConnected()
        exclusionPolicy = PackageExclusionPolicy(packageName)
        eventStore = ForegroundEventStore(applicationContext)
        Log.i(TAG, "serviceConnected=true contentRetrieval=false")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType !in SUPPORTED_EVENT_TYPES) return

        val sourcePackage = event.packageName?.toString()?.trim().orEmpty()
        if (sourcePackage.isEmpty()) return

        // Observe every package transition, including launchers and system UI. This lets a
        // target app produce a new event after the user leaves it and opens it again, while
        // still ensuring that one foreground session produces only one stored event.
        if (!debouncer.shouldAccept(sourcePackage)) return
        if (exclusionPolicy.shouldExclude(sourcePackage)) return

        val record = ForegroundPackageEventRecord(
            packageName = sourcePackage,
            eventType = AccessibilityEvent.eventTypeToString(event.eventType),
            detectedAtMs = System.currentTimeMillis(),
        )
        eventStore.append(record)
        ForegroundEventBus.publish(record)

        Log.i(
            TAG,
            "detectedAtMs=" + record.detectedAtMs +
                " eventType=" + record.eventType +
                " packageName=" + record.packageName,
        )
    }

    override fun onInterrupt() {
        Log.w(TAG, "serviceInterrupted=true")
    }

    override fun onDestroy() {
        Log.i(TAG, "serviceDestroyed=true")
        super.onDestroy()
    }

    private companion object {
        const val TAG = "PauseNow.ForegroundEvent"
        val SUPPORTED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        )
    }
}

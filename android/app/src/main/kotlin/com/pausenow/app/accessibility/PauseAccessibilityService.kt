package com.pausenow.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.pausenow.app.MainActivity
import com.pausenow.app.R
import com.pausenow.app.events.ForegroundEventBus
import com.pausenow.app.notifications.NotificationChannels

class PauseAccessibilityService : AccessibilityService() {
    private val debouncer = EventDebouncer()
    private lateinit var exclusionPolicy: PackageExclusionPolicy
    private lateinit var eventStore: ForegroundEventStore

    override fun onServiceConnected() {
        super.onServiceConnected()
        exclusionPolicy = PackageExclusionPolicy(packageName)
        eventStore = ForegroundEventStore(applicationContext)
        // 阶段 1 真机验证发现：国产 ROM 会冻结后台无障碍服务的事件下发。
        // 把自身提升为前台服务（常驻通知），让进程保持 FOREGROUND_SERVICE 优先级。
        promoteToForeground()
        Log.i(TAG, "serviceConnected=true contentRetrieval=false foreground=true")
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

    private fun promoteToForeground() {
        NotificationChannels.ensureChannels(applicationContext)
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationChannels.FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(
                NotificationChannels.FOREGROUND_NOTIFICATION_ID,
                notification,
            )
        }
    }

    private fun buildForegroundNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NotificationChannels.FOREGROUND_SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private companion object {
        const val TAG = "PauseNow.ForegroundEvent"
        val SUPPORTED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        )
    }
}
